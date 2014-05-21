package org.f1x.v1.state;

import org.f1x.v1.InvalidFixMessageException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread safe
 */
public class MemorySessionState extends AbstractSessionState {

    private final AtomicInteger nextSenderSeqNum = new AtomicInteger(1);
    private final AtomicInteger nextTargetSeqNum = new AtomicInteger(1);

    private volatile long lastLogonTimestamp = -1;

    @Override
    public void setLastLogonTimestamp(long newValue) {
        this.lastLogonTimestamp = newValue;
    }

    @Override
    public long getLastLogonTimestamp() {
        return lastLogonTimestamp;
    }

    @Override
    public void setNextSenderSeqNum(int newValue) {
        this.nextSenderSeqNum.set(newValue);
    }

    @Override
    public int getNextSenderSeqNum() {
        return nextSenderSeqNum.get();
    }

    @Override
    public int consumeNextSenderSeqNum() {
        return nextSenderSeqNum.getAndIncrement();
    }

    @Override
    public void setNextTargetSeqNum(int newValue) {
        this.nextTargetSeqNum.set(newValue);
    }

    @Override
    public int getNextTargetSeqNum() {
        return nextTargetSeqNum.get();
    }

    @Override
    public int consumeNextTargetSeqNum() {
        return nextTargetSeqNum.getAndIncrement();
    }

    @Override
    public void resetNextTargetSeqNum(int newValue) throws InvalidFixMessageException {
        while (true) {
            int currentValue = nextTargetSeqNum.get();
            if (newValue <= currentValue)
                throw InvalidFixMessageException.RESET_BELOW_CURRENT_SEQ_LARGE;

            if (nextTargetSeqNum.compareAndSet(currentValue, newValue))
                break;
        }
    }

}
