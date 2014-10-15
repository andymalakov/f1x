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

import org.f1x.SessionIDBean;
import org.f1x.api.message.MessageParser;
import org.f1x.io.EmptyOutputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.util.AsciiUtils;
import org.f1x.io.PredefinedInputChannel;
import org.f1x.util.StoredTimeSource;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

/** This test validates parser in case when FIX message is split between different TCP packets (Message requires multiple InputChannel.reads()) */
public class Test_FixCommunicatorSplitMessages {

    private final OutputChannel out = new EmptyOutputChannel();

    private final MessageCollectingTestFixCommunicator fix = new MessageCollectingTestFixCommunicator ();

    private static class MessageCollectingTestFixCommunicator extends TestFixCommunicator{
        private List<String> parsedMessages = new ArrayList<>();


        public MessageCollectingTestFixCommunicator() {
            super(new SessionIDBean("CLIENT", "SERVER"), StoredTimeSource.makeFromUTCTimestamp("20140101-10:10:10.100"));
        }

        @Override
        protected void processInboundMessage(MessageParser parser, CharSequence msgType, int msgSeqNum) {
            String message = MessageParser2String.convert(parser);
            parsedMessages.add(message.replace('\u0001', '|'));
        }

        @Override
        protected void errorProcessingMessage(String errorText, Exception e, boolean logStackTrace) {
            if (e != ConnectionProblemException.NO_SOCKET_DATA)
               throw new RuntimeException(errorText + ": " + e.getMessage());
        }
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSingleMessageWithBufferLengthMoreMax() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(CoreMatchers.equalTo("Protocol Error: Message is too large"));

        String message = createMessageWithGivenLength(fix.getSettings().getMaxInboundMessageSize() + 1);
        int halfOfMessage = message.length() / 2;
        PredefinedInputChannel in = new PredefinedInputChannel(
                message.substring(0, halfOfMessage),
                message.substring(halfOfMessage)
        );
        fix.connect(in, out);
        fix.processInboundMessages();
    }

    @Test
    public void testSingleMessageWithoutMsgSeqNum(){
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(CoreMatchers.equalTo("Protocol Error: No MsgSeqNum(34) in message"));

        String message = "8=FIX.4.4|9=77|35=A|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|";
        PredefinedInputChannel in = new PredefinedInputChannel (
                message
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(message);
    }

    @Test
    public void testSingleMessage() {
        String message = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|";
        PredefinedInputChannel in = new PredefinedInputChannel (
                message
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(message);
    }

    @Test
    public void testTwoMessages() {
        String messageOne = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|";
        String messageTwo = "8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|";
        PredefinedInputChannel in = new PredefinedInputChannel (
                messageOne,
                messageTwo
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(
            messageOne,
            messageTwo
        );
    }

    @Test
    public void testSplitSingleMessage() {
        String messagePartOne = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:1";
        String messagePartTwo = "0:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|";
        PredefinedInputChannel in = new PredefinedInputChannel (
                messagePartOne, messagePartTwo
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(messagePartOne + messagePartTwo);
    }

    @Test
    public void testSplitSecondMessage() {
        PredefinedInputChannel in = new PredefinedInputChannel (
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|8=FIX.4.4|",   // here we have beginning of the next message
            "9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
    }

    @Test
    public void testSplitSecondMessageMultipleTimes() {
        PredefinedInputChannel in = new PredefinedInputChannel (
                "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|8=FIX.4.4|",   // here we have beginning of the next message
                "9=67|35=1|3", "4=1|49=CLI", "ENT|52=2014", "0101-10:10:10", ".100|56=SERVE", "R|112=TEST123|10=245|"
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(
                "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
                "8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|");
    }

    @Test
    public void testLogonBuffer() {
        String logonBuffer = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|8=FIX.4.4|9=67|35=1|34=1|49=CLI";

        PredefinedInputChannel in = new PredefinedInputChannel (
            "ENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|" // part of this message was read together with LOGON
        );
        fix.connect(in, out);
        fix.processInboundMessages(AsciiUtils.getBytes(logonBuffer.replace('|', '\u0001')), logonBuffer.length());
        assertParsedMessages(
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
    }

    private static String createMessageWithGivenLength(int length) {
        String beginString = "8=FIX.4.4|";
        String bodyLengthPattern = "9=??????????|";
        String checkSum = "10=000|";
        String msgType = "35=A|";
        String msgSeqNum = "34=1|";

        int neededBodyLength = length - (beginString.length() + bodyLengthPattern.length() + checkSum.length());
        String bodyLength = String.format("9=%010d|", neededBodyLength);
        int accountValueLength = neededBodyLength - (msgType.length() + msgSeqNum.length() + "1=|".length());
        String account = "1=" + new String(new byte[accountValueLength]) + '|';
        String message = beginString + bodyLength + msgType + msgSeqNum + account + checkSum;
        Assert.assertEquals(length, message.length());

        return message;
    }

    private void assertParsedMessages (String ... expectedMessages) {
        Assert.assertEquals("number of messages", expectedMessages.length, fix.parsedMessages.size());

        for (int i=0; i < expectedMessages.length ; i++)
            Assert.assertEquals(expectedMessages[i], fix.parsedMessages.get(i));

    }

}
