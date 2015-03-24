package org.f1x.v1;

import org.f1x.api.session.SessionStatus;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.util.TimerTask;

/** Timer responsible for sending HEARTBEATs from our side, and TEST request of we do not receive HEARTBEATs from other side */
final class SessionMonitoringTask extends TimerTask {

    private static final GFLog LOGGER = GFLogFactory.getLog(SessionMonitoringTask.class);

    private final FixCommunicator communicator;
    private final int heartbeatInterval;

    public SessionMonitoringTask(FixCommunicator communicator) {
        this.communicator = communicator;
        heartbeatInterval = communicator.getSettings().getHeartBeatIntervalSec() * 1000;
    }

    @Override
    public void run() {
        final long currentTime = communicator.timeSource.currentTimeMillis();
        if (communicator.getSessionStatus() == SessionStatus.ApplicationConnected)
            checkInbound(currentTime);

        if (communicator.getSessionStatus() == SessionStatus.ApplicationConnected)
            checkOutbound(currentTime);
    }

    /** Check when we received last message from other side (send TEST if that happen long time ago) */
    private void checkInbound(long currentTime) {
        long lastReceivedMessageTimestamp = communicator.getSessionState().getLastReceivedMessageTimestamp();
        if (lastReceivedMessageTimestamp < currentTime - heartbeatInterval) {
            LOGGER.debug().append("Haven't heard from the other side for a while. Sending TEST(1) message to validate connection.").commit();
            try {
                //TODO: Other than sending Test request we need to add a logic that will force socket disconnect if we don't hear back
                communicator.sendTestRequest("Are you there?");
            } catch (Throwable e) {
                LOGGER.warn().append("Error sending TEST(1):").append(e).commit();
            }
        }
    }

    /** Check when we sent last message to other side (send HEARTBEAT if that happened long time ago) */
    private void checkOutbound(long currentTime) {
        long lastSentMessageTimestamp = communicator.getSessionState().getLastSentMessageTimestamp();
        if (lastSentMessageTimestamp < currentTime - heartbeatInterval) {
            LOGGER.debug().append("Connection is idle. Sending HEARTBEAT(0) to confirm connection.").commit();
            try {
                communicator.sendHeartbeat(null);
            } catch (Throwable e) {
                LOGGER.warn().append("Error sending HEARTBEAT(0):").append(e).commit();
            }
        }
    }

}
