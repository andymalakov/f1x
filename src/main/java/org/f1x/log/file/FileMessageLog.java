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

import org.f1x.util.TimeSource;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.session.SessionID;
import org.f1x.log.MessageLog;
import org.f1x.util.AsciiUtils;
import org.f1x.util.format.TimeOfDayFormatter;

import java.io.*;
import java.net.URLEncoder;

public class FileMessageLog extends Thread implements MessageLog {
    private static final byte [] IN_PREFIX =  AsciiUtils.getBytes("IN >");
    private static final byte [] OUT_PREFIX = AsciiUtils.getBytes("OUT>");
    private static final GFLog LOGGER = GFLogFactory.getLog(FileMessageLog.class);
    private final TimeSource timeSource;
    private final int flushPeriod;

    private final Object lock = new Object();
    private OutputStream os; //guarded by lock
    private int bytesWritten; //guarded by lock
    //private int fileNameSuffix = 1;  //TODO: Switch to another file as soon as file size reaches certain limit
    private final byte [] timestampBuffer = new byte [TimeOfDayFormatter.LENGTH]; //guarded by lock
    private volatile boolean active = true;

    public FileMessageLog(File dir, SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        super("Log flusher for " + sessionID);
        this.timeSource = timeSource;
        this.flushPeriod = flushPeriod;
        try {
            File file = new File (dir, encodeAsLogFilename(sessionID));
            this.os = new BufferedOutputStream(new FileOutputStream(file, true), 8192);
        } catch (IOException e) {
            throw new RuntimeException("Error creating FIX message log.", e);
        }
    }


    String encodeAsLogFilename(SessionID sessionID) {
        String result = sessionID.getSenderCompId().toString() + '-' + sessionID.getTargetCompId().toString() /* + '.' + (fileNameSuffix)*/ + ".log"; //TODO: Something better please

        try {
            return URLEncoder.encode(result, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void logInbound(byte[] buffer, int offset, int length) {
        log(IN_PREFIX, buffer, offset, length);
    }

    @Override
    public void logOutbound(byte[] buffer, int offset, int length) {
        log(OUT_PREFIX, buffer, offset, length);
    }

    private void log(byte [] prefix, byte[] buffer, int offset, int length) {
        try {
            synchronized (lock) {
                TimeOfDayFormatter.format(timeSource.currentTimeMillis(), timestampBuffer, 0);
                os.write(timestampBuffer, 0, timestampBuffer.length);
                os.write(' ');
                os.write(prefix, 0, prefix.length);
                os.write(' ');
                os.write(buffer, offset, length);
                os.write(Character.LINE_SEPARATOR);

                bytesWritten += timestampBuffer.length + prefix.length + length + 3;
            }
        } catch (IOException e) {
            LOGGER.error().append("Error writing FIX message into the log.").append(e).commit();
        }
    }

    @Override
    public void close() {
        active = false;

        try {
            synchronized (lock) {
                os.close();
            }
        } catch (IOException e) {
            LOGGER.error().append("Error closing to FIX log").append(e).commit();
        }
        if (FileMessageLog.this.isAlive())
            FileMessageLog.this.interrupt();
    }

    @Override
    public void run () {

        int lastBytesWritten = 0;
        while (active) {
            try {
                synchronized (lock) {
                    if (lastBytesWritten !=  bytesWritten) {
                        os.flush();
                        lastBytesWritten = bytesWritten;
                    }
                }
                timeSource.sleep(flushPeriod);
            } catch (Exception e) {
                if (active  || ! ( e instanceof InterruptedException))
                    LOGGER.error().append("Error writing FIX log").append(e).commit();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            LOGGER.error().append("Error closing FIX log").append(e).commit();
        }
    }
}
