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

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

import static org.junit.Assert.fail;

public class Test_DoubleFormatter {

    private static final double EPSILON = 0.0000000000000000000001;
    private static final int MAX_WIDTH = 512;
    private static final DecimalFormat [] DECIMAL_FORMATTERS = new DecimalFormat[DoubleFormatter.MAX_PRECISION+1];
    private static final DoubleFormatter [] DOUBLE_FORMATTERS = new DoubleFormatter[DoubleFormatter.MAX_PRECISION+1];

    private final byte [] buffer = new byte [MAX_WIDTH];

    @BeforeClass
    public static void initFormattersCache() {
        String standardFormatPattern = "###.";
        for (int i = 0; i < DoubleFormatter.MAX_PRECISION+1; i++) {
            DECIMAL_FORMATTERS[i] = new DecimalFormat(standardFormatPattern);
            DOUBLE_FORMATTERS[i] = new DoubleFormatter(i);

            standardFormatPattern += '#';
        }
    }


    @Test
    public void testAroundBoundariesOfLong() {
        assertFormat(0.99e19, 1, "99000000000000000000");
        assertFormat(0.99e-19, 1, "0");
    }

    @Test
    public void testSimple() {
        assertFormat(1.234, 3, "1.234");
        assertFormat(0.123, 3, "0.123");

        assertFormat(12.345, 0, "12");
        assertFormat(12.345, 1, "12.3");
        assertFormat(12.345, 2, "12.35");  // rounding up
        assertFormat(12.344, 2, "12.34");  // rounding down
        assertFormat(12.345, 3, "12.345");
        assertFormat(12.345, 4, "12.345");
        assertFormat(12.345, 5, "12.345");
    }



    @Test
    public void testNegative() {
        assertFormat(-1.23, 3, "-1.23");
    }

    @Test
    public void powerOfTen() {
        assertFormat(0.0001, 3, "0");
        assertFormat(0.001, 3, "0.001");
        assertFormat(0.01, 3, "0.01");
        assertFormat(0.1, 3, "0.1");
        assertFormat(1, 3, "1");
        assertFormat(10, 3, "10");
        assertFormat(100, 3, "100");
        assertFormat(1000, 3, "1000");
        assertFormat(10000, 3, "10000");
        assertFormat(100000, 3, "100000");
    }

    @Test
    public void powerOfTenNegative() {
        assertFormat(-0.0001, 3, "0");
        //assertFormat(-0.0001, "-0", 3);
        assertFormat(-0.001, 3, "-0.001");
        assertFormat(-0.01, 3, "-0.01");
        assertFormat(-0.1, 3, "-0.1");
        assertFormat(-1, 3, "-1");
        assertFormat(-10, 3, "-10");
        assertFormat(-100, 3, "-100");
        assertFormat(-1000, 3, "-1000");
        assertFormat(-10000, 3, "-10000");
        assertFormat(-100000, 3, "-100000");
    }

    @Test
    public void testWholeNumbers() {
        assertFormat(1, 3, "1");
        assertFormat(123, 3, "123");
        assertFormat(1, 0, "1");
        assertFormat(123, 0, "123");
    }

    @Test
    public void testPrecision0() {
        assertFormat(12345.6, 0, "12346");
        assertFormat(1234.56, 0, "1235");
        assertFormat(123.456, 0, "123");
        assertFormat(12.3456, 0, "12");
        assertFormat(1.23456, 0, "1");
        assertFormat(0.123456, 0, "0");
        assertFormat(0.0123456, 0, "0");
        assertFormat(0.0012345, 0, "0");
        assertFormat(0.0001234, 0, "0");
        assertFormat(0.0000123, 0, "0");
        assertFormat(0.0000012, 0, "0");
        assertFormat(0.0000001, 0, "0");
    }

    @Test
    public void testPrecision1() {
        assertFormat(123456., 1, "123456");
        assertFormat(12345.6, 1, "12345.6");
        assertFormat(1234.56, 1, "1234.6");
        assertFormat(123.456, 1, "123.5");
        assertFormat(12.3456, 1, "12.3");
        assertFormat(1.23456, 1, "1.2");
        assertFormat(0.123456, 1, "0.1");
        assertFormat(0.0123456, 1, "0");
        assertFormat(0.0012345, 1, "0");
        assertFormat(0.0001234, 1, "0");
        assertFormat(0.0000123, 1, "0");
        assertFormat(0.0000012, 1, "0");
        assertFormat(0.0000001, 1, "0");
    }

    @Test
    public void testPrecision2() {
        assertFormat(123456., 2, "123456");
        assertFormat(12345.6, 2, "12345.6");
        assertFormat(1234.56, 2, "1234.56");
        assertFormat(123.456, 2, "123.46");
        assertFormat(12.3456, 2, "12.35");
        assertFormat(1.23456, 2, "1.23");
        assertFormat(0.123456, 2, "0.12");
        assertFormat(0.0123456, 2, "0.01");
        assertFormat(0.0012345, 2, "0");
        assertFormat(0.0001234, 2, "0");
        assertFormat(0.0000123, 2, "0");
        assertFormat(0.0000012, 2, "0");
        assertFormat(0.0000001, 2, "0");
    }

    @Test
    public void testPrecision3() {
        assertFormat(123456., 3, "123456");
        assertFormat(12345.6, 3, "12345.6");
        assertFormat(1234.56, 3, "1234.56");
        assertFormat(123.456, 3, "123.456");
        assertFormat(12.3456, 3, "12.346");
        assertFormat(1.23456, 3, "1.235");
        assertFormat(0.123456, 3, "0.123");
        assertFormat(0.0123456, 3, "0.012");
        assertFormat(0.0012345, 3, "0.001");
        assertFormat(0.0001234, 3, "0");
        assertFormat(0.0000123, 3, "0");
        assertFormat(0.0000012, 3, "0");
        assertFormat(0.0000001, 3, "0");
    }

    @Test
    public void testZeros() {
        assertFormat(0, 3, "0");
        assertFormat(-2.0 / Float.POSITIVE_INFINITY, 3, "0");
        assertFormat(-0., 3, "0");
    }

    @Test
    public void testPeriodic() {
        assertFormat(1.0 / 3.0, 9, "0.333333333");
    }


    @Test
    public void testDoubleMaxValue() {
        assertFormat(Double.MAX_VALUE, DoubleFormatter.MAX_PRECISION, "179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368");
    }

    @Test
    public void testLongMaxValue() {
        assertFormat(Long.MAX_VALUE, DoubleFormatter.MAX_PRECISION, "9223372036854775808");
    }


    @Test
    public void testDoubleMinValue() {
        assertFormat(Double.MIN_VALUE, DoubleFormatter.MAX_PRECISION, "0");
    }

    @Test
    public void testLongMinValue() {
        assertFormat(Long.MIN_VALUE, DoubleFormatter.MAX_PRECISION, "-9223372036854775808");
    }

    @Test
    public void testNaN() {
        try {
            new DoubleFormatter(3).format(Double.NaN, buffer, 0);
            fail("Failed to detect NaN");
        } catch (IllegalArgumentException expected) {
            // ignore
        }
    }

    @Test
    public void testInfinity() {
        try {
            new DoubleFormatter(3).format(Double.POSITIVE_INFINITY, buffer, 0);
            fail("Failed to detect INFINITY");
        } catch (IllegalArgumentException expected) {
            // ignore
        }
        try {
            new DoubleFormatter(3).format(Double.NEGATIVE_INFINITY, buffer, 0);
            fail("Failed to detect INFINITY");
        } catch (IllegalArgumentException expected) {
            // ignore
        }
    }

    @Test
    public void testEnum() {

        double number = 0.01 * Long.MIN_VALUE;
        while (number < -0.01) {
            assertFormat(number, 2);
            number = number / 10;
        }

        number = 0.01 * Long.MAX_VALUE;
        while (number > 0.01) {
            assertFormat(number, 2);
            number = number / 10;
        }
    }

    @Test
    public void testRandom() {

        Random rnd = new Random(13132);

        for (int i = 0; i < 100000; i++) {
            double number = rnd.nextDouble() * Math.pow(10, rnd.nextInt(8));
            assertFormat(number, 5);
        }
    }

    @Test
    public void testBug() {
        assertFormat(-92233720368547760.0, 2, "-92233720368547760");
    }

    @Test
    public void testBug1() {
        assertFormat(49405.994999999995, 2, "49405.99");
    }

    @Test
    public void testBug2() {
        assertFormat(1.123450000000001, 5, "1.12345");
        assertFormat(1.123450000000005, 5, "1.12345");
        assertFormat(1.1234599999999, 5, "1.12346");
        assertFormat(1.123455, 5, true, "1.12346");
        assertFormat(1.123455, 5, false, "1.12345");
        assertFormat(120.37500001, 5, true, "120.375");
        assertFormat(120.37500001, 5, false, "120.375");

        assertFormat(120.3751, 9, "120.3751");
        assertFormat(120.37501, 9, "120.37501");
        assertFormat(120.375001, 9, "120.375001");
        assertFormat(120.3750001, 9, "120.3750001");
        assertFormat(120.37500001, 9, "120.37500001");
        assertFormat(120.375000001, 9, "120.375000001");
        assertFormat(120.3750000001, 9, "120.375");
    }

    @Test
    public void test3() {
        double ask = 123.00006;
        double bid = 123.00005;
        double quote1 = bid + (ask-bid)/3;
        double quote2 = ask + (ask-bid)/3;

        // 1/10 of PIP precision
        assertFormat(quote1, 5, "123.00005");
        assertFormat(quote2, 5, "123.00006");

        // default precision
        assertFormat(quote1, 9, "123.000053333");
        assertFormat(quote2, 9, "123.000063333");

        // after rounding
        assertFormat(roundTickUp(quote1, 5), 9, "123.00006");
        assertFormat(roundTickUp(quote2, 5), 9, "123.00007");

        assertFormat(roundTickDown(quote1, 5), 9, "123.00005");
        assertFormat(roundTickDown(quote2, 5), 9, "123.00006");

    }

    @Test
    public void test4() {
        double value = 1.0 / 3;
        assertFormat(value, 9, "0.333333333");
        assertFormat(value*3, 9, "1");

        assertFormat(1.0/7, 9, "0.142857143");
    }

    private static double roundTickUp(double value, int precision) {
        double tickSize = 1;
        while (precision-- > 0)
            tickSize /= 10;

        double accuracy = tickSize * 0.01;
        return Math.ceil((value - accuracy) / tickSize) * tickSize;
    }

    private static double roundTickDown(double value, int precision) {
        double tickSize = 1;
        while (precision-- > 0)
            tickSize /= 10;

        double accuracy = tickSize * 0.01;
        return Math.floor((value + accuracy) / tickSize) * tickSize;
    }

    //TODO: Runs tpo slow (re-test periodically)
    //@Test
    public static void enumerateAll() {
        double number = -10000, step = 0.000003;

        while (number < 100000) {
            assertFormat(number, 2);
            number += step;
        }
    }

    //TODO: Runs tpo slow (re-test periodically)
    //@Test // Very slow (days)
    public void decimalRange() {
        long value = 0;

        while (value < 10_000_000_000L) {
            assertFormat( ((double)value) / 1_000_000_000, 9);
            value ++;
        }
    }


    ////////// Tools ///////////////////////////////////////////////////////////////////////////////////////////////////
//
//    private void verifyFormat (double value, int precision, String expectedFormat) {
//        verifyFormat (value, precision, true, expectedFormat);
//    }
//
//    private void verifyFormat (double value, int precision, boolean roundUp, String expectedFormat) {
//        DoubleFormatter df = DOUBLE_FORMATTERS[precision];
//        DecimalFormat standardFormat = DECIMAL_FORMATTERS[precision];
//        standardFormat.setRoundingMode(RoundingMode.HALF_UP);
//        System.out.println (value);
//        assertEquals("Java Format", expectedFormat, standardFormat.format(value));
//        assertFormat(df, value, standardFormat);
//    }
//
//    private static void assertFormat(double number, int precision, String expected) {
//        DoubleFormatter df = DOUBLE_FORMATTERS[precision];
//        byte [] buffer= new byte[MAX_WIDTH];
//        int length = df.format(number, buffer, 0);
//        String actual = new String(buffer, 0, length);
//        if (!expected.equals(actual)) {
//            assertEquals(number, Double.valueOf(actual), EPSILON);
//        }
//    }
//


    private static void assertFormat(double number, int precision) {
        assertFormat(number, precision, true);
    }

    private static void assertFormat(double number, int precision, boolean roundUp) {
        DecimalFormat standardFormat = DECIMAL_FORMATTERS[precision];
        standardFormat.setRoundingMode(roundUp ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN);
        String expected = standardFormat.format(number);
        if (expected.equals("-0"))
            expected = "0";

        assertFormat(number, precision, roundUp, expected);
    }

    private static void assertFormat(double number, int precision, String expected) {
        assertFormat(number, precision, true, expected);
    }

    private static void assertFormat(double number, int precision, boolean roundUp, String expected) {
        String actual;
        try {
            DoubleFormatter df = DOUBLE_FORMATTERS[precision];
            byte [] buffer= new byte[MAX_WIDTH];
            int length = df.format(number, precision, roundUp, DoubleFormatter.MAX_WIDTH, buffer, 0);
            actual = new String (buffer, 0, length);

            //System.out.println (number + "=>" + actual);
        } catch (Exception e) {
            throw new RuntimeException("Error formatting " + number + ": " + e.getMessage(), e);
        }

        if ( ! expected.equals(actual)) {
            // Slow method:
            String expectedBigDecimal = new BigDecimal(number).setScale(precision, (roundUp ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN)).toString();
            if ( ! expectedBigDecimal.equals(actual)) {
                System.err.printf("WARN: %f formatted as %s instead of %s with precision %d and rounding " + (roundUp ? "up" : "down") + '\n', number, actual, expected, precision);

                if (Math.abs(number - Double.valueOf(actual)) > EPSILON)
                    fail("Format mismatch for number " + number + ":\nexpected:" + expected + "\n  actual:" + actual);
            }
        }
    }

    public static void main(String[] args) {
        enumerateAll();
    }
}
