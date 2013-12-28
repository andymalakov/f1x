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
import com.lmax.disruptor.SequenceBarrier;

/** ByteRingConsumer that consumers blocks of fixed size data packets from ByteRing. Each block begins with INT32 section that stores block size (big-endian). */
public class VariableBlockSizeRingConsumer extends AbstractByteRingConsumerEx {

    private static final int SIZE_OF_INT32 = 4;
    private int nextBlockSize = 0;

    /**
     * @param ring             source byte ring containing inbound messages
     * @param sequenceBarrier  on which it is waiting.
     * @param delegate         is the delegate to which message are dispatched.
     * @param exceptionHandler to be called back when an error occurs
     *                         as {@link com.lmax.disruptor.Sequencer#INITIAL_CURSOR_VALUE}
     */
    public VariableBlockSizeRingConsumer(ByteRing ring, SequenceBarrier sequenceBarrier, RingBufferBlockProcessor delegate, ExceptionHandler exceptionHandler) {
        super(ring, sequenceBarrier, delegate, exceptionHandler);
    }

    @Override
    protected int getNextBlockSize() {
        return nextBlockSize == 0 ? SIZE_OF_INT32 : nextBlockSize;
    }

    @Override
    protected void start() {
        nextBlockSize = 0;
        super.start();
    }

    @Override
    protected void process(long sequence, int messageSize) {
        if (nextBlockSize == 0) {
            nextBlockSize = ring.readInt(sequence);
        } else {
            super.process(sequence, messageSize);
            nextBlockSize = 0;
        }
    }
}
