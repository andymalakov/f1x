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
 * First implementation that copies each field into a buffer
 */
public abstract class MessageParser {

    // 2.	The first three fields in the standard header are BeginString (tag #8) followed by BodyLength (tag #9) followed by MsgType (tag #35).
    // 3.	The last field in the standard trailer is the CheckSum (tag #10).
    // 5.	A tag number (field) should only appear in a message once [With exception of field groups ]

    private static final char SOH = 1; // field separator

    private static final class ValueBuffer implements CharSequence {

        private static final int MAX_FIELD_LENGTH = 128;
        private final byte [] buf = new byte [MAX_FIELD_LENGTH];

        private int len = 0;

        public int length() {
            return len;
        }

        public char charAt(int index) {
            return (char) buf[index]; //TODO: Encoding
        }

        public CharSequence subSequence(int start, int end) {
            return new String(buf, start, end-start);
        }

        @Override
        public String toString () {
            return new String (buf, 0, len);
        }

    }

    private final ValueBuffer fieldValue = new ValueBuffer();

    public final void parse (byte [] message) throws InvalidMessageFormatException {

        int fieldId = 0;
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
                    fieldValue.len = 0;
                    isParsingFieldId = false;
                } else
                if (ch == SOH) {
                    throw new InvalidMessageFormatException("Empty value for tag " + fieldId + " at position #" + fieldId);
                } else {
                    throw new InvalidMessageFormatException("Unexpected character 0x" + Integer.toHexString(ch) + " at position #" + fieldId);
                }
            } else {
                if (ch == SOH) {
                    if ( ! onField(fieldId))
                        return; // skip the rest of the message
                    fieldId = 0;
                    isParsingFieldId = true;
                }
                fieldValue.buf[fieldValue.len++ ] = ch;
            }

            if (message[length - 1] != SOH)
                throw new InvalidMessageFormatException("Message must end with SOH symbol");
        }
    }


    /** @return false to terminate parsing of current message */
    protected abstract boolean onField (int field) throws InvalidMessageFormatException;


    private int extractNumber (int offset, int length) {
        int result = 0;
        for (int i=0; i < length; i++)
            result = 10*result + (fieldValue.buf[offset++] - '0'); //TODO: check that char holds a digit
        return result;
    }

    public CharSequence getCharSequenceValue () {
        return fieldValue;
    }

    public String getStringValue () {
        return new String (fieldValue.buf, 0, fieldValue.len);
    }

    public char getCharValue () throws InvalidMessageFormatException {
        if (fieldValue.len != 1)
            throw new InvalidMessageFormatException("Field is not a character");

        return (char) fieldValue.buf[0];
    }

    public int getIntValue () {
        int result = 0;
        for (int i=0; i < fieldValue.len; i++) {
            byte ch = fieldValue.buf[i];
            if (ch >= '0' && ch <= '9')
                result = 10*result + (ch - '0');
            else
                throw new RuntimeException("Illegal character in integer field value [" + (i+1) + "]: '" + ch + "'");
        }
        return result;
    }

    public long getLongValue () {
        long result = 0;
        for (int i=0; i < fieldValue.len; i++) {
            byte ch = fieldValue.buf[i];
            if (ch >= '0' && ch <= '9')
                result = 10*result + (ch - '0');
            else
                throw new RuntimeException("Illegal character in integer field value [" + (i+1) + "]: '" + ch + "'");
        }
        return result;
    }

    public boolean getBooleanValue () {
        return fieldValue.len == 1 && fieldValue.buf[0] == 'Y';
    }

//    public double getDoubleValue () {
//        return CharSequenceParser.parseDouble(fieldValue);
//    }


    private final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private long lastTimestampImage;

    public long getDateTimeValue () {
        long datetimeImage = getLongValue();

        long delta = datetimeImage - lastTimestampImage;

        if (Math.abs(delta) < 1000) {
            calendar.add (Calendar.MILLISECOND, (int) delta);
        } else {
            // 2010 01 15 20 59 44 292
            calendar.set (Calendar.YEAR,  extractNumber(0, 4));
            calendar.set (Calendar.MONTH, extractNumber(4, 2) - 1);
            calendar.set (Calendar.DAY_OF_MONTH, extractNumber(6, 2));
            calendar.set (Calendar.HOUR_OF_DAY, extractNumber(8, 2));
            calendar.set (Calendar.MINUTE, extractNumber(10, 2));
            calendar.set (Calendar.SECOND, extractNumber(12, 2));
            calendar.set (Calendar.MILLISECOND, extractNumber(14, 3));
        }

        lastTimestampImage = datetimeImage;
        return calendar.getTimeInMillis();
    }

}




