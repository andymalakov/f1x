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

import java.util.concurrent.atomic.AtomicBoolean;

/** WorkProcessor adapted for ByteRing consumption */
public abstract class AbstractByteRingConsumer implements EventProcessor {

    private final AtomicBoolean running = new AtomicBoolean(false);
    protected final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    protected final ByteRing ring;
    protected final SequenceBarrier sequenceBarrier;

    /**
     * Construct a {@link com.lmax.disruptor.WorkProcessor}.
     *
     * @param ring            source byte ring containing inbound messages
     * @param sequenceBarrier on which it is waiting.
     */
    public AbstractByteRingConsumer(ByteRing ring, SequenceBarrier sequenceBarrier) {
        this.ring = ring;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void halt() {
        shutdown();
        sequenceBarrier.alert();
    }

//    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * It is ok to have another thread re-run this method after a halt().  //TODO: Do we always return from this method in re-usable state?
     *
     * @throws IllegalStateException if this processor is already running
     */
    @Override
    public void run() {
        start();

        try {
            consumeRing();
        } catch (InterruptedException | AlertException | TimeoutException e) {
            if (running.get())
                System.err.println("Aborted " + this); //TODO: Log
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Log
        }

        shutdown();
    }

    protected void start() {
        if (!running.compareAndSet(false, true))
            throw new IllegalStateException("Thread is already running");
        sequenceBarrier.clearAlert();
    }

    protected void shutdown() {
        running.set(false);
    }

    protected void consumeRing() throws AlertException, InterruptedException, TimeoutException {
        long consumed = sequence.get();
        long availableSequence = Sequencer.INITIAL_CURSOR_VALUE;

        while (true) {
            final int blockSize = getNextBlockSize();
            final long nextSequenceToWait = consumed + blockSize;  // points to the last byte of to-be-consumed sequence
            if (availableSequence < nextSequenceToWait) {
                availableSequence = sequenceBarrier.waitFor(nextSequenceToWait);
                assert availableSequence >= nextSequenceToWait;
            }

            process(consumed + 1, blockSize);

            consumed = nextSequenceToWait;
            sequence.set(consumed);
        }
    }

    /**
     * @return size of the next block to consume, greater than zero. This call may be blocked until ring buffer has enough data.
     */
    protected abstract int getNextBlockSize();

    /**
     * @param sequence start seqence of the block
     * @param blockSize block size
     */
    protected abstract void process(long sequence, int blockSize);

}
