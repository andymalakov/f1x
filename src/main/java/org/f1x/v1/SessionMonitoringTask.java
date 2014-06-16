package org.f1x.v1;

import org.f1x.api.session.SessionStatus;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.util.TimerTask;

// TODO: add disconnect logic
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
        SessionStatus currentStatus = communicator.getSessionStatus();
        if (currentStatus == SessionStatus.ApplicationConnected) {
            long currentTime = communicator.timeSource.currentTimeMillis();
            checkInbound(currentTime);
            checkOutbound(currentTime);
        }
    }

    private void checkInbound(long currentTime) {
        long lastReceivedMessageTimestamp = communicator.getSessionState().getLastReceivedMessageTimestamp();
        if (lastReceivedMessageTimestamp < currentTime - heartbeatInterval) {
            LOGGER.debug().append("Inactivity of opposite side reached the limit. Send Test Request (1) message.").commit();
            try {
                communicator.sendTestRequest("Are you there?"); // TODO: testReqId Generator?
            } catch (IOException e) {
                LOGGER.warn().append("Error sending Test Request (1)").append(e).commit();
            }
        }
    }

    private void checkOutbound(long currentTime) {
        long lastSentMessageTimestamp = communicator.getSessionState().getLastSentMessageTimestamp();
        if (lastSentMessageTimestamp < currentTime - heartbeatInterval) {
            LOGGER.debug().append("Inactivity of our side reached the limit. Send Heartbeat (0) message.").commit();
            try {
                communicator.sendHeartbeat(null);
            } catch (IOException e) {
                LOGGER.warn().append("Error sending Heartbeat(0)").append(e).commit();
            }
        }
    }

}
