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

import org.f1x.api.session.SessionID;
import org.f1x.log.LogFormatter;
import org.f1x.util.TimeSource;

import java.io.*;

/**
 * Start a new log file when size of current fle exceeds given maximum.
 */
public class RollingFileMessageLog extends PeriodicFlushingMessageLog {

    private final OutputStreamFactory streamFactory;
    private final long bytesPerFile;
    private volatile long bytesWritten; // updated under OutputStreamMessageLog.lock

    private final File [] logFiles;

    /**
     * @param sessionID identifies session for this log
     * @param timeSource time source used for formatting timestamps
     * @param bytesPerFile Logger will roll to the next file once current log file size exceeds this limit (in bytes).
     * @param flushPeriod  This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing.
     */
    public RollingFileMessageLog(File [] logFiles, OutputStreamFactory streamFactory, SessionID sessionID, TimeSource timeSource, long bytesPerFile, int flushPeriod) {
        super(streamFactory.create(logFiles[0]), sessionID, timeSource, flushPeriod);

        this.logFiles = logFiles;
        this.streamFactory = streamFactory;
        this.bytesPerFile = bytesPerFile;
    }

    /**
     * @param sessionID identifies session for this log
     * @param timeSource time source used for formatting timestamps
     * @param bytesPerFile Logger will roll to the next file once current log file size exceeds this limit (in bytes).
     * @param flushPeriod This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing.
     */
    public RollingFileMessageLog(File [] logFiles, OutputStreamFactory streamFactory, SessionID sessionID, LogFormatter formatter, TimeSource timeSource, long bytesPerFile, int flushPeriod) {
        super(streamFactory.create(logFiles[0]), sessionID, formatter, timeSource, flushPeriod);
        this.logFiles = logFiles;
        this.streamFactory = streamFactory;
        this.bytesPerFile = bytesPerFile;
    }

    @Override
    public void log (boolean isInbound, byte[] buffer, int offset, int length) {
        try {
            synchronized (lock) {
                if (formatter != null) {
                    bytesWritten += formatter.log(isInbound, buffer, offset, length, os);
                } else {
                    os.write(buffer, offset, length);
                    bytesWritten += length;
                }
            }
        } catch (IOException e) {
            LOGGER.error().append("Error writing FIX message into the log.").append(e).commit();
        }
    }

    protected Flusher createFlusher(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        return new RollingFlusher(sessionID, timeSource, flushPeriod);
    }


    private class RollingFlusher extends Flusher {
        private int fileIndex = 0;
        private long bytesBeforeNextFile = bytesPerFile;

        protected RollingFlusher(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
            super(sessionID, timeSource, flushPeriod);
        }

        @Override
        protected void onFlushComplete() {
            if (bytesBeforeNextFile > 0 && bytesWritten >= bytesBeforeNextFile) {
                final OutputStream next = nextOutputStream();
                final OutputStream prev = os;
                synchronized (lock) {
                    os = next;
                }
                safeClose(prev);
                bytesBeforeNextFile += bytesPerFile;
            }
        }

        private OutputStream nextOutputStream() {
            File nextFile = logFiles[++fileIndex % logFiles.length];  // accessed only by Flusher thread
            return streamFactory.create(nextFile);
        }
    }
}
