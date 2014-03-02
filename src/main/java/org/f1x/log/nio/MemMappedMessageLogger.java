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

package org.f1x.log.nio;

import org.f1x.log.MessageLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Message logger that keeps last <code>maxSize</code> bytes of FIX traffic.
 */
public class MemMappedMessageLogger implements MessageLog {

    private final RandomAccessFile raf;
    private final MappedByteBuffer out;

    public MemMappedMessageLogger (File file, int maxSize) {
        try {
            raf = new RandomAccessFile(file, "rw");
            out = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, maxSize);
        } catch (IOException e) {
            throw new RuntimeException("Error opening log file " + file.getAbsolutePath(), e);
        }
    }

    private static final byte IN = (byte) '>';
    private static final byte OUT = (byte) '<';
    private static final byte [] EOF = { (byte)'\n', (byte)'E', (byte)'O', (byte)'F'};

    @Override
    public void log(boolean isInbound, byte[] buffer, int offset, int length) {
        if (out.remaining() < length)
            out.position(0);
        else
            out.position(out.position() - EOF.length - 1);  // step back

        out.put(isInbound ? IN : OUT);
        out.put(buffer, offset, length);
        out.put(EOF, 0, EOF.length);

    }

    @Override
    public void close() throws IOException {
        out.force();
        raf.close();
    }
}
