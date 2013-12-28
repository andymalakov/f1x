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

package org.f1x.io.socket;

import com.lmax.disruptor.ExceptionHandler;
import org.f1x.io.disruptor.RingBufferBlockProcessor;

import java.io.IOException;
import java.io.OutputStream;

/**
 * RingBuffer processor that copies ring buffer into output stream
 */
public class RingBuffer2StreamProcessor implements RingBufferBlockProcessor {
    protected final ExceptionHandler exceptionHandler;
    protected final OutputStream os;

    public RingBuffer2StreamProcessor(OutputStream os, ExceptionHandler exceptionHandler) {
        this.os = os;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public final int process(byte[] buffer, int offset, int length, int ringBufferSize) {
        try {
            copy(buffer, offset, length, ringBufferSize);
        } catch (IOException e) {
            exceptionHandler.handleEventException(e, -1L, "Error sending data");
        }
        return length;
    }

    protected void copy (byte[] buffer, int offset, int length, int ringBufferSize) throws IOException {
        if (offset + length <= ringBufferSize) {
            os.write(buffer, offset, length);
        } else {

            int wrappedSize = offset + length - ringBufferSize;
            assert wrappedSize > 0;
            assert wrappedSize < length;
            final int numberOfBytesToWrite = length - wrappedSize;

            //TODO: Intermediate System.arraycopy(source, current, buffer, offset, length);

            os.write(buffer, offset, numberOfBytesToWrite);
            os.write(buffer, 0, wrappedSize);
        }
    }

    @Override
    public void close() {
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Log me
        }
    }
}
