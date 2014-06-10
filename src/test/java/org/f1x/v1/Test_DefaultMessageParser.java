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
import org.f1x.api.message.Tools;
import org.f1x.api.message.fields.MsgType;
import org.f1x.api.message.fields.OrdRejReason;
import org.f1x.api.message.fields.Side;
import org.f1x.api.message.types.ByteEnumLookup;
import org.f1x.api.message.types.IntEnumLookup;
import org.f1x.util.ByteArrayReference;
import org.f1x.util.TestUtils;
import org.f1x.util.format.TimestampFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class Test_DefaultMessageParser {

    private final DateFormat utcTimestampFormat = TestUtils.UTC_TIMESTAMP_FORMAT;
    private DefaultMessageParser parser = new DefaultMessageParser();


    @Test
    public void testLogon() {
        assertParser("8=FIX.4.4\u00019=116\u000135=A\u000134=1\u000149=DEMO2Kweoj_DEMOFIX\u000152=20121009-13:14:57.089\u000156=DUKASCOPYFIX\u000198=0\u0001108=30\u0001141=Y\u0001553=DEMO2Kweoj\u0001554=**********\u000110=202\u0001");
    }

    @Test
    public void testNewOrderSingle() {
        assertParser("8=FIX.4.4\u00019=144\u000135=D\u000134=6\u000149=DEMO2Kweoj_DEMOFIX\u000152=20121009-13:59:01.666\u000156=DUKASCOPYFIX\u000111=512\u000121=1\u000138=1000\u000140=1\u000154=1\u000155=EUR/USD\u000159=1\u000160=20121009-13:59:01.666\u000110=000\u0001");
    }

    @Test
    public void testExecutionReport() {
        assertParser("8=FIX.4.4\u00019=251\u000135=8\u000134=7\u000149=DUKASCOPYFIX\u000152=20121009-13:59:21.158\u000156=DEMO2Kweoj_DEMOFIX\u00016=0\u000111=506\u000114=0\u000117=506\u000137=506\u000138=0\u000139=8\u000154=7\u000155=UNKNOWN\u000158=Your order has been rejected due to validation failure.  Order amount can't be less than <MIN_OPEN_AMOUNT>\u0001150=8\u0001151=0\u000110=196\u0001");
    }

    @Test
    public void testBasicTypesParser() throws ParseException {
        String order = "8=FIX.4.4\u00019=144\u000135=D\u000134=6\u000149=DEMO2Kweoj_DEMOFIX\u000152=20121009-13:59:01.666\u000156=DUKASCOPYFIX\u000111=512\u000121=1\u000138=1000\u000140=1\u000154=1\u000155=EUR/USD\u000159=1\u000160=20121009-13:59:01.666\u000110=000\u0001";
        String report = "8=FIX.4.4\u00019=251\u000135=8\u000134=7\u000149=DUKASCOPYFIX\u000152=20121009-13:59:21.158\u000156=DEMO2Kweoj_DEMOFIX\u00016=0\u000111=506\u000114=0\u000117=506\u000137=506\u000138=0\u000139=8\u000154=7\u000155=UNKNOWN\u000158=Your order has been rejected due to validation failure.  Order amount can't be less than <MIN_OPEN_AMOUNT>\u0001150=8\u0001151=0\u000110=196\u0001";


        parseTag (report, 49);
        assertEquals("DUKASCOPYFIX", parser.getStringValue());

        parseTag (report, 49);
        assertTrue(Tools.equals("DUKASCOPYFIX", parser.getCharSequenceValue()));

        parseTag (report, 11);
        assertEquals(506, parser.getIntValue());

        parseTag (report, 11);
        assertEquals(506, parser.getLongValue());

        parseTag (order, 59);
        assertEquals('1', parser.getByteValue());

        parseTag (order, 60);
        assertEquals(utcTimestampFormat.parse("20121009-13:59:01.666").getTime(), parser.getUTCTimestampValue());


    }


    @Test
    public void testEveryTypeParser () throws ParseException {
        DateFormat localDateFormat = TestUtils.createDateFormat(TimestampFormatter.DATE_ONLY_FORMAT, TimeZone.getDefault());
        MessageBuilder mb = new ByteBufferMessageBuilder(256, 5);

        mb.add(1, "ABC");
        mb.add(2, 123L);
        mb.add(3, 123);
        mb.add(4, 3.14159);
        mb.add(5, (byte) 'x');
        mb.add(6, true);
        mb.add(7, false);
        mb.add(8, Side.BUY);
        mb.add(9, OrdRejReason.ORDER_EXCEEDS_LIMIT);
        mb.add(10, MsgType.NEWS);
        mb.addUTCTimestamp(11, TestUtils.parseUTCTimestamp("20121009-13:44:49.421"));
        mb.addUTCDateOnly(12, TestUtils.parseUTCTimestamp("20121009-00:00:00.000"));
        mb.addUTCTimeOnly(13, TestUtils.parseUTCTimestamp("20121009-13:44:49.421"));
        mb.addLocalMktDate(14, localDateFormat.parse("20121122").getTime());
        byte [] rawField = "RAW".getBytes();
        mb.addRaw(15, rawField, 0, rawField.length);

        byte [] buffer = new byte[mb.getLength()];
        mb.output(buffer, 0);

        Assert.assertEquals("1=ABC|2=123|3=123|4=3.14159|5=x|6=Y|7=N|8=1|9=3|10=B|11=20121009-13:44:49.421|12=20121009|13=13:44:49.421|14=20121122|15=RAW|", new String(buffer).replace('\u0001', '|'));

        DefaultMessageParser parser = new DefaultMessageParser();
        parser.set(buffer, 0, buffer.length);
        ByteEnumLookup<Side> sideByteEnumLookup = new ByteEnumLookup<>(Side.class);
        IntEnumLookup<OrdRejReason> rejReasonIntEnumLookup = new IntEnumLookup<>(OrdRejReason.class);
        //StringEnumLookup<Side> sideByteEnumLookup1 = new StringEnumLookup<>(Side.class);
        ByteArrayReference array = new ByteArrayReference();
        while (parser.next()) {
            switch (parser.getTagNum()) {
                case 1: assertEquals("ABC", String.valueOf(parser.getCharSequenceValue())); break;
                case 2: assertEquals(123L, parser.getLongValue()); break;
                case 3: assertEquals(123, parser.getIntValue()); break;
                case 4: assertEquals(3.14159, parser.getDoubleValue(),  0.00001); break; // formatter is set to print 5 decimal places
                case 5: assertEquals('x', parser.getByteValue()); break;
                case 6: assertTrue(parser.getBooleanValue()); break;
                case 7: assertFalse(parser.getBooleanValue()); break;
                case 8: assertEquals(Side.BUY, sideByteEnumLookup.get(parser.getByteValue())); break;
                case 9: assertEquals(OrdRejReason.ORDER_EXCEEDS_LIMIT, rejReasonIntEnumLookup.get(parser.getIntValue())); break;
                //TODO:case 10: assertEquals(MsgType.NEWS, rejReasonIntEnumLookup.get(parser.getByteValue())); break;
                case 11: assertEquals(TestUtils.parseUTCTimestamp("20121009-13:44:49.421"), parser.getUTCTimestampValue()); break;
                case 12: assertEquals(TestUtils.parseUTCTimestamp("20121009-00:00:00.000"), parser.getUTCDateOnly()); break;
                case 13: assertEquals(13*60*60000+ 44*60000 + 49*1000 +421, parser.getUTCTimeOnly()); break;
                case 14:
                    assertEquals("20121122", localDateFormat.format(new Date(parser.getLocalMktDate())));
                    assertEquals(20121122, parser.getLocalMktDate2()); break;
                case 15: parser.getByteSequence(array); assertEquals("RAW", array.toString()); break;
            }
        }

    }

    private void parseTag (String message, int tagNo) {
        byte [] messageBytes = message.getBytes();
        parser.set(messageBytes, 0, messageBytes.length);

        while(parser.next()) {
            if (parser.getTagNum() == tagNo)
                return;
        }
        Assert.fail("Tag " + tagNo + " is not found");

    }

    private void assertParser(String message) {
        byte [] messageBytes = message.getBytes();
        parser.set(messageBytes, 0, messageBytes.length);

        StringBuilder sb =  new StringBuilder(messageBytes.length);
        while (parser.next()) {
            sb.append (parser.getTagNum());
            sb.append ('=');
            sb.append (parser.getCharSequenceValue());
            sb.append ((char)1);
        }
        Assert.assertEquals(message, sb.toString());
    }


}
