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
import org.f1x.api.message.fields.FixTags;
import org.f1x.io.parsers.SimpleMessageScanner;

/**
 * Extracts Session identity from byte array containing inbound/outbound message
 */
public class SessionIDParser {

    private static final SessionIDScanner SESSION_ID_SCANNER = new SessionIDScanner();
    private static final SessionIDScanner OPPOSITE_SESSION_ID_SCANNER = new OppositeSessionIDParser();

    private SessionIDParser() {
        // hide
    }

    public static int parse(byte[] buffer, int offset, int length, SessionIDByteReferences sessionID) throws SimpleMessageScanner.MessageFormatException {
        return parse(buffer, offset, length, sessionID, SESSION_ID_SCANNER);
    }

    public static int parseOpposite(byte[] buffer, int offset, int length, SessionIDByteReferences sessionID) throws SimpleMessageScanner.MessageFormatException {
        return parse(buffer, offset, length, sessionID, OPPOSITE_SESSION_ID_SCANNER);
    }

    private static int parse(byte[] buffer, int offset, int length, SessionIDByteReferences sessionID, SessionIDScanner scanner) throws SimpleMessageScanner.MessageFormatException {
        return scanner.parse(buffer, offset, length, sessionID);
    }

    private static class SessionIDScanner extends SimpleMessageScanner<SessionIDByteReferences> {

        @Override
        protected boolean onTagNumber(int tag, SessionIDByteReferences sessionID) throws FixParserException {
            return (tag == FixTags.SenderCompID) || (tag == FixTags.SenderSubID) ||
                    (tag == FixTags.TargetCompID) || (tag == FixTags.TargetSubID);
        }

        @Override
        protected boolean onTagValue(int tag, byte[] message, int valueStart, int valueLength, SessionIDByteReferences sessionID) throws FixParserException {
            switch (tag) {
                case FixTags.SenderCompID:
                    sessionID.setSenderCompId(message, valueStart, valueLength);
                    break;
                case FixTags.SenderSubID:
                    sessionID.setSenderSubId(message, valueStart, valueLength);
                    break;
                case FixTags.TargetCompID:
                    sessionID.setTargetCompId(message, valueStart, valueLength);
                    break;
                case FixTags.TargetSubID:
                    sessionID.setTargetSubId(message, valueStart, valueLength);
                    break;
            }

            return true;
        }
    }

    private static class OppositeSessionIDParser extends SessionIDScanner {

        @Override
        protected boolean onTagValue(int tag, byte[] message, int valueStart, int valueLength, SessionIDByteReferences sessionID) throws FixParserException {
            switch (tag) {
                case FixTags.SenderCompID:
                    sessionID.setTargetCompId(message, valueStart, valueLength);
                    break;
                case FixTags.SenderSubID:
                    sessionID.setTargetSubId(message, valueStart, valueLength);
                    break;
                case FixTags.TargetCompID:
                    sessionID.setSenderCompId(message, valueStart, valueLength);
                    break;
                case FixTags.TargetSubID:
                    sessionID.setSenderSubId(message, valueStart, valueLength);
                    break;
            }

            return true;
        }

    }

}
