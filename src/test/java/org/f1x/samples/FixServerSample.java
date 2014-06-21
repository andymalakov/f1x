package org.f1x.samples;

import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionManager;
import org.f1x.util.ObjectFactory;
import org.f1x.v1.*;
import org.f1x.v1.state.MemorySessionState;

public class FixServerSample extends TestCommon {

    private static void sample1() {
        SessionIDBean sessionID = new SessionIDBean("Sender", "Receiver");
        ServerSocketSessionAcceptor acceptor = new SingleSessionAcceptor("localhost", 9999, sessionID, new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings()));

        new Thread(acceptor).start();
    }

    private static void sample2() {
        SessionManager manager = new SimpleSessionManager(10);
        SessionIDBean sessionID = new SessionIDBean("Sender", "Receiver");
        manager.add(sessionID, new MemorySessionState());

        ObjectFactory<FixSessionAcceptor> acceptorFactory = new ObjectFactory<FixSessionAcceptor>() {
            @Override
            public FixSessionAcceptor create() {
                return new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings());
            }
        };

        ServerSocketSessionAcceptor acceptor = new MultiSessionAcceptor("localhost", 9999, 1000, 1000, acceptorFactory, manager);

        new Thread(acceptor).start();
    }

}
