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

import org.junit.Assert;
import org.junit.Test;

public class Test_LongFormatter {

    private static final int MAX_WIDTH = 512;
    private final byte [] buffer = new byte [MAX_WIDTH];


    @Test
    public void testSimple() {
        assertFormat(0, "0");
        assertFormat(1, "1");
        assertFormat(-1, "-1");
        assertFormat(Integer.MIN_VALUE, "-2147483648");
        assertFormat(Integer.MAX_VALUE,  "2147483647");
        assertFormat(Long.MIN_VALUE, "-9223372036854775808");
        assertFormat(Long.MAX_VALUE,  "9223372036854775807");
    }

    @Test
    public void enumerateAll() {
        long number = -10000, step = 1;

        while (number < 100000) {
            assertFormat(number, String.valueOf(number));
            number += step;
        }

    }

    private void assertFormat(long value, String expected) {
        int length = LongFormatter.format(value, buffer, 0);
        String actual = new String (buffer, 0, length);
        Assert.assertEquals(expected, actual);
    }

    public static void main(String[] args) {
        new Test_IntFormatter().enumerateAll();
    }

}
