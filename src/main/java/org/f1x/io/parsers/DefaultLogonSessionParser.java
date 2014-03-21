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
import org.f1x.api.session.SessionID;

/**
 * Extracts Session identity from byte array containing message
 */
public class DefaultLogonSessionParser implements LogonSessionParser {
    private static final int MAX_COMPONENT_ID_LENGTH = 64;

    private static final int SENDER_COMP_ID = 49;
    private static final int SENDER_SUB_ID = 50;
    private static final int TARGET_COMP_ID = 56;
    private static final int TARGET_SUB_ID = 57;

    private final SimpleMessageScanner<SessionIDByteSequences> scanner;

    public DefaultLogonSessionParser() {
        this.scanner = new SimpleMessageScanner<SessionIDByteSequences>() {

            @Override
            protected boolean onTagNumber(int tagNum, SessionIDByteSequences sessionID) throws FixParserException {
                return (tagNum == SENDER_COMP_ID) || (tagNum == SENDER_SUB_ID) ||
                       (tagNum == TARGET_COMP_ID) || (tagNum == TARGET_SUB_ID);
            }

            @Override
            protected boolean onTagValue(int tagNum, byte[] message, int tagValueStart, int tagValueLen, SessionIDByteSequences sessionID) throws FixParserException {
                switch (tagNum) {
                    case SENDER_COMP_ID: sessionID.setSenderCompId (message, tagValueStart, tagValueLen); break;
                    case SENDER_SUB_ID:  sessionID.setSenderSubId(message, tagValueStart, tagValueLen); break;
                    case TARGET_COMP_ID: sessionID.setTargetCompId(message, tagValueStart, tagValueLen); break;
                    case TARGET_SUB_ID:  sessionID.setTargetSubId(message, tagValueStart, tagValueLen); break;
                }
                return true;
            }
        };
    }

    @Override
    public SessionID parse(byte[] buffer, int offset, int length) throws FixParserException {
        SessionIDByteSequences sessionID = new SessionIDByteSequences(MAX_COMPONENT_ID_LENGTH); //TODO: Pool or ThreadLocal
        scanner.parse(buffer, offset, length, sessionID);
        return sessionID;
    }
}
