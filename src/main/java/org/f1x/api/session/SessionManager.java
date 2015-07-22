package org.f1x.api.session;

import org.f1x.v1.FixSessionAcceptor;

public interface SessionManager {

    void addSession(FixSessionAcceptor acceptor);

    FixSessionAcceptor removeSession(SessionID sessionID);

    FixSessionAcceptor getSession(SessionID sessionID);

    FixSessionAcceptor lockSession(SessionID sessionID) throws FailedLockException;

    FixSessionAcceptor unlockSession(SessionID sessionID);

    void close();

}
