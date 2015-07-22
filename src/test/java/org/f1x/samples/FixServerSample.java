package org.f1x.samples;

import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.FixVersion;
import org.f1x.v1.FixSessionAcceptor;
import org.f1x.v1.ServerSocketSessionAcceptor;
import org.f1x.v1.SingleSessionAcceptor;

public class FixServerSample extends TestCommon {

    private static void sample1() {
        SessionIDBean sessionID = new SessionIDBean("Sender", "Receiver");
        ServerSocketSessionAcceptor acceptor = new SingleSessionAcceptor("localhost", 9999, new FixSessionAcceptor(FixVersion.FIX44, sessionID, new FixAcceptorSettings()));

        new Thread(acceptor).start();
    }

    private static void sample2() {
       /* SessionManager manager = new SimpleSessionManager();
        SessionIDBean sessionID = new SessionIDBean("Sender", "Receiver");
        manager.addSession(new FixSessionAcceptor(FixVersion.FIX42, new FixAcceptorSettings()));

        ServerSocketSessionAcceptor acceptor = new MultiSessionAcceptor("localhost", 9999, 1000, 1000, acceptorFactory, manager);

        new Thread(acceptor).start();*/
    }

}
