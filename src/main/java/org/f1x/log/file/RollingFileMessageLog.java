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
import org.f1x.util.TimeSource;

import java.io.*;

/**
 * Start a new log file when size of current fle exceeds given maximum.
 */
@Deprecated
public class RollingFileMessageLog extends PeriodicFlushingMessageLog {

    private final OutputStreamFactory outputStreamFactory;
    private final long bytesPerFile;
    private volatile long bytesWritten; // updated under OutputStreamMessageLog.lock

    public interface OutputStreamFactory {
        OutputStream create();
    }

    public RollingFileMessageLog(OutputStreamFactory outputStreamFactory, SessionID sessionID, TimeSource timeSource, int flushPeriod, long bytesPerFile) {
        super(outputStreamFactory.create(), sessionID, timeSource, flushPeriod);

        this.outputStreamFactory = outputStreamFactory;
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

    protected OutputStream nextOutputStream() {
        return outputStreamFactory.create();
    }

    protected Flusher createFlusher(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        return new RollingFlusher(sessionID, timeSource, flushPeriod);
    }


    private class RollingFlusher extends Flusher {
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
    }
}
