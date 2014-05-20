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
import org.f1x.io.InputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.util.AsciiUtils;
import org.f1x.util.StoredTimeSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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
            active = true;
        }

        @Override
        protected void processInboundMessage(MessageParser parser, CharSequence msgType) {
            String message = MessageParser2String.convert(parser);
            parsedMessages.add(message.replace('\u0001', '|'));
        }

        @Override
        protected void errorProcessingMessage(String errorText, Exception e, boolean logStackTrace) {
            if (e == ConnectionProblemException.NO_SOCKET_DATA)
                active = false;
            else
                Assert.fail(errorText);
        }
    }

    @Test
    public void testSingleMessage() {
        PredefinedInputChannel in = new PredefinedInputChannel (
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|"
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages("34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|");
    }

    @Test
    public void testTwoMessages() {
        PredefinedInputChannel in = new PredefinedInputChannel (
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages(
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
    }

    @Test
    public void testSplitSingleMessage() {
        PredefinedInputChannel in = new PredefinedInputChannel (
            "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:1"  ,  "0:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|"
        );
        fix.connect(in, out);
        fix.processInboundMessages();
        assertParsedMessages("34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|");
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
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
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
                "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
                "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|");
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
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|",
            "34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|"
        );
    }

    private void assertParsedMessages (String ... expectedMessages) {
        Assert.assertEquals("number of messages", expectedMessages.length, fix.parsedMessages.size());

        for (int i=0; i < expectedMessages.length ; i++)
            Assert.assertEquals(expectedMessages[i], fix.parsedMessages.get(i));

    }

    static class PredefinedInputChannel implements InputChannel {
        private final String [] chunks;
        private int index;

        PredefinedInputChannel(String... chunks) {
            this.chunks = chunks;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (index >= chunks.length)
                return -1;

            String chunk = chunks[index++].replace('|', '\u0001');
            byte [] bytes = AsciiUtils.getBytes(chunk);

            if (bytes.length > length)
                throw new IllegalStateException("FIX Communicator buffer is too small to fit message of size " + bytes.length);
            System.arraycopy(bytes, 0, buffer, offset, bytes.length);
            return bytes.length;
        }

        @Override
        public void close() throws IOException {
            index = chunks.length;
        }
    }
}
