package org.f1x.v1.state;

import org.f1x.v1.InvalidFixMessageException;

/**
 * Not thread safe. Only for tests.
 */
public class TestSessionState extends AbstractSessionState {

    private long lastLogonTimestamp = -1;
    private int nextSenderSeqNum = 1;
    private int nextTargetSeqNum = 1;

    @Override
    public void setLastConnectionTimestamp(long newValue) {
        lastLogonTimestamp = newValue;
    }

    @Override
    public long getLastConnectionTimestamp() {
        return lastLogonTimestamp;
    }

    @Override
    public void setNextSenderSeqNum(int newValue) {
        nextSenderSeqNum = newValue;
    }

    @Override
    public int getNextSenderSeqNum() {
        return nextSenderSeqNum;
    }

    @Override
    public int consumeNextSenderSeqNum() {
        return nextSenderSeqNum++;
    }

    @Override
    public void setNextTargetSeqNum(int newValue) {
        nextTargetSeqNum = newValue;
    }

    @Override
    public int getNextTargetSeqNum() {
        return nextTargetSeqNum;
    }

    @Override
    public int consumeNextTargetSeqNum() {
        return nextTargetSeqNum++;
    }

    @Override
    public void resetNextTargetSeqNum(int newValue) throws InvalidFixMessageException {
        if (newValue <= nextTargetSeqNum)
            throw InvalidFixMessageException.RESET_BELOW_CURRENT_SEQ_LARGE;

        nextTargetSeqNum = newValue;
    }

}
