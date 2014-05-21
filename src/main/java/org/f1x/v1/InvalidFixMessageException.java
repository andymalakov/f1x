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

public final class InvalidFixMessageException extends Exception {

    static final InvalidFixMessageException EMPTY_MESSAGE = new InvalidFixMessageException ("Message has no data");
    static final InvalidFixMessageException BAD_FIRST_TAG = new InvalidFixMessageException ("Message does not begin with BeginString(8) tag.");
    static final InvalidFixMessageException INVALID_BEGIN_STRING = new InvalidFixMessageException ("Message tag BeginString(8) has unexpected value. Wrong FIX version?");
    static final InvalidFixMessageException MISSING_BODY_LENGTH = new InvalidFixMessageException ("Message has BodyLength(9) tag");
    static final InvalidFixMessageException BAD_BODY_LENGTH = new InvalidFixMessageException ("Message BodyLength(9) is invalid");
    static final InvalidFixMessageException MISSING_MSG_TYPE = new InvalidFixMessageException ("Message has no MsgType(35) tag");
    static final InvalidFixMessageException MESSAGE_TOO_LARGE = new InvalidFixMessageException ("Message is too large");
    public static final InvalidFixMessageException RESET_BELOW_CURRENT_SEQ_LARGE = new InvalidFixMessageException ("SequenceReset can only increase the sequence number");
    static final InvalidFixMessageException EXPECTING_LOGON_MESSAGE = new InvalidFixMessageException ("Application-level connection is not yet established (Unfinished LOGON)");

    private InvalidFixMessageException (String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace () {
        return null;
    }
}
