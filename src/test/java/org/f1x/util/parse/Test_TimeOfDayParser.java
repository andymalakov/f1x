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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class Test_TimeOfDayParser {

    @Test
    public void testBadCasesTime() {
        assertBad("");
        assertBad(":");
        assertBad(":10");
        assertBad(":10 am");
        assertBad("10 amABC");

        // extra : character
        assertBad("1:");
        assertBad("1: am");
        assertBad("11:");
        assertBad("11: am");
        assertBad("1:1:");
        assertBad("1:1:am");
        assertBad("11:11:");
        assertBad("11:11:am");
        assertBad("1:1:1:");
        assertBad("1:1:1: am");
        assertBad("11:11:11:");
        assertBad("11:11:11: am");

        // h/m/s exceed max allowed
        assertBad("24:00:00");
        assertBad("00:60:00");
        assertBad("00:00:60");

        // illegal am-pm hours
        assertBad("00:00 am");
        assertBad("00:00 pm");
        assertBad("13:00 pm");
        assertBad("13:00 am");

        // maximum two digits per group
        assertBad("001:01:01");
        assertBad("01:001:01");
        assertBad("01:01:001");

        // no trailing/leading spaces and space separators
        assertBad(" 11:11:11");
        assertBad("11: 11:11");
        assertBad("11:11: 11");
        assertBad("11:11:11 ");

        // unterminated string
        assertBad("1:");
        assertBad("1:1:");
        assertBad("1:1:1 ");
        assertBad("1:1:1 a");

        assertBad("-1:01:01");
        assertBad("01:-1:01");
        assertBad("01:01:-1");

    }

    @Test
    public void testOneDigitCasesTime() {
        assertTimeParsed("00:00:00", 0);
        assertTimeParsed("00:01:00", 1 * 60_000);
        assertTimeParsed("01:01:00", 1 * 60 * 60_000 + 1 * 60_000);
        assertTimeParsed("00:00:01", 1000);
        assertTimeParsed("00:01:01", 1 * 60_000 + 1000);
        assertTimeParsed("01:01:01", 1 * 60 * 60_000 + 1 * 60_000 + 1000);

        assertTimeParsed("00:00:00.000", 0);
        assertTimeParsed("00:01:00.000", 1 * 60_000);
        assertTimeParsed("01:01:00.000", 1 * 60 * 60_000 + 1 * 60_000);
        assertTimeParsed("00:00:01.000", 1000);
        assertTimeParsed("00:01:01.000", 1 * 60_000 + 1000);
        assertTimeParsed("01:01:01.000", 1 * 60 * 60_000 + 1 * 60_000 + 1000);

        assertTimeParsed("00:00:00.001", 1);
        assertTimeParsed("00:01:00.001", 1 * 60_000 + 1);
        assertTimeParsed("01:01:00.001", 1 * 60 * 60_000 + 1 * 60_000 + 1);
        assertTimeParsed("00:00:01.001", 1000 + 1);
        assertTimeParsed("00:01:01.001", 1 * 60_000 + 1000 + 1);
        assertTimeParsed("01:01:01.001", 1 * 60 * 60_000 + 1 * 60_000 + 1000 + 1);

        assertTimeParsed("00:00:00.321", 321);
        assertTimeParsed("00:01:00.321", 1 * 60_000 + 321);
        assertTimeParsed("01:01:00.321", 1 * 60 * 60_000 + 1 * 60_000 + 321);
        assertTimeParsed("00:00:01.321", 1000 + 321);
        assertTimeParsed("00:01:01.321", 1 * 60_000 + 1000 + 321);
        assertTimeParsed("01:01:01.321", 1 * 60 * 60_000 + 1 * 60_000 + 1000 + 321);
    }

    @Test
    public void testMilitaryFormatTime() {
        assertTimeParsed("01:00:00",  1 * 60 * 60_000);
        assertTimeParsed("12:00:00", 12 * 60 * 60_000);
        assertTimeParsed("12:03:00", 12 * 60 * 60_000 +  3 * 60_000);
        assertTimeParsed("12:34:00", 12 * 60 * 60_000 + 34 * 60_000);
        assertTimeParsed("12:34:05", 12 * 60 * 60_000 + 34 * 60_000 + 5000);
        assertTimeParsed("12:34:56", 12 * 60 * 60_000 + 34 * 60_000 + 56000);

        assertTimeParsed("01:00:00.000",  1 * 60 * 60_000);
        assertTimeParsed("12:00:00.000", 12 * 60 * 60_000);
        assertTimeParsed("12:03:00.000", 12 * 60 * 60_000 +  3 * 60_000);
        assertTimeParsed("12:34:00.000", 12 * 60 * 60_000 + 34 * 60_000);
        assertTimeParsed("12:34:05.000", 12 * 60 * 60_000 + 34 * 60_000 + 5000);
        assertTimeParsed("12:34:56.000", 12 * 60 * 60_000 + 34 * 60_000 + 56000);
    }

//    private static int standardParse(String value) {
//
//        StringTokenizer t = new StringTokenizer(value, ":", false);
//
//        int numTokens = t.countTokens();
//        int seconds = 0;
//
//        if (numTokens > 2)
//            seconds += Integer.parseInt(t.nextToken()) * 3600;
//
//        if (numTokens > 1)
//            seconds += Integer.parseInt(t.nextToken()) * 60
//                + Integer.parseInt(t.nextToken());
//        else if (numTokens > 0)
//            seconds += Integer.parseInt(t.nextToken()) * 60;
//
//        return seconds;
//    }

    private static void assertTimeParsed(String input, int expectedSeconds) {
        try {
            byte [] inputBytes = input.getBytes();

            int actualSeconds = TimeOfDayParser.parseTimeOfDay(wrap(inputBytes), 1, inputBytes.length);
            Assert.assertEquals("Number of seconds in '" + input + '\'',expectedSeconds, actualSeconds);
        } catch (NumberFormatException ex) {
            Assert.fail("Parsing of '" + input + "' failed with message [" + ex.getMessage() + ']');
        }
    }

    private static byte[] wrap(byte[] valueBytes) {
        //to make it more interesting
        byte [] result = new byte[valueBytes.length + 2];
        Arrays.fill(result, 0, result.length, (byte) '9');
        System.arraycopy(valueBytes, 0, result, 1, valueBytes.length);
        return result;
    }

    private static void assertBad(String input) {
        try {
            byte [] inputBytes = input.getBytes();
            TimeOfDayParser.parseTimeOfDay(inputBytes, 0, inputBytes.length);
            Assert.fail("Expected to detect a problem in time string \"" + input + '"');
        } catch (Exception expected) {
        }
    }

}
