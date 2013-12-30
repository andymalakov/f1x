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

import org.f1x.api.message.fields.FixTags;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.FixParserException;
import org.f1x.api.message.MessageParser;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.parse.NumbersParser;
import org.f1x.util.parse.TimeOfDayParser;
import org.f1x.util.parse.TimestampParser;

public class DefaultMessageParser implements MessageParser {

    private static final GFLog LOGGER = GFLogFactory.getLog(DefaultMessageParser.class);

    private static final char SOH = 1; // field separator

    private final TimestampParser utcTimestampParser = TimestampParser.createUTCTimestampParser();
    private final TimestampParser localTimestampParser = TimestampParser.createLocalTimestampParser();
    private final ByteArrayReference charSequenceBuffer = new ByteArrayReference();

    private byte[] buffer;
    private int offset; // next byte to read
    private int limit;
    private int tagNum;
    private int valueOffset, valueLength;


    public void set (byte [] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.limit = offset + length;
    }

    @Override
    public boolean next() {
        try {
            final boolean result = _next();
            if (result) {
                if (valueLength == 0)
                    throw new FixParserException("Tag " + tagNum + " has empty value at position " + offset);

//                if (tagNum == FixTags.MsgSeqNum)
//                    validateSequenceNumber(getIntegerValue());
            }
            return result;

        } catch (FixParserException e) {
            throw new FixParserException("Parser error (at " + offset + "): " + e.getMessage());
        }
    }

    private boolean _next () {
        boolean isParsingTagNum = true;
        tagNum = 0;
        while (offset < limit) {
            byte ch = buffer[offset++];
            if (isParsingTagNum) {
                if (ch >= '0' && ch <= '9') {
                    tagNum = 10*tagNum + (ch - '0');
                } else
                if (ch == '=') {
                    if (tagNum == 0)
                        throw new FixParserException("Unexpected '=' character instead of a tag number digit");
                    isParsingTagNum = false;
                    valueOffset = offset;
                    valueLength = 0;
                } else {
                    throw new FixParserException("Unexpected character (0x" + Integer.toHexString(ch) + " where a tag number digit or '=' is expected");
                }

            } else {
                if (ch == SOH)
                    return true;

                valueLength++;
            }
        }
        return false;
    }


    @Override
    public int getTagNum() {
        return tagNum;
    }

    @Override
    public byte getByteValue() {
        if (valueLength > 1)
            throw new FixParserException("Value is not a single byte");

        return buffer[valueOffset];
    }

    @Override
    public int getIntValue() {
        return NumbersParser.parseInt(buffer, valueOffset, valueLength);
    }

    @Override
    public long getLongValue() {
        return NumbersParser.parseLong(buffer, valueOffset, valueLength);
    }

    @Override
    public double getDoubleValue() {
        return NumbersParser.parseDouble(buffer, valueOffset, valueLength);
    }

    @Override
    public CharSequence getCharSequenceValue() {
        charSequenceBuffer.set(buffer, valueOffset, valueLength);
        return charSequenceBuffer;
    }

    @Override
    public void getByteSequence(ByteArrayReference seq) {
        seq.set(buffer, valueOffset, valueLength);
    }

    @Override
    public String getStringValue () {
        return new String (buffer, valueOffset, valueLength);
    }

    @Override
    public boolean getBooleanValue () {
        if (valueLength > 1)
            throw new FixParserException("Field is not a character");

        if (buffer[valueOffset] == 'Y') return true;

        if (buffer[valueOffset] == 'N') return false;

        throw new FixParserException("Field cannot be parsed as FIX boolean");
    }

    @Override
    public long getUTCTimestampValue () {
        return utcTimestampParser.getUTCTimestampValue(buffer, valueOffset, valueLength);
    }

    @Override
    public long getUTCDateOnly() {
        return utcTimestampParser.getUTCDateOnly(buffer, valueOffset, valueLength);
    }

    @Override
    public long getLocalMktDate() {
        return localTimestampParser.getUTCDateOnly(buffer, valueOffset, valueLength);
    }

    @Override
    public int getUTCTimeOnly() {
        return TimeOfDayParser.parseTimeOfDay(buffer, valueOffset, valueLength);
    }

    @Override
    public boolean isValueEquals(byte[] constant) {
        if (valueLength != constant.length)
            return false;

        for (int i=0; i < valueLength; i++)
            if (buffer[valueOffset+i] != constant[i])
                return false;

        return true;
    }



    int getOffset() {
        return offset; //TODO: Get rid of this method
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append ("Buffer pos ");
        sb.append (offset);
        sb.append ('/');
        sb.append (limit);
        sb.append (" Last tag ");
        sb.append (tagNum);
        if (valueLength > 0) {
            sb.append ('=');
            sb.append (new String(buffer, valueOffset, valueLength));
        }
        return sb.toString();
    }
}


