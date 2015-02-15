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

package org.f1x.api.message;

import org.f1x.api.message.fields.MsgType;
import org.f1x.api.message.types.ByteEnum;
import org.f1x.api.message.types.IntEnum;
import org.f1x.api.message.types.StringEnum;
import org.f1x.util.ByteArrayReference;

/**
 * Reusable constructor of FIX message.
 * The following tags will be added automatically by the framework:
 * BeginString(8), BodyLength(9), MsgSeq(34), SenderCompId(49), TargetCompId(56), and CheckSum(10).
 *
 * Not thread safe.
 */
public interface MessageBuilder {

    CharSequence getMessageType();
    void setMessageType(MsgType msgType);
    void setMessageType(CharSequence msgType);

    /**
     * Appends ASCII CharSequence tag value pair. Use {@link #addRaw(int, byte[], int, int)} for non-ASCII content and don't forget to specify MessageEncoding(347).
     * @param value tag value as ASCII text
     */
    void add (int tag, CharSequence value);

    /**
     * Appends ASCII CharSequence tag value pair. Use {@link #addRaw(int, byte[], int, int)} for non-ASCII content and don't forget to specify MessageEncoding(347).
     * @param value tag value as ASCII text
     * @param  start The index of the first character in the subsequence
     * @param  end The index of the character following the last character in the subsequence
     */
    void add (int tag, CharSequence value, int start, int end);
    void add (int tag, long value);
    void add (int tag, int value);

    /** Appends given double value formatted with default precision and rounded-up*/
    void add (int tag, double value);

    /** Appends given double value formatted with given precision rounded-up*/
    void add (int tag, double value, int precision);

    /** Appends given double value formatted with given precision rounded up or down */
    void add (int tag, double value, int precision, boolean roundUp);
    void add (int tag, byte value);
    void add (int tag, boolean value);
    void add (int tag, ByteEnum value);
    void add (int tag, IntEnum value);
    void add (int tag, StringEnum value);
    /** Adds UTCTimestamp field (in "yyyyMMdd-HH:mm:ss.SSS" format) */
    void addUTCTimestamp (int tag, long timestamp);
    /** Adds UTCTimeOnly field (in "HH:mm:ss.SSS" format) */
    void addUTCTimeOnly (int tag, long timestamp);
    /** Adds UTCDateOnly field (in "yyyyMMdd" format) */
    void addUTCDateOnly (int tag, long timestamp);
    /** Adds LocalMktDate (in "yyyyMMdd" format) */
    void addLocalMktDate (int tag, long timestamp);
    /** Adds LocalMktDate (in "yyyyMMdd" format) */
    void addLocalMktDate2 (int tag, int yyyymmdd);
    /** Copies value of given tag from the provided byte buffer */
    void addRaw (int tag, byte[] buffer, int offset, int length);
    /** Copies value of given tag from the provided byte array reference */
    void addRaw (int tag, ByteArrayReference bytes);

    /** Adds tag that with complex value.
     * Since FIX doesn't allow empty values caller is required to call one of {@link AppendableValue} method to provide tag value.
     * Also caller is required to call {@link AppendableValue#end()} at the end. Example:
     * <pre>
     * mb.add (FixTags.ClOrdId).append("FXI").append(orderId++).end();
     * </pre>
     */
    AppendableValue add (int tag);

    /** Copy current content into given buffer */
    int output(byte[] buffer, int offset);

    /** @return current length of message body (in bytes) */
    int getLength();

    /** Clear current content before building content for a new message */
    void clear();

}
