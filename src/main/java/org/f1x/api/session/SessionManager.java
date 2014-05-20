package org.f1x.api.session;

import org.f1x.v1.FixSessionAcceptor;

import java.net.Socket;

/**
 * Manages inbound session. Must be thread safe.
 */
public interface SessionManager {

    /**
     * Adds session id and session state.
     *
     * @param sessionID session id
     */
    void add(SessionID sessionID, SessionState state);

    /**
     * Removes session id and session state.
     *
     * @param sessionID
     */
    void remove(SessionID sessionID);

    /**
     * Locks session id.
     * @param sessionID
     * @return session state if it is found by session id and it is not used by another session acceptor
     * @throws FailedLockException if unable to lock
     */
    SessionState lock(SessionID sessionID, FixSessionAcceptor acceptor) throws FailedLockException;

    /**
     * Unlocks session id.
     * @param sessionID
     */
    void unlock(SessionID sessionID);

    /**
     * Gets session acceptor by session id.
     * @param sessionID
     * @return session acceptor if it is found by session id otherwise null
     */
    FixSessionAcceptor getSessionAcceptor(SessionID sessionID);

    int getMaxManagedSessions();

}
