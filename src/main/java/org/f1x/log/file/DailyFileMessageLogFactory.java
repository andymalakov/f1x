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
import org.f1x.log.MessageLog;
import org.f1x.log.MessageLogFactory;
import org.f1x.log.SimpleLogFormatter;
import org.f1x.util.RealTimeSource;
import org.f1x.util.TimeSource;

import java.io.File;
import java.util.TimeZone;

/**
 * Factory for {@link DailyFileMessageLog}.
 */
public class DailyFileMessageLogFactory extends AbstractFileMessageLogFactory {

    public static final int DEFAULT_FILE_BUFFER_SIZE = 8192;
    public static final int DEFAULT_FLUSH_PERIOD = 15000;

    private final OutputStreamFactory streamFactory;

    private TimeZone tz = TimeZone.getDefault();
    private int flushPeriod = DEFAULT_FLUSH_PERIOD;
    private TimeSource timeSource = RealTimeSource.INSTANCE;
    private LogFormatter logFormatter;

    /**
     * @param logDir directory where log files will reside
     */
    public DailyFileMessageLogFactory (File logDir) {
        this(logDir, DEFAULT_FILE_BUFFER_SIZE);
    }

    /**
     * @param logDir directory where log files will reside
     * @param fileBufferSize Defines buffer size.
     */
    public DailyFileMessageLogFactory (File logDir, int fileBufferSize) {
        super(logDir);
        this.streamFactory = new BufferedOutputStreamFactory(fileBufferSize, true);
    }

    @Override
    public MessageLog create(SessionID sessionID) {
        if (logFormatter == null)
            logFormatter = new SimpleLogFormatter(timeSource);
        return new DailyFileMessageLog(sessionID, logDir, streamFactory, logFormatter, timeSource, tz, flushPeriod);
    }

    /** This logger begins new file each midnight in given time zone */
    public TimeZone getTimeZone() {
        return tz;
    }

    /** This logger begins new file each midnight in given time zone */
    public void setTimeZone(TimeZone tz) {
        this.tz = tz;
    }

    /** @return Most loggers use buffers. This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing */
    public int getFlushPeriod() {
        return flushPeriod;
    }

    /** @param flushPeriod This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing */
    public void setFlushPeriod(int flushPeriod) {
        this.flushPeriod = flushPeriod;
    }

    /** @return TimeSource used to determine midnight */
    public TimeSource getTimeSource() {
        return timeSource;
    }

    /** @param timeSource TimeSource used to determine midnight */
    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    /** Used to format each log record in a file, can be null */
    public LogFormatter getLogFormatter() {
        return logFormatter;
    }

    /** Used to format each log record in a file, can be null */
    public void setLogFormatter(LogFormatter logFormatter) {
        this.logFormatter = logFormatter;
    }
}
