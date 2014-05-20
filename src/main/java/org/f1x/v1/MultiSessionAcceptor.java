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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiSessionAcceptor extends ServerSocketSessionAcceptor {

    private static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);

    private final ObjectPool<SessionAcceptorWrapper> acceptorPool;
    private final SessionAcceptorWrapper[] allAcceptors; //TODO: necessary to properly close acceptors
    private final ThreadPoolExecutor threadPool;
    private final int logoutTimeout;
    private final SessionManager manager;

    private boolean closed;

    /**
     * @param logonTimeout  in milliseconds
     * @param logoutTimeout in milliseconds
     */
    public MultiSessionAcceptor(String bindAddress, int bindPort, final int logonTimeout, int logoutTimeout, final ObjectFactory<? extends FixSessionAcceptor> acceptorFactory, final SessionManager manager) {
        super(bindAddress, bindPort);
        if (logonTimeout < 1)
            throw new IllegalArgumentException("logonTimeout < 1");
        if (logoutTimeout < 1)
            throw new IllegalArgumentException("logoutTimeout < 1");
        if (manager == null)
            throw new NullPointerException("manager == null");
        if (acceptorFactory == null)
            throw new NullPointerException("acceptorFactory == null");

        int maxManagedSessions = manager.getMaxManagedSessions();
        if (maxManagedSessions < 1)
            throw new IllegalArgumentException("maxManagedSessions < 1");

        this.acceptorPool = new ObjectPool<>(maxManagedSessions, new ObjectFactory<SessionAcceptorWrapper>() {
            @Override
            public SessionAcceptorWrapper create() {
                return new SessionAcceptorWrapper(acceptorFactory.create(), manager, logonTimeout) {

                    @Override
                    void onStop() {
                        acceptorPool.release(this);
                    }

                };
            }
        });
        this.allAcceptors = acceptorPool.toArray(new SessionAcceptorWrapper[maxManagedSessions]);
        this.logoutTimeout = logoutTimeout;
        this.threadPool = new ThreadPoolExecutor(maxManagedSessions, maxManagedSessions, 0, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(maxManagedSessions * 2));
        this.manager = manager;
    }

    @Override
    protected boolean processInboundConnection(Socket socket) throws IOException {
        SessionAcceptorWrapper acceptor = acceptorPool.borrow();
        if (acceptor == null) {
            LOGGER.warn().append("Session Manager run out of free acceptors.").commit();
            return false;
        } else {
            return accept(socket, acceptor);
        }
    }

    private boolean accept(Socket socket, SessionAcceptorWrapper acceptor) {
        acceptor.setSocket(socket);
        try {
            threadPool.execute(acceptor);
            return false;
        } catch (RejectedExecutionException e) {
            LOGGER.warn().append("Someone called accept after calling close.").append(e).commit();
            acceptor.setSocket(null);
            acceptorPool.release(acceptor);
            return true;
        }
    }

    public SessionManager getSessionManager() {
        return manager;
    }

    public void close() {
        super.close();

        synchronized (this) {
            if (!closed) {
                threadPool.shutdown();

                for (SessionAcceptorWrapper acceptor : allAcceptors)
                    acceptor.logout("Server shutdown");

                try {
                    if (threadPool.awaitTermination(logoutTimeout, TimeUnit.MILLISECONDS)) {
                        LOGGER.warn().append("Logout timeout is over. Force closing acceptors").commit();
                        for (SessionAcceptorWrapper acceptor : allAcceptors)
                            acceptor.close();
                    }
                } catch (InterruptedException unexpected) {
                    LOGGER.warn().append("Unexpected: ").append(unexpected).commit();
                }

                closed = true;
            }
        }
    }

}
