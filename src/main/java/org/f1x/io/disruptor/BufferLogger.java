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

import com.lmax.disruptor.ExceptionHandler;
import org.f1x.io.socket.RingBuffer2StreamProcessor;

import java.io.*;

@Deprecated // this was quick and dirty implementation
public class BufferLogger extends RingBuffer2StreamProcessor implements Runnable{

    private volatile boolean active = true;

    public static RingBufferBlockProcessor createLogger(File file, int bufferSize, ExceptionHandler exceptionHandler) {
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file, true), bufferSize);
            BufferLogger result = new BufferLogger(os, exceptionHandler);
            new Thread(result, "FIX Log Flusher").start();
            return result;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot write into " + file, e);
        }
    }

    protected BufferLogger(OutputStream os, ExceptionHandler exceptionHandler) {
        super(os, exceptionHandler);
    }

    @Override
    public synchronized void copy (byte[] buffer, int offset, int length, int ringBufferSize) throws IOException {
        super.copy(buffer, offset, length, ringBufferSize);
        os.write(Character.LINE_SEPARATOR);
    }

    @Override
    public void close() {
        active = false;
        super.close();
    }

    @Override
    public void run () {
        while (active) {
            try {
                synchronized (this) {
                    os.flush();
                }
                Thread.sleep(2000); //TODO: Add parameter
            } catch (Exception e) {
                e.printStackTrace();  //TODO: Log
            }
        }
    }
}

