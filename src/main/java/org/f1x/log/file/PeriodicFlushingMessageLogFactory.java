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
import org.f1x.log.SimpleLogFormatter;
import org.f1x.util.RealTimeSource;
import org.f1x.util.TimeSource;

import java.io.File;

/**
 * Factory for {@link PeriodicFlushingMessageLog}.
 */
public class PeriodicFlushingMessageLogFactory extends AbstractFileMessageLogFactory {
    public static final int DEFAULT_FILE_BUFFER_SIZE = 8192;
    public static final int DEFAULT_FLUSH_PERIOD = 15000;

    protected FileNameGenerator fileNameGenerator;
    protected OutputStreamFactory streamFactory;

    protected int flushPeriod = DEFAULT_FLUSH_PERIOD;
    protected TimeSource timeSource = RealTimeSource.INSTANCE;
    protected LogFormatter logFormatter;

    /**
     * @param logDir directory where log files will reside
     */
    public PeriodicFlushingMessageLogFactory(File logDir) {
        this(logDir, new SimpleFileNameGenerator(), new BufferedOutputStreamFactory(DEFAULT_FILE_BUFFER_SIZE, false));
    }

    /**
     * @param logDir directory where log files will reside
     */
    public PeriodicFlushingMessageLogFactory(File logDir, FileNameGenerator fileNameGenerator, OutputStreamFactory streamFactory) {
        super(logDir);
        this.fileNameGenerator = fileNameGenerator;
        this.streamFactory = streamFactory;
    }

    @Override
    public MessageLog create(SessionID sessionID) {
        if (logFormatter == null)
            logFormatter = new SimpleLogFormatter(timeSource);
        File logFile = new File (logDir, fileNameGenerator.getLogFile(sessionID));
        PeriodicFlushingMessageLog result = new PeriodicFlushingMessageLog(streamFactory.create(logFile), logFormatter);
        result.start(sessionID, timeSource, flushPeriod);
        return result;
    }

    /** Used to format each log record in a file, can be null */
    public LogFormatter getLogFormatter() {
        return logFormatter;
    }

    /** Used to format each log record in a file, can be null */
    public void setLogFormatter(LogFormatter logFormatter) {
        this.logFormatter = logFormatter;
    }

    /** @return TimeSource used to determine midnight */
    public TimeSource getTimeSource() {
        return timeSource;
    }

    /** @param timeSource TimeSource used to determine midnight */
    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    /** @return Most loggers use buffers. This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing */
    public int getFlushPeriod() {
        return flushPeriod;
    }

    /** @param flushPeriod This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing */
    public void setFlushPeriod(int flushPeriod) {
        this.flushPeriod = flushPeriod;
    }

    public FileNameGenerator getFileNameGenerator() {
        return fileNameGenerator;
    }

    public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    public OutputStreamFactory getStreamFactory() {
        return streamFactory;
    }

    public void setStreamFactory(OutputStreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }
}
