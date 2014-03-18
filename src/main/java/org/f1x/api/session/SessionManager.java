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
