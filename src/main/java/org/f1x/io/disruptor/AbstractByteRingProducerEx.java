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

/**
 * Adapts disruptor buffer to memcopy-like interface ByteProducer
 */
public class AbstractByteRingProducerEx extends AbstractByteRingProducer {

    private final RingBufferBlockProcessor byteProducer;

    public AbstractByteRingProducerEx(ByteRing ring, int minimumProducerBufferSize, RingBufferBlockProcessor byteProducer) {
        super(ring, minimumProducerBufferSize);
        this.byteProducer = byteProducer;
    }

    /**
     * Produce content into the ringBuffer between positions <code>low</code> and <code>high</code>.
     * @param sequence start sequence
     * @param maxBytesToProduce maximum number of bytes to produce
     * @return size of produced content in bytes, greater than zero.
     */
    protected int produce(long sequence, int maxBytesToProduce) {
        return ring.processBlock(sequence, maxBytesToProduce, byteProducer);
    }
}
