package org.f1x.samples;

import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionManager;
import org.f1x.api.session.SessionState;
import org.f1x.api.session.SessionStatus;
import org.f1x.util.ObjectFactory;
import org.f1x.v1.*;

public class FixServerSample extends TestCommon {

    private static void sample1() {
        ServerSocketSessionAcceptor acceptor = new SingleSessionAcceptor("localhost", 9999, new SessionIDBean("Sender", "Receiver"), new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings()));

        new Thread(acceptor).start();
    }

    private static void sample2() {
        SessionManager manager = new SimpleSessionManager(10);
        manager.add(new SessionIDBean("Sender", "Receiver"), new SampleSessionState());

        ServerSocketSessionAcceptor acceptor = new MultiSessionAcceptor("localhost", 9999, 1000, 1000, new ObjectFactory<FixSessionAcceptor>() {
            @Override
            public FixSessionAcceptor create() {
                return new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings());
            }
        }, manager);

        new Thread(acceptor).start();
    }

    private static class SampleSessionState implements SessionState {
        @Override
        public long getLastLogonTimestamp() {
            return -1;
        }

        @Override
        public int getSenderSeqNum() {
            return -1;
        }

        @Override
        public int getTargetSeqNum() {
            return -1;
        }

        @Override
        public SessionStatus getSessionStatus() {
            return null;
        }
    }

}
