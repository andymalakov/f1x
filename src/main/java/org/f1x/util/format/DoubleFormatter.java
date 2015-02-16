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
package org.f1x.util.format;

import java.math.BigDecimal;

/**
 * This class formats floating point number to textual representation with given precision.
 * Not thread safe.
 * For large numbers this method falls back to BigDecimal.toPlainString().
 * Note: formatter always uses '.' dot as decimal separator (regardless of current locale).
 *
 */
public final class DoubleFormatter {

    public static final int MAX_WIDTH = 21;
    public static final int MAX_PRECISION = 15;
    private static final long MAX = Long.MAX_VALUE / 10;

    private final byte [] buffer = new byte [MAX_WIDTH];

    private final int maxLength;
    private final int precision;
    private final long factor;

    public DoubleFormatter (int precision) {
        this (precision, MAX_WIDTH);
    }

    /**
     * @param precision maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded.
     * @param maxLength maximum length a whole string should take (e.g. 16).
     */
    public DoubleFormatter (int precision, int maxLength) {
        if (precision < 0 || precision > MAX_PRECISION)
            throw new IllegalArgumentException("Precision");
        if (maxLength < 0 || maxLength > MAX_WIDTH)
            throw new IllegalArgumentException("Length");
        this.precision = precision;
        this.maxLength = maxLength;
        this.factor = Math.round(Math.pow(10, precision));
    }

    /** Formats given number into output byte buffer */
    public int format (double number, byte [] output, int offset) {
        return format(number, precision, true, maxLength, factor, output, offset);
    }

    /** Formats given number into output byte buffer */
    public int format (double number, int precision, byte [] output, int offset) {
        return format(number, precision, MAX_WIDTH, output, offset);
    }

    /**
     * @param precision maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded.
     * @param maxLength maximum length a whole string should take (e.g. 16).
     */
    public int format (double number, int precision, int maxLength, byte [] output, int offset) {
        return format(number, precision, true, maxLength, output, offset);
    }

    /**
     * @param precision maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded.
     * @param roundUp defines rounding mode (RoundingMode.HALF_UP or RoundingMode.HALF_DOWN)
     * @param maxLength maximum length a whole string should take (e.g. 16).
     */
    public int format (double number, int precision, boolean roundUp, int maxLength, byte [] output, int offset) {
        long factor = 10;
        for (int i = 0; i < precision; i++)
            factor*= 10;
        return format(number, precision, roundUp, maxLength, factor, output, offset);
    }

    /**
     * @param precision maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded.
     * @param roundUp defines rounding mode (HALF_UP or HALF_DOWN)
     * @param maxLength maximum length a whole string should take (e.g. 16).
     */
    private int format (double number, int precision, boolean roundUp, int maxLength, long factor10, byte [] output, int offset) {
        if (precision < 0 || precision > MAX_PRECISION)
            throw new IllegalArgumentException("Precision");
        if (maxLength < 0 || maxLength > MAX_WIDTH)
            throw new IllegalArgumentException("Length");
        if (Double.isNaN(number)) // Infinity will be checked a bit later
            throw new IllegalArgumentException("NaN");

        boolean sign = false;
        double factoredNumber = number;
        if (number < 0) {
            sign = true;
            factoredNumber = Math.abs(number);
        }
        factoredNumber = factoredNumber*factor10;
        if (Double.isInfinite(factoredNumber) || factoredNumber > MAX)
            return formatLargeNumber (number, output, offset);


        //long numberAsDecimal = Math.round(factoredNumber);  // this call costs 8% of total time we spend in this method on avg.
        long numberAsDecimal;
        {
            //factoredNumber = factoredNumber * 10;
            numberAsDecimal = (long) factoredNumber;
            double smallestDigit = factoredNumber % 10;
            numberAsDecimal = numberAsDecimal / 10;
            if (roundUp) {
                if (smallestDigit >= 5)
                    numberAsDecimal++; // round up;
            } else {
                if (smallestDigit > 5)
                    numberAsDecimal++; // round up;
            }
        }
        if (numberAsDecimal == 0)
            return formatZero(output, offset);

        int i = MAX_WIDTH;
        int fractional_part = precision;

        boolean needLeadingZero = (fractional_part > 0);
        while (numberAsDecimal > 0) {
            int digit = (int) (numberAsDecimal % 10);
            if (digit != 0 || i != MAX_WIDTH || fractional_part == 0) // skip trailing zeros
                buffer [--i] = IntFormatter.Digits[digit];

            numberAsDecimal = numberAsDecimal / 10;
            if (fractional_part > 0) {
                fractional_part --;
                if (fractional_part == 0) {
                    if (i != MAX_WIDTH)
                        buffer [--i] = '.';
                    needLeadingZero = (numberAsDecimal == 0);
                }
            }
        }
        if (needLeadingZero) {
            if (fractional_part > 0) { // if number was less than zero
                while (fractional_part-- > 0)
                    buffer [--i] = '0';
                buffer [--i] = '.';
            }
            buffer [--i] = '0';
        }

        if (sign)
            buffer [--i] = '-';

        int len = Math.min(MAX_WIDTH - i, maxLength);
        System.arraycopy(buffer, i, output, offset, len);
        return offset + len;
    }

    private static int formatZero(byte[] output, int offset) {
        output[offset++] = '0';
        return offset;
    }

    // Super Rarely used to represent prices
    private static int formatLargeNumber(double number, byte[] output, int offset) {
        final String text = new BigDecimal(number).toPlainString();
        final int cnt = text.length();
        for(int i=0; i < cnt; i++)
            output[offset++] = (byte) text.charAt(i);
        return offset;
    }

}
