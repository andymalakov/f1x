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

import com.lmax.disruptor.*;
import com.lmax.disruptor.util.Util;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageProcessorPool {

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ByteRing ringBuffer;

    // MessageProcessor are created to wrap each of the provided RingBufferBlockProcessor
    private final MessageProcessor[] messageProcessors;

    /**
     * Create a worker pool to enable an array of {@link RingBufferBlockProcessor}s to consume published sequences.
     * <p/>
     * This option requires a pre-configured {@link RingBuffer} which must have {@link RingBuffer#addGatingSequences(Sequence...)}
     * called before the work pool is started.
     *
     * @param ringBuffer       of events to be consumed.
     * @param sequenceBarrier  on which the workers will depend.
     * @param exceptionHandler to callback when an error occurs which is not handled by the {@link WorkHandler}s.
     * @param processors       array of processors to distribute the work load across.
     */
    public MessageProcessorPool(ByteRing ringBuffer,
                                final SequenceBarrier sequenceBarrier,
                                final ExceptionHandler exceptionHandler,
                                final RingBufferBlockProcessor... processors) {
        this.ringBuffer = ringBuffer;
        final int numWorkers = processors.length;
        messageProcessors = new MessageProcessor[numWorkers];

        for (int i = 0; i < numWorkers; i++) {
            messageProcessors[i] = new MessageProcessor(ringBuffer,
                sequenceBarrier,
                processors[i],
                exceptionHandler);
        }

        //TODO: ringBuffer.addGatingSequences(getWorkerSequences());
    }

    /**
     * Get an array of {@link Sequence}s representing the progress of the workers.
     *
     * @return an array of {@link Sequence}s representing the progress of the workers.
     */
    public Sequence[] getWorkerSequences() {
        final Sequence[] sequences = new Sequence[messageProcessors.length];
        for (int i = 0, size = messageProcessors.length; i < size; i++) {
            sequences[i] = messageProcessors[i].getSequence();
        }

        return sequences;
    }

    /**
     * Start the worker pool processing events in sequence.
     *
     * @param executor providing threads for running the workers.
     * @return the {@link RingBuffer} used for the work queue.
     * @throws IllegalStateException if the pool has already been started and not halted yet
     */
    public ByteRing start(final Executor executor) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("WorkerPool has already been started and cannot be restarted until halted.");
        }

        final long cursor = ringBuffer.getCursor();
        for (MessageProcessor processor : messageProcessors) {
            processor.getSequence().set(cursor);
            executor.execute(processor);
        }

        return ringBuffer;
    }

    /**
     * Wait for the {@link RingBuffer} to drain of published events then halt the workers.
     */
    public void drainAndHalt() {
        Sequence[] workerSequences = getWorkerSequences();

        while (ringBuffer.getCursor() > Util.getMinimumSequence(workerSequences)) {
            Thread.yield();
        }

        for (MessageProcessor processor : messageProcessors) {
            processor.halt();
        }

        started.set(false);
    }

    /**
     * Halt all workers immediately at the end of their current cycle.
     */
    public void halt() {
        for (MessageProcessor processor : messageProcessors) {
            processor.halt();
        }

        started.set(false);
    }

}
