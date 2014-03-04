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

import org.f1x.log.MessageLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Message logger that keeps last <code>maxSize</code> bytes of FIX traffic.
 * Fixed size logger backed by memory mapped file.
 */
public class MemMappedMessageLogger implements MessageLog {

    private static final byte IN = (byte) '>';
    private static final byte OUT = (byte) '<';

    static final byte [] EOF =  { (byte)'E', (byte)'O', (byte)'F'};
    static final byte [] TAIL = { (byte)' ', (byte)' ', (byte)' '};

    private final RandomAccessFile raf;
    private final MappedByteBuffer out;

    public MemMappedMessageLogger (File file, int maxSize) {
        assert EOF.length == TAIL.length; // we use TAIL to wipe "EOF"

        try {
            raf = new RandomAccessFile(file, "rw");
            out = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, maxSize);

//            if (out.hasArray())
//                Arrays.fill(out.array(), (byte)' ');

            out.put(EOF, 0, EOF.length);



        } catch (IOException e) {
            throw new RuntimeException("Error opening log file " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public synchronized void log(boolean isInbound, byte[] buffer, int offset, int length) {
        //TODO: bug may produce negative
        out.position(out.position() - EOF.length);  // step back

        if (out.remaining() < length + EOF.length + 1) {
            out.put(TAIL, 0, TAIL.length);
            out.position(0);
        }

        out.put(isInbound ? IN : OUT);
        out.put(buffer, offset, length);
        out.put((byte)'\n');
        out.put(EOF, 0, EOF.length);

    }

    @Override
    public synchronized void close() throws IOException {
        out.force();
        raf.close();
    }
}
