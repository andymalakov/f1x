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

import org.junit.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Test_DoubleFormatter2 {

    private static final int MAX_WIDTH = 512;
    private final byte[] buffer = new byte[MAX_WIDTH];

    @Test
    public void testAroundBoundariesOfLong() {
        assertFormat(0.99e19, "99000000000000000000", 1);
        //assertFormat(0.99e-19, "0.99", 1);
        assertFormat(0.99e-19, "0", 1);
    }

    @Test
    public void testSimple() {
        assertFormat(1.234, "1.234", 3);
        assertFormat(0.123, "0.123", 3);

        assertFormat(12.345, "12", 0);
        assertFormat(12.345, "12.3", 1);
        assertFormat(12.345, "12.35", 2);  // rounding up
        assertFormat(12.344, "12.34", 2);  // rounding down
        assertFormat(12.345, "12.345", 3);
        assertFormat(12.345, "12.345", 4);
        assertFormat(12.345, "12.345", 5);
    }

    @Test
    public void testBug() {
        assertFormat(-92233720368547760.0, "-92233720368547760", 2);
    }

    @Test
    public void testNegative() {
        assertFormat(-1.23, "-1.23", 3);
    }

    @Test
    public void powerOfTen() {
        assertFormat(0.0001, "0", 3);
        assertFormat(0.001, "0.001", 3);
        assertFormat(0.01, "0.01", 3);
        assertFormat(0.1, "0.1", 3);
        assertFormat(1, "1", 3);
        assertFormat(10, "10", 3);
        assertFormat(100, "100", 3);
        assertFormat(1000, "1000", 3);
        assertFormat(10000, "10000", 3);
        assertFormat(100000, "100000", 3);
    }

    @Test
    public void powerOfTenNegative() {
        assertFormat(-0.0001, "-0", 3);
        assertFormat(-0.001, "-0.001", 3);
        assertFormat(-0.01, "-0.01", 3);
        assertFormat(-0.1, "-0.1", 3);
        assertFormat(-1, "-1", 3);
        assertFormat(-10, "-10", 3);
        assertFormat(-100, "-100", 3);
        assertFormat(-1000, "-1000", 3);
        assertFormat(-10000, "-10000", 3);
        assertFormat(-100000, "-100000", 3);
    }

    @Test
    public void testWholeNumbers() {
        assertFormat(1, "1", 3);
        assertFormat(123, "123", 3);
        assertFormat(1, "1", 0);
        assertFormat(123, "123", 0);
    }

    @Test
    public void testPrecision0() {
        assertFormat(12345.6, "12346", 0);
        assertFormat(1234.56, "1235", 0);
        assertFormat(123.456, "123", 0);
        assertFormat(12.3456, "12", 0);
        assertFormat(1.23456, "1", 0);
        assertFormat(0.123456, "0", 0);
        assertFormat(0.0123456, "0", 0);
        assertFormat(0.0012345, "0", 0);
        assertFormat(0.0001234, "0", 0);
        assertFormat(0.0000123, "0", 0);
        assertFormat(0.0000012, "0", 0);
        assertFormat(0.0000001, "0", 0);
    }

    @Test
    public void testPrecision1() {
        assertFormat(123456., "123456", 1);
        assertFormat(12345.6, "12345.6", 1);
        assertFormat(1234.56, "1234.6", 1);
        assertFormat(123.456, "123.5", 1);
        assertFormat(12.3456, "12.3", 1);
        assertFormat(1.23456, "1.2", 1);
        assertFormat(0.123456, "0.1", 1);
        assertFormat(0.0123456, "0", 1);
        assertFormat(0.0012345, "0", 1);
        assertFormat(0.0001234, "0", 1);
        assertFormat(0.0000123, "0", 1);
        assertFormat(0.0000012, "0", 1);
        assertFormat(0.0000001, "0", 1);
    }

    @Test
    public void testPrecision2() {
        assertFormat(123456., "123456", 2);
        assertFormat(12345.6, "12345.6", 2);
        assertFormat(1234.56, "1234.56", 2);
        assertFormat(123.456, "123.46", 2);
        assertFormat(12.3456, "12.35", 2);
        assertFormat(1.23456, "1.23", 2);
        assertFormat(0.123456, "0.12", 2);
        assertFormat(0.0123456, "0.01", 2);
        assertFormat(0.0012345, "0", 2);
        assertFormat(0.0001234, "0", 2);
        assertFormat(0.0000123, "0", 2);
        assertFormat(0.0000012, "0", 2);
        assertFormat(0.0000001, "0", 2);
    }

    @Test
    public void testPrecision3() {
        assertFormat(123456., "123456", 3);
        assertFormat(12345.6, "12345.6", 3);
        assertFormat(1234.56, "1234.56", 3);
        assertFormat(123.456, "123.456", 3);
        assertFormat(12.3456, "12.346", 3);
        assertFormat(1.23456, "1.235", 3);
        assertFormat(0.123456, "0.123", 3);
        assertFormat(0.0123456, "0.012", 3);
        assertFormat(0.0012345, "0.001", 3);
        assertFormat(0.0001234, "0", 3);
        assertFormat(0.0000123, "0", 3);
        assertFormat(0.0000012, "0", 3);
        assertFormat(0.0000001, "0", 3);
    }

    @Test
    public void testZeros() {
        assertFormat(0, "0", 3);
        assertFormat(-2.0 / Float.POSITIVE_INFINITY, "0", 3);
        assertFormat(-0., "0", 3);
    }

    @Test
    public void testPeriodic() {
        assertFormat(1.0 / 3.0, "0.333333333", 9);
    }


    @Test
    public void testDoubleMaxValue() {
        assertFormat(Double.MAX_VALUE, "179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368", DoubleFormatter.MAX_PRECISION);
    }

    @Test
    public void testLongMaxValue() {
        assertFormat(Long.MAX_VALUE, "9223372036854775808", DoubleFormatter.MAX_PRECISION);
    }


    @Test
    public void testDoubleMinValue() {
        assertFormat(Double.MIN_VALUE, "0", DoubleFormatter.MAX_PRECISION);
    }

    @Test
    public void testLongMinValue() {
        assertFormat(Long.MIN_VALUE, "-9223372036854775808", DoubleFormatter.MAX_PRECISION);
    }

    @Test
    public void testNaN() {
        try {
            DoubleFormatter2.format(Double.NaN, 3, buffer, 0);
            fail("Failed to detect NaN");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInfinity() {
        try {
            DoubleFormatter2.format(Double.POSITIVE_INFINITY, 3, buffer, 0);
            fail("Failed to detect positive INFINITY");
        } catch (IllegalArgumentException expected) {
        }
        try {
            DoubleFormatter2.format(Double.NEGATIVE_INFINITY, 3, buffer, 0);
            fail("Failed to detect negative INFINITY");
        } catch (IllegalArgumentException expected) {
        }
    }


    private static void assertFormat(double number, int precision, DecimalFormat standardFormat) {
        String expected = standardFormat.format(number);
        if (expected.equals("-0"))
            expected = "0";

        String actual;
        try {
            byte[] buffer = new byte[MAX_WIDTH];
            int length = DoubleFormatter2.format(number, precision, buffer, 0);
            actual = new String(buffer, 0, length);
        } catch (Exception e) {
            throw new RuntimeException("Error formatting " + number + ": " + e.getMessage(), e);
        }

//        assertEquals (expected, actual);
        if (!expected.equals(actual)) {
            System.err.println("WARN: " + number + " formatted as " + actual);
            assertEquals(number, Double.valueOf(actual), PRECISION);
        }
    }

    private static final double PRECISION = 0.0000000000000000000001;

    private static void assertFormat(double number, String expected, int precision) {
        byte[] buffer = new byte[MAX_WIDTH];
        int length = DoubleFormatter2.format(number, precision, buffer, 0);
        String actual = new String(buffer, 0, length);
        if (!expected.equals(actual)) {
            assertEquals(number, Double.valueOf(actual), PRECISION);
        }
    }

    @Test
    public void testEnum() {
        DecimalFormat standardFormat = new DecimalFormat("###.##");
        standardFormat.setRoundingMode(RoundingMode.HALF_UP);

        double number = 0.01 * Long.MIN_VALUE;
        while (number < -0.01) {
            assertFormat(number, 2, standardFormat);
            number = number / 10;
        }

        number = 0.01 * Long.MAX_VALUE;
        while (number > 0.01) {
            assertFormat(number, 2, standardFormat);
            number = number / 10;
        }
    }

    @Test
    public void testRandom() {
        DecimalFormat standardFormat = new DecimalFormat("###.#####");
        standardFormat.setRoundingMode(RoundingMode.HALF_UP);
        Random rnd = new Random(13132);

        for (int i = 0; i < 100000; i++) {
            double number = rnd.nextDouble() * Math.pow(10, rnd.nextInt(8));
            assertFormat(number, 5, standardFormat);
        }
    }


    @Test
    public void testBug1() {
        DecimalFormat standardFormat = new DecimalFormat("###.##");
        standardFormat.setRoundingMode(RoundingMode.HALF_UP);
        assertEquals("standardFormat", "49405.99", standardFormat.format(49405.994999999995));
        assertFormat(49405.994999999995, 2, standardFormat);
    }

    //@Test
    public static void enumerateAll() {
        double number = -10000, step = 0.000003;
        DecimalFormat standardFormat = new DecimalFormat("###.##");
        standardFormat.setRoundingMode(RoundingMode.HALF_UP);
        while (number < 100000) {
            assertFormat(number, 2, standardFormat);
            number += step;
        }

    }

    public static void main(String[] args) {
        enumerateAll();
    }
}
