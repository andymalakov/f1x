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
import org.gflogger.config.xml.XmlLogFactoryConfigurator;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class LatencyTestClient extends FixSessionInitiator {
    private static final long BASE_NANOTIME = System.nanoTime();

    private final MessageBuilder mb;
    private final int intervalBetweenMessagesInNanos;
    private int orderCounter = 0;

    private final int [] LATENCIES;
    private int signalsReceived = -500; // warmup

    private static final int MICROS_TIMESTAMP_TAG = 8888;

    private static final MsgType TEST_MSG_TYPE = (true) ? MsgType.ORDER_SINGLE : MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH;
    /**
     * @param messageRate messages per second
     */
    public LatencyTestClient(String host, int port, SessionID sessionID, int messageRate, int count) {
        super(host, port, FixVersion.FIX44, sessionID, new FixInitiatorSettings());

        getSettings().setResetSequenceNumbersOnEachLogon(true);

        LOGGER.info().append("Message rate: ").append(messageRate).append(" msg/sec").commit();

        LATENCIES = new int [count];
        mb = createMessageBuilder();

        intervalBetweenMessagesInNanos = (int) TimeUnit.SECONDS.toNanos(1) / messageRate;

    }

    public void sendMessage () throws IOException {
        assert getSessionStatus() == SessionStatus.ApplicationConnected;

        if (TEST_MSG_TYPE == MsgType.ORDER_SINGLE) {
            mb.clear();
            mb.setMessageType(MsgType.ORDER_SINGLE);
            mb.add(MICROS_TIMESTAMP_TAG, getMicrosecondClock()); // timestamp of signal creation
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
        } else {
            final int ORDER_BOOK_DEPTH = 4;
            mb.clear();
            mb.setMessageType(MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH);
            mb.add(FixTags.Symbol, "EUR/USD");
            mb.add(FixTags.MDReqID, "dummyRequestId");
            mb.add(MICROS_TIMESTAMP_TAG, getMicrosecondClock());  // timestamp of signal creation

            mb.add(FixTags.NoMDEntries, 2 * ORDER_BOOK_DEPTH);

            for (int i = 0; i < ORDER_BOOK_DEPTH; i++) {
                mb.add(FixTags.MDEntryType, MDEntryType.BID);
                mb.add(FixTags.MDEntryPx, 1.35);
                mb.add(FixTags.Currency, "EUR");
                mb.add(FixTags.MDEntrySize, 10000);
                mb.add(FixTags.QuoteCondition, "A");
                mb.add(FixTags.MDEntryOriginator, "Originator");
                mb.add(FixTags.QuoteEntryID, "BID1");

                mb.add(FixTags.MDEntryType, MDEntryType.OFFER);
                mb.add(FixTags.MDEntryPx, 0.74);
                mb.add(FixTags.Currency, "EUR");
                mb.add(FixTags.MDEntrySize, 15000);
                mb.add(FixTags.QuoteCondition, "A");
                mb.add(FixTags.MDEntryOriginator, "Originator");
                mb.add(FixTags.QuoteEntryID, "OFFER1");
            }
        }
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
                while (true) { //TODO: Was: while (active)
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
    protected void processInboundAppMessage(CharSequence msgType, int msgSeqNum, boolean possDup, MessageParser parser) throws IOException {
        if (Tools.equals(TEST_MSG_TYPE, msgType)) {
            while(parser.next()) {
                if (parser.getTagNum() == MICROS_TIMESTAMP_TAG) {
                    recordLatency(parser.getIntValue());  // stored as getMicrosecondClock()
                    break;
                }
            }
        } else {
            super.processInboundAppMessage(msgType, msgSeqNum, possDup, parser);
        }
    }


    private void recordLatency(int signalCreation) {
        int signalLatency = getMicrosecondClock() - signalCreation;
        ++signalsReceived;

        if (signalsReceived >= 0) {
            if (signalsReceived < LATENCIES.length) {
                LATENCIES[signalsReceived] = signalLatency;
            } else
            if (signalsReceived == LATENCIES.length) {
                printStats();
            }
        }
    }

    private void printStats() {
        System.out.println("Experiment complete. Preparing latencies (in microseconds) ...");

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
        System.out.println("99.99999%:" + LATENCIES[ (int)(9999999L*LATENCIES.length/10000000)]);

        close();
        System.exit(0);
    }

    public static void main (String [] args) throws InterruptedException, IOException {
        try {
            XmlLogFactoryConfigurator.configure();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int rate = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        int count = args.length > 3 ? Integer.parseInt(args[3]) : 1000000;
        final LatencyTestClient client = new LatencyTestClient(host, port, new SessionIDBean("CLIENT", "SERVER"), rate, count);
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
