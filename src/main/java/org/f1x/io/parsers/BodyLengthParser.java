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

import org.f1x.util.AsciiUtils;
import org.f1x.util.ByteRingReader;

public class BodyLengthParser {

    private static final int CHECKSUM_FIELD_LENGTH = 7; // Checksum(10) is always expressed using 3 digits and includes terminating SOH byte: "10=123|"
    private static final byte SOH = (byte) 1; // FIX field separator
    private static final byte WILDCARD = '*';
    private static final byte [] EXPECTED_HEADER = AsciiUtils.getBytes("8=FIX.*.*\0019=");
    private static final int EXPECTED_HEADER_LENGTH = EXPECTED_HEADER.length;


    /** @return size of message remaining to read  */
    public static int getRemainingMessageSize(ByteRingReader reader) {
        validateMessagePrefix (reader);  // cost ~20 nanos
        int bodyLength = parseBodyLength(reader);
        return (bodyLength + CHECKSUM_FIELD_LENGTH) - reader.getRemainingLength(); // reader.getRemainingLength() is positioned at the first byte after SOH separator that follows BodyLength
    }

    /** Ensures that message starts with BeginString and BodyLength tags */
    static void validateMessagePrefix (ByteRingReader reader) {
        for (int i=0; i < EXPECTED_HEADER_LENGTH; i++) {
            byte expectedByte = EXPECTED_HEADER[i];
            byte actualByte = reader.next();

            if (expectedByte != WILDCARD && expectedByte != actualByte)
                throw InvalidFixMessageException.UNEXPECTED_MESSAGE_START;
        }
    }

    /**
     * BodyLength is always the second field in the message.
     * Count the number of characters in the message following the BodyLength field up to, and including,
     * the delimiter immediately preceding the CheckSum tag ("10=").
     */
     static int parseBodyLength(ByteRingReader reader) {
        int result = 0;
        byte b = reader.next();
        if (b == SOH)
            throw InvalidFixMessageException.EMPTY_BODY_LENGTH_TAG;

        do {
            if (b >= '0' && b <= '9')
                result = 10*result + (b - '0');
            else
                throw InvalidFixMessageException.INVALID_BODY_LENGTH_TAG;
        } while ( (b = reader.next()) != SOH);

         return result;
    }

    private static final class InvalidFixMessageException extends IllegalArgumentException {
        /** Pre-allocated exception to avoid garbage generation */
        public static final InvalidFixMessageException INVALID_BODY_LENGTH_TAG = new InvalidFixMessageException("Message BodyLength is invalid");
        public static final InvalidFixMessageException EMPTY_BODY_LENGTH_TAG = new InvalidFixMessageException("Message BodyLength is empty");
        public static final InvalidFixMessageException UNEXPECTED_MESSAGE_START = new InvalidFixMessageException("Message must begin with BeginString and BodyLength tags");

        private InvalidFixMessageException(String message) { super(message); }

        @Override
        public Throwable fillInStackTrace()
        {
            return this;
        }
    }

}
