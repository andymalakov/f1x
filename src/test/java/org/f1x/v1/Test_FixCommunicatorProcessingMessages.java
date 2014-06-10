package org.f1x.v1;

import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.FixTags;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionState;
import org.f1x.api.session.SessionStatus;
import org.f1x.io.PredefinedInputChannel;
import org.f1x.io.TextOutputChannel;
import org.f1x.store.InMemoryMessageStore;
import org.f1x.store.MessageStore;
import org.f1x.util.AsciiUtils;
import org.f1x.util.StoredTimeSource;
import org.f1x.util.TimeSource;
import org.f1x.v1.state.TestSessionState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test_FixCommunicatorProcessingMessages extends TestCommon {

    protected static final String LOGON = "8=FIX.4.4|9=84|35=A|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|98=0|108=30|141=N|383=8192|10=209|";
    protected static final String LOGOUT = "8=FIX.4.4|9=89|35=5|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|58=Responding to Logout request|10=101|";
    protected static final String HEARTBEAT = "8=FIX.4.4|9=70|35=0|34=2|49=RECEIVER|52=20140101-10:10:10.100|56=SENDER|112=TEST#123|10=024|";
    protected static final String TEST_REQUEST = "8=FIX.4.4|9=67|35=1|34=2|49=RECEIVER|52=20140101-10:10:10.100|56=SENDER|112=TEST123|10=245|";
    protected static final String REJECT = "8=FIX.4.4|9=79|35=3|34=5|49=RECEIVER|52=20140101-10:10:10.100|56=SENDER|45=123|373=1|58=Cause|10=053|";
    protected static final String RESEND_REQUEST = "8=FIX.4.4|9=67|35=2|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=5|16=10|10=136|";
    protected static final String SEQUENCE_RESET = "8=FIX.4.4|9=72|35=4|34=776|49=RECEIVER|52=20140101-10:10:10.100|56=SENDER|36=777|123=N|10=043|";
    protected static final String NEW_ORDER = "8=FIX.4.4|9=78|35=D|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|1=Account|11=OrderID|10=053|";

    private static final SessionID SESSION_ID = new SessionIDBean("RECEIVER", "SENDER");
    private static final TimeSource TIME_SOURCE = new StoredTimeSource(0);


    private final List<SessionStatus> sessionStatusFlow = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private final SessionState sessionState = new TestSessionState();
    private final MessageStore messageStore = new InMemoryMessageStore(1 << 15);

    private FixCommunicator communicator;

    @Before
    public void init() {
        communicator = new TestFixCommunicator(SESSION_ID, TIME_SOURCE) {

            @Override
            protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
                sendHeartbeat(null); // just increases sender seq num
            }

            @Override
            protected void errorProcessingMessage(String errorText, Exception e, boolean logStackTrace) {
                if (e != ConnectionProblemException.NO_SOCKET_DATA)
                    errors.add(e.toString());

                super.errorProcessingMessage(errorText, e, logStackTrace);
            }
        };

        communicator.setEventListener(new SessionEventListener() {
            @Override
            public void onStatusChanged(SessionID sessionID, SessionStatus oldStatus, SessionStatus newStatus) {
                sessionStatusFlow.add(newStatus);
            }
        });

        communicator.active = true;
        communicator.setSessionState(sessionState);
        communicator.setMessageStore(messageStore);
    }

    @After
    public void close() {
        messageStore.clean();
        sessionState.resetNextSeqNums();
        sessionStatusFlow.clear();
        errors.clear();
    }

    // ---------- LOGON ----------

    @Test
    public void testLogonWithoutHeartbeatInterval() {
        String inboundLogon = "8=FIX.4.4|9=57|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertErrorsOccurred(InvalidFixMessageException.NO_HEARTBEAT_INTERVAL);
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonMismatchingHeartbeatInterval() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=11|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertErrorsOccurred(ConnectionProblemException.HEARTBEAT_INTERVAL_MISMATCH);
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonWithResetSeqNumAndSeqNumMoreOne() {
        String inboundLogon = "8=FIX.4.4|9=70|35=A|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|141=Y|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertErrorsOccurred(InvalidFixMessageException.MSG_SEQ_NUM_MUST_BE_ONE);
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonWithResetSeqNum() {
        String inboundLogon = "8=FIX.4.4|9=70|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|141=Y|10=020|";
        String expectedOutboundLogon = "8=FIX.4.4|9=84|35=A|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|98=0|108=30|141=Y|383=8192|10=216|";

        setSessionStatus(SessionStatus.SocketConnected);
        setNextSeqNums(100, 100);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundLogon);
        assertNextSeqNums(2, 2);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.ReceivedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonWithSeqNumLessExpected() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=2|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(5, 5);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogon() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundLogon = "8=FIX.4.4|9=84|35=A|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|98=0|108=30|141=N|383=8192|10=209|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundLogon);
        assertNextSeqNums(6, 6);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.ReceivedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonWithTargetSeqNumMoreExpected() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundLogon = "8=FIX.4.4|9=84|35=A|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|98=0|108=30|141=N|383=8192|10=205|";
        String expectedResendRequest = "8=FIX.4.4|9=66|35=2|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=1|16=4|10=083|";

        setSessionStatus(SessionStatus.SocketConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundLogon, expectedResendRequest);
        assertNextSeqNums(6, 3);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.SocketConnected,
                SessionStatus.ReceivedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonResponse() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.InitiatedLogon);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(2, 1);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogonResponseWithSeqNumMoreExpected() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=1|16=4|10=082|";

        setSessionStatus(SessionStatus.InitiatedLogon);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest);
        assertNextSeqNums(6, 2);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testInSessionLogonWithoutSeqReset() {
        String inboundLogon = "8=FIX.4.4|9=64|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertErrorsOccurred(InvalidFixMessageException.IN_SESSION_LOGON_MESSAGE_WITHOUT_MSG_SEQ_RESET_NOT_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testInSessionLogon() {
        String inboundLogon = "8=FIX.4.4|9=70|35=A|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|108=30|141=Y|10=020|";
        String expectedOutboundLogon = "8=FIX.4.4|9=84|35=A|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|98=0|108=30|141=Y|383=8192|10=216|";

        setNextSeqNums(100, 100);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogon);

        assertMessages(actualOutboundMessages, expectedOutboundLogon);
        assertNextSeqNums(2, 2);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- LOGOUT ----------

    @Test
    public void testLogout() {
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=020|";
        String expectedOutboundLogout = "8=FIX.4.4|9=89|35=5|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|58=Responding to Logout request|10=101|";

        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundLogout);
        assertNextSeqNums(2, 2);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.InitiatedLogout,
                SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogoutWithSeqNumMoreExpected() {
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=020|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=1|16=4|10=082|";
        String expectedOutboundLogout = "8=FIX.4.4|9=89|35=5|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|58=Responding to Logout request|10=102|";

        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest, expectedOutboundLogout);
        assertNextSeqNums(6, 3);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.InitiatedLogout,
                /// Per FIX spec we keep connection open to allow LOGOUT initiator to fill gaps// SessionStatus.SocketConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogoutWithSeqNumLessExpected() {
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=1|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=020|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.ApplicationConnected);
        setNextTargetSeqNum(5);
        String actualOutboundMessages = simulateProcessing(inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(5, 1);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogoutResponseWithSeqNumMoreExpected() {
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=024|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.InitiatedLogout);
        String actualOutboundMessages = simulateProcessing(inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogout,
                SessionStatus.Disconnected
        );
    }

    /** We initiated logout but other side detected a gap and requested resend before responding with LOGOUT */
    @Test
    public void testResendWhileLogout() {
        String inboundResend = "8=FIX.4.4|9=66|35=2|34=2|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=1|16=0|10=136|"; // Pls resend 1...
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=3|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=024|";

        String expectedOutboundGapFill = "8=FIX.4.4|9=73|35=4|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=4|123=Y|10=217|"; // Gap fill [1..3]

        setSessionStatus(SessionStatus.InitiatedLogout);
        fillMessageStore(1, LOGON, HEARTBEAT, TEST_REQUEST); // Next SenderCompID=4
        setNextSeqNums(2, 4);
        String actualOutboundMessages = simulateProcessing(inboundResend, inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundGapFill);
        assertNextSeqNums(4, 4);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogout,
                SessionStatus.Disconnected
        );
    }

    /** Same as above, but this time we have to resend REJECT */
    @Test
    public void testResendWhileLogout2() {
        String inboundResend = "8=FIX.4.4|9=66|35=2|34=2|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=1|16=0|10=136|"; // Pls resend 1...
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=3|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=024|";

        String expectedOutboundGapFill1 =  "8=FIX.4.4|9=73|35=4|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=2|123=Y|10=215|"; // Gap fill [1..1]
        String expectedRejectRebroadcast = "8=FIX.4.4|9=84|35=3|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|45=123|373=1|58=Cause|10=184|"; // REJECT
        String expectedOutboundGapFill2 =  "8=FIX.4.4|9=73|35=4|34=3|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=4|123=Y|10=219|"; // Gap fill [3..3]

        setSessionStatus(SessionStatus.InitiatedLogout);
        fillMessageStore(1, LOGON, REJECT, TEST_REQUEST); // Next SenderCompID=4
        setNextSeqNums(2, 4);
        String actualOutboundMessages = simulateProcessing(inboundResend, inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundGapFill1, expectedRejectRebroadcast, expectedOutboundGapFill2);
        assertNextSeqNums(4, 4);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogout,
                SessionStatus.Disconnected
        );
    }


    @Test
    public void testMultipleGaps() {
        String inboundLogon   = "8=FIX.4.4|9=84|35=A|34=1|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|98=0|108=30|141=N|383=8192|10=209|";
        String inboundAppMsg1 = "8=FIX.4.4|9=107|35=h|34=10|49=SENDER|52=20140605-17:13:28.568|56=RECEIVER|335=242131811009569|336=SESS#1401988408564|340=2|10=152|";
        String inboundAppMsg2 = "8=FIX.4.4|9=107|35=h|34=20|49=SENDER|52=20140605-17:13:28.568|56=RECEIVER|335=242131811009569|336=SESS#1401988408564|340=2|10=152|";

        String expectedOutboundResend1 = "8=FIX.4.4|9=66|35=2|34=1|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=2|16=8|10=087|";
        String expectedOutboundAppMsg1 = "8=FIX.4.4|9=57|35=0|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|10=210|";
        String expectedOutboundResend2 = "8=FIX.4.4|9=68|35=2|34=3|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=11|16=18|10=188|";
        String expectedOutboundAppMsg2 = "8=FIX.4.4|9=57|35=0|34=4|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|10=212|";

        setSessionStatus(SessionStatus.InitiatedLogon);
        setNextSeqNums(1, 1);
        String actualOutboundMessages = simulateProcessing(inboundLogon, inboundAppMsg1, inboundAppMsg2);

        assertMessages(actualOutboundMessages, expectedOutboundResend1, expectedOutboundAppMsg1, expectedOutboundResend2, expectedOutboundAppMsg2);
        assertNextSeqNums(21, 5);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testLogoutResponseWithSeqNumMoreExpectedOnInvalidLogon() {
        String inboundLogout = "8=FIX.4.4|9=57|35=5|34=5|49=SENDER|52=20140522-12:07:39.552|56=RECEIVER|10=024|";
        String expectedOutboundMessages = "";

        setSessionStatus(SessionStatus.InitiatedLogon);
        String actualOutboundMessages = simulateProcessing(inboundLogout);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(1, 1);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.InitiatedLogon,
                SessionStatus.Disconnected
        );
    }

    // ---------- HEARTBEAT ----------

    @Test
    public void testHeartbeat() {
        String inboundHeartbeat = "8=FIX.4.4|9=70|35=0|34=2|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST#123|10=024|";
        String expectedOutboundMessages = "";

        setNextSeqNums(2, 2);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundHeartbeat);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(3, 2);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testHeartbeatWithSeqNumMoreExpected() {
        String inboundHeartbeat = "8=FIX.4.4|9=70|35=0|34=3|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST#123|10=024|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=2|16=2|10=082|";

        setNextSeqNums(2, 2);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundHeartbeat);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest);
        assertNextSeqNums(4, 3);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testHeartbeatWithSeqNumLessExpected() {
        String inboundHeartbeat = "8=FIX.4.4|9=70|35=0|34=1|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST#123|10=024|";
        String expectedOutboundMessages = "";

        setNextSeqNums(3, 3);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundHeartbeat);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(3, 3);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- TEST REQUEST ----------

    @Test
    public void testTestRequest() {
        String inboundTestRequest = "8=FIX.4.4|9=69|35=1|34=2|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST123|10=245|";
        String expectedOutboundHeartbeat = "8=FIX.4.4|9=69|35=0|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|112=TEST123|10=125|";

        setNextSeqNums(2, 2);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundHeartbeat);
        assertNextSeqNums(3, 3);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testTestRequestWithSeqNumMoreExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=69|35=1|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST123|10=245|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=2|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=2|16=4|10=084|";

        setNextSeqNums(2, 2);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest);
        assertNextSeqNums(6, 3);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testTestRequestWithSeqNumLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=69|35=1|34=1|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|112=TEST123|10=245|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(5, 5);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- SEQUENCE RESET ----------

    @Test
    public void testSequenceResetResetWithSeqNumLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=72|35=4|34=776|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|36=777|123=N|10=043|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(777, 5);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testSequenceResetToValueLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=72|35=4|34=776|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|36=777|123=N|10=043|";
        String expectedOutboundReject = "8=FIX.4.4|9=125|35=3|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|45=776|373=6|58=SequenceReset can only increase the sequence number|10=104|";

        setNextSeqNums(5000, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundReject);
        assertNextSeqNums(5000, 6);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testSequenceResetGapFill() {
        String inboundTestRequest = "8=FIX.4.4|9=75|35=4|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|43=N|36=100|123=Y|10=079|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(100, 5);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testSequenceResetGapFillWithSeqNumMoreExpected() {
        String inboundResetRequest = "8=FIX.4.4|9=76|35=4|34=10|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|43=N|36=100|123=Y|10=079|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=5|16=9|10=095|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundResetRequest); // MsgSeqNum=10 instead of 5

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest);
        assertNextSeqNums(100, 6);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    /** Same as above but this time inbound RESEND marked as PossDupFlag=Y (we should ignore it) */
    @Test
    public void testSequenceResetGapFillWithSeqNumMoreExpectedPossDup() {
        String inboundResetRequest = "8=FIX.4.4|9=76|35=4|34=10|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|43=Y|36=100|123=Y|10=079|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundResetRequest); // MsgSeqNum=10 instead of 5

        assertMessages(actualOutboundMessages);
        assertNextSeqNums(5, 5);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testSequenceResetGapFillWithSeqNumLessExpected() {
        String inboundSeqResetGapFillRequest = "8=FIX.4.4|9=76|35=4|34=10|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|43=N|36=100|123=Y|10=079|";
        String expectedOutboundMessages = "";

        setNextSeqNums(15, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundSeqResetGapFillRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(15, 5);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- REJECT ----------

    @Test
    public void testReject() {
        String inboundTestRequest = "8=FIX.4.4|9=79|35=3|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(6, 5);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testRejectWithSeqNumMoreExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=80|35=3|34=10|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";
        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=5|16=9|10=095|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest);
        assertNextSeqNums(11, 6);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testRejectWithSeqNumLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=79|35=3|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";
        String expectedOutboundMessages = "";

        setNextSeqNums(15, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(15, 5);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- APPLICATION MESSAGES ----------

    @Test
    public void testNewOrder() {
        String inboundTestRequest = "8=FIX.4.4|9=79|35=D|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";
        String expectedOutboundHeartbeat = "8=FIX.4.4|9=57|35=0|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|10=213|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundHeartbeat);
        assertNextSeqNums(6, 6);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testNewOrderWithSeqNumMoreExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=80|35=D|34=10|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";

        String expectedOutboundResendRequest = "8=FIX.4.4|9=66|35=2|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=5|16=8|10=094|";
        String expectedOutboundTestResponse = "8=FIX.4.4|9=57|35=0|34=6|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|10=214|";

        setNextSeqNums(5, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundResendRequest, expectedOutboundTestResponse);
        assertNextSeqNums(11, 7);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testNewOrderWithSeqNumLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=79|35=D|34=5|49=SENDER|52=20140101-10:10:10.100|56=RECEIVER|45=123|373=1|58=Cause|10=053|";
        String expectedOutboundMessages = "";

        setNextSeqNums(15, 5);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(15, 5);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    // ---------- RESEND REQUEST ----------

    @Test
    public void testResendRequestForInfinity() {
        String inboundTestRequest = "8=FIX.4.4|9=66|35=2|34=5|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=5|16=0|10=136|";
        String firstExpectedOutboundGapFill = "8=FIX.4.4|9=73|35=4|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=9|123=Y|10=226|";
        String expectedOutboundReject = "8=FIX.4.4|9=84|35=3|34=9|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|45=123|373=1|58=Cause|10=191|";
        String secondExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=10|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=11|123=Y|10=057|";
        String expectedOutboundNewOrder = "8=FIX.4.4|9=84|35=D|34=11|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|1=Account|11=OrderID|10=121|";
        String thirdExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=12|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=15|123=Y|10=063|";

        setNextSeqNums(5, 15);
        setSessionStatus(SessionStatus.ApplicationConnected);
        fillMessageStore(5, LOGON, HEARTBEAT, TEST_REQUEST, RESEND_REQUEST, REJECT, SEQUENCE_RESET, NEW_ORDER, LOGOUT);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, firstExpectedOutboundGapFill, expectedOutboundReject,
                secondExpectedOutboundGapFill, expectedOutboundNewOrder, thirdExpectedOutboundGapFill);

        assertNextSeqNums(6, 15);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testResendRequest() {
        String inboundTestRequest = "8=FIX.4.4|9=67|35=2|34=5|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=5|16=20|10=136|";
        String firstExpectedOutboundGapFill = "8=FIX.4.4|9=73|35=4|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=9|123=Y|10=226|";
        String expectedOutboundReject = "8=FIX.4.4|9=84|35=3|34=9|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|45=123|373=1|58=Cause|10=191|";
        String secondExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=10|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=11|123=Y|10=057|";
        String expectedOutboundNewOrder = "8=FIX.4.4|9=84|35=D|34=11|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|1=Account|11=OrderID|10=121|";
        String thirdExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=12|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=21|123=Y|10=060|";

        setNextSeqNums(5, 25);
        setSessionStatus(SessionStatus.ApplicationConnected);
        fillMessageStore(5, LOGON, HEARTBEAT, TEST_REQUEST, RESEND_REQUEST, REJECT, SEQUENCE_RESET, NEW_ORDER, LOGOUT);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, firstExpectedOutboundGapFill, expectedOutboundReject,
                secondExpectedOutboundGapFill, expectedOutboundNewOrder, thirdExpectedOutboundGapFill);

        assertNextSeqNums(6, 25);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    /** In this test we receive LOGON immediately followed by an app message. Problem is that LOGON has higher than expected sequence number (a gap). Test ensures that we request single RESEND request */
    @Test
    public void testLogonPlusAppMessageAndMessageGap() {

        // FIX Acceptor that we simulate always sends SessionStatus(35=h) right after LOGON
        // Let's assume that during the PREVIOUS session Acceptor sent:
        // 8=FIX.4.4|9=84|35=A|34=1|49=..
        // 8=FIX.4.3|9=113|35=h|34=2|49=..
        // 8=FIX.4.3|9=113|35=5|34=3|49=..

        setNextSeqNums(3, 4); // We stored targetNext as 3 but they sent LOGOUT and actual nextTarget will be 4
        setSessionStatus(SessionStatus.InitiatedLogon);
        fillMessageStore(1, LOGON, TEST_REQUEST, LOGOUT);

        // NEW SESSION
        String inboundLogon         = "8=FIX.4.4|9=84|35=A|34=4|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|98=0|108=30|141=N|383=8192|10=209|";
        String inboundSessionStatus = "8=FIX.4.4|9=106|35=h|34=5|49=SENDER|52=20140605-17:13:28.568|56=RECEIVER|335=242131811009569|336=SESS#1401988408564|340=2|10=152|";
        String actualOutboundMessages = simulateProcessing(inboundLogon, inboundSessionStatus);
        // assert state becomes ApplicationConnected

        String expectedLogonConfirmation = "8=FIX.4.4|9=66|35=2|34=4|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=3|16=3|10=086|";
        String expectedSessionStatResponse = "8=FIX.4.4|9=57|35=0|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|10=213|";

        assertMessages(actualOutboundMessages, expectedLogonConfirmation, expectedSessionStatResponse);

        assertNextSeqNums(6, 6);
        assertNoErrorsOccurred();

        assertSessionStatusFlow(
                SessionStatus.InitiatedLogon,
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testResendRequestWithSeqNumLessExpected() {
        String inboundTestRequest = "8=FIX.4.4|9=67|35=2|34=2|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=5|16=20|10=136|";
        String expectedOutboundMessages = "";

        setNextSeqNums(5, 25);
        setSessionStatus(SessionStatus.ApplicationConnected);
        String actualOutboundMessages = simulateProcessing(inboundTestRequest);

        assertMessages(actualOutboundMessages, expectedOutboundMessages);
        assertNextSeqNums(5, 25);
        assertErrorsOccurred(InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED);
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    @Test
    public void testResendRequestWithSeqNumMoreExpected() {
        String inboundResendRequest = "8=FIX.4.4|9=67|35=2|34=9|49=SENDER|52=19700101-00:00:00.000|56=RECEIVER|7=5|16=20|10=136|";
        String firstExpectedOutboundGapFill = "8=FIX.4.4|9=73|35=4|34=5|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=9|123=Y|10=226|";
        String expectedOutboundReject = "8=FIX.4.4|9=84|35=3|34=9|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|45=123|373=1|58=Cause|10=191|";
        String secondExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=10|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=11|123=Y|10=057|";
        String expectedOutboundNewOrder = "8=FIX.4.4|9=84|35=D|34=11|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|1=Account|11=OrderID|10=121|";
        String thirdExpectedOutboundGapFill = "8=FIX.4.4|9=75|35=4|34=12|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|43=Y|36=21|123=Y|10=060|";
        String expectedResendRequest = "8=FIX.4.4|9=67|35=2|34=25|49=RECEIVER|52=19700101-00:00:00.000|56=SENDER|7=5|16=8|10=145|";

        setNextSeqNums(5, 25);
        setSessionStatus(SessionStatus.ApplicationConnected);
        fillMessageStore(5, LOGON, HEARTBEAT, TEST_REQUEST, RESEND_REQUEST, REJECT, SEQUENCE_RESET, NEW_ORDER, LOGOUT);
        String actualOutboundMessages = simulateProcessing(inboundResendRequest);

        assertMessages(actualOutboundMessages, firstExpectedOutboundGapFill, expectedOutboundReject,
                secondExpectedOutboundGapFill, expectedOutboundNewOrder, thirdExpectedOutboundGapFill, expectedResendRequest);

        assertNextSeqNums(10, 26);
        assertNoErrorsOccurred();
        assertSessionStatusFlow(
                SessionStatus.ApplicationConnected,
                SessionStatus.Disconnected
        );
    }

    protected void setSessionStatus(SessionStatus status) {
        communicator.setSessionStatus(status);
    }

    protected void setNextSeqNums(int target, int sender) {
        setNextTargetSeqNum(target);
        setNextSenderSeqNum(sender);
    }

    protected void setNextTargetSeqNum(int msgSeqNum) {
        sessionState.setNextTargetSeqNum(msgSeqNum);
    }

    protected void setNextSenderSeqNum(int msgSeqNum) {
        sessionState.setNextSenderSeqNum(msgSeqNum);
    }

    protected void assertErrorsOccurred(Throwable... errors) {
        String[] stringErrors = new String[errors.length];

        for (int index = 0; index < errors.length; index++)
            stringErrors[index] = errors[index].toString();

        assertErrorsOccurred(stringErrors);
    }

    protected void assertErrorsOccurred(String... errors) {
        Assert.assertEquals("mismatching numbers of errors", errors.length, this.errors.size());
        for (int index = 0; index < errors.length; index++)
            Assert.assertEquals("mismatching #" + index + " errors", errors[index], this.errors.get(index));
    }

    protected void assertNoErrorsOccurred() {
        Assert.assertEquals("Expected no errors", 0, errors.size());
    }

    protected void assertSessionStatusFlow(SessionStatus... statuses) {
        if (statuses.length != sessionStatusFlow.size())
            Assert.fail("Mismatching numbers of status transitions: expected " + Arrays.toString(statuses) + " actual " + sessionStatusFlow.toString() + "");
        for (int index = 0; index < statuses.length; index++)
            Assert.assertEquals("mismatching #" + index + " statuses", statuses[index], sessionStatusFlow.get(index));
    }

    protected void assertNextTargetSeqNum(int expected) {
        Assert.assertEquals("mismatching target seq nums", expected, sessionState.getNextTargetSeqNum());
    }

    protected void assertNextSenderSeqNum(int expected) {
        Assert.assertEquals("mismatching sender seq nums", expected, sessionState.getNextSenderSeqNum());
    }

    protected void assertNextSeqNums(int target, int sender) {
        assertNextTargetSeqNum(target);
        assertNextSenderSeqNum(sender);
    }

    protected String simulateProcessing(String... inboundMessages) {
        TextOutputChannel outputChannel = new TextOutputChannel() {

            @Override
            public void write(byte[] buffer, int offset, int length) throws IOException {
                if (sb.length() > 0)
                    sb.append('\n');
                super.write(buffer, offset, length);
            }

            @Override
            public void close() throws IOException {
                // do not clear
            }
        };

        for (int i = 0; i < inboundMessages.length; i++) {
            String inboundMessage = inboundMessages[i];
            int messageLength = findMessageLength(inboundMessage.replace('|', (char) 0x01));
            Assert.assertEquals("BodyLen(9) specified in the message #" + i + " matches string size", messageLength, inboundMessage.length());
        }

        communicator.connect(new PredefinedInputChannel(inboundMessages), outputChannel);
        communicator.processInboundMessages();
        return outputChannel.toString();
    }

    protected void fillMessageStore(int startMsgSeqNum, String... messages) {
        for (String message : messages)
            messageStore.put(startMsgSeqNum++, AsciiUtils.getBytes(message.replace('|', '\u0001')), 0, message.length());
    }

    protected static void assertMessages(String actualMessages, String... expectedMessages) {
        Assert.assertEquals("Messages are not equal", toString(expectedMessages), actualMessages);
    }

    private static String toString(String... messages) {
        StringBuilder builder = new StringBuilder(1024);
        for (String message : messages) {
            if (builder.length() > 0)
                builder.append('\n');
            builder.append(message);
        }
        return builder.toString();
    }


    private static int findMessageLength(String message) {
        DefaultMessageParser parser = new DefaultMessageParser();
        parser.set(AsciiUtils.getBytes(message), 0, message.length()); //Assuming ASCII
        while (parser.next()) {
            final int tagNum = parser.getTagNum();
            if (tagNum == FixTags.BodyLength) {
                return parser.getIntValue() + parser.getOffset() + FixCommunicator.CHECKSUM_LENGTH;
            }
        }
        throw new IllegalArgumentException("Can't find BodyLength(9)");
    }
}
