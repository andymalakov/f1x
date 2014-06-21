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

import org.f1x.api.FixVersion;
import org.f1x.api.message.Tools;
import org.f1x.api.session.SessionID;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.fields.FixTags;
import org.f1x.io.OutputChannel;
import org.f1x.store.MessageStore;
import org.f1x.util.AsciiUtils;
import org.f1x.util.TimeSource;
import org.f1x.util.format.IntFormatter;
import org.f1x.util.format.TimestampFormatter;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;

/** Assembles message using header information and MessageBuilder. Not thread safe.
 * The following header fields are used:
 * <ul>
 * <li>BodyLength</li>
 * <li>MsgType</li>
 * <li>MsgSeqNum</li>
 * <li>SenderCompID,SenderSubID</li>
 * <li>TargetCompID,TargetSubID</li>
 * <li>SendingTime</li>
 * </ul>
 */
final class RawMessageAssembler {

    private final static byte SOH = 1;

    private final TimestampFormatter timestampFormatter = TimestampFormatter.createUTCTimestampFormatter();  //TODO Reuse instance kept by MessageBuilder?
    private final byte [] BEGIN_STRING;

    private final byte [] buffer;

    RawMessageAssembler(FixVersion version, int maxMessageSize) {
        buffer = new byte[maxMessageSize];

        BEGIN_STRING = AsciiUtils.getBytes("" + FixTags.BeginString + '=' + version.getBeginString() + (char) SOH);
        System.arraycopy(BEGIN_STRING, 0, buffer, 0, BEGIN_STRING.length);
    }

    void send(SessionID sessionID, int msgSeqNum, MessageBuilder messageBuilder, MessageStore messageStore, long sendingTime,  OutputChannel out) throws IOException {
        int offset = BEGIN_STRING.length;

        final CharSequence msgType = messageBuilder.getMessageType();
        final CharSequence senderSubId = sessionID.getSenderSubId();
        final CharSequence targetSubId = sessionID.getTargetSubId();

        // BodyLength is the number of characters in the message following the BodyLength field up to, and including, the delimiter immediately preceding the CheckSum tag ("10=")
        int bodyLength = (4 + msgType.length()) +
            (4 + IntFormatter.stringSize(msgSeqNum)) +
            (4 + TimestampFormatter.DATE_TIME_LENGTH) +
            (4 + sessionID.getSenderCompId().length()) +   //TODO: Precompute
            (4 + sessionID.getTargetCompId().length()) +
            messageBuilder.getLength();

        if (senderSubId != null)
            bodyLength += 4 + senderSubId.length();

        if (targetSubId != null)
            bodyLength += 4 + targetSubId.length();


        offset = setIntField(FixTags.BodyLength, bodyLength, buffer, offset);
        offset = setTextField(FixTags.MsgType, msgType, buffer, offset);
        offset = setIntField(FixTags.MsgSeqNum, msgSeqNum, buffer, offset);
        offset = setTextField(FixTags.SenderCompID, sessionID.getSenderCompId(), buffer, offset);
        if (senderSubId != null)
            offset = setTextField(FixTags.SenderSubID, senderSubId, buffer, offset);
        offset = setUtcTimestampField(FixTags.SendingTime, sendingTime, buffer, offset);
        offset = setTextField(FixTags.TargetCompID, sessionID.getTargetCompId(), buffer, offset);
        if (targetSubId != null)
            offset = setTextField(FixTags.TargetSubID, targetSubId, buffer, offset);

        offset = messageBuilder.output(buffer, offset);

        int checkSum = Tools.calcCheckSum(buffer, offset);  //TODO: Let  MessageBuilder accumulate payload checksum?
        offset = set3DigitIntField(FixTags.CheckSum, checkSum, buffer, offset);

        try {
            out.write(buffer, 0, offset);
        } finally {
            if (messageStore != null)
                messageStore.put(msgSeqNum, buffer, 0, offset);
        }
    }

    private static int setTextField(int tagNo, CharSequence value, byte [] buffer, int offset) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        for (int i=0; i < value.length(); i++)
            buffer[offset++] = (byte)value.charAt(i);

        buffer[offset++] = SOH;
        return offset;
    }

    private static int setIntField(int tagNo, int value, byte [] buffer, int offset) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = IntFormatter.format(value, buffer, offset);
        buffer[offset++] = SOH;
        return offset;
    }

    private static int set3DigitIntField(int tagNo, int value, byte [] buffer, int offset) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = IntFormatter.format3digits(value, buffer, offset);
        buffer[offset++] = SOH;
        return offset;
    }

    private int setUtcTimestampField(int tagNo, long value, byte[] buffer, int offset) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        //synchronized (timestampFormatter) {
            offset = timestampFormatter.formatDateTime(value, buffer, offset);
        //}
        buffer[offset++] = SOH;
        return offset;
    }


}
