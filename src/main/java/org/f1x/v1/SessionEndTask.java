package org.f1x.v1;

import org.f1x.api.session.FixSession;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.util.TimerTask;

final class SessionEndTask extends TimerTask {

    private final FixSession session;

    public SessionEndTask(FixSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        try {
            FixCommunicator.LOGGER.info().append("Scheduled end time for FIX session ").append(session.getSessionID()).commit();
            session.logout("Scheduled end time");
        } catch (Throwable e) {
            FixCommunicator.LOGGER.warn().append("Error occurred during ending session").commit();
        }
    }
}
