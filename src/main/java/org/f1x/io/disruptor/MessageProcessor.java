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

class MessageProcessor implements EventProcessor {
    private static final int SIZE_OF_INT32 = 4;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private final ByteRing ring;
    private final SequenceBarrier sequenceBarrier;
    private final RingBufferBlockProcessor delegate;
    private final ExceptionHandler exceptionHandler;

    /**
     * Construct a {@link com.lmax.disruptor.WorkProcessor}.
     *
     * @param ring source byte ring containing inbound messages
     * @param sequenceBarrier on which it is waiting.
     * @param delegate is the delegate to which message are dispatched.
     * @param exceptionHandler to be called back when an error occurs
     * as {@link com.lmax.disruptor.Sequencer#INITIAL_CURSOR_VALUE}
     */
    public MessageProcessor(ByteRing ring,
                            SequenceBarrier sequenceBarrier,
                            RingBufferBlockProcessor delegate,
                            ExceptionHandler exceptionHandler)
    {
        this.ring = ring;
        this.sequenceBarrier = sequenceBarrier;
        this.delegate = delegate;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Sequence getSequence()
    {
        return sequence;
    }

    @Override
    public void halt()
    {
        running.set(false);
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
    public void run()
    {
        if (!running.compareAndSet(false, true))
        {
            throw new IllegalStateException("Thread is already running");
        }
        sequenceBarrier.clearAlert();

        notifyStart();

        try
        {
            long consumed = sequence.get();  // points to the last byte of already-consumed sequence
            long availableSequence = Sequencer.INITIAL_CURSOR_VALUE;

            while (true)
            {
                /// Step 1: Read message length
                final long messageLengthSequence = consumed + SIZE_OF_INT32;
                availableSequence = waitForData(availableSequence, messageLengthSequence);

                final int messageSize = ring.readInt(consumed + 1);
                consumed = messageLengthSequence;
                //sequence.set(consumed); let's postpone till we consume message as well (4 bytes are not releasing much buffer space anyway)

                /// Step 2: Read message itself
                final long nextSequenceToWait = consumed + messageSize;
                availableSequence = waitForData(availableSequence, nextSequenceToWait);

                try {
                    ring.processBlock(consumed+1, messageSize, delegate);
                } catch (Throwable e) {
                    exceptionHandler.handleEventException(e, consumed, null);
                }

                consumed = nextSequenceToWait;
                sequence.set(consumed);
            }

        } catch (InterruptedException | AlertException | TimeoutException e) {
            if ( running.get())
                System.err.println ("Aborted " + this); //TODO: Log
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Log
        }

        notifyShutdown();

        running.set(false);
    }

    private long waitForData(long availableSequence, long nextSequenceToWait) throws AlertException, InterruptedException, TimeoutException {
        if (availableSequence < nextSequenceToWait) {
            availableSequence = sequenceBarrier.waitFor(nextSequenceToWait);
            assert availableSequence >= nextSequenceToWait;
        }
        return availableSequence;
    }

    private void notifyStart()
    {
        if (delegate instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) delegate).onStart();
            }
            catch (final Throwable ex)
            {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    private void notifyShutdown()
    {
        if (delegate instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) delegate).onShutdown();
            }
            catch (final Throwable ex)
            {
                exceptionHandler.handleOnShutdownException(ex);
            }
        }
    }


}
