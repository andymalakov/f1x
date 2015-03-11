package org.f1x.log.file;

import org.f1x.api.session.SessionID;
import org.gflogger.FormattedGFLogEntry;
import org.gflogger.GFLog;
import org.gflogger.GFLogEntry;

public class LogUtils {

    public static void log (SessionID sessionID, GFLogEntry entry) {
        entry.append('[');
        entry.append(sessionID.getTargetCompId());
        entry.append(':');
        entry.append(sessionID.getSenderCompId());
        entry.append(']');
    }



}
