package org.f1x.v1.state;

import org.f1x.api.session.SessionState;
import org.f1x.api.session.SessionStatus;

public abstract class AbstractSessionState implements SessionState {

    @Override
    public void resetNextSeqNums() {
        setNextSenderSeqNum(1);
        setNextTargetSeqNum(1);
    }

}
