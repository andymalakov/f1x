package org.f1x.v1;

import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionManager;
import org.f1x.util.ObjectFactory;
import org.f1x.util.ObjectPool;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Thread safe.
 */
public class SessionManagerImpl implements SessionManager {

    public static final String ERROR_CODE_SESSION_ID_ALREADY_USED = "SessionID is already used";
    private static final GFLog LOGGER = GFLogFactory.getLog(SessionManagerImpl.class);

    private final ObjectPool<AcceptorWrapper> acceptorPool;
    private final AcceptorWrapper[] allAcceptors;
    private final ThreadPoolExecutor threadPool;
    private final int logoutTimeout;
    private final Set<SessionID> sessionIDs;
    private final ConcurrentMap<SessionID, FixSessionAcceptor> idToAcceptor;

    /**
     * @param logoutTimeout time in milliseconds
     */
    public SessionManagerImpl(int maxNumOfManagedSessions, int logoutTimeout, final ObjectFactory<? extends FixSessionAcceptor> factory) {
        if(maxNumOfManagedSessions < 1)
            throw new IllegalArgumentException("maxNumOfManagedSessions < 1");

        this.acceptorPool = new ObjectPool<>(maxNumOfManagedSessions, new ObjectFactory<AcceptorWrapper>() {
            @Override
            public AcceptorWrapper create() {
                return new AcceptorWrapper(factory.create());
            }
        });
        this.allAcceptors = acceptorPool.toArray(new AcceptorWrapper[maxNumOfManagedSessions]);
        this.logoutTimeout = logoutTimeout;
        this.threadPool = new ThreadPoolExecutor(maxNumOfManagedSessions, maxNumOfManagedSessions, 0, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(maxNumOfManagedSessions * 2));
        sessionIDs = Collections.newSetFromMap(new ConcurrentHashMap<SessionID, Boolean>(32));
        idToAcceptor = new ConcurrentHashMap<>(maxNumOfManagedSessions);
    }

    @Override
    public boolean accept(Socket socket) {
        AcceptorWrapper acceptor = acceptorPool.borrow();
        if (acceptor == null) {
            return false;
        } else {
            acceptor.setSocket(socket);
            try {
                threadPool.execute(acceptor);
            } catch (RejectedExecutionException e) {
                LOGGER.warn().append("Someone called accept after calling close.").append(e).commit();
                return false;
            }
            return true;
        }
    }

    @Override
    public void add(SessionID sessionID) {
        sessionIDs.add(sessionID.copy());
    }

    @Override
    public void remove(SessionID sessionID) {
        sessionIDs.remove(sessionID);
    }

    @Override
    public String lock(SessionID sessionID, FixSessionAcceptor acceptor) {
        return idToAcceptor.putIfAbsent(sessionID, acceptor) == null ?
                null :
                ERROR_CODE_SESSION_ID_ALREADY_USED;
    }

    @Override
    public void unlock(SessionID sessionID, FixSessionAcceptor acceptor) {
        idToAcceptor.remove(sessionID);
    }

    @Override
    public void close() {     // TODO: does not support multiple invocation
        threadPool.shutdown();

        for (AcceptorWrapper acceptor : allAcceptors)
            acceptor.logout("Bye");

        try {
            if (threadPool.awaitTermination(logoutTimeout, TimeUnit.MILLISECONDS)) {
                LOGGER.warn().append("Logout timeout is over. Closing acceptor socket").commit();
                for (AcceptorWrapper acceptor : allAcceptors)
                    acceptor.close();
            }
        } catch (InterruptedException unexpected) {
            LOGGER.warn().append("Unexpected: ").append(unexpected).commit();
        }
    }

    private final class AcceptorWrapper implements Runnable {

        private final FixSessionAcceptor acceptor;

        private Socket socket;

        private AcceptorWrapper(FixSessionAcceptor acceptor) {
            this.acceptor = acceptor;
        }

        @Override
        public void run() {
            try {
                acceptor.connect(socket, null); // TODO: implement
                acceptor.run();
            } catch (IOException e) {
                LOGGER.warn().append("Error connecting: ").append(e);
            } finally {
                acceptorPool.release(this);
            }
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        void logout(String cause) {
            acceptor.logout(cause);
        }

        void close() {
            acceptor.close();
        }

    }

}
