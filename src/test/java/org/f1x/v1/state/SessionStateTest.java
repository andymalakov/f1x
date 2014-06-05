package org.f1x.v1.state;

import org.f1x.api.session.SessionState;
import org.f1x.v1.InvalidFixMessageException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public abstract class SessionStateTest {

    protected SessionState sessionState;

    @Before
    public void init() throws Exception {
        sessionState = createSessionState();
    }

    protected abstract SessionState createSessionState() throws Exception;

    @Test
    public void testLastLogonTimestamp() {
        Assert.assertEquals(-1, sessionState.getLastConnectionTimestamp());

        for (int lastLogonTimestamp = 1; lastLogonTimestamp < 2000; lastLogonTimestamp++) {
            sessionState.setLastConnectionTimestamp(lastLogonTimestamp);
            Assert.assertEquals(lastLogonTimestamp, sessionState.getLastConnectionTimestamp());
        }
    }

    @Test
    public void testNextSenderSeqNum() {
        Assert.assertEquals(1, sessionState.getNextSenderSeqNum());

        for (int nextSenderSeqNum = 1; nextSenderSeqNum < 2000; nextSenderSeqNum++) {
            Assert.assertEquals(nextSenderSeqNum, sessionState.getNextSenderSeqNum());
            Assert.assertEquals(nextSenderSeqNum, sessionState.consumeNextSenderSeqNum());
        }

        for (int nextSenderSeqNum = 1; nextSenderSeqNum < 2000; nextSenderSeqNum++) {
            sessionState.setNextSenderSeqNum(nextSenderSeqNum);
            Assert.assertEquals(nextSenderSeqNum, sessionState.getNextSenderSeqNum());
        }
    }

    @Test
    public void testNextTargetSeqNum() throws InvalidFixMessageException {
        Assert.assertEquals(1, sessionState.getNextTargetSeqNum());

        for (int nextTargetSeqNum = 1; nextTargetSeqNum < 2000; nextTargetSeqNum++) {
            Assert.assertEquals(nextTargetSeqNum, sessionState.getNextTargetSeqNum());
            Assert.assertEquals(nextTargetSeqNum, sessionState.consumeNextTargetSeqNum());
        }

        for (int nextTargetSeqNum = 1; nextTargetSeqNum < 2000; nextTargetSeqNum++) {
            sessionState.setNextTargetSeqNum(nextTargetSeqNum);
            Assert.assertEquals(nextTargetSeqNum, sessionState.getNextTargetSeqNum());
        }

        for (int nextTargetSeqNum = 2; nextTargetSeqNum < 2000; nextTargetSeqNum++) {
            sessionState.setNextTargetSeqNum(nextTargetSeqNum - 1);
            sessionState.resetNextTargetSeqNum(nextTargetSeqNum);
            Assert.assertEquals(nextTargetSeqNum, sessionState.getNextTargetSeqNum());

            try {
                sessionState.resetNextTargetSeqNum(nextTargetSeqNum);
                Assert.fail();
            } catch (InvalidFixMessageException e) {
                // skip
            }
        }
    }

    @Test
    public void testAllInOne() throws InvalidFixMessageException {
        testLastLogonTimestamp();
        testNextSenderSeqNum();
        testNextTargetSeqNum();
    }

    @After
    public void destroy() throws IOException {
        sessionState = null;
    }

}
