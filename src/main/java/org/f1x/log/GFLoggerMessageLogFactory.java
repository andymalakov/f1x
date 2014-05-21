package org.f1x.log;

import org.f1x.api.session.SessionID;

public class GFLoggerMessageLogFactory implements MessageLogFactory {

    private final GFLoggerMessageLog INSTANCE = new GFLoggerMessageLog();

    @Override
    public MessageLog create(SessionID sessionID) {
        return INSTANCE;
    }
}
