package org.f1x.v1.state;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread safe
 */
public class MemorySessionState extends AbstractSessionState {

    private final AtomicInteger nextSenderSeqNum = new AtomicInteger(1);
    private final AtomicInteger nextTargetSeqNum = new AtomicInteger(1);

    private volatile long lastLogonTimestamp = -1;

    @Override
    public void setLastConnectionTimestamp(long newValue) {
        this.lastLogonTimestamp = newValue;
    }

    @Override
    public long getLastConnectionTimestamp() {
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

}
