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
import org.f1x.api.session.SessionState;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread safe.
 */
public class SimpleSessionManager implements SessionManager {

    protected final int maxManagedSessions;
    protected final ConcurrentHashMap<SessionID, SessionState> idToState;
    protected final ConcurrentHashMap<SessionID, FixSessionAcceptor> idToAcceptor;

    public SimpleSessionManager(int maxManagedSessions) {
        if (maxManagedSessions < 1)
            throw new IllegalArgumentException("maxManagedSessions < 1");

        this.maxManagedSessions = maxManagedSessions;
        this.idToState = new ConcurrentHashMap<>(maxManagedSessions);
        this.idToAcceptor = new ConcurrentHashMap<>(maxManagedSessions);
    }

    @Override
    public void add(SessionID sessionID, SessionState state) {
        idToState.put(sessionID, state);
    }

    @Override
    public void remove(SessionID sessionID) {
        idToState.remove(sessionID);
    }

    @Override
    public SessionState lock(SessionID sessionID, FixSessionAcceptor acceptor) throws FailedLockException {
        SessionState state = idToState.get(sessionID);
        if (state == null)
            throw FailedLockException.UNREGISTERED_SESSION_ID;

        FixSessionAcceptor previousSessionAcceptor = idToAcceptor.putIfAbsent(sessionID, acceptor);
        if (previousSessionAcceptor != null)
            throw FailedLockException.SESSION_ID_IS_ALREADY_USED;

        return state;
    }

    @Override
    public void unlock(SessionID sessionID) {
        idToAcceptor.remove(sessionID);
    }

    @Override
    public FixSessionAcceptor getSessionAcceptor(SessionID sessionID) {
        return idToAcceptor.get(sessionID);
    }

    @Override
    public int getMaxManagedSessions() {
        return maxManagedSessions;
    }

}
