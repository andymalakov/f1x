package org.f1x.api.session;

import org.f1x.v1.FixSessionAcceptor;

import java.net.Socket;

/**
 * Used by Session Acceptor. Manages inbound session.
 */
public interface SessionManager {

    /**
     * Accepts a socket.
     *
     * @param socket socket
     * @return true if this socket was accepted otherwise false
     */
    boolean accept(Socket socket);

    /**
     * Adds session id to list of allowed session ids.
     *
     * @param sessionID session id
     */
    void add(SessionID sessionID);

    /**
     * Removes session id from list of allowed session ids.
     *
     * @param sessionID
     */
    void remove(SessionID sessionID);

    /**
     * Locks given sessionID.
     *
     * @return errorCode if session id was not locked otherwise null
     */
    String lock(SessionID sessionID, FixSessionAcceptor acceptor);

    /**
     * Unlocks given sessionID.
     */
    void unlock(SessionID sessionID, FixSessionAcceptor acceptor);


    /**
     * Closes session manager.
     */
    void close();

}
