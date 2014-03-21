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
import org.f1x.util.AsciiUtils;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class Test_SimpleMessageScanner {

    private final TestSimpleMessageScanner scanner = new TestSimpleMessageScanner(49, 56);

    @Test
    public void testSimpleGoodMessage() throws Exception {
        assertParsed("8=FIX.4.2|9=63|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "49=EZ, 56=SERVER");
    }

    /** Verify that parser returns correct number of bytes parsed */
    @Test
    public void testMoreThanOneMessage() throws Exception {
        assertMessageEnd("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|8=FIX.4.4|9=55|...", 104);
    }


    @Test
    public void testMissingBodyLenTag() throws Exception {
        assertScannerFailed("8=FIX.4.4|1=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|", "Missing BodyLength(9) tag");   // tag 1 instead of tag 9
    }

    @Test
    public void testSplitLogonMessage() throws Exception {
        assertParsed          ("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|", "49=CLIENT, 56=SERVER");

        // now let's try various cases where buffer does not contain entire message
        assertMessageTruncated("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080", 1); // missing final SOH
        assertMessageTruncated("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|", 7); // missing CheckSum(10) field
        assertMessageTruncated("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|", 44); // missing everything starting from tag TargetCompID(56)
        assertMessageTruncated("8=FIX.4.2|9=63|35=A|34=1|49=EZ", 55);

        assertScannerFailed("8=FIX.4.2|9=63", "Message is too small");
    }

    @Test
    public void testMissingTags() throws Exception {
        // what if both tags we are looking for are missing?
        assertParsed("8=FIX.4.2|9=47|35=A|34=1|52=20131004-02:27:25.762|98=0|108=30|10=000|", "");
        assertParsed("8=FIX.4.2|9=53|35=A|34=1|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=000|", "56=SERVER");
        assertParsed("8=FIX.4.2|9=53|35=A|34=1|49=EZ|52=20131004-02:27:25.762|98=0|108=30|10=XXX|", "49=EZ");
    }


    @Test
    public void testSmallMessage () {
        assertScannerFailed("", "Message is too small");
        assertScannerFailed(" ", "Message is too small");
        assertScannerFailed("8=FIX", "Message is too small");
        assertScannerFailed("8=FIX.4.4|", "Message is too small");
    }

    @Test
    public void testNotAFixMessage () {
        assertScannerFailed("I Don't know what is this but not a FIX message", "Not a FIX message");
    }

    @Test
    public void testBadTagNum () {
        assertScannerFailed("8=FIX.4.2|9=63|ABC=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "Expecting a number");
    }

    @Test
    public void testBadBodyLength () {
        assertScannerFailed("8=FIX.4.2|9=ABC|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|98=0|108=30|10=098|", "Expecting a number");
    }

    @Test
    public void testDuplicateTag () throws IOException {
        assertScannerFailed("8=FIX.4.2|9=53|35=A|34=1|49=EZ|52=20131004-02:27:25.762|49=EZ|56=SERVER|10=098|", "Invalid Logon message: duplicate tag 49 or missing BodyLength(9) tag");
        assertScannerFailed("8=FIX.4.2|9=61|35=A|34=1|49=EZ|52=20131004-02:27:25.762|56=SERVER|56=SERVER|10=098|", "Invalid Logon message: duplicate tag 56 or missing BodyLength(9) tag");
    }


    @Test
    public void testInterruptedParsing () throws IOException {
        SingleTagMessageScanner scanner = new SingleTagMessageScanner(49);

        String message = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|8=FIX...";

        byte [] msgBytes = AsciiUtils.getBytes(message.replace('|', '\u0001'));


        try {
            int result = scanner.parse(msgBytes, 0, msgBytes.length, null);
            if (result <= 0)
                Assert.fail("Message is expected to be complete but buffer is too small (missing " + (-result) + " bytes)");

            String actualTagValue = new String (msgBytes, scanner.tagValueStart, scanner.tagValueLen, "US-ASCII");
            Assert.assertEquals("CLIENT", actualTagValue);

            String nextMessage = message.substring(result);
            Assert.assertEquals("8=FIX...", nextMessage);

        } catch (FixParserException e) {
            e.printStackTrace();
            Assert.fail("Unexpected parser error: " + e.getMessage());
        }

    }

    /// Helpers


    private void assertScannerFailed(String message, String expectedError) {
        try {
            int offset = parse(message);
            if (offset > 0)
                Assert.fail("Scanner was expected to fail but it didn't: " + message);
            else
                Assert.fail("Scanner was expected to fail with exception but it reported truncated message instead (Missing " + (-offset) + " bytes)");
        } catch (Exception e) {
            Assert.assertEquals(expectedError, e.getMessage());
        }
    }

    private void assertMessageTruncated(String message, int expectedMissingByteCount) {
        int actualMissingByteCount = parse(message);
        Assert.assertTrue("Parser didn't detect that message is truncated", actualMissingByteCount < 0);
        Assert.assertEquals("Trucated message tail size", expectedMissingByteCount, -actualMissingByteCount);
    }

    private void assertMessageEnd(String message, int expectedByteCount) {
        int actualByteCount = parse(message);

        if (actualByteCount <= 0)
            Assert.fail("Message was supposed to parse normally but instead parser reported missing " + (-actualByteCount) + " bytes");

        Assert.assertEquals("Parsed byte count", expectedByteCount, actualByteCount);
    }

    private static final int WRAPPER = 3;

    public static byte [] wrap (String message, int wrapSize) {
        byte [] msgBytes = AsciiUtils.getBytes(message.replace('|', '\u0001'));
        return TestUtils.wrap(msgBytes, wrapSize);
    }

    private void assertParsed(String message, String extractedTags) throws Exception {

        try {
            int offset = parse(message);
            if (offset <= 0)
                Assert.fail("Message is expected to be complete but buffer is too small (missing " + (-offset) + " bytes)");

        } catch (FixParserException e) {
            e.printStackTrace();
            Assert.fail("Unexpected parser error: " + e.getMessage());
        }

        byte [] msgBytes = wrap(message, WRAPPER);

        StringBuilder sb = new StringBuilder();
        for (int i=0; i < scanner.tagsToWatch.length; i++) {
           if (scanner.tagValueStart[i] != -1) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(scanner.tagsToWatch[i]);
                sb.append('=');
                sb.append(new String (msgBytes, scanner.tagValueStart[i], scanner.tagValueLen[i], "US-ASCII"));
            }
        }
        Assert.assertEquals(extractedTags, sb.toString());
    }

    private int parse(String message) throws SimpleMessageScanner.LogonParserException {
        byte [] msgBytes = AsciiUtils.getBytes(message.replace('|', '\u0001'));
        int result = scanner.parse(wrap(message, WRAPPER), WRAPPER, msgBytes.length, null);
        if (result >= 0) {
            Assert.assertTrue(result > WRAPPER);
            result = result - WRAPPER;
        }
        return result;
    }

    class TestSimpleMessageScanner extends SimpleMessageScanner<Object> {
        final int [] tagsToWatch;
        final int [] tagValueStart;
        final int [] tagValueLen;

        TestSimpleMessageScanner(int... tagsToWatch) {
            this.tagsToWatch = tagsToWatch;
            this.tagValueStart = new int [tagsToWatch.length];
            this.tagValueLen = new int [tagsToWatch.length];
        }

        @Override
        public int parse(byte[] buffer, int start, int len, Object cookie) throws LogonParserException {
            //tagsLeft = tagsToWatch.length;
            Arrays.fill(tagValueStart, -1);
            Arrays.fill(tagValueLen, -1);
            return super.parse(buffer, start, len, cookie);
        }

        @Override
        protected boolean onTagNumber(int tagNum, Object cookie) throws FixParserException {
            return (indexOf(tagNum) >= 0);
        }

        @Override
        protected boolean onTagValue(int tagNum, byte[] message, int tagValueStart, int tagValueLen, Object cookie) throws FixParserException {
            int currentTagIndex = indexOf(tagNum);
            assert currentTagIndex != -1;
            if (this.tagValueStart[currentTagIndex] != -1)
                throw new FixParserException("Invalid Logon message: duplicate tag " + tagNum + " or missing BodyLength(9) tag");

            this.tagValueStart[currentTagIndex] = tagValueStart;
            this.tagValueLen[currentTagIndex] = tagValueLen;
            return true; // continue parsing
        }

        private int indexOf(int tagNum) {
            for (int i=0; i < tagsToWatch.length; i++) {
                if (tagsToWatch[i] == tagNum)
                    return i;
            }
            return -1;
        }
    }

    class SingleTagMessageScanner extends SimpleMessageScanner<Object> {
        final int tagToWatch;
        int tagValueStart;
        int tagValueLen;

        SingleTagMessageScanner(int tagToWatch) {
            this.tagToWatch = tagToWatch;
        }

        @Override
        protected boolean onTagNumber(int tagNum, Object cookie) throws FixParserException {
            return tagToWatch == tagNum;
        }

        @Override
        protected boolean onTagValue(int tagNum, byte[] message, int tagValueStart, int tagValueLen, Object cookie) throws FixParserException {
            this.tagValueStart = tagValueStart;
            this.tagValueLen = tagValueLen;
            return false; // stop parsing
        }
    }

}
