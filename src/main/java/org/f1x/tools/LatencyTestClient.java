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
import org.f1x.api.FixInitiatorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.Tools;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.v1.FixSessionInitiator;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class LatencyTestClient extends FixSessionInitiator {
    private static final long BASE_NANOTIME = System.nanoTime();
//    static {
//        try {
//            XmlLogFactoryConfigurator.configure("/config/gflogger.xml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    private final MessageBuilder mb;
    private final int intervalBetweenMessagesInNanos;
    private int orderCounter = 0;

    /**
     * @param messageRate messages per second
     */
    public LatencyTestClient(String host, int port, SessionID sessionID, int messageRate) {
        super(host, port, FixVersion.FIX44, sessionID, new FixInitiatorSettings());

        mb = createMessageBuilder();

        intervalBetweenMessagesInNanos = (int) TimeUnit.SECONDS.toNanos(1) / messageRate;

    }

    public void sendMessage () throws IOException {
        assert getSessionStatus() == SessionStatus.ApplicationConnected;
        mb.clear();
        mb.setMessageType(MsgType.ORDER_SINGLE);
        mb.add(1, getMicrosecondClock()); // timestamp of signal creation
        mb.add(FixTags.ClOrdID, ++orderCounter);
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
    }

    @Override
    protected void onSessionStatusChanged(SessionStatus oldStatus, SessionStatus newStatus) {
        super.onSessionStatusChanged(oldStatus, newStatus);

        if (newStatus == SessionStatus.ApplicationConnected)
            new Thread("Message Generator") {
                @Override
                public void run() {
                    sendMessages();
                }
            }.start();
    }

    private void sendMessages () {
        while (getSessionStatus() == SessionStatus.ApplicationConnected) {
            try {
                long nextNanoTime = (intervalBetweenMessagesInNanos != 0) ? System.nanoTime() + intervalBetweenMessagesInNanos : 0;
                while (active) {
                    if (intervalBetweenMessagesInNanos != 0) {
                        if (System.nanoTime() < nextNanoTime)
                            continue; // spin-wait
                    }

                    sendMessage();

                    nextNanoTime += intervalBetweenMessagesInNanos;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
        if (Tools.equals(MsgType.ORDER_SINGLE, msgType)) {
            while(parser.next()) {
                if (parser.getTagNum() == FixTags.Account) {
                    recordLatency(parser.getIntValue());
                    break;
                }
            }
        } else {
            super.processInboundAppMessage(msgType, parser);
        }
    }

    private final int [] LATENCIES = new int [100000000];
    private int signalsReceived;

    private void recordLatency(int signalCreation) {
        int signalLatency = getMicrosecondClock() - signalCreation;
        if (signalsReceived < LATENCIES.length) {
            LATENCIES[signalsReceived] = signalLatency;
            if (++signalsReceived == LATENCIES.length) {
                System.out.println("DONE!");

                Arrays.sort(LATENCIES);


                System.out.println("MIN: " + LATENCIES[0]);
                System.out.println("MAX: " + LATENCIES[LATENCIES.length -1]);
                System.out.println("MEDIAN:" + LATENCIES[LATENCIES.length / 2]);
                System.out.println("MEDIAN: " + LATENCIES[LATENCIES.length/2]);

                System.out.println("99.000%:  " + LATENCIES[ (int)   (99L*LATENCIES.length/100)]);
                System.out.println("99.900%:  " + LATENCIES[ (int)  (999L*LATENCIES.length/1000)]);
                System.out.println("99.990%:  " + LATENCIES[ (int) (9999L*LATENCIES.length/10000)]);
                System.out.println("99.999%:  " + LATENCIES[ (int)(99999L*LATENCIES.length/100000)]);
                System.out.println("99.9999%: " + LATENCIES[ (int)(999999L*LATENCIES.length/1000000)]);

                
                System.exit(0);
            }
        }
    }

    public static void main (String [] args) throws InterruptedException, IOException {
        String host =args[0];
        int port = Integer.parseInt(args[1]);
        final LatencyTestClient client = new LatencyTestClient(host, port, new SessionIDBean("CLIENT", "SERVER"), 1000);
        final Thread acceptorThread = new Thread(client, "EchoClient");
        acceptorThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info().append("Exiting...").commit();
                client.close();
            }
        });
    }

    private static int getMicrosecondClock() {
        return (int)  ((System.nanoTime() - BASE_NANOTIME) / 1000L);
    }
}
