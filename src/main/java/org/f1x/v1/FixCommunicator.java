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

import org.f1x.api.FixSettings;
import org.f1x.api.message.fields.SessionRejectReason;
import org.f1x.api.session.FixSession;
import org.f1x.api.message.fields.EncryptMethod;
import org.f1x.util.TimeSource;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.FixTags;
import org.f1x.api.message.fields.MsgType;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionState;
import org.f1x.io.InputChannel;
import org.f1x.io.LoggingOutputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.log.MessageLog;
import org.f1x.log.MessageLogFactory;
import org.f1x.log.file.FileMessageLogFactory;
import org.f1x.tools.AdminMessageTypes;
import org.f1x.util.AsciiUtils;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.RealTimeSource;

import java.io.IOException;
import java.net.SocketException;

/**
 * Networking common for FIX Acceptor and FIX Initiator
 */
public abstract class FixCommunicator implements FixSession {
    private static final int MIN_FIX_MESSAGE_LENGTH = 24; // approx
    private static final int CHECKSUM_LENGTH = 7; // length("10=123|") --check sum always expressed using 3 digits


    protected static final GFLog LOGGER = GFLogFactory.getLog(FixCommunicator.class);

    private SessionEventListener eventListener;

    private final FixSettings settings;
    private final boolean logInboundMessages;
    private final boolean logOutboundMessages;
    private final MessageLogFactory messageLogFactory;
    private MessageLog messageLog;


    // Defined during initialization
    private InputChannel in;
    private OutputChannel out;


    private SessionState state = SessionState.Disconnected;
    protected volatile boolean active = true; // close() sets this to false
    private final SequenceNumbers seqNum = new SequenceNumbers(); // hidden for thread-safety reasons

    // used by receiver thread only
    private final DefaultMessageParser parser = new DefaultMessageParser();
    private final byte [] inboundMessageBuffer;
    private final ByteArrayReference msgType = new ByteArrayReference();
    private final byte [] beginString;

    // Used by senders
    private final MessageBuilder sessionMessageBuilder;
    private final RawMessageAssembler messageAssembler;

    public FixCommunicator (FixVersion fixVersion, FixSettings settings) {
        this(fixVersion, settings, RealTimeSource.INSTANCE);
    }

    protected FixCommunicator (FixVersion fixVersion, FixSettings settings, TimeSource timeSource) {
        this.settings = settings;

        this.logInboundMessages = settings.isLogInboundMessages();
        this.logOutboundMessages = settings.isLogOutboundMessages();
        if (logInboundMessages || logOutboundMessages)
            messageLogFactory = new FileMessageLogFactory(settings.getLogDirectory());
        else
            messageLogFactory = null;

        this.beginString = AsciiUtils.getBytes(fixVersion.getBeginString());

        sessionMessageBuilder = new ByteBufferMessageBuilder(settings.getMaxOutboundMessageSize(), settings.getDoubleFormatterPrecision());
        messageAssembler = new RawMessageAssembler(fixVersion, settings.getMaxOutboundMessageSize(), timeSource);
        inboundMessageBuffer = new byte [settings.getMaxInboundMessageSize()];
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
    public SessionState getSessionState() {
        return state;
    }

    protected void setSessionState(SessionState state) {
        final SessionState oldState = this.state;
        if (oldState == state) {
            LOGGER.warn().append("Already in the state ").append(state).commit();
        } else {
            this.state = state;
            onSessionStateChanged(oldState, state);
        }
    }

    protected void onSessionStateChanged(final SessionState oldState, final SessionState newState) {
        SessionID sessionID = getSessionID();
        LOGGER.info().append("Session ").append(sessionID).append(" changed state ").append(oldState).append(" => ").append(newState).commit();
        if (eventListener != null)
            eventListener.onStateChanged(sessionID, oldState, newState);
    }

    protected final void assertSessionState(SessionState expectedState) {
        final SessionState actualState = getSessionState();
        if (actualState != expectedState)
            throw new IllegalStateException("Expecting " + expectedState + " state instead of " + actualState);
    }


    public void connect(InputChannel in, OutputChannel out) {
        this.messageLog = (messageLogFactory != null) ? messageLogFactory.create(getSessionID()) : null;

        this.in = in;
        this.out = (logOutboundMessages) ? new LoggingOutputChannel(messageLog, out) : out;
        //TODO:this.seqNum.read(sessionID);
    }

    /** Process inbound messages until session ends */
    protected final void processInboundMessages() {
        LOGGER.info().append("Processing FIX Session").commit();
        try {
            int offset = 0;
            while (active) { ///prevents logged out session from re-connect
                int bytesRead = in.read(inboundMessageBuffer, offset, inboundMessageBuffer.length - offset);
                if (bytesRead <= 0) {
                    throw ConnectionProblemException.NO_SOCKET_DATA;
                } else {
                    offset = processInboundMessages(offset+bytesRead);
                }
            }
            LOGGER.error().append("Finishing FIX session").commit();
        } catch (InvalidFixMessageException e) {
            errorProcessingMessage("Protocol Error", e, false);
        } catch (ConnectionProblemException e) {
            errorProcessingMessage("Connection Problem", e, false);
        } catch (SocketException e) {
            errorProcessingMessage("Socket Error (Other side disconnected?)", e, false);
        } catch (Exception e) {
            errorProcessingMessage("General error", e, true);
        }

        assertSessionState(SessionState.Disconnected);
    }

    private void errorProcessingMessage(String errorText, Exception e, boolean logStackTrace) {
        if (active) {
            if (logStackTrace)
                LOGGER.error().append(errorText).append(" : ").append(e).commit();
            else
                LOGGER.error().append(errorText).append(" : ").append(e.getMessage()).commit();
            disconnect(errorText);
        }
    }


    protected int processInboundMessages(int bytesRead) throws IOException, InvalidFixMessageException {
        assert bytesRead > 0;
        int offset = 0;
        while (bytesRead - offset >= MIN_FIX_MESSAGE_LENGTH) {

            parser.set(inboundMessageBuffer, offset, bytesRead - offset);

            // All FIX messages begin with 3 required tags: BeginString, BodyLength, and MsgType.
            parseBeginString();
            final int messageLength = parseBodyLength() + CHECKSUM_LENGTH; // BodyLength is the number of characters in the message following the BodyLength field up to, and including, the delimiter immediately preceding the CheckSum tag ("10=123|")
            if (messageLength > inboundMessageBuffer.length)
                throw InvalidFixMessageException.MESSAGE_TOO_LARGE;

            final int msgTypeStarts = parser.getOffset();
            if (msgTypeStarts + messageLength > bytesRead)
                break; // retry after we read full message in the buffer


            //TODO:parseMsgSeqNum();

            // set parser limit to consume single message
            parser.set(inboundMessageBuffer, msgTypeStarts, messageLength);

            if ( ! parser.next())
                throw InvalidFixMessageException.MISSING_MSG_TYPE;

            if (logInboundMessages)
                messageLog.logInbound(inboundMessageBuffer, offset, messageLength + msgTypeStarts-offset);


            parser.getByteSequence(msgType);
            processInboundMessage(parser, msgType);

            offset = msgTypeStarts + messageLength;
        }

        // Move remaining part at the beginning of buffer
        int remainingSize = bytesRead - offset;
        if (remainingSize > 0 && offset != 0)
            System.arraycopy(inboundMessageBuffer, offset, inboundMessageBuffer, 0, remainingSize);
        return remainingSize;
    }

    /** Send LOGOUT but do not drop socket connection */
    @Override
    public void logout(String cause) {
        LOGGER.info().append("Initiating FIX Logout: ").append(cause).commit();

        if (state == SessionState.ApplicationConnected) {
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

        setSessionState(SessionState.Disconnected);
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
        if (state == SessionState.ApplicationConnected) {
            try {
                sendLogout("Goodbye");
            } catch (IOException e) {
                LOGGER.warn().append("Error logging out from FIX session: ").append(e).commit();
            }
        }

        if (state != SessionState.Disconnected)
            disconnect("Closing");
    }

    @Override
    public void send(MessageBuilder messageBuilder) throws IOException {
        send(messageBuilder, seqNum.consumeOutbound()); //TODO: Not thread safe!!!
    }

    private void send(MessageBuilder messageBuilder, int msgSeqNum) throws IOException {
        synchronized (messageAssembler) {
            messageAssembler.send(getSessionID(), msgSeqNum, messageBuilder, out);
        }
    }

    protected void sendLogon(boolean resetSequenceNumbers) throws IOException {
        if ( ! resetSequenceNumbers)
            resetSequenceNumbers = settings.isResetSequenceNumbersOnEachLogon();

        if (resetSequenceNumbers) {
            seqNum.reset(); //TODO: Not-thread-safe: may happen in parallel with another send()
        }

        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGON);
            sessionMessageBuilder.add(FixTags.EncryptMethod, EncryptMethod.NONE_OTHER);
            sessionMessageBuilder.add(FixTags.HeartBtInt, settings.getHeartBeatIntervalSec());
            sessionMessageBuilder.add(FixTags.ResetSeqNumFlag, resetSequenceNumbers);

            if (settings.isLogonWithNextExpectedMsgSeqNum()) {
                sessionMessageBuilder.add(FixTags.NextExpectedMsgSeqNum, seqNum.getNextInbound());
            }
            sessionMessageBuilder.add(FixTags.MaxMessageSize, settings.getMaxInboundMessageSize());
            send(sessionMessageBuilder);
        }
    }

    protected void sendLogout(CharSequence cause) throws IOException {
        assertSessionState(SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGOUT);
            if (cause != null)
                sessionMessageBuilder.add(FixTags.Text, cause);
            send(sessionMessageBuilder);
        }
        setSessionState(SessionState.InitiatedLogout);
    }

    /**
     * Sends FIX Heartbeat(0) message
     * @param testReqId required when heartbeat is sent in response to TestRequest(1)
     */
    protected void sendHeartbeat(CharSequence testReqId) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
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
        assertSessionState (SessionState.ApplicationConnected);
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
        assertSessionState(SessionState.ApplicationConnected);
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
    protected void sendResendReq(int beginSeqNo, int endSeqNo) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.RESEND_REQUEST);

            sessionMessageBuilder.add(FixTags.BeginSeqNo, beginSeqNo);
            sessionMessageBuilder.add(FixTags.EndSeqNo, endSeqNo);
            send(sessionMessageBuilder);
        }
    }

    /**
     * Sends SequenceReset(4) in response to ResendRequest when
     * resending a range of administrative messages or when resending actual application messages is not appropriate (e.g. stale messages).
     *
     * @param beginSeqNo first message of the range (will be used as MsgSeqNum)
     * @param endSeqNo end of range (pass 0 to resend up to the latest)
     */
    protected void sendGapFill(int beginSeqNo, int endSeqNo) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.SEQUENCE_RESET);

            if (endSeqNo == 0)
                endSeqNo = seqNum.consumeOutbound();

            sessionMessageBuilder.add(FixTags.PossDupFlag, true);
            sessionMessageBuilder.add(FixTags.NewSeqNo, endSeqNo);
            sessionMessageBuilder.add(FixTags.GapFillFlag, true);
            send(sessionMessageBuilder, beginSeqNo);

            //TODO: update seqNum?
        }
    }

    /**
     * Sends SequenceReset(4) in response to ResendRequest when
     * resending a range of administrative messages or when resending actual application messages is not appropriate (e.g. stale messages).
     *
     * @param newSeqNo new sequence number
     */
    protected void sendSequenceReset(int newSeqNo) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.SEQUENCE_RESET);


            //TODO: Confirm sessionMessageBuilder.add(FixTags.PossDupFlag, true);
            sessionMessageBuilder.add(FixTags.NewSeqNo, newSeqNo);
            sessionMessageBuilder.add(FixTags.GapFillFlag, false);
            send(sessionMessageBuilder, newSeqNo); // In reset mode MsgSeqNum should be ignored
        }
    }

    private void parseBeginString() throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.EMPTY_MESSAGE;

        if (parser.getTagNum() != FixTags.BeginString)
            throw InvalidFixMessageException.BAD_FIRST_TAG;

        if (this.beginString != null && ! parser.isValueEquals(this.beginString))
            throw InvalidFixMessageException.INVALID_BEGIN_STRING;
    }

    private int parseBodyLength() throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        if (parser.getTagNum() != FixTags.BodyLength)
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        int bodyLength = parser.getIntValue();
        if (bodyLength <= 0)
            throw InvalidFixMessageException.BAD_BODY_LENGTH;
        return bodyLength;
    }

    protected void processInboundMessage(MessageParser parser, CharSequence msgType) throws IOException, InvalidFixMessageException {
        switch (getSessionState()) {
            case ApplicationConnected:
            case InitiatedLogout:
                processInSessionMessage(parser, msgType);
                break;
            case SocketConnected:
            case InitiatedLogon:
                if (msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGON) {
                    processInboundLogon(parser);
                } else {
                    throw InvalidFixMessageException.EXPECTING_LOGON_MESSAGE;
                }
                break;
            default:
                LOGGER.warn().append("Received unexpected message (35=").append(msgType).append(") in state ").append(getSessionState()).commit();
                //TODO: sendReject();
        }

    }

    private void processInSessionMessage(MessageParser parser, CharSequence msgType) throws IOException {
        boolean processed = true;
        if (msgType.length() == 1) { // All session-level messages have MsgType expressed using single char
            switch (msgType.charAt(0)) {
                case AdminMessageTypes.LOGON:
                    processInboundLogon(parser); break;
                case AdminMessageTypes.LOGOUT:
                    processInboundLogout(parser); break;
                case AdminMessageTypes.HEARTBEAT:
                    LOGGER.debug().append("Received hearbeat from other party").commit();
                    break; //TODO: Handle heartbeats later
                case AdminMessageTypes.TEST:
                    processInboundTestRequest(parser); break;
                case AdminMessageTypes.RESEND:
                    processInboundResendRequest(parser); break;
                case AdminMessageTypes.REJECT:
                    processInboundReject(parser); break;
                case AdminMessageTypes.RESET:
                    processInboundSequenceReset(parser); break;
                default:
                    processed = false;
            }
        } else {
            processed = false;
        }
        if ( ! processed)
            processInboundAppMessage(msgType, parser);
    }

    protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
        // by default do nothing
    }

    /**
     * Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current state
     */
    protected void processInboundLogon(MessageParser parser) throws IOException {
        int heartbeatInterval = -1; //TODO: Acceptor ensures interval and sends it back with its LOGON
        int nextExpectedMsgSeqNum = -1;
        boolean isSequenceNumberReset = false;
        while (parser.next()) {

            if (parser.getTagNum() == FixTags.HeartBtInt) {
                heartbeatInterval = parser.getIntValue();
            } else
            if (parser.getTagNum() == FixTags.NextExpectedMsgSeqNum) {
                nextExpectedMsgSeqNum = parser.getIntValue();
            } else
            if (parser.getTagNum() == FixTags.ResetSeqNumFlag) {
                isSequenceNumberReset = parser.getBooleanValue();
            } else
            if (parser.getTagNum() == FixTags.PossDupFlag) { // must be part of header
                if (parser.getBooleanValue())
                    return; // ignore retransmitted LOGON
            }
        }

//TODO: Actually responder may mimic inbound logon here, need smarter logic
//        if (isSequenceNumberReset) { // inbound on any side may request sequence reset
//            if (getSessionState() != SessionState.InitiatedLogon && ! settings.isResetSequenceNumbersOnEachLogon())
//                seqNum.reset();
//        }

//TODO
//        if (heartbeatInterval != -1 && heartbeatInterval != settings.getHeartBeatIntervalSec())
//            throw ConnectionProblemException.HEARTBEAT_INTERVAL_MISMATCH;

        //TODO: Validate that the connection was established with the correct party (SessionID matching)
//        if (nextExpectedMsgSeqNum > 0) {
//            //TODO:
//            if (seqNum.getNextOutbound() < nextExpectedMsgSeqNum)
//                drop();
//            else
//                resend?
//        }
        processInboundLogon();
    }

    /** Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current state */
    protected abstract void processInboundLogon() throws IOException;

    @SuppressWarnings("unused")
    protected void processInboundLogout(MessageParser parser) throws IOException {
        while (parser.next()) {
            if (parser.getTagNum() == FixTags.PossDupFlag) {
                if (parser.getBooleanValue())
                    return; // ignore retransmitted LOGOUT
            } else
            if (parser.getTagNum() == FixTags.Text) {
                LOGGER.info().append("LOGOUT: ").append(parser.getCharSequenceValue()).commit();
            }
        }

        if (state == SessionState.ApplicationConnected) {
            sendLogout("Responding to LOGOUT request");
            setSessionState(SessionState.SocketConnected);
        } else if (state == SessionState.InitiatedLogout) {
            disconnect("Logout received");
        } else {
            LOGGER.info().append("Unexpected LOGOUT").commit();
        }
    }

    protected void processInboundTestRequest(MessageParser parser) throws IOException {
        while (parser.next()) {
            if (parser.getTagNum() == FixTags.PossDupFlag) { // must be part of header
                if (parser.getBooleanValue())
                    return; // ignore retransmitted TEST
            } else
            if (parser.getTagNum() == FixTags.TestReqID) { // required
                sendHeartbeat(parser.getCharSequenceValue());
                break;
            }
        }
    }

//    protected void processInboundNews(MessageParser parser) {
//        while (parser.next()) {
//            if (parser.getTagNum() == FixTags.Text) { // required
//                LOGGER.info().append("NEWS: ").append(parser.getCharSequenceValue()).commit();
//                break;
//            }
//        }
//    }

    private void processInboundResendRequest(MessageParser parser) throws IOException {
        int msgSeqNum = -1;
        int beginSeqNo = -1;
        int endSeqNo = -1;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.MsgSeqNum:  // required
                    msgSeqNum = parser.getIntValue();
                    break;
                case FixTags.BeginSeqNo:  // required
                    beginSeqNo = parser.getIntValue();
                    break;
                case FixTags.EndSeqNo:  // required
                    endSeqNo = parser.getIntValue();
                    break;
                case FixTags.PossDupFlag: // All FIX implementations must monitor incoming messages to detect inadvertently retransmitted administrative messages (PossDupFlag flag set indicating a resend).
                    if (parser.getBooleanValue())
                        return; // ignore retransmitted LOGON
                    break;
            }
        }

        if (beginSeqNo == -1) {
            sendReject(msgSeqNum, SessionRejectReason.REQUIRED_TAG_MISSING, "Missing BeginSeqNo(7)");
            return;
        }
        if (beginSeqNo == 0) {
            sendReject(msgSeqNum, SessionRejectReason.VALUE_IS_INCORRECT, "Invalid BeginSeqNo(7)");
            return;
        }
        if (endSeqNo == -1) {
            sendReject(msgSeqNum, SessionRejectReason.REQUIRED_TAG_MISSING, "Missing EndSeqNo(16)");
            return;
        }
        sendGapFill (beginSeqNo, endSeqNo); //TODO: Temporary until we have MessageStore
    }


    private void processInboundSequenceReset(MessageParser parser) throws IOException {
        boolean isGapFill = false;
        int newSeqNum = -1;
        int msgSeqNum = -1;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.NewSeqNo:  // required
                    newSeqNum = parser.getIntValue();
                    break;
                case FixTags.GapFillFlag:
                    isGapFill = parser.getBooleanValue();
                    break;
                case FixTags.MsgSeqNum:  // required
                    msgSeqNum = parser.getIntValue();
                    break;
            }
        }
        LOGGER.info().append("Processing inbound message sequence reset to ").append(newSeqNum).commit();
        try {
            if (isGapFill) {
                // Gap Fill mode: nothing to do since we are not placing any 'future' messages in the queue
            } else {
                // Reset mode
                seqNum.resetInbound(newSeqNum);
            }
        } catch (InvalidFixMessageException e) {
            sendReject(msgSeqNum, SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE, e.getMessage());
        }
    }

    protected void processInboundReject(MessageParser parser) {
        int refSeqNum = -1;
        ByteArrayReference text = new ByteArrayReference(); //ALLOC
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.RefSeqNum: refSeqNum = parser.getIntValue(); break;
                case FixTags.Text:      parser.getByteSequence(text);     break;
            }
        }
        if (text.length() != 0)
            LOGGER.warn().append("Received session-level Reject:").append(refSeqNum).append(": ").append(text).commit();
        else
            LOGGER.warn().append("Received session-level Reject:").append(refSeqNum).commit();
    }

}
