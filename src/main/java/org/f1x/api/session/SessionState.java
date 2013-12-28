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

public enum SessionState {
    /** No network connection */
    Disconnected,

    /** Network connection is up, but FIX connection is down */
    SocketConnected,

    /** [Initiator only] Socket connection has been established, LOGON has been sent to counter-party, but no Logon response has been received yet. */
    InitiatedLogon,

    /** [Acceptor only] Socket connection has been established, LOGON has been received from counter-party, but no Logon response has been sent yet. */
    ReceivedLogon,

    /** This side initiated Logout, but the other side has not yet acknowledged it yet */
    InitiatedLogout,

    /** Socket connection is up and Logon messages exchanged, both sides can exchange application messages */
    ApplicationConnected

}
