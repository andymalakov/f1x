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

import org.f1x.api.session.FailedLockException;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionManager;

import java.util.*;

/**
 * Thread safe.
 */
public class SimpleSessionManager implements SessionManager {

    protected final Set<SessionID> lockedSessions = new HashSet<>();

    protected volatile Map<SessionID, FixSessionAcceptor> sessions = Collections.emptyMap();
    protected boolean active = true;

    @Override
    public synchronized void addSession(FixSessionAcceptor acceptor) {
        SessionID sessionID = acceptor.getSessionID();
        if (sessions.containsKey(sessionID))
            throw new IllegalArgumentException("Session with such session id already exists: " + sessionID);

        Map<SessionID, FixSessionAcceptor> newSessions = copySessions();
        newSessions.put(sessionID, acceptor);

        this.sessions = newSessions;
    }

    @Override
    public synchronized FixSessionAcceptor removeSession(SessionID sessionID) {
        FixSessionAcceptor acceptor = null;
        if (sessions.containsKey(sessionID)) {
            Map<SessionID, FixSessionAcceptor> newSessions = copySessions();
            acceptor = newSessions.remove(sessionID);
            acceptor.close();
            this.sessions = newSessions;
        }

        return acceptor;
    }

    @Override
    public FixSessionAcceptor getSession(SessionID sessionID) {
        return sessions.get(sessionID);
    }

    @Override
    public synchronized FixSessionAcceptor lockSession(SessionID sessionID) throws FailedLockException {
        checkActive();

        FixSessionAcceptor acceptor = sessions.get(sessionID);
        if (acceptor == null)
            throw FailedLockException.UNREGISTERED_SESSION_ID;

        if (lockedSessions.contains(sessionID))
            throw FailedLockException.SESSION_ID_IS_ALREADY_USED;

        lockedSessions.add(sessionID);

        return acceptor;
    }

    @Override
    public synchronized FixSessionAcceptor unlockSession(SessionID sessionID) {
        lockedSessions.remove(sessionID);
        return getSession(sessionID);
    }

    @Override
    public synchronized void close() {
        if (active) {
            active = false;

            for (FixSessionAcceptor acceptor : sessions.values())
                acceptor.close();

            sessions = Collections.emptyMap();
            lockedSessions.clear();
        }
    }

    protected HashMap<SessionID, FixSessionAcceptor> copySessions() {
        return new HashMap<>(this.sessions);
    }

    protected synchronized void checkActive() throws FailedLockException {
        if(!active)
            throw FailedLockException.CLOSED_SESSION_MANAGER;
    }

}
