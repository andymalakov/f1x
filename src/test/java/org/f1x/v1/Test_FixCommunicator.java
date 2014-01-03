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

import junit.framework.Assert;
import org.f1x.SessionIDBean;
import org.f1x.api.FixSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.SessionRejectReason;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionState;
import org.f1x.io.InputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.io.socket.ConnectionInterceptor;
import org.f1x.util.StoredTimeSource;
import org.junit.Test;

import java.io.IOException;

/** Verify format of administrative messages */
public class Test_FixCommunicator {

    private final TextOutputChannel out = new TextOutputChannel();
    private final FixCommunicator fix = new TestFixCommunicator (out);

    @Test
    public void testHeartbeat() throws IOException {
        fix.sendHeartbeat("TEST#123");
        assertFix("8=FIX.4.4|9=68|35=0|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST#123|10=024|");
    }

    @Test
    public void testTestReq() throws IOException {
        fix.sendTestRequest("TEST123");
        assertFix("8=FIX.4.4|9=67|35=1|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|112=TEST123|10=245|");
    }

    @Test
    public void testResendRequest() throws IOException {
        fix.sendResendReq(1, 2);
        assertFix("8=FIX.4.4|9=64|35=2|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|7=1|16=2|10=200|");
    }

    @Test
    public void testReject() throws IOException {
        fix.sendReject(123, SessionRejectReason.REQUIRED_TAG_MISSING, "Cause");
        assertFix("8=FIX.4.4|9=77|35=3|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|45=123|373=1|58=Cause|10=053|");
    }

    @Test
    public void testSeqReset() throws IOException {
        fix.sendSequenceReset(777);
        assertFix("8=FIX.4.4|9=70|35=4|34=777|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|36=777|123=N|10=044|");
    }

    @Test
    public void testGapFill() throws IOException {
        fix.sendGapFill(1, 2);
        assertFix("8=FIX.4.4|9=66|35=4|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|36=2|123=Y|10=085|");
    }

    @Test
    public void testLogon() throws IOException {
        fix.sendLogon(true);
        assertFix("8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|");
    }

    @Test
    public void testLogout() throws IOException {
        fix.sendLogout("TestCause");
        assertFix("8=FIX.4.4|9=68|35=5|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|58=TestCause|10=142|");
    }

    private void assertFix(String expected) {
        Assert.assertEquals("Unexpected FIX message", expected, out.toString());
    }

    private static class TestFixCommunicator extends FixCommunicator {
        private final SessionID sessionID = new SessionIDBean("CLIENT", "SERVER");

        public TestFixCommunicator(OutputChannel out) {
            super(FixVersion.FIX44, new FixSettings(), new StoredTimeSource("20140101-10:10:10.100"));

            connect(new EmptyInputChannel(), out);

            setSessionState(SessionState.ApplicationConnected);
        }

        @Override
        public void setConnectionInterceptor(ConnectionInterceptor connectionInterceptor) {
        }

        @Override
        public SessionID getSessionID() {
            return sessionID;
        }

        @Override
        protected void processInboundLogon() throws IOException {
        }

        @Override
        public void run() {
        }
    }

    private static class EmptyInputChannel implements InputChannel {

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return -1; // no data
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class TextOutputChannel implements OutputChannel {

        private StringBuilder sb = new StringBuilder();

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            sb.append (new String (buffer, offset, length));
        }

        @Override
        public void close() throws IOException {
            sb.setLength(0);
        }

        @Override
        public String toString() {
            String result = sb.toString().replaceAll("\u0001", "|");
            sb.setLength(0);
            return result;
        }
    }
}
