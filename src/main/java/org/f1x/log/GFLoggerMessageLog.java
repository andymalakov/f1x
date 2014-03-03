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

import org.f1x.util.ByteArrayReference;
import org.f1x.util.MutableByteSequence;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;

/**
 * Stores FIX messages into GF Logger
 */
public class GFLoggerMessageLog implements MessageLog {
    private static final GFLog LOGGER = GFLogFactory.getLog(GFLoggerMessageLog.class);

    //TODO: Ask gflogger dev to add GFLog.append(byte[], offset,lenght) to avoid this
    private final ThreadLocal<ByteArrayReference> byteSequences = new ThreadLocal<>();

    public GFLoggerMessageLog() {
    }

    @Override
    public void log(boolean isInbound, byte[] buffer, int offset, int length) {

        ByteArrayReference byteSequence = byteSequences.get();
        if (byteSequence == null) {
            byteSequence = new ByteArrayReference();
            byteSequences.set(byteSequence);
        }
        byteSequence.set(buffer, offset, length);

        LOGGER.info().append((CharSequence) byteSequence).commit();
    }

    @Override
    public void close() throws IOException {
        // does nothing
    }
}
