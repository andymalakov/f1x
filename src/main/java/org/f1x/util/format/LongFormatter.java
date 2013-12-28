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
 * Adaptation of java.lang.Long.toString() to format long number into byte array.
 */
public class LongFormatter {

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

    public static int format (long value, byte [] buffer, int offset) {
        if (value == Long.MIN_VALUE) {
            System.arraycopy(MIN_VALUE_REPRESENTATION, 0, buffer, offset, MIN_VALUE_REPRESENTATION.length);
            return offset+MIN_VALUE_REPRESENTATION.length;
        } else {
            final int stringSize = (value < 0) ? stringSize(-value) + 1 : stringSize(value);
            int endIndex = offset+stringSize;
            LongFormatter.getBytes(value, buffer, endIndex);
            return endIndex;
        }
    }

    private static final byte [] MIN_VALUE_REPRESENTATION = AsciiUtils.getBytes(Long.toString(Long.MIN_VALUE));

    /**
     * Places characters representing the integer value into the
     * character array buffer. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if value == Long.MIN_VALUE
     */
    static void getBytes(long value, final byte[] buffer, final int stringSize) {
        long q;
        int r;
        int charPos = stringSize;
        byte sign = 0;

        if (value < 0) {
            sign = '-';
            value = -value;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (value > Integer.MAX_VALUE) {
            q = value / 100;
            // really: r = value - (q * 100);
            r = (int)(value - ((q << 6) + (q << 5) + (q << 2)));
            value = q;
            buffer[--charPos] = IntFormatter.DigitOnes[r];
            buffer[--charPos] = IntFormatter.DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)value;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buffer[--charPos] = IntFormatter.DigitOnes[r];
            buffer[--charPos] = IntFormatter.DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buffer[--charPos] = IntFormatter.Digits[r];
            i2 = q2;
            if (i2 == 0) break;
        }
        if (sign != 0) {
            buffer[--charPos] = sign;
        }
    }

    // Requires positive x
    static int stringSize(long x) {
        long p = 10;
        for (int i=1; i<19; i++) {
            if (x < p)
                return i;
            p = 10*p;
        }
        return 19;
    }

}
