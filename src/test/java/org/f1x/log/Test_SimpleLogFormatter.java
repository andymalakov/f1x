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

package org.f1x.log;

import org.junit.Assert;
import org.f1x.util.StoredTimeSource;
import org.f1x.util.TestUtils;
import org.f1x.util.TimeSource;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Test_SimpleLogFormatter {

    private final TimeSource TIME_SOURCE = StoredTimeSource.makeFromUTCTimestamp("20140101-10:10:10.100");

    @Test
    public void testAsIsLogFormatter () throws IOException {
        LogFormatter formatter = new AsIsLogFormatter();

        assertFormat(formatter, "", "");
        assertFormat(formatter, "ABC", "ABC");
    }

    @Test
    public void testSimpleLogFormatter () throws IOException {
        LogFormatter formatter = new SimpleLogFormatter(TIME_SOURCE);

        assertFormat(formatter, "10:10:10.100 IN", "", true);
        assertFormat(formatter, "10:10:10.100 IN  ABC", "ABC", true);
        assertFormat(formatter, "10:10:10.100 OUT ABC", "ABC", false);
    }

    private void assertFormat(LogFormatter formatter, String expectedOutput, String message) throws IOException {
        assertFormat(formatter, expectedOutput, message, true);
    }

    private void assertFormat(LogFormatter formatter, String expectedOutput, String message, boolean isInput) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        int len = formatter.log(isInput, TestUtils.wrap(message.getBytes(), 3), 3, message.length(), baos);
        Assert.assertEquals(baos.size(), len);
        String actualOutput = new String(baos.toByteArray());
        Assert.assertEquals("Formatted output", expectedOutput, actualOutput.trim());
    }
}
