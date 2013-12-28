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

package org.f1x.io.disruptor;

import org.f1x.util.AsciiUtils;

class PlaybackByteProducer implements RingBufferBlockProcessor {
    private final byte [] source;
    private int current = 0;

    public PlaybackByteProducer(String... messages) {
        source = combine(messages);
    }

    private static byte[] combine(String ... messages) {
        int len = 0;
        for (String message : messages)
            len += message.length(); // assuming ASCII only

        int offset = 0;
        byte [] result = new byte [2*len];
        for (String message : messages) {
            byte [] messageBytes = AsciiUtils.getBytes(message);
            System.arraycopy(messageBytes, 0, result, offset, messageBytes.length);
            offset += messageBytes.length;
        }
        for (String message : messages) {
            byte [] messageBytes = AsciiUtils.getBytes(message);
            System.arraycopy(messageBytes, 0, result, offset, messageBytes.length);
            offset += messageBytes.length;
        }
        return result;
    }

    @Override
    public int process(byte[] buffer, int offset, int length, int ringBufferSize) {
        if (offset + length <= ringBufferSize) {
            return write(buffer, offset, length);
        } else {
            int wrappedSize = offset + length - ringBufferSize;
            assert wrappedSize > 0;
            assert wrappedSize < length;
            final int numberOfBytesToWrite = length - wrappedSize;
            int result = write(buffer, offset, numberOfBytesToWrite);
            if (result == numberOfBytesToWrite)
                result += write(buffer, 0, wrappedSize);
            return result;
        }
    }


    public int write(byte[] buffer, int offset, int length) {
        assert current >= 0;
        assert current < source.length;
        assert current + length < source.length;


        System.arraycopy(source, current, buffer, offset, length);

        current = (current + length) % (source.length/2);
        return length;
    }

    @Override
    public void close() {

    }
}
