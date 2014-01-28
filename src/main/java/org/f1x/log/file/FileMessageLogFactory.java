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
import org.f1x.util.RealTimeSource;

import java.io.File;

public class FileMessageLogFactory implements MessageLogFactory {

    private final File dir;
    private final FileLogSettings logSettings;

    public FileMessageLogFactory(String dir) {
        this(new FileLogSettings (dir));
    }


    public FileMessageLogFactory(FileLogSettings logSettings) {
        dir = nonNullFile(logSettings.getLogDir());
        dir.mkdirs();
        if ( ! dir.isDirectory())
            throw new RuntimeException("Bad Log directory: \"" + dir + "\"");

        this.logSettings = logSettings;
    }


    @Override
    public MessageLog create(SessionID sessionID) {
        FileMessageLog result = new FileMessageLog(dir, sessionID, RealTimeSource.INSTANCE, logSettings.getFlushPeriod());
        result.start();
        return result;
    }

    private static File nonNullFile (String dir) {
        if (dir == null || dir.isEmpty())
            throw new IllegalArgumentException("Log directory is not defined");
        return new File(dir);
    }

}
