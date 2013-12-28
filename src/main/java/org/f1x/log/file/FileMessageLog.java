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

package org.f1x.log.file;

import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.SessionID;
import org.f1x.log.MessageLog;
import org.f1x.util.AsciiUtils;
import org.f1x.util.format.TimeOfDayFormatter;

import java.io.*;

public class FileMessageLog extends Thread implements MessageLog {
    protected static final GFLog LOGGER = GFLogFactory.getLog(FileMessageLog.class);
    private final OutputStream os;
    private final Object lock = new Object();
    private final byte [] timestampBuffer = new byte [TimeOfDayFormatter.LENGTH];
    private volatile boolean active = true;

    public FileMessageLog(File dir, SessionID sessionID) {
        super("Log flusher for " + sessionID); //TODO: Alloc

        try {
            File file = new File (dir, encodeAsLogFilename(sessionID));
            os = new BufferedOutputStream(new FileOutputStream(file, true), 8192);
        } catch (IOException e) {
            throw new RuntimeException("Error creating FIX message log.", e);
        }
    }

    private static String encodeAsLogFilename(SessionID sessionID) {
        return sessionID.getSenderCompId().toString() + '-' + sessionID.getTargetCompId().toString() + ".log"; //TODO: Something better please
    }

    private static final byte [] IN =  AsciiUtils.getBytes("IN >");
    private static final byte [] OUT = AsciiUtils.getBytes("OUT>");

    @Override
    public void logInbound(byte[] buffer, int offset, int length) {
        log(IN, buffer, offset, length);
    }

    @Override
    public void logOutbound(byte[] buffer, int offset, int length) {
        log(OUT, buffer, offset, length);
    }

    private void log(byte [] prefix, byte[] buffer, int offset, int length) {
        try {
            synchronized (lock) {
                TimeOfDayFormatter.format(System.currentTimeMillis(), timestampBuffer, 0);
                os.write(timestampBuffer, 0, timestampBuffer.length);
                os.write(' ');
                os.write(prefix, 0, prefix.length);
                os.write(' ');
                os.write(buffer, offset, length);
                os.write(Character.LINE_SEPARATOR);
            }

        } catch (IOException e) {
            LOGGER.error().append("Error writing FIX message into the log.").append(e).commit();
        }
    }

    @Override
    public void close() {
        active = false;
        if (FileMessageLog.this.isAlive())
            FileMessageLog.this.interrupt();
    }

    @Override
    public void run () {
        while (active) {
            try {
                synchronized (lock) {
                    os.flush();
                }
                Thread.sleep(5000); //TODO: Add parameter
            } catch (Exception e) {
                if (active  || ! ( e instanceof InterruptedException))
                    LOGGER.error().append("Error appending to FIX log").append(e).commit();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            LOGGER.error().append("Error closing to FIX log").append(e).commit();
        }
    }
}
