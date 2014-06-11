package org.f1x.v1.state;

import org.f1x.api.session.SessionState;
import org.f1x.api.session.SessionStatus;

public abstract class AbstractSessionState implements SessionState {

    private volatile long lastReceivedMessageTimestamp = -1;
    private volatile long lastSentMessageTimestamp = -1;

    @Override
    public void resetNextSeqNums() {
        setNextSenderSeqNum(1);
        setNextTargetSeqNum(1);
    }

    @Override
    public long getLastSentMessageTimestamp() {
        return lastSentMessageTimestamp;
    }

    @Override
    public void setLastSentMessageTimestamp(long newValue) {
        lastSentMessageTimestamp = newValue;
    }

    @Override
    public long getLastReceivedMessageTimestamp() {
        return lastReceivedMessageTimestamp;
    }

    @Override
    public void setLastReceivedMessageTimestamp(long newValue) {
        lastReceivedMessageTimestamp = newValue;
    }

}
