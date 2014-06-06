package org.f1x.util;

/** Realtime Time Source that can be interrupted during sleep */
public class InterruptableSleepTimeSource implements TimeSource {

    private volatile boolean isInterrupted;

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void sleep(long delay) throws InterruptedException {
        final long wakeUpTime = System.currentTimeMillis() + delay;
        long remainingSleepTime = delay;

        isInterrupted = false;

        while (remainingSleepTime > 0) {

            synchronized (this) {
                this.wait(remainingSleepTime);
            }
            if (isInterrupted)
                throw new InterruptedException();

            remainingSleepTime = wakeUpTime - System.currentTimeMillis();
        }
    }

    public void interrupt () {
        synchronized (this) {
            isInterrupted = true;
            this.notify();
        }
    }
}
