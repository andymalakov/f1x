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
import org.f1x.log.MessageLog;
import org.f1x.log.MessageLogFactory;
import org.f1x.log.OutputStreamMessageLog;
import org.f1x.util.RealTimeSource;

import java.io.*;
import java.net.URLEncoder;

public class FileMessageLogFactory implements MessageLogFactory {

    private final File dir;
    private final FileLogSettings logSettings;

    public FileMessageLogFactory(String dir) {
        this(new FileLogSettings (dir));
    }


    public FileMessageLogFactory(FileLogSettings logSettings) {
        dir = nonNullFile(logSettings.getLogDir());

        if (! dir.exists() || ! dir.isDirectory())
            throw new RuntimeException("Log directory does not exist: \"" + dir + "\"");

        this.logSettings = logSettings;
    }


    @Override
    public MessageLog create(SessionID sessionID) {
        try {
            File file = new File (dir, encodeAsLogFilename(sessionID));
            OutputStream os = new FileOutputStream(file, true); // append
            if (logSettings.getFileBufferSize() > 0)
                os = new BufferedOutputStream(os, logSettings.getFileBufferSize());

            //TODO: Use SimpleFileMessageLog if all settings are default
            if (logSettings.getMaxLogSize() > 0) {
                // Should this also do a periodic flush?
                RollingFileMessageLog.OutputStreamFactory streamFactory = new RollingFileMessageLog.OutputStreamFactory(){
                    @Override
                    public OutputStream create() {
                        return null;  //TODO:
                    }
                };

                RollingFileMessageLog result = new RollingFileMessageLog(streamFactory, sessionID, RealTimeSource.INSTANCE, logSettings.getFlushPeriod(), logSettings.getMaxLogSize());
                result.start();
                return result;
            }

            if (logSettings.getFlushPeriod() > 0) {
                PeriodicFlushingMessageLog result = new PeriodicFlushingMessageLog(os, sessionID, RealTimeSource.INSTANCE, logSettings.getFlushPeriod());
                result.start();
                return result;
            }

            return new OutputStreamMessageLog(os, RealTimeSource.INSTANCE);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can't create a log file ", e);
        }
    }

    static String encodeAsLogFilename(SessionID sessionID) {
        String result = sessionID.getSenderCompId().toString() + '-' + sessionID.getTargetCompId().toString() /* + '.' + (fileNameSuffix)*/ + ".log";

        try {
            return URLEncoder.encode(result, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private static File nonNullFile (String dir) {
        if (dir == null || dir.isEmpty())
            throw new IllegalArgumentException("Log directory is not defined");
        return new File(dir);
    }

}
