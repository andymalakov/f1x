package org.f1x.api.session;

import org.f1x.SessionIDBean;
import org.f1x.io.parsers.SessionIDByteSequences;
import org.f1x.util.AsciiUtils;
import org.junit.Assert;
import org.junit.Test;

public class Test_SessionID {

    @Test
    public void testEmptySessionsIDsAreEqual() {
        SessionIDBean sessionID1 = new SessionIDBean();
        SessionIDByteSequences sessionID2 = new SessionIDByteSequences(32);
        assertThatEqual(sessionID1, sessionID2);

        fill("", "", "", "", sessionID1, sessionID2);
        assertThatEqual(sessionID1, sessionID2);
    }

    @Test
    public void testSameSessionIDsAreEqual() {
        SessionIDBean sessionID1 = new SessionIDBean();
        SessionIDByteSequences sessionID2 = new SessionIDByteSequences(32);
        fill("senderCompID", "senderSubID", "targetCompID", "targetSubID", sessionID1, sessionID2);
        assertThatEqual(sessionID1, sessionID2);

        fill("h182h3h154564u8hd", "dasd13123v3rqcfd111", "da7843171351vrf21ss8yhdi", "912783478978g981juj d78912789", sessionID1, sessionID2);
        assertThatEqual(sessionID1, sessionID2);
    }

    @Test
    public void testDifferentSessionIDsAreNotEqual() {
        SessionIDBean sessionID1 = new SessionIDBean();
        SessionIDByteSequences sessionID2 = new SessionIDByteSequences(32);
        fill("sc", "ss", "tc", "ts", sessionID1);
        fill("sc", null, "ss", null, sessionID2);
        assertThatDifferent(sessionID1, sessionID2);

        fill(null, "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc", "ts", sessionID2);
        assertThatDifferent(sessionID1, sessionID2);

        fill(null, "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc", "ts", sessionID2);
        assertThatDifferent(sessionID1, sessionID2);

        fill("sc", "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc1", "ts", sessionID2);
        assertThatDifferent(sessionID1, sessionID2);
    }

    @Test
    public void testSameSessionIDsMustHaveSameHashCode() {
        SessionIDBean sessionID1 = new SessionIDBean();
        SessionIDByteSequences sessionID2 = new SessionIDByteSequences(32);
        assertThatHaveSameHashCode(sessionID1, sessionID2);

        fill("senderCompID", "senderSubID", "targetCompID", "targetSubID", sessionID1, sessionID2);
        assertThatHaveSameHashCode(sessionID1, sessionID2);

        fill("h182h3h1u8h651156asdd", "dasd1312d1478211", "da7843fsfsfs178yhdi", "9127834981juj d78912789", sessionID1, sessionID2);
        assertThatHaveSameHashCode(sessionID1, sessionID2);
    }

    @Test
    public void testDifferentSessionIDsShouldHaveDifferentHashCode() {
        SessionIDBean sessionID1 = new SessionIDBean();
        SessionIDByteSequences sessionID2 = new SessionIDByteSequences(32);
        fill("sc", "ss", "tc", "ts", sessionID1);
        fill("sc", null, "ss", null, sessionID2);
        assertThatHaveDifferentHashCode(sessionID1, sessionID2);

        fill(null, "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc", "ts", sessionID2);
        assertThatHaveDifferentHashCode(sessionID1, sessionID2);

        fill(null, "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc", "ts", sessionID2);
        assertThatHaveDifferentHashCode(sessionID1, sessionID2);

        fill("sc", "ss", "tc", "ts", sessionID1);
        fill("sc", "ss", "tc1", "ts", sessionID2);
        assertThatHaveDifferentHashCode(sessionID1, sessionID2);
    }

    private static void assertThatEqual(SessionID sessionID1, SessionID sessionID2) {
        Assert.assertEquals(sessionID1, sessionID2);
    }

    private static void assertThatDifferent(SessionID sessionID1, SessionID sessionID2) {
        Assert.assertNotEquals(sessionID1, sessionID2);
    }

    private static void assertThatHaveSameHashCode(SessionID sessionID1, SessionID sessionID2) {
        Assert.assertEquals(sessionID1.hashCode(), sessionID2.hashCode());
    }

    private static void assertThatHaveDifferentHashCode(SessionID sessionID1, SessionID sessionID2) {
        Assert.assertNotEquals(sessionID1.hashCode(), sessionID2.hashCode());
    }

    private static void fill(String senderCompID, String senderSubID, String targetCompID, String targetSubID, SessionIDBean sessionID1, SessionIDByteSequences sessionID2) {
        fill(senderCompID, senderSubID, targetCompID, targetSubID, sessionID1);
        fill(senderCompID, senderSubID, targetCompID, targetSubID, sessionID2);
    }

    private static void fill(String senderCompID, String senderSubID, String targetCompID, String targetSubID, SessionIDBean sessionID) {
        sessionID.setSenderCompId(senderCompID);
        sessionID.setSenderSubId(senderSubID);
        sessionID.setTargetCompId(targetCompID);
        sessionID.setTargetSubId(targetSubID);
    }

    private static void fill(String senderCompID, String senderSubID, String targetCompID, String targetSubID, SessionIDByteSequences sessionID) {
        sessionID.clear();
        if (senderCompID != null)
            sessionID.setSenderCompId(AsciiUtils.getBytes(senderCompID), 0, senderCompID.length());
        if (senderSubID != null)
            sessionID.setSenderSubId(AsciiUtils.getBytes(senderSubID), 0, senderSubID.length());
        if (targetCompID != null)
            sessionID.setTargetCompId(AsciiUtils.getBytes(targetCompID), 0, targetCompID.length());
        if (targetSubID != null)
            sessionID.setTargetSubId(AsciiUtils.getBytes(targetSubID), 0, targetSubID.length());
    }

}

