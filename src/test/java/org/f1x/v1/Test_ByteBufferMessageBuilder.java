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

package org.f1x.v1;

import org.junit.Assert;
import org.junit.Test;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.fields.*;
import org.f1x.util.TestUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Test_ByteBufferMessageBuilder {
    private MessageBuilder mb = new ByteBufferMessageBuilder(256, 3);

    @Test
    public void testEveryKindOfAdd () {
        mb.add(1, "ABC");
        mb.add(2, "ABC", 1, 2);
        mb.add(3, 123L);
        mb.add(4, 123);
        mb.add(5, 3.14159);
        mb.add(6, (byte) 'x');
        mb.add(7, true);
        mb.add(8, false);
        mb.add(9, Side.BUY);
        mb.add(10, OrdRejReason.ORDER_EXCEEDS_LIMIT);
        mb.add(11, MsgType.NEWS);
        mb.addUTCTimestamp(12, TestUtils.parseUTCTimestamp("20121009-13:44:49.421"));
        mb.addUTCDateOnly(13, TestUtils.parseUTCTimestamp("20121009-00:00:00.000"));
        mb.addUTCTimeOnly(14, TestUtils.parseUTCTimestamp("20121009-13:44:49.421"));
        String utcTimestampForMktDate = "20121009-00:00:00.000";
        mb.addLocalMktDate(15, TestUtils.parseUTCTimestamp(utcTimestampForMktDate));
        byte [] rawField = "RAW".getBytes();
        mb.addRaw(16, rawField, 0, rawField.length);
        mb.add(17).append((byte)'A').append('B').append("CD").append("**EF**", 2, 4).append(123).append(456L).append(.001).end();

        assertContentEquals("1=ABC|2=B|3=123|4=123|5=3.142|6=x|7=Y|8=N|9=1|10=3|11=B|12=20121009-13:44:49.421|13=20121009|14=13:44:49.421|15=" +
                getMktDateFromUTCTimestamp(utcTimestampForMktDate) +
                "|16=RAW|17=ABCDEF1234560.001|");
    }

    @Test
    public void testEncoding () throws UnsupportedEncodingException {
        mb.add(FixTags.MessageEncoding, MessageEncoding.SHIFT_JIS);
        mb.add(FixTags.Text, "Hello");
        String hello = new String ("\u3053\u3093\u306b\u3061\u306f");
        byte [] helloEncoded = hello.getBytes("Shift_JIS");

        mb.add(FixTags.EncodedTextLen, helloEncoded.length);
        mb.addRaw(FixTags.EncodedText, helloEncoded, 0, helloEncoded.length);

        assertContentEquals("347=SHIFT_JIS|58=Hello|354=10|355=\ufffd\ufffd\ufffd\ufffd\u0242\ufffd\ufffd\ufffd|");
    }

    @Test
    public void testRepeatingGroup () throws UnsupportedEncodingException {
        mb.add(FixTags.NoMDEntries, 2);

        mb.add(FixTags.MDEntryType, MDEntryType.BID);
        mb.add(FixTags.MDEntryPx, 12.32);
        mb.add(FixTags.MDEntrySize, 100);
        mb.add(FixTags.QuoteEntryID, "BID123");

        mb.add(FixTags.MDEntryType, MDEntryType.OFFER);
        mb.add(FixTags.MDEntryPx, 12.32);
        mb.add(FixTags.MDEntrySize, 100);
        mb.add(FixTags.QuoteEntryID, "OFFER123");

        assertContentEquals("268=2|269=0|270=12.32|271=100|299=BID123|269=1|270=12.32|271=100|299=OFFER123|");
    }

    @Test
    public void testMessageTooLarge () throws UnsupportedEncodingException {
        byte [] largeField = new byte [200];
        Arrays.fill(largeField, (byte)'a');

        try {
            mb.addRaw(FixTags.Text, largeField, 0, largeField.length);
            mb.output(new byte[largeField.length + 4 - 1], 0);
            Assert.fail("Failed to detect out of bounds");
        } catch (IndexOutOfBoundsException expected) {
            // ok
        }
    }

    private void assertContentEquals(String expected) {
        byte [] buffer = new byte[mb.getLength()];
        mb.output(buffer, 0);
        String actual = new String(buffer).replace('\u0001', '|');
        Assert.assertEquals(expected, actual);
    }

    private String getMktDateFromUTCTimestamp(String utc) {
        long utcTimeInMillis = TestUtils.parseUTCTimestamp(utc);
        return TestUtils.LOCAL_TIMESTAMP_FORMAT.format(utcTimeInMillis).substring(0, 8);
    }

}
