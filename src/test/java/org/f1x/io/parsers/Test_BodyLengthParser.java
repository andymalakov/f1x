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

import org.f1x.util.AsciiUtils;
import org.f1x.util.ByteRingReader;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Test_BodyLengthParser {

    @Test
    public void testBodyLength () {
        assertBodyLength("8=FIX.4.4\u00019=0\u000110=056\u0001", 0);
        assertBodyLength("8=FIX.4.4\u00019=1\u000135=A\u000134=1\u0001", 1);
        assertBodyLength("8=FIX.4.1\u00019=12\u000135=A\u000134=1\u0001", 12);
        assertBodyLength("8=FIX.5.0\u00019=123\u000135=A\u000134=1\u0001", 123);
        assertBodyLength("8=FIX.5.0\u00019=1234\u000135=A\u000134=1\u0001", 1234);
        assertBodyLength("8=FIX.5.0\u00019=000001234\u000135=A\u000134=1\u0001", 1234);
    }

    @Test
    public void testMessageSize () {
        assertRemainingMessageSize("8=FIX.4.1\u00019=0\u000110=056\u0001", 0);
        assertRemainingMessageSize("8=FIX.4.2\u00019=73\u000135=1\u000134=4\u000149=CITIFX\u000156=XXXXXXXXXX\u000152=20131013-21:10:22\u0001112=1381698622643\u000110=143\u0001", 0);
        assertRemainingMessageSize("8=FIX.4.3\u00019=128\u000135=A\u000134=1\u000149=XXXXXXXXXX\u000150=XXXXXXXXXX\u000152=20131013-21:10:17.498\u000156=CITIFX\u000157=CITIFX\u000198=0\u0001108=30\u0001141=Y\u0001553=DT130429\u0001554=*********\u000110=075\u0001", 0);
    }

    @Test
    public void testInvalidMessagePrefix () {
        // Bad Prefix
        assertInvalid("NOT-FIX\u00019=73\u000135=1\u000134=4\u000149=CITIFX\u000156=XXXXXXXXXX\u000152=20131013-21:10:22\u0001112=1381698622643\u000110=143\u0001",
            "Message must begin with BeginString and BodyLength tags");

        // Bad length (1X)
        assertInvalid("8=FIX.4.4\u00019=\u000135=1\u000134=4\u000149=CITIFX\u000156=XXXXXXXXXX\u000152=20131013-21:10:22\u0001112=1381698622643\u000110=143\u0001",
            "Message BodyLength is empty");
        assertInvalid("8=FIX.4.4\u00019=X\u000135=1\u000134=4\u000149=CITIFX\u000156=XXXXXXXXXX\u000152=20131013-21:10:22\u0001112=1381698622643\u000110=143\u0001",
            "Message BodyLength is invalid");
        assertInvalid("8=FIX.4.4\u00019=1X\u000135=1\u000134=4\u000149=CITIFX\u000156=XXXXXXXXXX\u000152=20131013-21:10:22\u0001112=1381698622643\u000110=143\u0001",
            "Message BodyLength is invalid");
    }

    @Test
    public void testPerformance () {
        String message = "8=FIX.4.3\u00019=128\u000135=A\u000134=1\u000149=XXXXXXXXXX\u000150=XXXXXXXXXX\u000152=20131013-21:10:17.498\u000156=CITIFX1\u000157=CITIFX1\u000198=0\u0001108=30\u0001141=Y\u0001553=DT130429\u0001554=*********\u000110=075\u0001";
        final int messageSize = message.length();
        ByteRingReader reader = new ByteRingReader(AsciiUtils.getBytes(message));


        final int WARMUP = 20000;
        final int TEST   = 200000;

        int result = 1;
        for (int i=0; i < WARMUP; i++) {
            reader.reset(0, messageSize);
            result += parseBodyLength(reader);
        }
        long start = System.currentTimeMillis();
        for (int i=0; i < TEST; i++) {
            reader.reset(0, messageSize);
            result |= parseBodyLength(reader);
        }
        long end = System.currentTimeMillis();
        System.out.println(" Cost of BodyLengthParser call: " + 1000000L*(end-start)/TEST + " nanoseconds/call");
        if (result % 2 == 1)
            System.out.print("Odd");
    }

    private static final byte [] EXPECTED_HEADER = AsciiUtils.getBytes("8=FIX.*.*\0019=");
    private static final int EXPECTED_HEADER_LENGTH = EXPECTED_HEADER.length;


    private static int parseBodyLength(ByteRingReader reader) {
        //BodyLengthParser.validateMessagePrefix(reader);
        reader.skip(EXPECTED_HEADER_LENGTH);
        return BodyLengthParser.parseBodyLength(reader);
    }


    private static void assertInvalid(String header, String expectedError) {
        try {
            ByteRingReader reader = mockByteRingReader(header);
            BodyLengthParser.getRemainingMessageSize(reader);
            fail("Parsing was supposed to fail but it didn't");
        } catch (Exception expected) {
            assertEquals(expectedError, expected.getMessage());
        }
    }


    private static void assertBodyLength (String header, int expectedBodyLength) {
        ByteRingReader reader = mockByteRingReader(header);
        BodyLengthParser.validateMessagePrefix(reader);
        int actualBodyLength = BodyLengthParser.parseBodyLength(reader);
        Assert.assertEquals(expectedBodyLength, actualBodyLength);
    }

    private static void assertRemainingMessageSize(String header, int expectedMessageSize) {
        ByteRingReader reader = mockByteRingReader(header);
        int actualMessageSize = BodyLengthParser.getRemainingMessageSize(reader);
        Assert.assertEquals(expectedMessageSize, actualMessageSize);
    }

    static ByteRingReader mockByteRingReader (String text) {
        ByteRingReader reader = new ByteRingReader(AsciiUtils.getBytes(text));
        reader.reset(0, text.length());
        return  reader;
    }
}
