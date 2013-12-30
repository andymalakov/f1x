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

package org.f1x;

import org.f1x.api.FixVersion;
import org.f1x.api.SessionID;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.Tools;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.SessionState;
import org.f1x.tools.EchoServer;
import org.f1x.v1.ByteBufferMessageBuilder;
import org.f1x.v1.FixAcceptorSettings;
import org.f1x.v1.FixInitiatorSettings;
import org.f1x.v1.FixSessionInitiator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Verifies that simple FIX server and client can exchange messages */
public class Test_EchoServer {
    private static final String INITIATOR_SENDER_ID = "INITIATOR";
    private static final String ACCEPTOR_SENDER_ID = "ACCEPTOR";


    @Test(timeout = 120000)
    public void simpleMessageLoop() throws InterruptedException, IOException {

        final EchoServer server = new EchoServer(7890, new SessionIDBean(ACCEPTOR_SENDER_ID, INITIATOR_SENDER_ID), new FixAcceptorSettings());
        final EchoServerClient client = new EchoServerClient ("localhost", 7890, new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID));


        final Thread acceptorThread = new Thread(server, "EchoServer");
        acceptorThread.start();

        final Thread initiatorThread = new Thread(client, "EchoClient");
        initiatorThread.start();

        if ( ! client.messageCount.await(15, TimeUnit.SECONDS))
            Assert.fail("Communication failed (timed out waiting for echo)");
        client.disconnect("End of test");
    }

    private static class EchoServerClient extends FixSessionInitiator {
        private final static int N = 3;
        private final CountDownLatch messageCount = new CountDownLatch(N);
        private final MessageBuilder mb = new ByteBufferMessageBuilder(256, 3);

        public EchoServerClient(String host, int port, SessionID sessionID) {
            super(host, port, FixVersion.FIX44, sessionID, new FixInitiatorSettings());
        }

        public void sendMessage () {
            assert getSessionState() == SessionState.ApplicationConnected;
            try {
                mb.clear();
                mb.setMessageType(MsgType.ORDER_SINGLE);
                mb.add(FixTags.ClOrdID, 123);
                mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
                mb.add(FixTags.OrderQty, 1);
                mb.add(FixTags.OrdType, OrdType.LIMIT);
                mb.add(FixTags.Price, 1.43);
                mb.add(FixTags.Side, Side.BUY);
                mb.add(FixTags.Symbol, "EUR/USD");
                mb.add(FixTags.SecurityType, SecurityType.FOREIGN_EXCHANGE_CONTRACT);
                mb.add(FixTags.TimeInForce, TimeInForce.DAY);
                mb.add(76, "MARKET-FEED-SIM");
                mb.add(FixTags.ExDestination, "#CANCEL-AFTER-OPEN");
                mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
                send(mb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onSessionStateChanged(SessionState oldState, SessionState newState) {
            super.onSessionStateChanged(oldState, newState);
            if (newState == SessionState.ApplicationConnected)
                for (int i=0; i < N; i++)
                    sendMessage();
        }


        @Override
        protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
            if (Tools.equals(MsgType.ORDER_SINGLE, msgType)) {
                messageCount.countDown();
            } else {
                super.processInboundAppMessage(msgType, parser);
            }
        }

    }

}
