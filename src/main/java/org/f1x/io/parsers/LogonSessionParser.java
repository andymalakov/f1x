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
import org.f1x.api.SessionID;

/**
 * Extract FIX session identity from byte buffer presumably containing FIX LOGON message.
 */
public interface LogonSessionParser {
    /**
     * @param buffer byte array containing message to parse
     * @param offset defines message start offset
     * @param length useful length of the buffer (not length of the message!)
     * @return SessionID extracted from the first message, never null.
     */
    SessionID parse(byte[] buffer, int offset, int length) throws FixParserException;
}
