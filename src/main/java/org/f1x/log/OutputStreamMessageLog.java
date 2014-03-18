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

package org.f1x.log;

import org.f1x.util.TimeSource;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamMessageLog implements MessageLog {
    protected static final GFLog LOGGER = GFLogFactory.getLog(OutputStreamMessageLog.class);

    protected final Object lock = new Object();
    protected final LogFormatter formatter; // GuardedBy(lock)
    protected OutputStream os; // GuardedBy(lock)

    /**
     * Constructs OutputStreamMessageLog that uses default formatter
     * @param os destination stream
     * @param timeSource time source used for formatting timestamps
     */
    public OutputStreamMessageLog(OutputStream os, TimeSource timeSource) {
        this(os, new SimpleLogFormatter(timeSource));
    }

    /**
     * @param os destination stream
     * @param formatter Optional formatter to be applied for logging each inbound/outbound message
     */
    public OutputStreamMessageLog(OutputStream os, LogFormatter formatter) {
        this.formatter = formatter;
        this.os = os;
    }


    @Override
    public void log (boolean isInbound, byte[] buffer, int offset, int length) {
        try {
            synchronized (lock) {
                if (formatter != null)
                    formatter.log(isInbound, buffer, offset, length, os);
                else
                    os.write(buffer, offset, length);
            }
        } catch (IOException e) {
            LOGGER.error().append("Error writing FIX message into the log.").append(e).commit();
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            safeClose(os);
            os = null;
        }
    }

    protected static void safeClose(OutputStream os) {
        try {
            os.close();
        } catch (IOException e) {
            LOGGER.error().append("Error closing to FIX log").append(e).commit();
        }
    }

}
