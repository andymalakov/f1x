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

import org.f1x.api.FixInitiatorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.io.InputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.v1.schedule.SessionTimes;

import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FIX Communicator that initiates outbound FIX connections
 */
public class FixSessionInitiator extends FixSocketCommunicator {

    private final SessionID sessionID;
    private final String host;
    private final int port;

    private final AtomicReference<Thread> initiatorThread = new AtomicReference<>();

    public FixSessionInitiator(String host, int port, FixVersion fixVersion, SessionID sessionID) {
        this(host, port, fixVersion, sessionID, new FixInitiatorSettings());
    }

    public FixSessionInitiator(String host, int port, FixVersion fixVersion, SessionID sessionID, FixInitiatorSettings settings) {
        super(fixVersion, settings);
        this.host = host;
        this.port = port;
        this.sessionID = sessionID;
    }

    @Override
    public FixInitiatorSettings getSettings() {
        return (FixInitiatorSettings) super.getSettings();
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public void run() {
        if ( ! initiatorThread.compareAndSet(null, Thread.currentThread()))
            throw new IllegalStateException("Another thread already using this initiator");

        try {
            init();
            try {
                work();
            } finally {
                destroy();
            }
        } finally {
            // active = true; //TODO: Can we delay it till the moment we need to reuse
            initiatorThread.set(null);
            LOGGER.info().append("Terminating FIX Initiator thread").commit();
        }
    }

    protected void work() {
        boolean needPause = false;
        try {
            while ( ! closeInProgress) {
                try {
                    startSession(needPause);
                    needPause = !processInboundMessages();
                } finally {
                    endSession();
                }
            }
        } catch (Throwable e) {
            if (!(e instanceof InterruptedException))
                LOGGER.warn().append("Error in initiator loop (ignoring)").append(e).commit();
        }
    }

    @Override
    protected void connect(InputChannel in, OutputChannel out) {
        assert Thread.currentThread() == initiatorThread.get();
        super.connect(in, out);
    }

    protected void startSession(boolean needPause) throws InterruptedException {
        boolean newFixSession = waitForSessionStart();
        connect(needPause);
        if (newFixSession) {
            sessionState.resetNextSeqNums();
            messageStore.clean();
        }

        logon(getSettings().isResetSequenceNumbersOnEachLogon());
        scheduleSessionMonitoring(getSettings().getHeartBeatIntervalSec() * 1000);
    }

    private boolean waitForSessionStart() throws InterruptedException {
        boolean newFixSession = false;
        if (schedule != null) {
            long now = timeSource.currentTimeMillis();

            SessionTimes sessionTimes = schedule.getCurrentSessionTimes(now);
            final long sessionStart = sessionTimes.getStart();
            long timeToWaitUntilNextSession = sessionStart - now;
            if (timeToWaitUntilNextSession > 0) {
                LOGGER.info().append("Waiting ").append(timeToWaitUntilNextSession/1000).append(" seconds until next FIX Session").commit();
                timeSource.sleep(sessionStart - now);
                now = sessionStart;
            }

            final long lastConnectionTime = sessionState.getLastConnectionTimestamp();
            if (lastConnectionTime < sessionStart) {
                newFixSession = true;
            }

            final long sessionEnd = sessionTimes.getEnd();
            scheduleSessionEnd(sessionEnd - now);
        }
        return newFixSession;
    }

    protected void endSession() {
        getSessionState().flush();
        unscheduleSessionEnd();
        unscheduleSessionMonitoring();
    }

    /**
     * Connects to FIX counter-party (in several attempts if necessary)
     */
    private void connect(boolean needPause) throws InterruptedException {
        if (needPause) {
            LOGGER.warn().append("FIX: will reconnect after short pause...").commit();
            timeSource.sleep(getSettings().getErrorRecoveryInterval());
        }

        assertSessionStatus(SessionStatus.Disconnected);

        while ( ! closeInProgress && getSessionStatus() == SessionStatus.Disconnected) {
            try {
                LOGGER.info().append("Connecting...").commit();
                connect(new Socket(host, port));
            } catch (ConnectException e) {
                LOGGER.info().append("Server ").append(host).append(':').append(port).append(" is unreachable, will retry later").commit();
                timeSource.sleep(getSettings().getConnectInterval());
            } catch (Throwable e) {
                LOGGER.warn().append("Error connecting to server ").append(host).append(':').append(port).append(", will retry later").append(e).commit();
                timeSource.sleep(getSettings().getConnectInterval());
            }
        }
        assert getSessionStatus() == SessionStatus.SocketConnected; // TODO: may be false when active is set to false from close method
    }

    /**
     * Initiates a LOGON procedure
     * @param newFixSession true at the beginning of new session (will cause sequence number reset)
     */
    private void logon(boolean newFixSession) {
        LOGGER.info().append("Initiating FIX Logon").commit();
        try {
            assertSessionStatus(SessionStatus.SocketConnected);
            sendLogon(newFixSession || getSettings().isResetSequenceNumbersOnEachLogon());
            setSessionStatus(SessionStatus.InitiatedLogon);
        } catch (Throwable e) {
            LOGGER.warn().append("Error sending LOGON request, dropping connection").append(e).commit();
            disconnect("LOGON error");
        }
    }

    @Override
    public void close() {
        super.close();

        Thread initiatorThread = this.initiatorThread.get();
        if (initiatorThread != null)
            initiatorThread.interrupt();
    }

}
