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

import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiSessionAcceptor extends AbstractSessionAcceptor {

    private static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);

    private final ObjectPool<SessionAcceptorWrapper> acceptorPool;
    private final ExecutorService executor;
    private final SessionManager manager;

    public MultiSessionAcceptor(String host, int port, int logonBufferSize, int logonTimeout, int maxActiveSessions, SessionManager manager) {
        this(host, port, logonBufferSize, logonTimeout, maxActiveSessions, manager, createThreadPool(maxActiveSessions));
    }

    /**
     * @param logonTimeout in milliseconds
     */
    public MultiSessionAcceptor(String host, int port, int logonBufferSize, int logonTimeout, int maxActiveSessions, SessionManager manager, ExecutorService executor) {
        super(host, port, maxActiveSessions);
        check(logonTimeout, manager, executor);
        this.acceptorPool = createAcceptorPool(logonBufferSize, logonTimeout, maxActiveSessions, manager);
        this.executor = executor;
        this.manager = manager;
    }

    public SessionManager getSessionManager() {
        return manager;
    }

    @Override
    protected void processConnection(Socket socket) {
        SessionAcceptorWrapper acceptor = acceptorPool.borrow();
        if (acceptor == null) {
            LOGGER.warn().append("Multi Session Acceptor ran out of free acceptors.").commit();
            closeSocket(socket);
        } else {
            acceptor.setSocket(socket);
            executor.execute(acceptor);
        }
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        manager.close();
        executor.shutdown();
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

    protected static void check(int logonTimeout, SessionManager manager, ExecutorService executor) {
        if (logonTimeout < 1)
            throw new IllegalArgumentException("logonTimeout < 1");

        Objects.requireNonNull(manager, "manager == null");
        Objects.requireNonNull(executor, "executor == null");
    }

    protected static ThreadPoolExecutor createThreadPool(int maxActiveSessions) {
        return new ThreadPoolExecutor(0, maxActiveSessions, 0L, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(maxActiveSessions * 2));
    }

}
