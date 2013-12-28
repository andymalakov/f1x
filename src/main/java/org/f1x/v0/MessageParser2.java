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

package org.f1x.v0;


import java.util.Calendar;
import java.util.TimeZone;

//import java.util.TimeZone;
//import java.util.Calendar;


/**
 * The second implementation that keeps track of field value start index and length
 */
public abstract class MessageParser2 {

    // 2.	The first three fields in the standard header are BeginString (tag #8) followed by BodyLength (tag #9) followed by MsgType (tag #35).
    // 3.	The last field in the standard trailer is the CheckSum (tag #10).
    // 5.	A tag number (field) should only appear in a message once [With exception of field groups ]

    private static final char SOH = 1; // field separator


    public final void parse (byte [] message) throws InvalidMessageFormatException {

        int fieldId = 0;
        int fieldValueOffset = 0;
        int fieldValueLength = 0;
        boolean isParsingFieldId = true;

        final int length = message.length;
        for (int i = 0; i < length; i++) {
            byte ch = message[i];
            if (isParsingFieldId) {
                if (ch >= '0' && ch <= '9') {
                    fieldId = 10*fieldId + (ch - '0');
                } else
                if (ch == '=') {
                    if (fieldId == 0)
                        throw new InvalidMessageFormatException("Empty field number at position #" + i);
                    fieldValueOffset = i;
                    isParsingFieldId = false;
                } else
                if (ch == SOH) {
                    throw new InvalidMessageFormatException("Empty value for tag " + fieldId + " at position #" + fieldId);
                } else {
                    throw new InvalidMessageFormatException("Unexpected character 0x" + Integer.toHexString(ch) + " at position #" + fieldId);
                }
            } else {
                if (ch == SOH) {
                    if ( ! onField(fieldId, message, fieldValueOffset, fieldValueLength))
                        return; // skip the rest of the message
                    fieldId = 0;
                    isParsingFieldId = true;
                }
                fieldValueLength++;
            }

            if (message[length - 1] != SOH)
                throw new InvalidMessageFormatException("Message must end with SOH symbol");
        }
    }


    /** @return false to terminate parsing of current message */
    protected abstract boolean onField (int field, byte[] buffer, int offset, int length) throws InvalidMessageFormatException;


    private int extractNumber (byte [] buffer, int offset, int length) {
        int result = 0;
        for (int i=0; i < length; i++)
            result = 10*result + (buffer[offset++] - '0');  //TODO: check that char holds a digit
        return result;
    }


    private static final class CharSequenceBuffer implements CharSequence {

        private byte [] buffer;
        private int offset, length;

        public int length() {
            return length;
        }

        public char charAt(int index) {
            return (char) buffer[index]; //TODO: Encoding
        }

        public CharSequence subSequence(int start, int end) {
            return new String(buffer, offset+start, end-start);
        }

        @Override
        public String toString () {
            return new String (buffer, offset, length);
        }

    }
    private final CharSequenceBuffer charSequenceBuffer = new CharSequenceBuffer();

    public CharSequence getCharSequenceValue (byte [] buffer, int offset, int length) {
        charSequenceBuffer.buffer = buffer;
        charSequenceBuffer.offset = offset;
        charSequenceBuffer.length = length;
        return charSequenceBuffer;
    }

    public String getStringValue (byte [] buffer, int offset, int length) {
        return new String (buffer, offset, length);
    }

    public char getCharValue (byte [] buffer, int offset, int length) throws InvalidMessageFormatException {
        if (length > 1) //TODO: Assert length > 0 somewhere else
            throw new InvalidMessageFormatException("Field is not a character");

        return (char) buffer[offset];
    }

    public int getIntValue (byte [] buffer, int offset, int length) {
        int result = 0;
        for (int i=offset; i < length; i++) {
            byte ch = buffer[i];
            if (ch >= '0' && ch <= '9')
                result = 10*result + (ch - '0');
            else
                throw new RuntimeException("Illegal character in integer field value [" + (i+1) + "]: '" + ch + "'");
        }
        return result;
    }

    public long getLongValue (byte [] buffer, int offset, int length) {
        long result = 0;
        for (int i=offset; i < length; i++) {
            byte ch = buffer[i];
            if (ch >= '0' && ch <= '9')
                result = 10*result + (ch - '0');
            else
                throw new RuntimeException("Illegal character in integer field value [" + (i+1) + "]: '" + ch + "'");
        }
        return result;
    }

    public boolean getBooleanValue (byte [] buffer, int offset, int length) throws InvalidMessageFormatException {
        if (length > 1) //TODO: Assert length > 0 somewhere else
            throw new InvalidMessageFormatException("Field is not a character");

        if (buffer[offset] == 'Y') return true;

        if (buffer[offset] == 'N') return false;

        throw new InvalidMessageFormatException("Field cannot be parsed as FIX boolean");
    }

//    public double getDoubleValue (byte [] buffer, int offset, int length) {
//        return CharSequenceParser.parseDouble(buffer, offset, length);
//    }


    private final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    public long getDateTimeValue (byte [] buffer, int offset, int length) {
        // 2010 01 15 20 59 44 292
        calendar.set (Calendar.YEAR,  extractNumber(buffer, offset+0, 4));
        calendar.set (Calendar.MONTH, extractNumber(buffer, offset+4, 2) - 1);
        calendar.set (Calendar.DAY_OF_MONTH, extractNumber(buffer, offset+6, 2));
        calendar.set (Calendar.HOUR_OF_DAY, extractNumber(buffer, offset+8, 2));
        calendar.set (Calendar.MINUTE, extractNumber(buffer, offset+10, 2));
        calendar.set (Calendar.SECOND, extractNumber(buffer, offset+12, 2));
        calendar.set (Calendar.MILLISECOND, extractNumber(buffer, offset+14, 3));
        return calendar.getTimeInMillis();
    }

}




