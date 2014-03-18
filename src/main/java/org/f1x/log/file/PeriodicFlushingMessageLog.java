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
import org.f1x.log.OutputStreamMessageLog;
import org.f1x.util.TimeSource;

import java.io.OutputStream;

/**
 * Extension of FileMessageLog that has a thread that periodically flushes the buffer
 *
 */
public class PeriodicFlushingMessageLog extends OutputStreamMessageLog {

    protected Flusher flusher;

    /**
     * @param os destination stream
     * @param timeSource time source used for formatting timestamps
     */
    public PeriodicFlushingMessageLog(OutputStream os, TimeSource timeSource) {
        super(os, timeSource);
    }

    /**
     * @param os destination stream
     */
    public PeriodicFlushingMessageLog(OutputStream os, LogFormatter formatter) {
        super(os, formatter);
    }

    /**
     * @param sessionID identifies session for this log
     * @param timeSource time source used for formatting timestamps
     * @param flushPeriod This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing.
     */
    public void start(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        if (flushPeriod > 0) {
            flusher = new Flusher(sessionID, timeSource, flushPeriod);
            flusher.start();
        }
    }


    @Override
    public void close() {
        super.close();

        if (flusher != null && flusher.isAlive())
            flusher.interrupt();
    }


    protected class Flusher extends Thread {   //TODO: Replace by alloc-free version of ScheduledExecutorService ?
        private final TimeSource timeSource;
        protected final int flushPeriod;

        protected Flusher(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
            super("Log flusher for " + sessionID);
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY - 1);
            this.timeSource = timeSource;
            this.flushPeriod = flushPeriod;
        }

        @Override
        public void run () {
            while (true) {
                try {
                    timeSource.sleep(flushPeriod);

                    synchronized (lock) {
                        if (os == null)
                            break;

                        os.flush();
                    }

                    onFlushComplete();

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.error().append("Error writing FIX log").append(e).commit();
                }
            }

        }

        /** Called by flusher thread after each flush. Subclasses may perform additional actions (like switch to a different stream) */
        protected void onFlushComplete() {
            // by default does nothing
        }
    }

}
