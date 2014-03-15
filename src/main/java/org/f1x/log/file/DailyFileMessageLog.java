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
import org.f1x.log.LogFormatter;
import org.f1x.util.TimeSource;

import java.io.File;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.TimeZone;

public class DailyFileMessageLog extends PeriodicFlushingMessageLog {
    private final OutputStreamRollover dailyFileRoller;


    /**
     * @param logDir directory where log files will reside
     */
    public DailyFileMessageLog(SessionID sessionID, File logDir, OutputStreamFactory streamFactory, LogFormatter formatter, TimeSource timeSource, TimeZone tz) {
        super(null, formatter);

        this.dailyFileRoller = new OutputStreamRollover(logDir, streamFactory, sessionID, timeSource, tz);
        this.os = dailyFileRoller.createCurrentStream();

    }

    @Override
    public void start(SessionID sessionID, TimeSource timeSource, int flushPeriod) {
        super.start(sessionID, timeSource, flushPeriod);
        this.dailyFileRoller.start();
    }

    @Override
    public void close() {
        dailyFileRoller.interrupt();
        super.close();
    }

    private final class OutputStreamRollover extends Thread {
        private final StringBuilder nextFileName = new StringBuilder();
        private final Calendar calendar;
        private final SessionID sessionID;
        private final TimeSource timeSource;
        private final File logDir;
        private final OutputStreamFactory streamFactory;


        OutputStreamRollover(File logDir, OutputStreamFactory streamFactory, SessionID sessionID, TimeSource timeSource, TimeZone tz) {
            super("Daily roll for " + sessionID);
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY - 1);
            this.streamFactory = streamFactory;
            this.logDir = logDir;
            this.sessionID = sessionID;
            this.timeSource = timeSource;
            this.calendar = getCurrentDayStart(timeSource, tz);
        }

        @Override
        public void run() {
            while (true) {
                try {

                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    timeSource.sleep(Math.max(1, calendar.getTimeInMillis() - timeSource.currentTimeMillis()));

                    final OutputStream newStream = createCurrentStream();
                    final OutputStream oldStream = os;
                    synchronized (lock) {
                        DailyFileMessageLog.this.os = newStream;
                    }
                    safeClose(oldStream);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.error().append("Error writing FIX log").append(e).commit();
                }
            }
        }

        private void generateCurrentFileName() {
            nextFileName.setLength(0);
            nextFileName.append (sessionID.getSenderCompId().toString());
            nextFileName.append ('-');
            nextFileName.append (sessionID.getTargetCompId().toString());

            nextFileName.append ('-');
            nextFileName.append(calendar.get(Calendar.YEAR));
            appendPadZero(calendar.get(Calendar.MONTH) + 1);
            appendPadZero(calendar.get(Calendar.DAY_OF_MONTH));

            nextFileName.append(".log");
        }

        private void appendPadZero(int number) {
            if (number < 10)
                nextFileName.append('0');
            nextFileName.append(number);
        }


        private OutputStream createCurrentStream() {
            generateCurrentFileName();
            File file = new File (logDir, nextFileName.toString());
            return streamFactory.create(file);
        }
    }


    private static Calendar getCurrentDayStart(TimeSource timeSource, TimeZone tz) {
        // set time of day used to switch
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(timeSource.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() > System.currentTimeMillis())
            calendar.add(Calendar.DAY_OF_MONTH, -1);

        return calendar;
    }

}
