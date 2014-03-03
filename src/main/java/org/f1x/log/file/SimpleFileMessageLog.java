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

import org.f1x.log.LogFormatter;
import org.f1x.log.OutputStreamMessageLog;
import org.f1x.log.SimpleLogFormatter;
import org.f1x.util.TimeSource;

import java.io.*;

/** Simple message log backed by Buffered File Output Stream */
@Deprecated
public class SimpleFileMessageLog extends OutputStreamMessageLog {

    /**
     * Constructs log that uses default formatter
     * @param file destination file
     * @param timeSource time source used for formatting timestamps
     * @param bufferSize size of buffer (recommended size is 8192). Negative value or zero disable buffering (not recommended).
     */
    public SimpleFileMessageLog(File file, TimeSource timeSource, int bufferSize) {
        this(file, new SimpleLogFormatter(timeSource), bufferSize);
    }

    /**
     * @param file destination file
     * @param formatter Optional formatter to be applied for logging each inbound/outbound message
     * @param bufferSize size of buffer (recommended size is 8192). Negative value or zero disable buffering (not recommended).
     */
    public SimpleFileMessageLog(File file, LogFormatter formatter, int bufferSize) {
        super(openOutputStream(file, bufferSize), formatter);
    }

    protected static OutputStream openOutputStream(File file, int bufferSize){
        try {
            OutputStream result = new FileOutputStream(file, true);
            if (bufferSize > 0)
                result = new BufferedOutputStream(result, bufferSize);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Can't open log file " + file.getAbsolutePath(), e);
        }
    }

}
