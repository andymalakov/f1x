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

package org.f1x.util.parse;


import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class Test_NumbersParser {

    //@Test takes too long
    public void testAllIntNumbers() {
        for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
            assertIntParser(i);
        }
    }

    @Test
    public void testSelectedNumbers() {
        assertIntParser(Integer.MIN_VALUE);
        assertIntParser(Integer.MIN_VALUE / 2);
        assertIntParser(0);
        assertIntParser(Integer.MAX_VALUE / 2);
        assertIntParser(Integer.MAX_VALUE);

        assertLongParser(Long.MIN_VALUE);
        assertLongParser(Long.MIN_VALUE / 2);
        assertLongParser(0);
        assertLongParser(Long.MAX_VALUE / 2);
        assertLongParser(Long.MAX_VALUE);

        assertDoubleParser(-0.1);
        assertDoubleParser(0);
        assertDoubleParser(0.1);
        assertDoubleParser(Math.PI);
        assertDoubleParser(3.14159);

    }

    @Test
    public void badInputs () {
        assertBadIntNumber("");
        assertBadIntNumber(" 123");
        assertBadIntNumber("123 ");
        assertBadIntNumber("1-");
        assertBadIntNumber("1-2");

        assertBadLongNumber("");
        assertBadLongNumber(" 123");
        assertBadLongNumber("123 ");
        assertBadLongNumber("1-");
        assertBadLongNumber("1-2");
    }

    @Test
    public void goodInputs () {
        assertIntNumber("00001", 1); // leading zeros are allowed
        assertIntNumber("-00001", -1); // leading zeros are allowed
        assertIntNumber("-0", 0);

        assertLongNumber("00001", 1); // leading zeros are allowed
        assertLongNumber("-00001", -1); // leading zeros are allowed
        assertLongNumber("-0", 0);
    }

    @Test
    @Ignore //TODO
    public void tooLargeIntegers () {
        assertBadIntNumber("1234567890123456789012345678901234567890");
        assertBadLongNumber("1234567890123456789012345678901234567890");
    }

    private static void assertBadIntNumber(String value) {
        byte[] valueBytes = value.getBytes();
        try {
            int parsedValue = NumbersParser.parseInt(wrap(valueBytes), 1, valueBytes.length);
            fail("Parser was expected to fail on \"" + value + "\" but instead it produced: " + parsedValue);
        } catch (Exception expected) {
        }
    }

    private static void assertIntNumber (String value, int expectedValue) {
        byte[] valueBytes = value.getBytes();
        int parsedValue = NumbersParser.parseInt(wrap(valueBytes), 1, valueBytes.length);
        Assert.assertEquals(expectedValue, parsedValue);
    }


    private static void assertIntParser(int number) {
        String value = Integer.toString(number);
        byte[] valueBytes = value.getBytes();
        int parsedValue = NumbersParser.parseInt(wrap(valueBytes), 1, valueBytes.length);
        assertEquals(number, parsedValue);
    }

    private static void assertBadLongNumber(String value) {
        byte[] valueBytes = value.getBytes();
        try {
            long parsedValue = NumbersParser.parseLong(wrap(valueBytes), 1, valueBytes.length);
            fail("Parser was expected to fail on \"" + value + "\" but instead it produced: " + parsedValue);
        } catch (Exception expected) {
        }
    }

    private static void assertLongNumber (String value, long expectedValue) {
        byte[] valueBytes = value.getBytes();
        long parsedValue = NumbersParser.parseLong(wrap(valueBytes), 1, valueBytes.length);
        Assert.assertEquals(expectedValue, parsedValue);
    }


    private static void assertLongParser(long number) {
        String value = Long.toString(number);
        byte[] valueBytes = value.getBytes();
        long parsedValue = NumbersParser.parseLong(wrap(valueBytes), 1, valueBytes.length);
        assertEquals(number, parsedValue);
    }

    private static void assertDoubleParser(double number) {
        String value = Double.toString(number);
        byte[] valueBytes = value.getBytes();

        double parsedValue = NumbersParser.parseDouble(wrap(valueBytes), 1, valueBytes.length);
        assertEquals(number, parsedValue, .00001);
    }

    private static byte[] wrap(byte[] valueBytes) {
        //to make it more interesting
        byte [] result = new byte[valueBytes.length + 2];
        Arrays.fill(result, 0, result.length, (byte)'9');
        System.arraycopy(valueBytes, 0, result, 1, valueBytes.length);
        return result;
    }

}
