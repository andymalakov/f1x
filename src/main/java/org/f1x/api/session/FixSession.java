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

import org.f1x.api.FixSettings;
import org.f1x.api.message.MessageBuilder;
import org.f1x.io.socket.ConnectionInterceptor;

import java.io.IOException;

public interface FixSession extends Runnable {
    void setEventListener(SessionEventListener eventListener);
    void setConnectionInterceptor(ConnectionInterceptor connectionInterceptor);

    SessionID getSessionID();
    FixSettings getSettings();
    SessionState getSessionState();

    MessageBuilder createMessageBuilder();
    void send (MessageBuilder mb) throws IOException;

    /** Send LOGOUT but do not drop socket connection */
    void logout(String cause);

    /** Terminate socket connection (no logout message is sent if session is in process) */
    void disconnect(String cause);

    /** Logout current session (if needed) and terminate socket connection. */
    void close();
}
