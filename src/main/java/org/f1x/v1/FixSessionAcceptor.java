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

import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * FIX Communicator that plays FIX Accepts role for inbound FIX connections
 */
public class FixSessionAcceptor extends FixSocketCommunicator {
    protected static final GFLog LOGGER = GFLogFactory.getLog(FixSessionAcceptor.class);
    private SessionID sessionID;

    public FixSessionAcceptor(FixVersion fixVersion, FixAcceptorSettings settings) {
        super(fixVersion, settings);
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    protected void connect(Socket socket, SessionID sessionID) throws IOException {
        this.sessionID = sessionID;
        connect(socket);
    }

    @Override
    public void disconnect(String cause) {
        super.disconnect(cause);
        sessionID = null;
    }

    @Override
    public final void run() {
        assertSessionStatus(SessionStatus.SocketConnected);
        try {
            processInboundMessages();
        } catch (Throwable e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
        }
        assertSessionStatus(SessionStatus.Disconnected);
    }


    // Mockito does not mock final methods.
    public void run(byte [] logonBuffer, int length) {
        if (logonBuffer == null)
            throw new NullPointerException("logonBuffer == null");
        if (length < 0 || logonBuffer.length < length)
            throw new IllegalArgumentException("length < 0 || logonBuffer.length < length");

        assertSessionStatus(SessionStatus.SocketConnected);
        try {
            processInboundMessages(logonBuffer, length);
        } catch (Throwable e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
        }
        assertSessionStatus(SessionStatus.Disconnected);
    }

    @Override
    public FixAcceptorSettings getSettings() {
        return (FixAcceptorSettings) super.getSettings();
    }

    //TODO: What ensures that LOGON message is the first message we process?

    /**
     * Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current status
     */
    @Override
    protected void processInboundLogon(boolean isSequenceNumberReset) throws IOException {

        if (getSessionStatus() == SessionStatus.SocketConnected) {
            setSessionStatus(SessionStatus.ReceivedLogon);
            sendLogon(isSequenceNumberReset);
            setSessionStatus(SessionStatus.ApplicationConnected);
        } else {
            LOGGER.info().append("Unexpected LOGON (In-session sequence reset?)");
        }
    }
}
