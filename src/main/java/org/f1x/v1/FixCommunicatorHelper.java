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

import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.FixTags;

import java.io.IOException;

class FixCommunicatorHelper {

    static void parseBeginString(MessageParser parser, byte [] beginString) throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.EMPTY_MESSAGE;

        if (parser.getTagNum() != FixTags.BeginString)
            throw InvalidFixMessageException.BAD_FIRST_TAG;

        if (beginString != null && ! parser.isValueEquals(beginString))
            throw InvalidFixMessageException.INVALID_BEGIN_STRING;
    }

    static int parseBodyLength(MessageParser parser) throws InvalidFixMessageException {
        if ( ! parser.next())
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        if (parser.getTagNum() != FixTags.BodyLength)
            throw InvalidFixMessageException.MISSING_BODY_LENGTH;

        int bodyLength = parser.getIntValue();
        if (bodyLength <= 0)
            throw InvalidFixMessageException.BAD_BODY_LENGTH;
        return bodyLength;
    }

    static void checkMessageLength(int messageLength, int maxMessageLength) throws InvalidFixMessageException {
        if(messageLength > maxMessageLength)
            throw InvalidFixMessageException.MESSAGE_TOO_LARGE;
    }

    static boolean isLogon(CharSequence msgType) {
        return msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGON;
    }

    static boolean isLogout(CharSequence msgType) {
        return msgType.length() == 1 && msgType.charAt(0) == AdminMessageTypes.LOGOUT;
    }

    /**
     * @return message sequence number in current message. Method returns negated result ( - MsgSeqNum) if this message has PossDupFlag(43) set to Y.
     * @throws  InvalidFixMessageException if message is missing message sequence number of it is invalid
     */
    static int findMsgSeqNum(MessageParser parser) throws InvalidFixMessageException {
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
