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

package org.f1x.io.parsers;


import org.f1x.api.FixParserException;
import org.f1x.io.parsers.LogonMessageScanner;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Test_LogonMessageScanner {

    class TestLogonMessageScanner extends LogonMessageScanner<Object> {
        int [] tagsToWatch;
        int [] tagStart;
        int [] tagLen;

        TestLogonMessageScanner(int... tagsToWatch) {
            this.tagsToWatch = tagsToWatch;
            this.tagStart = new int [tagsToWatch.length];
            this.tagLen = new int [tagsToWatch.length];
        }

        @Override
        public void scan(byte[] buffer, int start, int len, Object cookie) throws FixParserException {
            //tagsLeft = tagsToWatch.length;
            Arrays.fill(tagStart, -1);
            Arrays.fill(tagLen, -1);
            super.scan(buffer, start, len, cookie);
        }

        @Override
        protected boolean onTagNumber(int tagNum, Object cookie) throws FixParserException {
            return (indexOf(tagNum) >= 0);
        }

        @Override
        protected boolean onTagValue(int tagNum, byte[] message, int tagValueStart, int tagValueLen, Object cookie) throws FixParserException {
            int currentTagIndex = indexOf(tagNum);
            assert currentTagIndex != -1;
            if (tagStart[currentTagIndex] != -1)
                throw new FixParserException("Invalid Logon message: duplicate tag " + tagNum + " or missing BodyLength(9) tag");

            tagStart[currentTagIndex] = tagValueStart;
            tagLen[currentTagIndex] = tagValueLen;
//                        if (--tagsLeft == 0)
//                            return false;
            return true;
        }

        private int indexOf(int tagNum) {
            for (int i=0; i < tagsToWatch.length; i++) {
                if (tagsToWatch[i] == tagNum)
                    return i;
            }
            return -1;
        }

    }

    private TestLogonMessageScanner scanner = new TestLogonMessageScanner(49, 56);

    @Test
    public void test1() throws IOException {
        assertScanned("8=FIX.4.2|9=63|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "49=EZ, 56=SERVER");
    }

    @Test
    public void testMissingTags() throws IOException {
        // what if both tags we are looking for are missing?
        assertScanned("8=FIX.4.2|9=47|35=A|34=1|52=20131004-02:27:25.762|98=0|108=30|10=000|", "");
        assertScanned("8=FIX.4.2|9=53|35=A|34=1|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=000|", "56=SERVER");
        assertScanned("8=FIX.4.2|9=55|35=A|34=1|49=EZ|52=20131004-02:27:25.762|98=0|108=30|10=XXX|", "49=EZ");
    }

    @Test
    public void testSpecialCases() throws IOException {

    }

    @Test
    public void testEmptyMessage () {
        assertScannerFailed("", null);
        assertScannerFailed(" ", "Invalid Logon message: message should begin with standard FIX prefix");
        assertScannerFailed("8=FIX", "Invalid Logon message: message should begin with standard FIX prefix");
    }

    @Test
    public void testBadTagNum () {
        assertScannerFailed("8=FIX.4.2|9=63|ABC=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "Invalid Logon message: non-numeric character appears in tag number (position 15)");
    }

    @Test
    public void testBadBodyLength () {
        assertScannerFailed("8=FIX.4.2|9=ABC|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "Invalid Logon message: non-numeric character appears in tag number (position 12)");
    }

    @Test
    public void testDuplicateTag () throws IOException {
        assertScannerFailed("8=FIX.4.2|9=64|35=A|34=1|49=EZ|52=20131004-02:27:25.762|49=EZ|56=SERVER|10=098|", "Invalid Logon message: duplicate tag 49 or missing BodyLength(9) tag");

        assertScannerFailed("8=FIX.4.2|9=64|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|56=SERVER|10=098|", "Invalid Logon message: duplicate tag 56 or missing BodyLength(9) tag");
    }

    private void assertScannerFailed(String message, String expectedError) {
        try {
            scan(message);
            Assert.fail("Scanner was expected to fail but it didn't: " + message);
        } catch (Exception e) {
            Assert.assertEquals(expectedError, e.getMessage());
        }
    }
    
    private void assertScanned(String message, String extractedTags) throws IOException {

        try {
            scan(message);
        } catch (FixParserException e) {
            e.printStackTrace();
            Assert.fail("Unexpected parser error: " + e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        for (int i=0; i < scanner.tagsToWatch.length; i++) {
           if (scanner.tagStart[i] != -1) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(scanner.tagsToWatch[i]);
                sb.append('=');
                sb.append(new String (message.getBytes("US-ASCII"), scanner.tagStart[i], scanner.tagLen[i], "US-ASCII"));
            }
        }
        Assert.assertEquals(extractedTags, sb.toString());
    }

    private void scan(String message) throws UnsupportedEncodingException, FixParserException {
        message = message.replace('|', '\u0001');
        byte [] msgBytes = message.getBytes("US-ASCII");
        scanner.scan(msgBytes, 0, msgBytes.length, null);
    }
}
