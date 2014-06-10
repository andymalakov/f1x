package org.f1x.v1;

import org.f1x.api.session.FixSession;

import java.util.TimerTask;

final class SessionEndTask extends TimerTask {

    private final FixSession session;

    public SessionEndTask(FixSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        try {
            session.logout("Ending Session");
        } catch (Throwable e) {
            FixCommunicator.LOGGER.warn().append("Error occurred during ending session").commit();
        }
    }
}
