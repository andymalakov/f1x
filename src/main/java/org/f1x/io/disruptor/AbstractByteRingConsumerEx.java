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
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.SequenceBarrier;

public abstract class AbstractByteRingConsumerEx extends AbstractByteRingConsumer {
    private final RingBufferBlockProcessor delegate;
    private final ExceptionHandler exceptionHandler;

    /**
     * @param ring             source byte ring containing inbound messages
     * @param sequenceBarrier  on which it is waiting.
     * @param delegate   is the delegate to which message are dispatched.
     * @param exceptionHandler to be called back when an error occurs
     *                         as {@link com.lmax.disruptor.Sequencer#INITIAL_CURSOR_VALUE}
     */
    public AbstractByteRingConsumerEx(ByteRing ring,
                                      SequenceBarrier sequenceBarrier,
                                      RingBufferBlockProcessor delegate,
                                      ExceptionHandler exceptionHandler) {
        super(ring, sequenceBarrier);
        this.delegate = delegate;
        this.exceptionHandler = exceptionHandler;
    }


    @Override
    protected void process(long sequence, int messageSize) {
        try {
            ring.processBlock(sequence, messageSize, delegate);
        } catch (Throwable e) {
            exceptionHandler.handleEventException(e, sequence, null);
        }
    }

    @Override
    protected void start() {
        super.start();
        if (delegate instanceof LifecycleAware) {
            try {
                ((LifecycleAware) delegate).onStart();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    @Override
    protected void shutdown() {
        super.shutdown();

        if (delegate instanceof LifecycleAware) {
            try {
                ((LifecycleAware) delegate).onShutdown();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnShutdownException(ex);
            }
        }
    }
}
