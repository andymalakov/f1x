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

package org.f1x.io;

import org.f1x.io.disruptor.ByteRing;
import java.io.IOException;


/** Output channel into ByteRing. Each stored block is prefixed with its length */
public final class RingBufferStreamChannel implements OutputChannel {
    private static final int SIZE_OF_INT32 = 4;
    private final ByteRing ring;

    public RingBufferStreamChannel(ByteRing ring) {
        this.ring = ring;
    }

    @Override
    public void write(final byte[] buffer, final int offset, final int length) throws IOException {
        final int allocSize = length + SIZE_OF_INT32;
        long high = ring.next(allocSize);
        long low = high - (allocSize - 1);

        ring.writeInt(low, length); //TODO: Develop alternative version that does not write length (this will allow feeding multiple messages directly into Socket)
        ring.write(low + SIZE_OF_INT32, buffer, offset, length);
        ring.publish(high);
    }

    @Override
    public void close() throws IOException {
        //nothing to do
    }
}


