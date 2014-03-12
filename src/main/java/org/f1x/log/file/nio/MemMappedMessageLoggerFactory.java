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
package org.f1x.log.file.nio;

import org.f1x.api.session.SessionID;
import org.f1x.log.MessageLog;
import org.f1x.log.file.AbstractFileMessageLogFactory;
import org.f1x.log.file.FileNameGenerator;
import org.f1x.log.file.SimpleFileNameGenerator;

import java.io.File;

/**
 * Factory for {@link MemMappedMessageLogger}.
 */
public class MemMappedMessageLoggerFactory extends AbstractFileMessageLogFactory {

    public static final int DEFAULT_LOG_FILE_SIZE = 4*1024*1024;

    /** This setting defines size of log file */
    private int maxSize;
    private FileNameGenerator fileNameGenerator;

    /**
     * Constricts factory using default file size and file name generator
     * @param logDir directory where log files will reside
     */
    public MemMappedMessageLoggerFactory (File logDir) {
        this(logDir, DEFAULT_LOG_FILE_SIZE, new SimpleFileNameGenerator()); // 4Mb
    }

    /**
     * @param logDir directory where log files will reside
     * @param maxSize This setting defines maximum size of log file. Specifying large value will cause page faults.
     */
    public MemMappedMessageLoggerFactory (File logDir, int maxSize, FileNameGenerator fileNameGenerator) {
        super(logDir);
        this.maxSize = maxSize;
        this.fileNameGenerator = fileNameGenerator;
    }

    @Override
    public MessageLog create(SessionID sessionID) {
        File logFile = new File(logDir, fileNameGenerator.getLogFile(sessionID));
        return new MemMappedMessageLogger(logFile, maxSize);
    }

    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @param maxSize This setting defines maximum size of log file.  Specifying large value will cause page faults.
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

}
