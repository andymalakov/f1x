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
import org.f1x.log.OutputStreamMessageLog;
import org.f1x.util.TimeSource;

import java.io.OutputStream;

/**
 * Extension of FileMessageLog that has a periodic file flusher and also
 *
 */
public class PeriodicFlushingMessageLog extends OutputStreamMessageLog {

    private final Flusher flusher;

    /**
     * @param os destination stream
     * @param sessionID identifies session for this log
     * @param timeSource time source used for formatting timestamps
     * @param flushPeriod flush period in milliseconds
     */
    public PeriodicFlushingMessageLog(OutputStream os, SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        super(os, timeSource);
        flusher = createFlusher(sessionID, timeSource, flushPeriod);
    }

    protected Flusher createFlusher(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        return new Flusher(sessionID, timeSource, flushPeriod);
    }

    public void start() {
        flusher.start();
    }

//    @Override
//    public void log(boolean isInbound, byte[] buffer, int offset, int length) {
//        try {
//            synchronized (lock) {
//                if (formatter != null)
//                    formatter.log(isInbound, buffer, offset, length, os);
//                else
//                    os.write(buffer, offset, length);
//            }
//        } catch (IOException e) {
//            LOGGER.error().append("Error writing FIX message into the log.").append(e).commit();
//        }
//    }


    @Override
    public void close() {
        super.close();

        if (flusher.isAlive())
            flusher.interrupt();
    }


    protected class Flusher extends Thread {
        private final TimeSource timeSource;
        private final int flushPeriod;

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
                    synchronized (lock) {
                        if (os == null)
                            break;

                        os.flush();
                    }

                    onFlushComplete();

                    timeSource.sleep(flushPeriod);
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
