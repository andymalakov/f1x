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

import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.FixVersion;
import org.f1x.api.SessionID;
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
public abstract class FixCommunicator implements Runnable {
    private static final int MIN_FIX_MESSAGE_LENGTH = 24; // approx
    private static final int CHECKSUM_LENGTH = 7; // length("10=123|") --check sum always expressed using 3 digits

// Moved to applications
//    static {
//        try {
//            org.gflogger.config.xml.XmlLogFactoryConfigurator.configure("/config/gflogger.xml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    protected static final GFLog LOGGER = GFLogFactory.getLog(FixCommunicator.class);

    private final SessionEventListener eventListener;
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
    private final SequenceNumbers seqNum = new SequenceNumbers();

    // used by receiver thread only
    private final DefaultMessageParser parser = new DefaultMessageParser();
    private final byte [] inboundMessageBuffer;
    private final ByteArrayReference msgType = new ByteArrayReference();
    private final byte [] beginString;

    // Used by senders
    private final MessageBuilder sessionMessageBuilder;
    private final RawMessageAssembler messageAssembler;


    public FixCommunicator (FixVersion fixVersion, FixSettings settings, SessionEventListener eventListener) {
        this.settings = settings;
        this.eventListener = eventListener;

        this.logInboundMessages = settings.isLogInboundMessages();
        this.logOutboundMessages = settings.isLogOutboundMessages();
        if (logInboundMessages || logOutboundMessages)
            messageLogFactory = new FileMessageLogFactory(settings.getLogDirectory());
        else
            messageLogFactory = null;

        this.beginString = AsciiUtils.getBytes(fixVersion.getBeginString());

        sessionMessageBuilder = new ByteBufferMessageBuilder(settings.getMaxOutboundMessageSize(), settings.getDoubleFormatterPrecision());
        messageAssembler = new RawMessageAssembler(fixVersion, settings.getMaxOutboundMessageSize(), RealTimeSource.INSTANCE);
        inboundMessageBuffer = new byte [settings.getMaxInboundMessageSize()];
    }

    public abstract SessionID getSessionID();

    protected FixSettings getSettings() {
        return settings;
    }

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
        LOGGER.info().append("FIX Connection changed state ").append(oldState).append(" => ").append(newState).commit();
        if (eventListener != null)
            eventListener.onStateChanged(getSessionID(), oldState, newState);
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
            while (active) {
                int bytesRead = in.read(inboundMessageBuffer, offset, inboundMessageBuffer.length - offset);
                if (bytesRead <= 0) {
                    throw ConnectionProblemException.NO_SOCKET_DATA;
                } else {
                    offset = processInboundMessages(offset+bytesRead);
                }
            }
            LOGGER.error().append("Finishing FIX session").commit();
        } catch (InvalidFixMessageException e) {
            LOGGER.error().append("Protocol error").append(e).commit();
            disconnect("Protocol Error");
        } catch (SocketException e) {
            LOGGER.error().append("Socket error").append(e).commit();
            disconnect("Socket Error");
        } catch (Exception e) {
            LOGGER.error().append("General error").append(e).commit();
            disconnect("General Error");
        }

        assertSessionState(SessionState.Disconnected);
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

    public void send(MessageBuilder messageBuilder) throws IOException {
        send(messageBuilder, seqNum.consumeOutbound()); //TODO: Not thread safe!!!
    }

    private void send(MessageBuilder messageBuilder, int msgSeqNum) throws IOException {
        messageAssembler.send(getSessionID(), msgSeqNum, messageBuilder, out); //TODO: Need critical section for messageAssembler
    }

    protected void sendLogon(boolean resetSequenceNumbers) throws IOException {
        if (resetSequenceNumbers) {
            seqNum.reset();
        }

        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGON);
            sessionMessageBuilder.add(FixTags.EncryptMethod, 0); //0=NONE
            sessionMessageBuilder.add(FixTags.HeartBtInt, 30);
            sessionMessageBuilder.add(FixTags.ResetSeqNumFlag, resetSequenceNumbers);
            send(sessionMessageBuilder);
        }
    }

    protected void sendLogout(String cause) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.LOGOUT);
            if (cause != null)
                sessionMessageBuilder.add(FixTags.Text, cause);
            send(sessionMessageBuilder);
        }
        setSessionState(SessionState.InitiatedLogout);
    }

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

    protected void sendReject(int rejectedMsgSeqNum, CharSequence cause) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.REJECT);
            sessionMessageBuilder.add(FixTags.RefSeqNum, rejectedMsgSeqNum);

            if (cause!= null)
                sessionMessageBuilder.add(FixTags.Text, cause);
            send(sessionMessageBuilder);
        }
    }

    protected void sendGapFill(int beginSeqNo, int endSeqNo) throws IOException {
        assertSessionState (SessionState.ApplicationConnected);
        synchronized (sessionMessageBuilder) {
            sessionMessageBuilder.clear();
            sessionMessageBuilder.setMessageType(MsgType.RESEND_REQUEST);

            if (endSeqNo == 0)
                endSeqNo = seqNum.consumeOutbound();

            sessionMessageBuilder.add(FixTags.NewSeqNo, endSeqNo);
            sessionMessageBuilder.add(FixTags.GapFillFlag, true);
            send(sessionMessageBuilder, beginSeqNo);
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
        if (getSessionState() == SessionState.ApplicationConnected) {
            boolean processed = true;
            if (msgType.length() == 1) { // All session-level messages have MsgType expressed using single char
                switch (msgType.charAt(0)) {
                    case AdminMessageTypes.LOGON:
                        processInboundLogon(parser); break;
                    case AdminMessageTypes.LOGOUT:
                        processInboundLogout(parser); break;
                    case AdminMessageTypes.HEARTBEAT:
                        LOGGER.info().append("Received hearbeat from other party").commit();
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

        } else {
            if (msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGON) {
                processInboundLogon(parser);
            } else {
                throw InvalidFixMessageException.LOGON_INCOMPLETE;
            }
        }

    }

    protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
//        if (msgType == 'B')
//            processInboundNews(parser);
//        else
        LOGGER.info().append("Received 35=").append(msgType).commit();
    }

    /** Handle inbound LOGON message depending on FIX session role (acceptor/initator) and current state */
    protected abstract void processInboundLogon(MessageParser parser) throws IOException;

    @SuppressWarnings("unused")
    protected void processInboundLogout(MessageParser parser) throws IOException {
        if (state == SessionState.ApplicationConnected) {
            sendLogout("Responding to LOGOUT request");
            setSessionState(SessionState.SocketConnected);
        } else if (state == SessionState.InitiatedLogout) {
            setSessionState(SessionState.SocketConnected);
        } else {
            LOGGER.info().append("Unexpected LOGOUT").commit();
        }
    }

    protected void processInboundTestRequest(MessageParser parser) throws IOException {
        while (parser.next()) {
            if (parser.getTagNum() == FixTags.TestReqID) { // required
                sendHeartbeat(parser.getCharSequenceValue());
                break;
            }
        }
    }

    protected void processInboundNews(MessageParser parser) {
        while (parser.next()) {
            if (parser.getTagNum() == FixTags.Text) { // required
                LOGGER.info().append("Received NEWS: ").append(parser.getCharSequenceValue()).commit();
                break;
            }
        }
    }

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
            }
        }
        if (beginSeqNo == -1) {
            sendReject(msgSeqNum, "Missing BeginSeqNo(7)");
            return;
        }
        if (beginSeqNo == 0) {
            sendReject(msgSeqNum, "Invalid BeginSeqNo(7)");
            return;
        }
        if (endSeqNo == -1) {
            sendReject(msgSeqNum, "Missing EndSeqNo(16)");
            return;
        }
        sendGapFill (beginSeqNo, endSeqNo);
    }


    private void processInboundSequenceReset(MessageParser parser) throws IOException {
//        boolean isGapFill = false;
        int newSeqNum = -1;
        int msgSeqNum = -1;
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.NewSeqNo:  // required
                    newSeqNum = parser.getIntValue();
                    break;
//                case FixTags.GapFillFlag:
//                    isGapFill = parser.getBooleanValue();
//                    break;
                case FixTags.MsgSeqNum:  // required
                    msgSeqNum = parser.getIntValue();
                    break;
            }
        }
        try {
            seqNum.resetInbound(newSeqNum);
            LOGGER.info().append("Processed inbound message sequence reset to ").append(newSeqNum).commit();
        } catch (InvalidFixMessageException e) {
            sendReject(msgSeqNum, e.getMessage());
        }
    }

    protected void processInboundReject(MessageParser parser) {
        int refSeqNum = -1;
        ByteArrayReference text = new ByteArrayReference(); //ALLOC
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case FixTags.RefSeqNum: refSeqNum = parser.getIntValue(); break;
                case FixTags.Text:      parser.getByteSequence(text);    break;
            }
        }
        if (text.length() != 0)
            LOGGER.warn().append("Received session-level Reject:").append(refSeqNum).append(": ").append(text).commit();
        else
            LOGGER.warn().append("Received session-level Reject:").append(refSeqNum).commit();
    }

}
