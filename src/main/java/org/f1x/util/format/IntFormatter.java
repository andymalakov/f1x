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

import org.f1x.util.AsciiUtils;

/**
 * Adaptation of java.lang.Integer.toString() to format integer number into byte array.
 */
public class IntFormatter {

    private static final byte [] MIN_VALUE_STRING = AsciiUtils.getBytes(Integer.toString(Integer.MIN_VALUE));

    public static int format(int value, byte [] buffer, int offset) {
        if (value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_VALUE_STRING, 0, buffer, offset, MIN_VALUE_STRING.length);
            return offset + MIN_VALUE_STRING.length;
        } else {
            int size = (value < 0) ? stringSize(-value) + 1 : stringSize(value);
            getChars(value, size+offset, buffer);
            return offset + size;
        }
    }

    final static byte[] Digits = {
        '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9'
    };

    final static byte [] DigitTens = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    final static byte [] DigitOnes = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    // I use the "invariant division by multiplication" trick to
    // accelerate Integer.toString.  In particular we want to
    // avoid division by 10.
    //
    // The "trick" has roughly the same performance characteristics
    // as the "classic" Integer.toString code on a non-JIT VM.
    // The trick avoids .rem and .div calls but has a longer code
    // path and is thus dominated by dispatch overhead.  In the
    // JIT case the dispatch overhead doesn't exist and the
    // "trick" is considerably faster than the classic code.
    //
    // TODO-FIXME: convert (x * 52429) into the equiv shift-add
    // sequence.
    //
    // RE:  Division by Invariant Integers using Multiplication
    //      T Gralund, P Montgomery
    //      ACM PLDI 1994
    //


    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    static void getChars(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf [--charPos] = DigitOnes[r];
            buf [--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (i * 52429) >>> (16+3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = Digits [r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            buf [--charPos] = '-';
        }
    }

    final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    // Requires positive x
    public static int stringSize(int x) {
        for (int i=0; ; i++)
            if (x <= sizeTable[i])
                return i+1;
    }

    public static int format2digits(int value, byte[] buffer, int offset) {
        if (value < 0 || value > 99)
            throw new IllegalArgumentException();
        buffer[offset++] = Digits[value/10];
        buffer[offset++] = Digits[value%10];
        return offset;
    }

    public static int format4digits(int value, byte[] buffer, int offset) {
        if (value < 0 || value > 9999)
            throw new IllegalArgumentException();

        for (int i=offset+3; i >= offset; i--) {
            buffer[i] = Digits[value%10];
            value = value / 10;
        }
        return offset+4;
    }

    public static int format3digits(int value, byte[] buffer, int offset) {
        if (value < 0 || value > 999)
            throw new IllegalArgumentException();

        buffer[offset++] = Digits[value/100];
        buffer[offset++] = Digits[(value%100)/10];
        buffer[offset++] = Digits[value%10];
        return offset;
    }
}
