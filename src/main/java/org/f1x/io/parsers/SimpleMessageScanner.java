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

package org.f1x.io.parsers;

import org.f1x.api.FixParserException;
import org.f1x.util.AsciiUtils;

/**
 * Micro FIX parser. Thread-safe.
 *
 * Override {@link #onTagNumber(int, Object)} and {@link #onTagValue(int, byte[], int, int, Object)}  to get callback on each tag and value.
 */
public abstract class SimpleMessageScanner<Cookie> {

    public static final int MIN_MESSAGE_LENGTH = "8=FIX.X.Y|9=123456789|".length();
    private static final byte [] HEADER_START = AsciiUtils.getBytes("8=FIX.");
    private static final int HEADER_START_LENGTH = HEADER_START.length;
    private static final byte SOH = 1;
    private static final byte EQUALS = '=';
    private static final int BODY_LENGTH_TAG_NUM = 9;
    private static final int CHECKSUM_LENGTH = 8;  // including SOH

    /**
     * Tries to parse single FIX message in given byte buffer.
     *
     * @return Result indicates one of the following.
     *  Negative value indicates that supplied buffer does not contain whole message (in which case result is the number of missing bytes).
     *  Positive value indicates that a single message is completely parsed (in which case result is offset of the first byte after the message).
     */
    public int parse (byte [] buffer, int offset, int len, Cookie cookie) throws MessageFormatException {
        if (len < MIN_MESSAGE_LENGTH)
            throw MessageFormatException.BUFFER_IS_TOO_SMALL;

        if ( ! AsciiUtils.equals(HEADER_START, buffer, offset, HEADER_START_LENGTH))
            throw MessageFormatException.NOT_FIX_MESSAGE;

        int end = len + offset;

        // Skip FIX version
        offset += HEADER_START_LENGTH + 4; // X.X|

        int bodyLength = 0;
        int currentTagNum = 0;
        int currentTagValueStart = 0;
        boolean parsingTagNum = true;

        for (int i=offset; i < end; i++) {
            byte b = buffer[i];
            if (parsingTagNum) {
                if (b == EQUALS) {
                    if (onTagNumber(currentTagNum, cookie)) {
                        currentTagValueStart = i+1;
                    }

                    parsingTagNum = false;
                } else {
                    currentTagNum = 10*currentTagNum + parseDigit(b);
                }
            } else {
                if (b == SOH) {
                    if (currentTagValueStart > 0) {
                        if ( ! onTagValue(currentTagNum, buffer, currentTagValueStart, i - currentTagValueStart, cookie))
                            break;

                        currentTagValueStart = -1;
                    }
                    if (currentTagNum == BODY_LENGTH_TAG_NUM) {
                        // number of characters in the message following the BodyLength field up to, and including, the delimiter immediately preceding the CheckSum tag ("10=")
                        int messageBodyEnd = bodyLength + i + CHECKSUM_LENGTH;  // index of first byte beyond message end
                        if (end < messageBodyEnd)
                            return - (messageBodyEnd - end);
                        else
                            end = messageBodyEnd;
                    }
                    parsingTagNum = true;
                    currentTagNum = 0;
                } else {
                    if (currentTagNum == BODY_LENGTH_TAG_NUM) {
                        bodyLength = 10*bodyLength + parseDigit(b);
                    }
                }
            }
        }
        if (bodyLength == 0)
            throw MessageFormatException.MISSING_BODY_LEN;
        else
            return end;

    }

    /** @return true to process value of this tag (get onTagValue callaback) or false to skip this tag */
    protected abstract boolean onTagNumber(int tagNum, Cookie cookie) throws FixParserException;

    /** @return true to continue parsing message or false to skip the rest tags */
    protected abstract boolean onTagValue(int tagNum, byte [] message, int tagValueStart, int tagValueLen, Cookie cookie) throws FixParserException;

    private static int parseDigit(byte b) throws MessageFormatException {
        int digit = b - '0';
        if (digit < 0 || digit > 9)
            throw MessageFormatException.INVALID_NUMBER;
        return digit;
    }


    public static class MessageFormatException extends FixParserException {
        public static final MessageFormatException BUFFER_IS_TOO_SMALL = new MessageFormatException("Message is too small");
        public static final MessageFormatException INVALID_NUMBER = new MessageFormatException("Expecting a number");
        public static final MessageFormatException NOT_FIX_MESSAGE = new MessageFormatException("Not a FIX message");
        public static final MessageFormatException MISSING_BODY_LEN = new MessageFormatException("Missing BodyLength(9) tag");

        private MessageFormatException(String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace () {
            return null;
        }
    }
}
