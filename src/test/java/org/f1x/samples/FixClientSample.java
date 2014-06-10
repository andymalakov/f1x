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

package org.f1x.samples;

import org.f1x.SessionIDBean;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.MessageParser;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.FixSession;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.v1.FixSessionInitiator;

import java.io.IOException;

public class FixClientSample {
    public static void main (String [] args) {
        sample3();
    }

    public static void sample1() {
        final FixSession session = new FixSessionInitiator("localhost", 9999, FixVersion.FIX44, new SessionIDBean("SENDER-COMP-ID", "TARGET-COMP-ID"));
        new Thread(session).start();
    }

    public static void sample2 () {
        final FixSession session = new FixSessionInitiator("localhost", 9999, FixVersion.FIX44, new SessionIDBean("SENDER-COMP-ID", "TARGET-COMP-ID"));
        session.setEventListener(new SessionEventListener() {
            @Override
            public void onStatusChanged(SessionID sessionID, SessionStatus oldStatus, SessionStatus newStatus) {
                if (newStatus == SessionStatus.ApplicationConnected)
                    sendSampleMessage(session);
            }
        });
        new Thread(session).start();
    }

    public static void sample3 () {
        final FixSession session = new FixSessionInitiator("localhost", 9999, FixVersion.FIX44, new SessionIDBean("SENDER-COMP-ID", "TARGET-COMP-ID")) {
            @Override
            protected void processInboundAppMessage(CharSequence msgType, int msgSeqNum, boolean possDup, MessageParser parser) throws IOException {
                //TODO:
            }
        };
        session.setEventListener(new SessionEventListener() {
            @Override
            public void onStatusChanged(SessionID sessionID, SessionStatus oldStatus, SessionStatus newStatus) {
                if (newStatus == SessionStatus.ApplicationConnected)
                    sendSampleMessage(session);
            }
        });
        new Thread(session).start();
    }

    private static void sendSampleMessage(FixSession client) {
        assert client.getSessionStatus() == SessionStatus.ApplicationConnected;
        MessageBuilder mb = client.createMessageBuilder(); // can be reused
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
            client.send(mb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
