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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NOTE: This producer cannot share a ByteRing with other producers.
 */
public abstract class AbstractByteRingProducer implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);

    protected final ByteRing ring;
    protected final int minimumProducerBufferSize;

    /**
     * This producer cannot share a ByteRing with other producers.
     * @param ring ByteRing
     * @param minimumProducerBufferSize minimum size of allocated buffer
     */
    public AbstractByteRingProducer(ByteRing ring, int minimumProducerBufferSize) {
        assert minimumProducerBufferSize > 0;
        assert ring.getCapacity() > minimumProducerBufferSize;
        this.ring = ring;
        this.minimumProducerBufferSize = minimumProducerBufferSize;
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Thread is already running");
        }

        long low = 0;
        int currentCapacity = 0;
        while (true) {
            if (currentCapacity < minimumProducerBufferSize) {
                final int allocSize = getAllocSize(currentCapacity);
                long high = ring.next(allocSize); //TODO: Batch version that return all available space
                currentCapacity += allocSize;
                assert low + currentCapacity == high;
            }

            final int bytesProduced = produce (low, currentCapacity);
            if (bytesProduced > 0) {
                low+= bytesProduced;
                ring.publish(low);
                currentCapacity -= bytesProduced;
            }
        }
        //TODO:running.set(false);
    }

    /**
     * Produce content into the ringBuffer between positions <code>low</code> and <code>high</code>.
     * @param sequence start sequence
     * @param maxBytesToProduce maximum number of bytes to produce
     * @return size of produced content in bytes, greater than zero.
     */
    protected abstract int produce(long sequence, int maxBytesToProduce);

    /**
     * Called each time producer's buffer is less than minimum.
     * @return additional buffer size to allocate
     */
    protected int getAllocSize(int currentCapacity) {
        return 2*minimumProducerBufferSize;
    }
}
