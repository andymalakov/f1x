package org.f1x.v1.state;

import org.f1x.api.session.SessionState;

public class Test_MemorySessionState extends SessionStateTest {

    @Override
    protected SessionState createSessionState() throws Exception {
        return new MemorySessionState();
    }

}
