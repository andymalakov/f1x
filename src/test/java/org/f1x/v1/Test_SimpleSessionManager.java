package org.f1x.v1;

import org.f1x.SessionIDBean;
import org.f1x.api.session.FailedLockException;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionManager;
import org.f1x.api.session.SessionState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class Test_SimpleSessionManager {

    private SessionManager manager;

    @Before
    public void init() {
        manager = new SimpleSessionManager();
    }

    @Test
    public void testSuccessfulLocking() {
        assertSuccessfulLock(new SessionIDBean("s", "t"));
        assertSuccessfulLock(new SessionIDBean("ss", "tt"));
        assertSuccessfulLock(new SessionIDBean("sss", "ttt"));
    }

    @Test
    public void testUnsuccessfulLocking() {
        SessionID sessionID = new SessionIDBean("s", "t");
        assertFailedLock(sessionID, "Unregistered session id");

        assertSuccessfulLock(sessionID);
        assertFailedLock(sessionID, "Session id is already used");

        simulateUnlock(sessionID);
        manager.removeSession(sessionID);
        assertSuccessfulLock(sessionID);
    }

    private void simulateUnlock(SessionID sessionID) {
        manager.unlockSession(sessionID);
    }

    private void assertSuccessfulLock(SessionID sessionID) {
        FixSessionAcceptor expectedAcceptor = createTestSessionAcceptor(sessionID);
        manager.addSession(expectedAcceptor);

        FixSessionAcceptor actualAcceptor = null;
        try {
            actualAcceptor = manager.lockSession(sessionID);
        } catch (FailedLockException e) {
            Assert.fail("Unexpected: " + e);
        }
        Assert.assertEquals(expectedAcceptor, actualAcceptor);
        Assert.assertEquals(expectedAcceptor, manager.getSession(sessionID));
    }

    private void assertFailedLock(SessionID sessionID, String expected) {
        try {
            manager.lockSession(sessionID);
        } catch (FailedLockException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private static SessionState createTestSessionState() {
        return Mockito.mock(SessionState.class);
    }

    private static FixSessionAcceptor createTestSessionAcceptor(SessionID sessionID) {
        FixSessionAcceptor acceptor = Mockito.mock(FixSessionAcceptor.class);
        Mockito.when(acceptor.getSessionID()).thenReturn(sessionID);
        return acceptor;
    }

}
