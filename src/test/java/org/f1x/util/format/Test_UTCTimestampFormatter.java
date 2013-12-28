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

import junit.framework.Assert;
import org.junit.Test;
import org.f1x.util.TestUtils;

import java.text.DateFormat;
import java.util.Date;

public class Test_UTCTimestampFormatter {

    private final DateFormat dateTimeFormat = TestUtils.UTC_TIMESTAMP_FORMAT;
    private final DateFormat dateOnlyFormat = TestUtils.UTC_DATE_ONLY_FORMAT;
    private final TimestampFormatter customFormat = TimestampFormatter.createUTCTimestampFormatter();

    @Test
    public void test () {
        long now = System.currentTimeMillis();
        assertDateTimeFormat(now);
        assertDateOnlyFormat(now);
    }

    private void assertDateTimeFormat(long timestamp) {
        String expected = dateTimeFormat.format(new Date(timestamp));

        byte [] buffer = new byte[TimestampFormatter.DATE_TIME_LENGTH];
        int length = customFormat.formatDateTime(timestamp, buffer, 0);
        String actual = new String (buffer, 0, length);

        Assert.assertEquals(expected, actual);
    }

    private void assertDateOnlyFormat(long timestamp) {
        String expected = dateOnlyFormat.format(new Date(timestamp));

        byte [] buffer = new byte[TimestampFormatter.DATE_ONLY_LENGTH];
        int length = customFormat.formatDateOnly(timestamp, buffer, 0);
        String actual = new String (buffer, 0, length);

        Assert.assertEquals(expected, actual);
    }
}
