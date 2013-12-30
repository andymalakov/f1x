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

import org.f1x.api.FixVersion;
import org.f1x.api.SessionID;
import org.f1x.api.message.MessageParser;
import org.f1x.api.session.SessionState;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

/**
 * FIX Communicator that initiates outbound FIX connections
 */
public class FixSessionInitiator extends FixSocketCommunicator {
    private final SessionID sessionID;
    private final String host;
    private final int port;

    public FixSessionInitiator(String host, int port, FixVersion fixVersion, SessionID sessionID, FixInitiatorSettings settings) {
        super(fixVersion, settings);
        this.host = host;
        this.port = port;
        this.sessionID = sessionID;
    }

    @Override
    protected FixInitiatorSettings getSettings() {
        return (FixInitiatorSettings) super.getSettings();
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public void run() {
        boolean needPause = false;
        try {
            while (active) {
                connect(needPause);
                logon();
                processInboundMessages();

                needPause = true;
            }
        } catch (Throwable e) {
            if ( ! (e instanceof InterruptedException))
                LOGGER.warn().append("Error in initiator loop (ignoring)").append(e).commit();
        }
        LOGGER.info().append("Terminating FIX Initiator thread").commit();
    }


    /** Connects to FIX counter-party (in several attempts if necessary) */
    private void connect (boolean needPause) throws InterruptedException {
        if (needPause) {
            LOGGER.warn().append("FIX: will reconnect after short pause...").commit();
            Thread.sleep(getSettings().getErrorRecoveryInterval());
        }

        assertSessionState (SessionState.Disconnected);

        while (active && getSessionState() == SessionState.Disconnected) {
            try {
                LOGGER.info().append("Connecting...").commit();
                connect(new Socket (host, port));

            } catch (ConnectException e) {
                LOGGER.info().append("Server ").append(host).append(':').append(port).append(" is unreachable, will retry later").commit();
                Thread.sleep(getSettings().getConnectInterval());
            } catch (Throwable e) {
                LOGGER.warn().append("Error connecting to server ").append(host).append(':').append(port).append(", will retry later").append(e).commit();
                Thread.sleep(getSettings().getConnectInterval());
            }
        }
        assert getSessionState() == SessionState.SocketConnected;
    }

    /** Initiates a LOGON procedure */
    private void logon () {
        LOGGER.info().append("Initiating FIX Logon").commit();
        try {
            assertSessionState (SessionState.SocketConnected);
            sendLogon(getSettings().isResetSequenceNumbers());
            setSessionState(SessionState.InitiatedLogon);
        } catch (Throwable e) {
            LOGGER.warn().append("Error sending LOGON request, dropping connection").append(e).commit();
            disconnect("LOGON error");
        }
    }

    /** Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current state */
    @Override
    protected void processInboundLogon(MessageParser parser) throws IOException {
        if (getSessionState() == SessionState.InitiatedLogon) {
            setSessionState(SessionState.ApplicationConnected);
            LOGGER.info().append("FIX Session established").commit();
        } else {
            LOGGER.info().append("Unexpected LOGON (In-session sequence reset?)").commit();
        }
    }


}
