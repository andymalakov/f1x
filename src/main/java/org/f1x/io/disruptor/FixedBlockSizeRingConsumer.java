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

/** ByteRingConsumer that consumers blocks of fixed size data packets from ByteRing */
public final class FixedBlockSizeRingConsumer extends AbstractByteRingConsumerEx {
    private final int blockSize;

    /**
     * @param ring             source byte ring containing inbound messages
     * @param sequenceBarrier  on which it is waiting.
     * @param delegate         is the delegate to which message are dispatched.
     * @param exceptionHandler to be called back when an error occurs
*                         as {@link com.lmax.disruptor.Sequencer#INITIAL_CURSOR_VALUE}
     * @param blockSize
     */
    public FixedBlockSizeRingConsumer(ByteRing ring, SequenceBarrier sequenceBarrier, RingBufferBlockProcessor delegate, ExceptionHandler exceptionHandler, int blockSize) {
        super(ring, sequenceBarrier, delegate, exceptionHandler);
        this.blockSize = blockSize;
    }

    @Override
    protected int getNextBlockSize() {
        return blockSize;
    }

}
