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
import org.f1x.log.MessageLog;
import org.f1x.log.SimpleLogFormatter;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Factory for {@link RollingFileMessageLog}.
 */
public class RollingFileMessageLogFactory extends PeriodicFlushingMessageLogFactory {

    private int maxNumberOfFiles;
    private long bytesPerFile;

    public RollingFileMessageLogFactory (File logDir, int maxNumberOfFiles, long bytesPerFile) {
        super(logDir, new RollingFileNameGenerator(), new BufferedOutputStreamFactory(DEFAULT_FILE_BUFFER_SIZE, false));
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.bytesPerFile = bytesPerFile;
    }

    @Override
    public MessageLog create(SessionID sessionID) {
        if (logFormatter == null)
            logFormatter = new SimpleLogFormatter(timeSource);
        File [] files = new File[maxNumberOfFiles];
        for(int i=0; i < maxNumberOfFiles; i++)
            files[i] = new File(logDir, fileNameGenerator.getLogFile(sessionID));
        return new RollingFileMessageLog(files, streamFactory, sessionID, logFormatter, timeSource, bytesPerFile, flushPeriod);
    }

    /** @return Logger will roll to the next file once current log file size exceeds this limit (in bytes). */
    public long getBytesPerFile() {
        return bytesPerFile;
    }

    public int getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    /** @param bytesPerFile Logger will roll to the next file once current log file size exceeds this limit (in bytes). */
    public void setBytesPerFile(long bytesPerFile) {
        this.bytesPerFile = bytesPerFile;
    }

    public void setMaxNumberOfFiles(int maxNumberOfFiles) {
        this.maxNumberOfFiles = maxNumberOfFiles;
    }


    public static class RollingFileNameGenerator implements FileNameGenerator {
        private int fileCounter;

        @Override
        public String getLogFile(SessionID sessionID) {
            StringBuilder sb = new StringBuilder();
            sb.append (sessionID.getSenderCompId().toString());
            sb.append ('-');
            sb.append (sessionID.getTargetCompId().toString());
            sb.append('.');
            sb.append(++fileCounter);
            sb.append(".log");

            try {
                return URLEncoder.encode(sb.toString(), "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
