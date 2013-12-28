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

package org.f1x.util;

/** Simple class to iterate over circular buffer of bytes. Not thread safe. */
public class ByteRingReader { //TODO: Similar to NIO ByteBuffer

    private final byte [] buffer;
    private final int bufferSize;
    private int offset;
    private int remainingLength;

    public ByteRingReader(byte[] buffer) {
        this.buffer = buffer;
        this.bufferSize = buffer.length;
    }

    /** Resets reader to read from position <code>offset</code>, allowing to read up to <code>length</code> bytes */
    public void reset(int offset, int length) {
        this.offset = offset;
        this.remainingLength = length;
    }

    /** Reads next byte and advances cursor */
    public byte next() {
        if (remainingLength == 0)
            throw new IndexOutOfBoundsException();
        byte result = buffer[offset];
        advance();
        return result;
    }

    /** Skips next byte and advances cursor */
    public void skip(int numberOfBytes) {
        offset = (offset + numberOfBytes)%bufferSize;
        remainingLength -= numberOfBytes;
    }

    private void advance() {
        offset++;
        if (offset == bufferSize)
            offset = 0;

        remainingLength --;
    }

    public int getRemainingLength() {
        return remainingLength;
    }
}