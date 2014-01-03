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


import org.f1x.api.message.AppendableValue;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.fields.MsgType;
import org.f1x.api.message.types.ByteEnum;
import org.f1x.api.message.types.IntEnum;
import org.f1x.api.message.types.StringEnum;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.format.*;

/**
 * Simple implementation of MessageBuilder that collects all fields in fixed size byte array.
 */
public final class ByteBufferMessageBuilder implements MessageBuilder, AppendableValue {

    private static final byte BYTE_Y = (byte) 'Y';
    private static final byte BYTE_N = (byte) 'N';
    private static final byte SOH = 1; // field separator

    private final TimestampFormatter gmtTimestampFormat = TimestampFormatter.createUTCTimestampFormatter();
    private final TimestampFormatter localTimestampFormat = TimestampFormatter.createLocalTimestampFormatter();
    private final DoubleFormatter doubleFormatter;

    private String msgType;
    private final byte [] buffer;
    private int offset;

    public ByteBufferMessageBuilder (int maxLength, int doubleFormatterPrecision) {
        buffer = new byte[maxLength];
        doubleFormatter = new DoubleFormatter(doubleFormatterPrecision);
    }

    public ByteBufferMessageBuilder (byte[] buff, int doubleFormatterPrecision) {
        buffer = buff;
        doubleFormatter = new DoubleFormatter(doubleFormatterPrecision);
    }

    @Override
    public void clear() {
        offset = 0;
    }

    @Override
    public void setMessageType(MsgType msgType) {
        this.msgType = msgType.getCode();
    }

    @Override
    public void setMessageType(String msgType) {
        this.msgType = msgType;
    }

    @Override
    public String getMessageType() {
        return msgType;
    }

    @Override
    public void add(int tagNo, final CharSequence value) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = CharSequenceFormatter.format(value, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void add(int tagNo, CharSequence value, int start, int end) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = CharSequenceFormatter.format(value, start, end, buffer, offset);
        buffer[offset++] = SOH;    }

    @Override
    public void add(int tagNo, long value) {
        offset = LongFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = LongFormatter.format(value, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void add(int tagNo, int value) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = IntFormatter.format(value, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void add(int tagNo, double value) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = doubleFormatter.format(value, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void add(int tagNo, byte value) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        buffer[offset++] = value;
        buffer[offset++] = SOH;
    }

    @Override
    public void add(int tagNo, boolean value) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        buffer[offset++] = (value) ? BYTE_Y : BYTE_N;
        buffer[offset++] = SOH;
    }

    @Override
    public void addUTCTimestamp(int tagNo, long timestamp) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = gmtTimestampFormat.formatDateTime(timestamp, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void addUTCDateOnly(int tagNo, long timestamp) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = gmtTimestampFormat.formatDateOnly(timestamp, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void addLocalMktDate(int tagNo, long timestamp) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = localTimestampFormat.formatDateOnly(timestamp, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void addUTCTimeOnly(int tagNo, long timestamp) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset = TimeOfDayFormatter.format(timestamp, buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public void addRaw(int tagNo, byte[] sourceBuffer, int sourceOffset, int sourceLength) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        System.arraycopy(sourceBuffer, sourceOffset, buffer, offset, sourceLength);
        offset += sourceLength;
        buffer[offset++] = SOH;
    }

    @Override
    public void addRaw(int tagNo, ByteArrayReference bytes) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        offset += bytes.copyTo(buffer, offset);
        buffer[offset++] = SOH;
    }

    @Override
    public AppendableValue add(int tagNo) {
        offset = IntFormatter.format(tagNo, buffer, offset);
        buffer[offset++] = '=';
        return this;
    }

    @Override
    public void add(int tag, ByteEnum value) {
        add (tag, value.getCode());
    }

    @Override
    public void add(int tag, IntEnum value) {
        add (tag, value.getCode());
    }

    @Override
    public void add(int tag, StringEnum value) {
        byte [] valueAsBytes = value.getBytes();
        addRaw (tag, valueAsBytes, 0, valueAsBytes.length);
    }

    @Override
    public int output(byte[] buffer, int offset) {
        if (this.offset > buffer.length - offset)
            throw new IndexOutOfBoundsException("Output FIX message exceeds maximum size");
        System.arraycopy(this.buffer, 0, buffer, offset, this.offset);
        return offset+this.offset;
    }

    @Override
    public int getLength() {
        return offset;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // AppendableValue


    @Override
    public AppendableValue append(CharSequence csq) {
        offset = CharSequenceFormatter.format(csq, buffer, offset);
        return this;
    }

    @Override
    public AppendableValue append(CharSequence csq, int start, int end) {
        offset = CharSequenceFormatter.format(csq, start, end, buffer, offset);
        return this;
    }

    @Override
    public AppendableValue append(char c) {
        if ((c & 0xFFFFFF00) != 0)
            throw new IllegalArgumentException("ASCII only");
        buffer[offset++] = (byte) c;
        return this;
    }

    @Override
    public AppendableValue append(byte b) {
        buffer[offset++] = b;
        return this;
    }

    @Override
    public AppendableValue append(int value) {
        offset = IntFormatter.format(value, buffer, offset);
        return this;
    }

    @Override
    public AppendableValue append(long value) {
        offset = LongFormatter.format(value, buffer, offset);
        return this;
    }

    @Override
    public AppendableValue append(double value) {
        offset = doubleFormatter.format(value, buffer, offset);
        return this;
    }

    @Override
    public void end() {
        buffer[offset++] = SOH;
    }
}
