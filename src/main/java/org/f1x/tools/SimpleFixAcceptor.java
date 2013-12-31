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
import org.f1x.api.message.Tools;
import org.f1x.api.message.fields.*;
import org.f1x.api.message.types.ByteEnumLookup;
import org.f1x.api.session.SessionState;
import org.f1x.util.ByteArrayReference;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.v1.FixSessionAcceptor;
import org.f1x.v1.SingleSessionAcceptor;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SimpleFixAcceptor extends SingleSessionAcceptor {
    public SimpleFixAcceptor(int bindPort, SessionID sessionID) {
        this(bindPort, sessionID, new FixAcceptorSettings());
    }

    public SimpleFixAcceptor(int bindPort, SessionID sessionID, FixAcceptorSettings settings) {
        super(null, bindPort, sessionID, new SimpleFixSessionAcceptor(FixVersion.FIX44, settings));
    }

    public static void main (String [] args) throws InterruptedException, IOException {

        final SimpleFixAcceptor acceptor = new SimpleFixAcceptor(2508, new SessionIDBean("SERVER", "CLIENT"), new FixAcceptorSettings());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                acceptor.close();
                LOGGER.info().append("Exiting...").commit();
            }
        });

        acceptor.run();
    }

    private static class SimpleFixSessionAcceptor extends FixSessionAcceptor {
        private final ByteEnumLookup<Side> ordSideLookup = new ByteEnumLookup<>(Side.class);
        private final ByteArrayReference symbol = new ByteArrayReference();
        private final MessageBuilder mb;

        private volatile int orderCount;

        public SimpleFixSessionAcceptor(FixVersion fixVersion, FixAcceptorSettings settings) {
            super(fixVersion, settings);
            scheduleStats(15000);

            mb = createMessageBuilder();
        }

        @Override
        protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
            if (Tools.equals(MsgType.ORDER_SINGLE, msgType)) {
                try {
                    processInboundOrderSingle(parser);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                super.processInboundAppMessage(msgType, parser);
        }

        private void scheduleStats(final int intervalInMillis) {
            TimerTask meter = new TimerTask() {
                private long lastOrderCount;
                private final int intervalInSeconds = intervalInMillis / 1000;
                public void run() {
                    final long currentOrderCount = orderCount; // volatile
                    System.out.println("Average orders per second: " + (currentOrderCount-lastOrderCount) / intervalInSeconds);
                    lastOrderCount = currentOrderCount;
                }
            };
            new Timer("Global Timer", true).scheduleAtFixedRate(meter, intervalInMillis, intervalInMillis);
        }


        private void processInboundOrderSingle(MessageParser parser) throws IOException {

            long clOrdId = -1;
            double orderPrice = Double.NaN;
            double orderQty = Double.NaN;
            Side side = null;

            while (parser.next()) {
                switch (parser.getTagNum()) {
                    case FixTags.ClOrdID:
                        clOrdId = parser.getIntValue();
                        break;
                    case FixTags.Side:
                        side = ordSideLookup.get(parser.getByteValue());
                        break;
                    case FixTags.Symbol:
                        parser.getByteSequence(symbol);
                        break;
                    case FixTags.Price:
                        orderPrice = parser.getDoubleValue();
                        break;
                    case FixTags.OrderQty:
                        orderQty  = parser.getDoubleValue();
                        break;
                }
            }
            //LOG.info("Execution report: order " + clOrdId + " " + side + " " + orderQty + " " + symbol.toString() + " $" + orderPrice);
            orderCount++;

            sendExecutionReport(clOrdId, side, orderQty, symbol, orderPrice);
        }

        private void sendExecutionReport (long clOrdId, Side side, double orderQty, CharSequence symbol, double orderPrice) throws IOException {

            assert getSessionState() == SessionState.ApplicationConnected;
            synchronized (mb) {
                mb.clear();
                mb.setMessageType(MsgType.EXECUTION_REPORT);
                mb.add(FixTags.ClOrdID, clOrdId);
                mb.add(FixTags.ExecType, ExecType.NEW);
                mb.add(FixTags.OrderQty, orderQty);
                mb.add(FixTags.OrdType, OrdType.LIMIT);
                mb.add(FixTags.Price, orderPrice);
                mb.add(FixTags.Side, side);
                mb.add(FixTags.Symbol, symbol);
                mb.add(FixTags.SecurityType, SecurityType.FOREIGN_EXCHANGE_CONTRACT);
                mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
                mb.add(FixTags.OrdStatus, OrdStatus.NEW);
                send(mb);
            }
        }
    }
}
