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
import org.f1x.v1.schedule.SessionTimes;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FIX Communicator that plays FIX Accepts role for inbound FIX connections
 */
public class FixSessionAcceptor extends FixSocketCommunicator {
    protected static final GFLog LOGGER = GFLogFactory.getLog(FixSessionAcceptor.class);
    private SessionID sessionID;
    private final AtomicBoolean running = new AtomicBoolean();

    public FixSessionAcceptor(FixVersion fixVersion, FixAcceptorSettings settings) {
        super(fixVersion, settings);
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    public void connect(Socket socket, SessionID sessionID) throws IOException {
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
        run0(null, 0);
    }

    // Mockito does not mock final methods.
    public void run(byte[] logonBuffer, int length) {
        checkLogonBuffer(logonBuffer, length);
        run0(logonBuffer, length);
    }

    private void run0(byte[] logonBuffer, int length) {
        if (!running.compareAndSet(false, true))
            throw new IllegalStateException("Already running");

        try {
            init();
            try {
                work(logonBuffer, length);
            } finally {
                destroy();
            }
        } finally {
            //TODO: Find a better way to recycle state
            // closeInProgress = false;
            running.set(false);
        }
    }

    protected void work(byte[] logonBuffer, int length) {
        assertSessionStatus(SessionStatus.SocketConnected);

        boolean started = startSession();
        if (!started) {
            disconnect("Session is down");
            return;
        }

        try {
            processInboundMessages(logonBuffer, length);
        } catch (Throwable e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error: ").append(e).commit();
        } finally {
            endSession();
        }

        assertSessionStatus(SessionStatus.Disconnected);
    }

    @Override
    public FixAcceptorSettings getSettings() {
        return (FixAcceptorSettings) super.getSettings();
    }

    protected boolean startSession() {
        if (schedule != null) {
            final long now = timeSource.currentTimeMillis();

            SessionTimes sessionTimes = schedule.getCurrentSessionTimes(now);
            final long sessionStart = sessionTimes.getStart();
            if (now < sessionStart)
                return false;

            final long lastConnectionTime = sessionState.getLastConnectionTimestamp();
            if (lastConnectionTime < sessionStart)
                sessionState.resetNextSeqNums();

            long sessionEnd = sessionTimes.getEnd();
            scheduleSessionEnd(sessionEnd - now);
        }

        //TODO: Do this when we transition to ApplicationConnected
        scheduleSessionMonitoring(getSettings().getHeartBeatIntervalSec() * 1000);

        return true;
    }

    protected void endSession() {
        unscheduleSessionEnd();

        //TODO: Do this when we transition from ApplicationConnected
        unscheduleSessionMonitoring();
    }

    private static void checkLogonBuffer(byte[] logonBuffer, int length) {
        if (logonBuffer == null)
            throw new NullPointerException("logonBuffer == null");
        if (length < 0 || logonBuffer.length < length)
            throw new IllegalArgumentException("length < 0 || logonBuffer.length < length");
    }

}
