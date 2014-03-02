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
package org.f1x.log;

import org.f1x.util.AsciiUtils;
import org.f1x.util.TimeSource;
import org.f1x.util.format.TimeOfDayFormatter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Simple and thread-safe version of LogFormatter.
 * Each message is formatted with IN/OUT prefix that represent message direction, current time of day and message itself.
 * Typically each message contains an additional time field that indicates more precise timestamp (e.g. SendingTime(52) or TransactTime(60)).</p>
 *
 * <p>Format example:
 * <pre>
 * IN  01:23:45 8=FIX4.2|9=123|35=A|...
 * </pre>
 * </p>
 *
 * */
public class SimpleLogFormatter implements LogFormatter {
    private static final byte [] IN_PREFIX =  AsciiUtils.getBytes("IN  ");
    private static final byte [] OUT_PREFIX = AsciiUtils.getBytes("OUT ");

    private final byte [] timestampBuffer = new byte [TimeOfDayFormatter.LENGTH];
    private final TimeSource timeSource;

    public SimpleLogFormatter(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    public int log(boolean isInbound, byte[] buffer, int offset, int length, OutputStream os) throws IOException {
        final byte[] prefix = isInbound ? IN_PREFIX : OUT_PREFIX;

        TimeOfDayFormatter.format(timeSource.currentTimeMillis(), timestampBuffer, 0);
        os.write(timestampBuffer, 0, timestampBuffer.length);
        os.write(' ');
        os.write(prefix, 0, prefix.length);
        os.write(' ');
        os.write(buffer, offset, length);
        os.write(Character.LINE_SEPARATOR);

        return timestampBuffer.length + prefix.length + length + 3;
    }

}

