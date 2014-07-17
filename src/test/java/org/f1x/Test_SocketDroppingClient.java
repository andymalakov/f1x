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

import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixInitiatorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageParser;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.v1.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Stress test to see how initiator reacts to random drops socket shortly after LOGON. We made 10000 attempts to initiate connection every 1..10 milliseconds.
 */
public class Test_SocketDroppingClient extends  TestCommon {
    private static final String INITIATOR_SENDER_ID = "INITIATOR";
    private static final String ACCEPTOR_SENDER_ID = "ACCEPTOR";
    private static final Timer TIMER = new Timer("Socket Dropping Timer", true);
    private static final Random RND = new Random(System.currentTimeMillis());


    /** Client and server connect, exchange 15 messages and disconnect */
    @Test(timeout = 120000)
    public void simpleMessageLoop() throws InterruptedException, IOException {
        final int numberOfReconnectAttempts = 1000;
        final SocketDroppingServer server = new SocketDroppingServer("localhost", 7890, new SessionIDBean(ACCEPTOR_SENDER_ID, INITIATOR_SENDER_ID));
        final SimpleClient client = new SimpleClient ("localhost", 7890, new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID), numberOfReconnectAttempts);


        final Thread acceptorThread = new Thread(server, "Server");
        acceptorThread.start();

        final Thread initiatorThread = new Thread(client, "Client");
        initiatorThread.start();

        client.awaitDisconnect();
        Assert.assertEquals(SessionStatus.Disconnected, client.getSessionStatus());
        client.close();
        server.close();
    }

    private static class SimpleClient extends FixSessionInitiator {
        private final CountDownLatch disconnectCounter;

        public SimpleClient(String host, int port, SessionID sessionID, int numberOfConnectionAttempts) {
            super(host, port, FixVersion.FIX44, sessionID, newFixInitiatorSettings());

            disconnectCounter = new CountDownLatch(numberOfConnectionAttempts);
        }

        private static FixInitiatorSettings newFixInitiatorSettings() {
            FixInitiatorSettings result = new FixInitiatorSettings();
            result.setErrorRecoveryInterval(1);
            return result;
        }

        @Override
        protected void onSessionStatusChanged(SessionStatus oldStatus, SessionStatus newStatus) {
            super.onSessionStatusChanged(oldStatus, newStatus);
            switch (newStatus) {
                case Disconnected:
                    disconnectCounter.countDown();
                    break;
                case ApplicationConnected:
                    TIMER.schedule(new SocketDroppingTimer(), RND.nextInt(10));
                    break;
            }
        }


        private class SocketDroppingTimer extends TimerTask {
            @Override
            public void run() {
                disconnect("Go away");
            }
        }

        void awaitDisconnect() throws InterruptedException {
            disconnectCounter.await(60, TimeUnit.SECONDS);
        }

    }

    /**
     * Drops socket shortly after LOGON
     */
    private static class SocketDroppingServer extends SingleSessionAcceptor {

        public SocketDroppingServer(String host, int bindPort, SessionID sessionID) {
            super(host, bindPort, sessionID, SocketDroppingServerSessionAcceptor.create());
        }
    }


    private static class SocketDroppingServerSessionAcceptor extends FixSessionAcceptor {
        private static SocketDroppingServerSessionAcceptor create() {
            return new SocketDroppingServerSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings());
        }

        private SocketDroppingServerSessionAcceptor(FixVersion fixVersion, FixAcceptorSettings settings) {
            super(fixVersion, settings);
        }

    }


}
