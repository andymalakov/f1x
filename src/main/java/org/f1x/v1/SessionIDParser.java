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
 * Extracts Session identity from byte array containing message
 */
public class SessionIDParser {

    private static final SimpleMessageScanner<SessionIDByteReferences> LOGON_MESSAGE_SCANNER = new SimpleMessageScanner<SessionIDByteReferences>() {

        @Override
        protected boolean onTagNumber(int tagNum, SessionIDByteReferences sessionID) throws FixParserException {
            return (tagNum == FixTags.SenderCompID) || (tagNum == FixTags.SenderSubID) ||
                    (tagNum == FixTags.TargetCompID) || (tagNum == FixTags.TargetSubID);
        }

        @Override
        protected boolean onTagValue(int tagNum, byte[] message, int tagValueStart, int tagValueLen, SessionIDByteReferences sessionID) throws FixParserException {
            switch (tagNum) {
                case FixTags.SenderCompID:
                    sessionID.setSenderCompId(message, tagValueStart, tagValueLen);
                    break;
                case FixTags.SenderSubID:
                    sessionID.setSenderSubId(message, tagValueStart, tagValueLen);
                    break;
                case FixTags.TargetCompID:
                    sessionID.setTargetCompId(message, tagValueStart, tagValueLen);
                    break;
                case FixTags.TargetSubID:
                    sessionID.setTargetSubId(message, tagValueStart, tagValueLen);
                    break;
            }
            return true;
        }
    };

    private SessionIDParser() {
        // hide
    }

    public static int parse(byte[] buffer, int offset, int length, SessionIDByteReferences sessionID) throws SimpleMessageScanner.MessageFormatException {
        return LOGON_MESSAGE_SCANNER.parse(buffer, offset, length, sessionID);
    }

}
