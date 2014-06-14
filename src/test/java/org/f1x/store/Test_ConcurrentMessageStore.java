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

package org.f1x.store;


import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixInitiatorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.v1.FixSessionAcceptor;
import org.f1x.v1.FixSessionInitiator;
import org.f1x.v1.SingleSessionAcceptor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Test_ConcurrentMessageStore extends TestCommon {
    private static final int NUM_CLIENT_THREADS = 16;
    private static final int NUM_MESSAGES_PER_CLIENT_THREAD = 1024;
    private static final int MAX_MESSAGE_SIZE = 4096;

    private static final int PORT = 7890;
    private static final String INITIATOR_SENDER_ID = "INITIATOR";
    private static final String ACCEPTOR_SENDER_ID = "ACCEPTOR";

    private final SessionID ServerSessionID = new SessionIDBean(ACCEPTOR_SENDER_ID, INITIATOR_SENDER_ID);
    private final SessionID ClientSessionID = new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID);


    /**
     * This test uses simple FIX client and FIX server that have in-memory message store.
     * Test uses NUM_CLIENT_THREADS threads to send messages to server concurrently.
     * At the end we validate that message stores on client and server match (server stores inbound messages into aux msg store).
     */
    @Test(timeout = 120000)
    public void concurrentSend() throws InterruptedException, IOException {


        final MessageStore sentMessages = new InMemoryMessageStore(1 << 24);
        final MessageStore receivedMessages = new InMemoryMessageStore(1 << 24);

        final TestServer server = new TestServer(PORT, ServerSessionID, receivedMessages);
        final TestClient client = new TestClient(PORT, ClientSessionID, sentMessages);


        final Thread acceptorThread = new Thread(server, "EchoServer");
        acceptorThread.start();

        final Thread initiatorThread = new Thread(client, "EchoClient");
        initiatorThread.start();


        client.waitForAllMessages();

        client.disconnect("End of test");
        client.close();
        server.close();

        // compare sent and received messages
        final int comparedPrefixSize = 150;
        final byte [] sentMessageBuffer= new byte [MAX_MESSAGE_SIZE];
        final byte [] receivedMessageBuffer= new byte [MAX_MESSAGE_SIZE];
        for (int i=2; i <= NUM_CLIENT_THREADS *NUM_MESSAGES_PER_CLIENT_THREAD; i++) {

            Assert.assertEquals("Sent MsgSeqNum", i, sentMessages.get(i, sentMessageBuffer));
            Assert.assertEquals("Received MsgSeqNum", i, receivedMessages.get(i, receivedMessageBuffer));

            for(int j=0; j < comparedPrefixSize; j++) {
                if (sentMessageBuffer[j] != receivedMessageBuffer[j])
                    Assert.assertEquals("Sent and Received messages do not match", new String(sentMessageBuffer, 0, comparedPrefixSize), new String(receivedMessageBuffer, 0, comparedPrefixSize));
            }

        }
    }

    private static class TestServer extends SingleSessionAcceptor {


        public TestServer(int bindPort, SessionID sessionID, final MessageStore testMessageStore) {
            super (null, bindPort, sessionID,
                new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings()) {
                    private MessageBuilder messageBuilder; {
                        messageBuilder = createMessageBuilder();
                    }

                    private final byte [] buffer = new byte[MAX_MESSAGE_SIZE];

                    @Override
                    protected void processInboundAppMessage(CharSequence msgType, int msgSeqNum, boolean possDup, MessageParser parser) throws IOException {

                        messageBuilder.clear();

                        while(parser.next()) {
                            messageBuilder.add(parser.getTagNum(), parser.getCharSequenceValue());
                        }

                        int length = messageBuilder.output(buffer, 0);
                        testMessageStore.put(msgSeqNum, buffer, 0, length);
                    }
                }
            );
        }
    }

    private static class TestClient extends FixSessionInitiator {
        private final ClientSendingThread [] clientThreads = new ClientSendingThread [NUM_CLIENT_THREADS];
        private final CountDownLatch activeClientThreads = new CountDownLatch(NUM_CLIENT_THREADS);

        public TestClient(int port, SessionID sessionID, MessageStore messageStore) {
            super(null, port, FixVersion.FIX44, sessionID, new FixInitiatorSettings());
            for (int i = 0; i < NUM_CLIENT_THREADS; i++)
                clientThreads [i] = new ClientSendingThread(this, i);

            setMessageStore(messageStore);
        }

        @Override
        protected void onSessionStatusChanged(SessionStatus oldStatus, SessionStatus newStatus) {
            super.onSessionStatusChanged(oldStatus, newStatus);
            if (newStatus == SessionStatus.ApplicationConnected)
                for (int i = 0; i < NUM_CLIENT_THREADS; i++)
                    if ( ! clientThreads[i].isAlive())
                        clientThreads[i].start();
        }

        void waitForAllMessages() throws InterruptedException {
            activeClientThreads.await();
        }
    }

    private static class ClientSendingThread extends Thread {
        private final int id;
        private final TestClient client;
        private final MessageBuilder mb;

        public ClientSendingThread(TestClient client, int id) {
            super("Client#"+id);
            this.id = id;
            this.client = client;
            this.mb = client.createMessageBuilder();
        }

        @Override
        public void run() {
            for(int i=0; i < NUM_MESSAGES_PER_CLIENT_THREAD; i++) {
                sendMessage(i);
            }
            client.activeClientThreads.countDown();
        }

        public void sendMessage (int i) {
            assert client.getSessionStatus() == SessionStatus.ApplicationConnected;
            try {
                mb.clear();
                mb.setMessageType(MsgType.ORDER_SINGLE);
                mb.add(FixTags.Account, id);
                mb.add(FixTags.ClOrdID, i);
                mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
                mb.add(FixTags.OrderQty, 1);
                mb.add(FixTags.OrdType, OrdType.LIMIT);
                mb.add(FixTags.Price, 1.43);
                mb.add(FixTags.Side, Side.BUY);
                mb.add(FixTags.Symbol, "EUR/USD");
                mb.add(FixTags.SecurityType, SecurityType.FOREIGN_EXCHANGE_CONTRACT);
                mb.add(FixTags.TimeInForce, TimeInForce.DAY);
                mb.add(FixTags.ExDestination, "GS");
                mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
                client.send(mb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
