/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f1x.v1;

import org.f1x.api.session.SessionManager;
import org.f1x.util.ObjectFactory;
import org.f1x.util.ObjectPool;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.*;

public class MultiSessionAcceptor extends ServerSocketSessionAcceptor {

    private static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);

    private final ObjectPool<SessionAcceptorWrapper> acceptorPool;
    private final ExecutorService executor;
    private final SessionManager manager;

    private boolean closed;

    public MultiSessionAcceptor(String bindAddress, int bindPort, int logonBufferSize, int logonTimeout, int maxActiveSessions, SessionManager manager) {
        this(bindAddress, bindPort, logonBufferSize, logonTimeout, maxActiveSessions, manager, new ThreadPoolExecutor(maxActiveSessions, maxActiveSessions, 0, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(maxActiveSessions * 2)));
    }

    /**
     * @param logonTimeout in milliseconds
     */
    public MultiSessionAcceptor(String bindAddress, int bindPort, int logonBufferSize, int logonTimeout, int maxActiveSessions, SessionManager manager, ExecutorService executor) {
        super(bindAddress, bindPort);
        check(logonTimeout, manager, executor);

        this.acceptorPool = createAcceptorPool(logonBufferSize, logonTimeout, maxActiveSessions, manager);
        this.executor = executor;
        this.manager = manager;
    }

    public SessionManager getSessionManager() {
        return manager;
    }

    public void close() {
        super.close();

        synchronized (this) {
            if (!closed) {
                executor.shutdown();
                manager.close();
                closed = true;
            }
        }
    }

    @Override
    protected boolean processInboundConnection(Socket socket) throws IOException {
        SessionAcceptorWrapper acceptor = acceptorPool.borrow();
        if (acceptor == null) {
            LOGGER.warn().append("Multi Session Acceptor ran out of free acceptors.").commit();
            return false;
        } else {
            return accept(socket, acceptor);
        }
    }

    protected boolean accept(Socket socket, SessionAcceptorWrapper acceptor) {
        acceptor.setSocket(socket);
        try {
            executor.execute(acceptor);
            return false;
        } catch (RejectedExecutionException e) {
            LOGGER.warn().append("Someone called accept after calling close.").append(e).commit();
            acceptor.setSocket(null);
            acceptorPool.release(acceptor);
            return true;
        }
    }

    protected ObjectPool<SessionAcceptorWrapper> createAcceptorPool(final int logonBufferSize, final int logonTimeout, int maxActiveSessions, final SessionManager manager) {
        return new ObjectPool<>(maxActiveSessions, new ObjectFactory<SessionAcceptorWrapper>() {
            @Override
            public SessionAcceptorWrapper create() {
                return new SessionAcceptorWrapper(logonBufferSize, logonTimeout, manager) {

                    @Override
                    protected void onStop() {
                        acceptorPool.release(this);
                    }

                };
            }
        });
    }

    protected void check(int logonTimeout, SessionManager manager, ExecutorService executor) {
        if (logonTimeout < 1)
            throw new IllegalArgumentException("logonTimeout < 1");

        Objects.requireNonNull(manager, "manager == null");
        Objects.requireNonNull(executor, "executor == null");
    }

}
