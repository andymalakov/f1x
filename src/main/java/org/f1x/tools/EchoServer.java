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

package org.f1x.tools;

import org.f1x.SessionIDBean;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.FixTags;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.v1.FixSessionAcceptor;
import org.f1x.v1.SingleSessionAcceptor;

import java.io.IOException;

/** Simple FIX acceptor that echos back all inbound application messages */
public class EchoServer extends SingleSessionAcceptor {

    public EchoServer(int bindPort, SessionID sessionID, FixAcceptorSettings settings) {
        super(null, bindPort, sessionID, new EchoServerSessionAcceptor(FixVersion.FIX44, settings));
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        final SimpleFixAcceptor acceptor = new SimpleFixAcceptor(2508, new SessionIDBean("SERVER", "CLIENT"), new FixAcceptorSettings());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                acceptor.close();
                LOGGER.info().append("Exiting...").commit();
            }
        });

        acceptor.run();
    }

    private static class EchoServerSessionAcceptor extends FixSessionAcceptor {
        private final MessageBuilder mb;

        public EchoServerSessionAcceptor(FixVersion fixVersion, FixAcceptorSettings settings) {
            super(fixVersion, settings);
            mb = createMessageBuilder();
        }

        @Override
        protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
            mb.clear();
            mb.setMessageType(msgType.toString());

            while (parser.next()) {
                int tag = parser.getTagNum();
                if (tag != FixTags.MsgSeqNum) {
                    mb.add(tag, parser.getCharSequenceValue());
                }
            }

            send(mb);
        }

    }
}
