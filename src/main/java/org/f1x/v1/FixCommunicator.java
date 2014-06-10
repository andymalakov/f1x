/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f1x.v1;

import org.f1x.api.FixParserException;
import org.f1x.api.FixSettings;
import org.f1x.api.message.fields.SessionRejectReason;
import org.f1x.api.session.*;
import org.f1x.api.message.fields.EncryptMethod;
import org.f1x.store.EmptyMessageStore;
import org.f1x.store.MessageStore;
import org.f1x.store.SafeMessageStore;
import org.f1x.util.TimeSource;
import org.f1x.util.timer.GlobalTimer;
import org.f1x.v1.schedule.SessionSchedule;
import org.f1x.v1.state.MemorySessionState;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.FixTags;
import org.f1x.api.message.fields.MsgType;
import org.f1x.io.InputChannel;
import org.f1x.io.LoggingOutputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.log.MessageLog;
import org.f1x.log.MessageLogFactory;
import org.f1x.util.AsciiUtils;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.RealTimeSource;

import java.io.IOException;
import java.net.SocketException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Networking common for FIX Acceptor and FIX Initiator
 */
public abstract class FixCommunicator implements FixSession {
    private static final int MIN_FIX_MESSAGE_LENGTH = 63; // min message example: 8=FIX.4.?|9=??|35=?|34=?|49=?|56=?|52=YYYYMMDD-HH:MM:SS|10=???|
    static final int CHECKSUM_LENGTH = 7; // length("10=123|") --check sum always expressed using 3 digits


    protected static final GFLog LOGGER = GFLogFactory.getLog(FixCommunicator.class);

    private SessionEventListener eventListener;

    private final FixSettings settings;
    private MessageLogFactory messageLogFactory;
    private MessageLog messageLog;
    protected SessionState sessionState;

    // Defined during initialization
    private InputChannel in;
    private OutputChannel out;

    private MessageStore messageStore;
    protected SessionSchedule schedule;

    // used by receiver thread only
    private final DefaultMessageParser parserForResend = new DefaultMessageParser();
    private final ByteArrayReference msgTypeForResend = new ByteArrayReference();
    private final byte[] messageBufferForResend;
    private final MessageBuilder messageBuilderForResend;

    private volatile SessionStatus status = SessionStatus.Disconnected;
    protected volatile boolean active = true; // close() sets this to false
    protected final AtomicBoolean running = new AtomicBoolean();

    // used by receiver thread only
    private final DefaultMessageParser parser = new DefaultMessageParser();
    private final byte [] inboundMessageBuffer;
    private final ByteArrayReference msgType = new ByteArrayReference();
    private final byte [] beginString;
    private final ByteArrayReference temporaryByteArrayReference = new ByteArrayReference();

    // Used by senders
    private final MessageBuilder sessionMessageBuilder;
    private final RawMessageAssembler messageAssembler;

    protected final TimeSource timeSource;
    private TimerTask sessionEndTask;

    private final Object sendLock = new Object();

    public FixCommunicator (FixVersion fixVersion, FixSettings settings) {
        this(fixVersion, settings, RealTimeSource.INSTANCE);
    }

    protected FixCommunicator (FixVersion fixVersion, FixSettings settings, TimeSource timeSource) {
        this.settings = settings;

//        this.logInboundMessages = settings.isLogInboundMessages();
//        this.logOutboundMessages = settings.isLogOutboundMessages();
//        if (logInboundMessages || logOutboundMessages)
//            messageLogFactory = new FileMessageLogFactory(settings.getLogDirectory());
//        else
//            messageLogFactory = null;

        this.beginString = AsciiUtils.getBytes(fixVersion.getBeginString());

        sessionMessageBuilder = new ByteBufferMessageBuilder(settings.getMaxOutboundMessageSize(), settings.getDoubleFormatterPrecision());
        messageBuilderForResend = new ByteBufferMessageBuilder(settings.getMaxOutboundMessageSize(), settings.getDoubleFormatterPrecision());
        messageAssembler = new RawMessageAssembler(fixVersion, settings.getMaxOutboundMessageSize(), timeSource);
        inboundMessageBuffer = new byte [settings.getMaxInboundMessageSize()];
        messageBufferForResend = new byte[settings.getMaxOutboundMessageSize()];
        this.timeSource = timeSource;
    }

    @Override
    public MessageBuilder createMessageBuilder() {
        return new ByteBufferMessageBuilder(settings.getMaxOutboundMessageSize(), settings.getDoubleFormatterPrecision());
    }

    @Override
    public void setEventListener(SessionEventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public abstract SessionID getSessionID();

    @Override
    public FixSettings getSettings() {
        return settings;
    }

    @Override
    public SessionStatus getSessionStatus() {
        return status;
    }

    public void setMessageLogFactory(MessageLogFactory messageLogFactory) {
        this.messageLogFactory = messageLogFactory;
    }

    public void setSessionState(SessionState sessionState){
        this.sessionState = sessionState;
    }

    public void setMessageStore(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public void setSessionSchedule(SessionSchedule schedule) {
        this.schedule = schedule;
    }

    protected void setSessionStatus(SessionStatus status) {
        final SessionStatus oldStatus = this.status;
        if (oldStatus == status) {
            LOGGER.warn().append("Already in the status ").append(status).commit();
        } else {
            this.status = status;
            onSessionStatusChanged(oldStatus, status);
        }
    }

    protected void onSessionStatusChanged(final SessionStatus oldStatus, final SessionStatus newStatus) {
        SessionID sessionID = getSessionID();
        LOGGER.info().append("Session ").append(sessionID).append(" changed status ").append(oldStatus).append(" => ").append(newStatus).commit();
        if (eventListener != null)
            eventListener.onStatusChanged(sessionID, oldStatus, newStatus);
    }

    protected final void assertSessionStatus(SessionStatus expectedStatus) {
        final SessionStatus actualStatus = getSessionStatus();
        if (actualStatus != expectedStatus)
            throw new IllegalStateException("Expecting " + expectedStatus + " status instead of " + actualStatus);
    }

    protected final void assertSessionStatus2(SessionStatus expectedStatus1, SessionStatus expectedStatus2) {
        final SessionStatus actualStatus = getSessionStatus();
        if (actualStatus != expectedStatus1 && actualStatus != expectedStatus2)
            throw new IllegalStateException("Expecting " + expectedStatus1 + " or " + expectedStatus2 + " status instead of " + actualStatus);
    }

    protected void connect(InputChannel in, OutputChannel out) {
        this.messageLog = (messageLogFactory != null) ? messageLogFactory.create(getSessionID()) : null;

        this.in = in;
        this.out = (messageLog != null) ? new LoggingOutputChannel(messageLog, out) : out;
    }

    protected void init() {
        if (sessionState == null)
            sessionState = new MemorySessionState();

        messageStore = messageStore == null ?
                EmptyMessageStore.getInstance() :
                new SafeMessageStore(messageStore);
    }

    protected void destroy(){
    }

    /** Process inbound messages until session ends */
    protected final boolean processInboundMessages() {
        return processInboundMessages(null, 0);
    }

    /** Process inbound messages until session ends
     * @param logonBuffer buffer containing session LOGON message and may be some other messages or a part of them
     * @param length actual number of bytes that should be consumed from logonBuffer
     */
    protected final boolean processInboundMessages(byte[] logonBuffer, int length) {
        LOGGER.info().append("Processing FIX Session").commit();
        boolean normalExit = false;
        try {
            int offset = 0;
            if (logonBuffer != null) {
                System.arraycopy(logonBuffer, 0, inboundMessageBuffer, 0, length);
                offset = processInboundMessages(length);
            }

            while (active) { ///prevents logged out session from re-connect
                int bytesRead = in.read(inboundMessageBuffer, offset, inboundMessageBuffer.length - offset);
                if (bytesRead <= 0) {
                    throw ConnectionProblemException.NO_SOCKET_DATA;
                } else {
                    offset = processInboundMessages(offset+bytesRead);
                }
            }
            LOGGER.error().append("Finishing FIX session").commit();
            normalExit = true;
        } catch (InvalidFixMessageException e) {
            errorProcessingMessage("Protocol Error", e, false);
        } catch (ConnectionProblemException e) {
            errorProcessingMessage("Connection Problem", e, false);
        } catch (SocketException e) {
            errorProcessingMessage("Socket Error (Other side disconnected?)", e, false);
        } catch (Exception e) {
            errorProcessingMessage("General error", e, true);
        }

        assertSessionStatus(SessionStatus.Disconnected);
        return normalExit;
    }

    protected void errorProcessingMessage(String errorText, Exception e, boolean logStackTrace) {
        if (active) {
            if (logStackTrace)
                LOGGER.error().append(errorText).append(" : ").append(e).commit();
            else
                LOGGER.error().append(errorText).append(" : ").append(e.getMessage()).commit();
            disconnect(errorText);
        }
    }


    protected int processInboundMessages(int bytesRead) throws IOException, InvalidFixMessageException, ConnectionProblemException {
        assert bytesRead > 0;
        int messageStart = 0;
        int readMessageLength;
        while ((readMessageLength = bytesRead - messageStart) >= MIN_FIX_MESSAGE_LENGTH) {

            parser.set(inboundMessageBuffer, messageStart, readMessageLength);

            // All FIX messages begin with 3 required tags: BeginString, BodyLength, and MsgType.
            parseBeginString(parser);
            final int bodyLength = parseBodyLength(parser);
            final int msgTypeStart = parser.getOffset();

            final int lengthOfBeginStringAndBodyLength = msgTypeStart - messageStart;
            final int messageLength = lengthOfBeginStringAndBodyLength + bodyLength + CHECKSUM_LENGTH; // BodyLength is the number of characters in the message following the BodyLength field up to, and including, the delimiter immediately preceding the CheckSum tag ("10=123|")
            checkMessageLength(messageLength, inboundMessageBuffer.length);

            if (readMessageLength < messageLength)
                break; // retry after we read full message in the buffer

            parser.set(inboundMessageBuffer, msgTypeStart, bodyLength);

            if ( ! parser.next())
                throw InvalidFixMessageException.MISSING_MSG_TYPE;

            parser.getByteSequence(msgType);

            final int msgSeqNum = findMsgSeqNum(parser);

            if (messageLog != null)
                messageLog.log(true, inboundMessageBuffer, messageStart, messageLength);

            // set parser limit to consume single message
            parser.set(inboundMessageBuffer, messageStart, messageLength);

            processInboundMessage(parser, msgType, msgSeqNum);

            messageStart += messageLength; // go to next message
        }

        // Move remaining part at the beginning of buffer
        int remainingSize = bytesRead - messageStart;
        if (remainingSize > 0 && messageStart != 0)
            System.arraycopy(inboundMessageBuffer, messageStart, inboundMessageBuffer, 0, remainingSize);
        return remainingSize;
    }

    /** Send LOGOUT but do not drop socket connection */
    @Override
    public void logout(String cause) {
        LOGGER.info().append("Initiating FIX Logout: ").append(cause).commit();

        if (status == SessionStatus.ApplicationConnected) { // TODO: lock
            try {
                sendLogout(cause);
            } catch (IOException e) {
                LOGGER.warn().append("Error logging out from FIX session: ").append(e).commit();
            }
        }
    }

    /** Terminate socket connection (no logout message is sent if session is in process) */
    @Override
    public void disconnect(String cause) {
        LOGGER.info().append("FIX Disconnect due to ").append(cause).commit();

        setSessionStatus(SessionStatus.Disconnected);
        try {
            in.close();
            out.close();
            if (messageLog != null) {
                messageLog.close();
                messageLog = null;
            }
        } catch (IOException e) {
            LOGGER.warn().append("Error closing socket: ").append(e).commit();
        }
    }



    /** Logout current session (if needed) and terminate socket connection. */
    @Override
    public void close() {
        active = false;
        if (status == SessionStatus.ApplicationConnected) {
            try {
                sendLogout("Goodbye");
            } catch (IOException e) {
                LOGGER.warn().append("Error logging out from FIX session: ").append(e).commit();
            }
        }

        if (status != SessionStatus.Disconnected)
            disconnect("Closing");
    }

    /**
     * Sends a message. This message is persisted in message store
     */
    @Override
    public void send(MessageBuilder messageBuilder) throws IOException {
        synchronized (sendLock) {
            messageAssembler.send(getSessionID(), sessionState.consumeNextSenderSeqNum(), messageBuilder, messageStore, out);
        }
    }

    /**
     * Resends a message with given msg seq num, this message is not persisted in message store.
     */
    protected void resend(MessageBuilder messageBuilder, int msgSeqNum) throws IOException {
        synchronized (sendLock){
            messageAssembler.send(getSessionID(), msgSeqNum, messageBuilder, null, out);
        }
    }

    protected void sendLogon(boolean resetSequenceNumbers) throws IOException {
        if ( ! resetSequenceNumbers)
            resetSequenceNumbers = settings.isResetSequenceNumbersOnEachLogon();

        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGON);
            sessionMessageBuilder.add(FixTags.EncryptMethod, EncryptMethod.NONE_OTHER);
            sessionMessageBuilder.add(FixTags.HeartBtInt, settings.getHeartBeatIntervalSec());
            sessionMessageBuilder.add(FixTags.ResetSeqNumFlag, resetSequenceNumbers);

            if (settings.isLogonWithNextExpectedMsgSeqNum()) {
                sessionMessageBuilder.add(FixTags.NextExpectedMsgSeqNum, sessionState.getNextTargetSeqNum());
            }
            sessionMessageBuilder.add(FixTags.MaxMessageSize, settings.getMaxInboundMessageSize());

            synchronized (sendLock) {
                if (resetSequenceNumbers) {
                    sessionState.setNextSenderSeqNum(1);
                    messageStore.clean();
                }

                send(sessionMessageBuilder);
            }
        }
    }

    protected void sendLogout(CharSequence cause) throws IOException {
        assertSessionStatus(SessionStatus.ApplicationConnected); // TODO: remove
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGOUT);
            if (cause != null)
                sessionMessageBuilder.add(FixTags.Text, cause);
            send(sessionMessageBuilder);
        }
        setSessionStatus(SessionStatus.InitiatedLogout); // TODO: CAS (SessionStatus.ApplicationConnected, SessionStatus.InitiatedLogout)
    }

    /**
     * Sends FIX Heartbeat(0) message
     * @param testReqId required when heartbeat is sent in response to TestRequest(1)
     */
    protected void sendHeartbeat(CharSequence testReqId) throws IOException {
        assertSessionStatus(SessionStatus.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.HEARTBEAT);
            if (testReqId!= null)
                sessionMessageBuilder.add(FixTags.TestReqID, testReqId);
            send(sessionMessageBuilder);
        }
    }

    /**
     * Sends FIX TestRequest(1) message.
     * @param testReqId Verifies that the opposite application is generating the heartbeat as the result of Test Request (1) and not a normal timeout.
     *                  The opposite application includes the TestReqID (112) in the resulting Heartbeat(0).
     *                  Any string can be used as the TestReqID (112) (one suggestion is to use a timestamp string).
     */
    protected void sendTestRequest(CharSequence testReqId) throws IOException {
        assertSessionStatus(SessionStatus.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.TEST_REQUEST);
            sessionMessageBuilder.add(FixTags.TestReqID, testReqId);
            send(sessionMessageBuilder);
        }
    }

    /**
     * @param rejectedMsgSeqNum MsgSeqNum(34) of rejected message
     * This method sends FIX Reject(3).
     * @param rejectReason optional reject reason
     * @param text optional explanation message
     */
    protected void sendReject(int rejectedMsgSeqNum, SessionRejectReason rejectReason, CharSequence text) throws IOException {
        assertSessionStatus(SessionStatus.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.REJECT);
            sessionMessageBuilder.add(FixTags.RefSeqNum, rejectedMsgSeqNum);

            if (rejectReason != null)
                sessionMessageBuilder.add(FixTags.SessionRejectReason, rejectReason);

            if (text != null)
                sessionMessageBuilder.add(FixTags.Text, text);
            send(sessionMessageBuilder);
        }
    }

    /**
     * This method sends ResendRequest(2).
     * @param beginSeqNo start of range to resend (inclusive)
     * @param endSeqNo end of range to resend (inclusive). Zero means infinity (resend up to the latest).
     */
    protected void sendResendRequest(int beginSeqNo, int endSeqNo) throws IOException {

        LOGGER.warn().append("Requesting RESEND from ").append(beginSeqNo).append(" to ").append(endSeqNo).commit();

        assertSessionStatus2(SessionStatus.ApplicationConnected, SessionStatus.InitiatedLogout);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.RESEND_REQUEST);

            sessionMessageBuilder.add(FixTags.BeginSeqNo, beginSeqNo);
            sessionMessageBuilder.add(FixTags.EndSeqNo, endSeqNo-1);
            send(sessionMessageBuilder);
        }

    }

    /**
     * Sends SequenceReset(4) in response to ResendRequest when
     * resending a range of administrative messages or when resending actual application messages is not appropriate (e.g. stale messages).
     *
     * @param msgSeqNum message sequence number of this message
     * @param newSeqNo new sequence number
     */
    protected void sendGapFill(int msgSeqNum, int newSeqNo) throws IOException {
        assertSessionStatus2(SessionStatus.ApplicationConnected, SessionStatus.InitiatedLogout);

        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.SEQUENCE_RESET);

            sessionMessageBuilder.add(FixTags.PossDupFlag, true);
            sessionMessageBuilder.add(FixTags.NewSeqNo, newSeqNo);
            sessionMessageBuilder.add(FixTags.GapFillFlag, true);
            resend(sessionMessageBuilder, msgSeqNum);
        }
    }

    /**
     * Sends SequenceReset(4) in response to ResendRequest when
     * resending a range of administrative messages or when resending actual application messages is not appropriate (e.g. stale messages).
     * Sets sender message sequence num to newSeqNo
     * @param newSeqNo new sequence number
     */
    protected void sendSequenceReset(int newSeqNo) throws IOException {
        assertSessionStatus(SessionStatus.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.SEQUENCE_RESET);

            sessionMessageBuilder.add(FixTags.NewSeqNo, newSeqNo);
            sessionMessageBuilder.add(FixTags.GapFillFlag, false);

            synchronized (sendLock) {
                sessionState.setNextSenderSeqNum(newSeqNo - 1);
                send(sessionMessageBuilder); // In reset mode MsgSeqNum should be ignored
            }
        }
    }

    private void parseBeginString(MessageParser parser) throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.EMPTY_MESSAGE;

        if (parser.getTagNum() != FixTags.BeginString)
            throw InvalidFixMessageException.BAD_FIRST_TAG;

        if (this.beginString != null && ! parser.isValueEquals(this.beginString))
            throw InvalidFixMessageException.INVALID_BEGIN_STRING;
    }

    private static int parseBodyLength(MessageParser parser) throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        if (parser.getTagNum() != FixTags.BodyLength)
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        int bodyLength = parser.getIntValue();
        if (bodyLength <= 0)
            throw InvalidFixMessageException.BAD_BODY_LENGTH;
        return bodyLength;
    }

    protected void processInboundMessage(MessageParser parser, CharSequence msgType, int msgSeqNumX) throws IOException, InvalidFixMessageException, ConnectionProblemException {
        SessionStatus currentStatus = getSessionStatus();
        switch (currentStatus) {
            case ApplicationConnected:
            case InitiatedLogout:
                processInSessionMessage(msgSeqNumX, msgType, parser);
                break;
            case SocketConnected:
                if (isLogon(msgType))
                    processInboundLogon(msgSeqNumX, parser);
                else
                    throw InvalidFixMessageException.EXPECTING_LOGON_MESSAGE;

                break;
            case InitiatedLogon:
                if(isLogon(msgType))
                    processInboundLogon(msgSeqNumX, parser);
                else if(isLogout(msgType))
                    processInboundLogout(msgSeqNumX, parser);
                else
                    throw InvalidFixMessageException.EXPECTING_LOGON_MESSAGE;
                break;
            default:
                LOGGER.warn().append("Received unexpected message (35=").append(msgType).append(") in status ").append(currentStatus).commit();
        }

    }

    private void processInSessionMessage(int msgSeqNumX, CharSequence msgType, MessageParser parser) throws IOException, InvalidFixMessageException, ConnectionProblemException {
        boolean processed = true;
        if (msgType.length() == 1) { // All session-level messages have MsgType expressed using single char
            switch (msgType.charAt(0)) {
                case AdminMessageTypes.LOGON:
                    processInboundLogon(msgSeqNumX, parser); break;
                case AdminMessageTypes.LOGOUT:
                    processInboundLogout(msgSeqNumX, parser); break;
                case AdminMessageTypes.HEARTBEAT:
                    processInboundHeartbeat(msgSeqNumX, parser);
                    break;
                case AdminMessageTypes.TEST:
                    processInboundTestRequest(msgSeqNumX, parser); break;
                case AdminMessageTypes.RESEND:
                    processInboundResendRequest(msgSeqNumX, parser); break;
                case AdminMessageTypes.REJECT:
                    processInboundReject(msgSeqNumX, parser); break;
                case AdminMessageTypes.RESET:
                    processInboundSequenceReset(msgSeqNumX, parser); break;
                default:
                    processed = false;
            }
        } else {
            processed = false;
        }
        if ( ! processed)
            processInboundAppMessage(msgSeqNumX, msgType, parser);
    }

    protected void processInboundAppMessage(int msgSeqNumX, CharSequence msgType, MessageParser parser) throws IOException, InvalidFixMessageException {
        LOGGER.debug().append("Processing inbound message with type: ").append(msgType).commit();

        if (msgSeqNumX > 0) { // PossDupFlag=N
            int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
            if ( ! checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum)) //Let's imagine we expected MsgSeqNum=5 but received 10
                sendResendRequest(expectedTargetSeqNum, msgSeqNumX - 1);   //This will send ResendRequest(5, 0) and set currentResendEndSeqNo=9

            sessionState.setNextTargetSeqNum(msgSeqNumX+1);
        }

        processInboundAppMessage(msgType, parser); //TODO: Add parameter PossDupFlag = (msgSeqNum < 0)
    }

    protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
        // by default do nothing
    }

    /**
     * Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current status
     */
    protected void processInboundLogon(int msgSeqNumX, MessageParser parser) throws IOException, InvalidFixMessageException, ConnectionProblemException {
        LOGGER.debug().append("Processing inbound Logon").commit();

        if (msgSeqNumX < 0) {
            LOGGER.warn().append("Received LOGON message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }


        boolean heartbeatIntervalPresent = false;
        boolean resetSeqNum = false;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.HeartBtInt:
                    if (parser.getIntValue() != settings.getHeartBeatIntervalSec())
                        throw ConnectionProblemException.HEARTBEAT_INTERVAL_MISMATCH; //TODO: Allow initiator to override heartbeat interval for acceptor

                    heartbeatIntervalPresent = true;
                    break;
                case FixTags.ResetSeqNumFlag:
                    resetSeqNum = parser.getBooleanValue();
                    if(resetSeqNum && msgSeqNumX != 1)
                       throw InvalidFixMessageException.MSG_SEQ_NUM_MUST_BE_ONE;

                    break;
            }
        }

        if (!heartbeatIntervalPresent)
            throw InvalidFixMessageException.NO_HEARTBEAT_INTERVAL;

        if (resetSeqNum)
            sessionState.setNextTargetSeqNum(1);

        SessionStatus currentStatus = getSessionStatus();
        if (currentStatus == SessionStatus.ApplicationConnected && !resetSeqNum)
            throw InvalidFixMessageException.IN_SESSION_LOGON_MESSAGE_WITHOUT_MSG_SEQ_RESET_NOT_EXPECTED;

        int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
        boolean expectedTargetMsgSeqNum = checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum);
        sessionState.setNextTargetSeqNum(msgSeqNumX + 1);

        if (currentStatus == SessionStatus.SocketConnected) {
            setSessionStatus(SessionStatus.ReceivedLogon);
            sendLogon(resetSeqNum);
            setSessionStatus(SessionStatus.ApplicationConnected);
        } else if (currentStatus == SessionStatus.InitiatedLogon) {
            setSessionStatus(SessionStatus.ApplicationConnected);
        } else if (currentStatus == SessionStatus.ApplicationConnected) {
            sendLogon(resetSeqNum);
        } else {
            LOGGER.warn().append("Unexpected Logon in status: ").append(currentStatus).commit();
            return;
        }

        // *After* sending a Logon confirmation back, send a ResendRequest
        if (!expectedTargetMsgSeqNum)
            sendResendRequest(expectedTargetSeqNum, msgSeqNumX);
    }

    @SuppressWarnings("unused")
    protected void processInboundLogout(int msgSeqNumX, MessageParser parser) throws IOException, InvalidFixMessageException {
        LOGGER.debug().append("Processing inbound Logout").commit();

        if (msgSeqNumX < 0) {
            LOGGER.warn().append("Received LOGOUT message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }


        int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
        boolean expectedTargetMsgSeqNum = checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum);

        temporaryByteArrayReference.clear();
        while (parser.next()) {
            if (parser.getTagNum() == FixTags.Text) {
                parser.getByteSequence(temporaryByteArrayReference);
                break;
            }
        }

        LOGGER.info().append("Logout received: ").append(temporaryByteArrayReference).commit();

        SessionStatus currentStatus = getSessionStatus();
        if (currentStatus == SessionStatus.ApplicationConnected) {
            sessionState.setNextTargetSeqNum(msgSeqNumX + 1);
            // If a message gap was detected, issue a ResendRequest to retrieve all missing messages followed by a Logout message which serves as a confirmation of the logout request.
            // DO NOT terminate the session.  The initiator of the Logout sequence has responsibility to terminate the session.
            // This allows the Logout initiator to respond to any ResendRequest message.
            if ( ! expectedTargetMsgSeqNum)
                sendResendRequest(expectedTargetSeqNum, msgSeqNumX);

            sendLogout("Responding to Logout request");
            if ( expectedTargetMsgSeqNum)
                setSessionStatus(SessionStatus.SocketConnected);
        } else if (currentStatus == SessionStatus.InitiatedLogout) {
            if (expectedTargetMsgSeqNum)
                sessionState.consumeNextTargetSeqNum();
            // If this side was the initiator of the Logout sequence,
            // then this is a Logout confirmation and the session should be immediately terminated upon receipt.
            disconnect("Logout response received");
        } else if (currentStatus == SessionStatus.InitiatedLogon){
            if (expectedTargetMsgSeqNum)
                sessionState.consumeNextTargetSeqNum();
            disconnect("Logout on Logon received");
        } else {
            LOGGER.info().append("Unexpected Logout in status: ").append(currentStatus).commit();
        }
    }

    @SuppressWarnings("unused")
    protected void processInboundHeartbeat(int msgSeqNumX, MessageParser parser) throws InvalidFixMessageException, IOException {
        LOGGER.debug().append("Processing Inbound Heartbeat").commit();

        if (msgSeqNumX < 0) {
            LOGGER.warn().append("Received HEARTBEAT message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }

        int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
        if( ! checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum))
            sendResendRequest(expectedTargetSeqNum, msgSeqNumX);

        sessionState.setNextTargetSeqNum(msgSeqNumX + 1);
    }

    protected void processInboundTestRequest(int msgSeqNumX, MessageParser parser) throws IOException, InvalidFixMessageException {
        LOGGER.debug().append("Processing inbound Test Request").commit();

        if (msgSeqNumX < 0) {
            LOGGER.warn().append("Received TEST message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }

        int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
        boolean expectedTargetMsgSeqNum = checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum);
        sessionState.setNextTargetSeqNum(msgSeqNumX + 1);
        if (expectedTargetMsgSeqNum) {

            temporaryByteArrayReference.clear();
            while (parser.next()) {
                if (parser.getTagNum() == FixTags.TestReqID) {
                    parser.getByteSequence(temporaryByteArrayReference);
                    break;
                }
            }

            if (temporaryByteArrayReference.length() == 0)
                sendReject(msgSeqNumX, SessionRejectReason.REQUIRED_TAG_MISSING, "Missing TestReqID(112)");
            else
                sendHeartbeat(temporaryByteArrayReference);
        } else {
            sendResendRequest(expectedTargetSeqNum, msgSeqNumX);
        }
    }

    private void processInboundResendRequest(int msgSeqNumX, MessageParser parser) throws IOException, InvalidFixMessageException {
        LOGGER.debug().append("Processing inbound Resend Request").commit();

        int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();

        if (msgSeqNumX < 0) {
            LOGGER.warn().append("Received RESEND message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }

        boolean expectedTargetMsgSeqNum = checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum);
        sessionState.setNextTargetSeqNum(msgSeqNumX + 1);

        int beginSeqNo = -1;
        int endSeqNo = -1;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.BeginSeqNo:  // required
                    beginSeqNo = parser.getIntValue();
                    break;
                case FixTags.EndSeqNo:  // required
                    endSeqNo = parser.getIntValue();
                    break;
            }
        }

        // If message gap is detected, perform the Resend processing first, followed by a ResendRequest of  your own in order to fill the incoming message gap.
        if (beginSeqNo == -1)
            sendReject(msgSeqNumX, SessionRejectReason.REQUIRED_TAG_MISSING, "Missing BeginSeqNo(7)");
        else if (beginSeqNo == 0)
            sendReject(msgSeqNumX, SessionRejectReason.VALUE_IS_INCORRECT, "Invalid BeginSeqNo(7)");
        else if (endSeqNo == -1)
            sendReject(msgSeqNumX, SessionRejectReason.REQUIRED_TAG_MISSING, "Missing EndSeqNo(16)");
        else
            resendMessages(beginSeqNo, endSeqNo != 0 ? endSeqNo : (sessionState.getNextSenderSeqNum() - 1));

        if( ! expectedTargetMsgSeqNum)
            sendResendRequest(expectedTargetSeqNum, msgSeqNumX);
    }

    protected void resendMessages(int beginSeqNo, int endSeqNo) throws IOException {
        if(beginSeqNo > endSeqNo){
            LOGGER.warn().append("Resending messages was skipped: beginSeqNo > endSeqNo").commit();
            return;
        }

        MessageStore.MessageStoreIterator iterator = messageStore.iterator(beginSeqNo, endSeqNo);

        int msgSeqNumOfLastResentMessage = beginSeqNo - 1;
        int msgSeqNum;
        while ((msgSeqNum = iterator.next(messageBufferForResend)) > 0) {
            if(resend(messageBufferForResend, msgSeqNum, msgSeqNumOfLastResentMessage))
                msgSeqNumOfLastResentMessage = msgSeqNum;
        }

        if (msgSeqNumOfLastResentMessage < endSeqNo)
            sendGapFill(msgSeqNumOfLastResentMessage + 1, endSeqNo + 1);
    }

    /**
     * @return true if message was resent otherwise false
     */
    private boolean resend(byte[] message, int msgSeqNum, int msgSeqNumOfLastResentMessage) throws IOException {
        try {
            parserForResend.set(message, 0 , message.length);

            parseBeginString(parserForResend);
            final int bodyLength = parseBodyLength(parserForResend);

            final int lengthOfBeginStringAndBodyLength = parserForResend.getOffset();
            final int messageLength = lengthOfBeginStringAndBodyLength + bodyLength + CHECKSUM_LENGTH;

            if (!parserForResend.next())
                throw InvalidFixMessageException.MISSING_MSG_TYPE;

            parserForResend.getByteSequence(msgTypeForResend);

            parserForResend.set(message, 0, messageLength);

            messageBuilderForResend.clear();
            if( ! onMessageResend(msgTypeForResend, parserForResend, messageBuilderForResend))
                return false;

            int msgSeqNumGap = msgSeqNum - msgSeqNumOfLastResentMessage;
            if(msgSeqNumGap > 1)
                sendGapFill(msgSeqNumOfLastResentMessage + 1, msgSeqNum);

            resend(messageBuilderForResend, msgSeqNum);
            return true;
        } catch (InvalidFixMessageException | FixParserException e) {
            LOGGER.warn().append("Got invalid message #").append(msgSeqNum).append(" from message store : ").append(e).commit();
            return false;
        }
    }

    /**
     * @param parser of resending message
     * @param messageBuilder that will be sent
     * @return true if a message requires the resending otherwise false
     */
    protected boolean onMessageResend(CharSequence msgType, MessageParser parser, MessageBuilder messageBuilder) {
        if(!isResendRequired(msgType))
            return false;

        messageBuilder.setMessageType(msgType);
        messageBuilder.add(FixTags.PossDupFlag, true);
        while (parser.next()) {
            int tagNum = parser.getTagNum();
            switch (tagNum) {
                case FixTags.MsgType:
                case FixTags.MsgSeqNum:
                case FixTags.BeginString:
                case FixTags.BodyLength:
                case FixTags.SendingTime:
                case FixTags.SenderCompID:
                case FixTags.SenderSubID:
                case FixTags.TargetCompID:
                case FixTags.TargetSubID:
                case FixTags.CheckSum:
                    break;
                default:
                    messageBuilder.add(tagNum, parser.getCharSequenceValue());
            }
        }

        return true;
    }

    /**
     * @return true if a message with this msgType requires the resending otherwise false
     */
    protected boolean isResendRequired(CharSequence msgType){
        return (msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.REJECT) ||
                !AdminMessageTypes.isAdmin(msgType);
    }

    private void processInboundSequenceReset(int msgSeqNumX, MessageParser parser) throws IOException, InvalidFixMessageException {
        LOGGER.debug().append("Processing inbound Sequence Reset").commit();

        if (msgSeqNumX < 0) {
            // Normal for gap fill to have PossDupFlag=Y
            LOGGER.info().append("Received RESET message with PossDupFlag=Y - ignoring. MsgSeqNum ").append(-msgSeqNumX).commit();
            return;
        }

        boolean isGapFill = false;
        int newSeqNum = -1;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.NewSeqNo:  // required
                    newSeqNum = parser.getIntValue();
                    break;
                case FixTags.GapFillFlag:
                    isGapFill = parser.getBooleanValue();
                    break;
            }
        }

        LOGGER.info().append("Processing inbound message sequence reset to ").append(newSeqNum).commit();
        //noinspection StatementWithEmptyBody
        if (isGapFill) {
            int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
            if( ! checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum)){
                sendResendRequest(expectedTargetSeqNum, msgSeqNumX);
            }
        } else {
            // If message gap is detected Ignore the incoming sequence number.
            // The NewSeqNo field of the SeqReset message will contain the sequence number of the next message to be transmitted.
        }

        try {
            sessionState.resetNextTargetSeqNum(newSeqNum);
        } catch (InvalidFixMessageException e) {
            sendReject(msgSeqNumX, SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE, e.getMessage());
        }
    }

    protected void processInboundReject(int msgSeqNumX, MessageParser parser) throws InvalidFixMessageException, IOException {
        LOGGER.debug().append("Processing inbound Reject").commit();

        // Skip sequence number checking if we are dealing with GapFill
        if (msgSeqNumX > 0) {
            int expectedTargetSeqNum = sessionState.getNextTargetSeqNum();
            if ( ! checkTargetMsgSeqNum(msgSeqNumX, expectedTargetSeqNum)) {
                sendResendRequest(expectedTargetSeqNum, msgSeqNumX);
            }
            sessionState.setNextTargetSeqNum(msgSeqNumX + 1);
        }

        int refSeqNum = -1;
        temporaryByteArrayReference.clear();
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.RefSeqNum:
                    refSeqNum = parser.getIntValue();
                    break;
                case FixTags.Text:
                    parser.getByteSequence(temporaryByteArrayReference);
                    break;
            }
        }

        //TODO: Notify client API: some brokers use session-level REJECT to reject abnormal order submissions
        if (temporaryByteArrayReference.length() != 0)
            LOGGER.warn().append("Received session-level REJECT:").append(refSeqNum).append(": ").append(temporaryByteArrayReference).commit();
        else
            LOGGER.warn().append("Received session-level REJECT:").append(refSeqNum).commit();

    }

    protected void scheduleSessionEnd(long timeout) {
        sessionEndTask = new SessionEndTask(this);
        GlobalTimer.getInstance().schedule(sessionEndTask, timeout);
    }

    protected void unscheduleSessionEnd() {
        if (sessionEndTask != null) {
            sessionEndTask.cancel();
            sessionEndTask = null;
        }
    }

    protected static boolean checkTargetMsgSeqNum(int actual, int expected) throws InvalidFixMessageException, IOException {
        // If the incoming message has a sequence number less than expected and the PossDupFlag is not set, it indicates a serious error.
        // It is strongly recommended that the session be terminated and manual intervention be initiated.
        if (actual < expected)
            throw InvalidFixMessageException.TARGET_MSG_SEQ_NUM_LESS_EXPECTED;

        return actual == expected;
    }

    private static void checkMessageLength(int messageLength, int maxMessageLength) throws InvalidFixMessageException {
        if(messageLength > maxMessageLength)
            throw InvalidFixMessageException.MESSAGE_TOO_LARGE;
    }

    private static boolean isLogon(CharSequence msgType) {
        return msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGON;
    }

    private static boolean isLogout(CharSequence msgType) {
        return msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGOUT;
    }

    /**
     * @return message sequence number in current message. Method returns negated result ( - MsgSeqNum) if this message has PossDupFlag(43) set to Y.
     * @throws  InvalidFixMessageException if message is missing message sequence number of it is invalid
     */
    private static int findMsgSeqNum(MessageParser parser) throws InvalidFixMessageException {
        Boolean possDupFlag = null;
        int msgSeqNum = 0;
        while (parser.next()) {
            final int tagNum = parser.getTagNum();
            if (tagNum == FixTags.MsgSeqNum) {
                msgSeqNum = parser.getIntValue();
                if(msgSeqNum < 1)
                    throw InvalidFixMessageException.MSG_SEQ_NUM_MUST_BE_POSITIVE;
                if (possDupFlag != null)
                    break; // we are done

            } else
            if (tagNum == FixTags.PossDupFlag) {
                possDupFlag = parser.getBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
                if (msgSeqNum != 0)
                    break; // we are done
            }
        }

        if (msgSeqNum == 0)
            throw InvalidFixMessageException.NO_MSG_SEQ_NUM;

        if (possDupFlag != null && possDupFlag)
            msgSeqNum = -msgSeqNum; // negative result marks duplicate
        return msgSeqNum;
    }

}
