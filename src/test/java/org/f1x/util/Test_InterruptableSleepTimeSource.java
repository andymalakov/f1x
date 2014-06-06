package org.f1x.util;

import org.junit.Assert;
import org.junit.Test;

public class Test_InterruptableSleepTimeSource {

    private InterruptableSleepTimeSource timeSource = new InterruptableSleepTimeSource();

    @Test(timeout = 15000)
    public void testSimple () throws InterruptedException {
        assertApproximateSleepTime(1000);
    }

    @Test(timeout = 15000)
    public void testZeroSleep () throws InterruptedException {
        assertApproximateSleepTime(0);
        assertApproximateSleepTime(-1000);
    }

    @Test(timeout = 15000)
    public void testInterruptedSleep () throws InterruptedException {

        final long uninterruptedSleepTime = 3000;
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(uninterruptedSleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeSource.interrupt();
            }
        }.start();
        assertApproximateSleepTime(60000, uninterruptedSleepTime);
    }

    private void assertApproximateSleepTime(long sleepTime) throws InterruptedException {
        assertApproximateSleepTime (sleepTime, sleepTime);
    }

    private void assertApproximateSleepTime(long initialSleepTime, long uninterruptedSleepTime) {
        long enter = System.currentTimeMillis();
        try {
            timeSource.sleep(initialSleepTime);
        } catch (InterruptedException e) {
            if (initialSleepTime == uninterruptedSleepTime)
                Assert.fail ("Sleep was interrupted unexpectedly");
        }
        long exit = System.currentTimeMillis();
        long actualSleepTime = exit - enter;
        long expectedSleepTime = Math.max(0, uninterruptedSleepTime);
        Assert.assertTrue("Expected sleep time: " + expectedSleepTime + " but actual was " + actualSleepTime, Math.abs(expectedSleepTime - actualSleepTime) <= 1);
    }

}
