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
import org.f1x.util.TestUtils;

import java.text.DateFormat;
import java.util.Date;

public class Test_TimeOfDayFormatter {
    private final static long MILLIS_PER_DAY = 24*60*60*1000L;

    @Test
    public void simpleTest() {
        assertFormat(0, "00:00:00.000");
        assertFormat(1000, "00:00:01.000");
        assertFormat(10 * 1000, "00:00:10.000");
        assertFormat(60 * 1000, "00:01:00.000");
        assertFormat(3600 * 1000, "01:00:00.000");
        assertFormat(3601 * 1000, "01:00:01.000");
        assertFormat(3601 * 1000, "01:00:01.000");

        assertFormat(MILLIS_PER_DAY - 1, "23:59:59.999");
    }

    @Test
    public void simpleMSTest() {
        assertFormat(0, "00:00:00.000");
        assertFormat(1, "00:00:00.001");
        assertFormat(123, "00:00:00.123");
        assertFormat(1234, "00:00:01.234");
        assertFormat(12345, "00:00:12.345");
    }

    @Test
    public void simpleOneDayTruncation() {
        assertFormat(MILLIS_PER_DAY, "00:00:00.000");
        assertFormat(MILLIS_PER_DAY+1, "00:00:00.001");
        assertFormat(MILLIS_PER_DAY+1000, "00:00:01.000");
        assertFormat(5*MILLIS_PER_DAY+1000, "00:00:01.000");
    }

    @Test
    public void negativeInput() {
        assertBad(-1L);
    }

    @Test
    public void testCurrentTime () {
        assertTimestampFormat(1386436719851L);
        assertTimestampFormat(System.currentTimeMillis());
    }

    private static void assertTimestampFormat (long timestamp) {
        DateFormat UTC_TIME_ONLY_FORMAT = TestUtils.UTC_TIME_ONLY_FORMAT;

        String expected = UTC_TIME_ONLY_FORMAT.format(new Date(timestamp));

        byte [] buffer = new byte[TimeOfDayFormatter.LENGTH];
        int length = TimeOfDayFormatter.format(timestamp, buffer, 0);
        String actual = new String (buffer, 0, length);

        Assert.assertEquals(expected, actual);

    }

    private static void assertFormat(long time, String expected) {
        byte [] buffer = new byte [TimeOfDayFormatter.LENGTH + 1];
        int offset = TimeOfDayFormatter.format(time, buffer, 1);
        String actual = new String (buffer, 1, offset-1);
        Assert.assertEquals(expected, actual);
    }

    private static void assertBad(long time) {
        byte [] buffer = new byte [TimeOfDayFormatter.LENGTH];

        try {
            int offset = TimeOfDayFormatter.format(time, buffer, 0);
            String actual = new String (buffer, 0, offset);
            Assert.fail("Formatting of ("+time+") was supposed to fail, but instead it produced: " + actual);
        } catch (Exception expected) {
            // ignore
        }
    }
}
