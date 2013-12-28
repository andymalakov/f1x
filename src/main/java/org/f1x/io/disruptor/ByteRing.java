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
import org.f1x.util.ByteRingReader;

public final class ByteRing {

    private final int bufferSize;
    private final byte [] entries;
    private final int indexMask;

    private final SingleProducerSequencer sequencer;

    /**
     * Create a new single producer ByteRing with the specified wait strategy.
     *
     * @see SingleProducerSequencer
     * @param bufferSize number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @throws IllegalArgumentException if bufferSize is less than 1 and not a power of 2
     */
    public ByteRing (int bufferSize, WaitStrategy waitStrategy) {
        sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        this.bufferSize = bufferSize;
        this.entries = new byte[bufferSize];
        this.indexMask = bufferSize - 1;
    }

    /**
     * Claim the next n bytes in sequence for publishing.  Producer will have to use a little care and some math.
     * <pre>
     * int n = 10;
     * long hi = ring.next(n);
     * long lo = hi - (n - 1);
     * for (long sequence = lo; sequence <= hi; sequence++) {
     *     // Do work.
     * }
     * ring.publish(lo, hi);
     * </pre>
     *
     * @param n the number of sequences to claim
     * @return the highest claimed sequence value
     */
    public long next(int n)
    {
        return sequencer.next(n);
    }

    /** Translates sequence number to ring buffer offset */
    public final int index(long sequence) {
        return (int) sequence & indexMask;
    }

    /**
     * Publish byte sequence.
     *
     * @param hi last sequence number to publish
     */
    public void publish(long hi)
    {
        sequencer.publish(hi);
    }

    /**
     * Get the current cursor value for the ring buffer.  The cursor value is
     * the last value that was published, or the highest available sequence
     * that can be consumed.
     */
    public final long getCursor()
    {
        return sequencer.getCursor();
    }

    /**
     * Create a new SequenceBarrier to be used by an EventProcessor to track which messages
     * are available to be read from the ring buffer given a list of sequences to track.
     *
     * @see SequenceBarrier
     * @param sequencesToTrack Optional dependency
     * @return A sequence barrier that will track the specified sequences.
     */
    public SequenceBarrier newBarrier(Sequence... sequencesToTrack)
    {
        return sequencer.newBarrier(sequencesToTrack);
    }

    public void addGatingSequences(Sequence... gatingSequences)
    {
        sequencer.addGatingSequences(gatingSequences);
    }


    int write (int offset, int value) {
        assert offset >=0 && offset < bufferSize;
        entries[offset++] = (byte) (0xFF & value);
        if (offset == bufferSize)
            offset = 0;
        return offset;
    }

    /** Store given INT32 number in Big-Endian notation (Used to store message length) */
    public void writeInt (long sequence, int value) {
        assert sequence >= 0;
        int offset = index(sequence);

        offset = write (offset, value >>> 24);
        offset = write (offset, value >>> 16);
        offset = write (offset, value >>> 8);
                 write (offset, value);
    }

    /** Read INT32 number stored in Big-Endian notation (Used to read message length) */
    int readInt (long sequence) {
        assert sequence >= 0;
        int offset = index(sequence);

        int result = (0xFF & entries[offset++]);
        if (offset == bufferSize)
            offset = 0;
        result = (result << 8) + (0xFF & entries[offset++]);
        if (offset == bufferSize)
            offset = 0;
        result = (result << 8) + (0xFF & entries[offset++]);
        if (offset == bufferSize)
            offset = 0;
        result = (result << 8) + (0xFF & entries[offset]);

        return result;
    }


    int processBlock(long current, int messageSize, RingBufferBlockProcessor processor) {
        return processor.process(entries, index(current), messageSize, bufferSize);
    }

    public int write(long current, byte [] src, int srcPos, int length) {
        final int index = index(current);
        final int wrappedSize = index + length - bufferSize;
        if (wrappedSize <= 0) {
            System.arraycopy(src, srcPos, entries, index, length);
        } else {
            assert wrappedSize < length;
            final int numberOfBytesToWrite = length - wrappedSize;
            System.arraycopy(src, srcPos, entries, index, numberOfBytesToWrite);
            System.arraycopy(src, srcPos + numberOfBytesToWrite, entries, 0, wrappedSize);
        }
        return length;
    }

//    /**
//     * Read data into the given byte array starting from given offset up to
//     *
//     * @param offset offset in the byte array
//     * @param length number of bytes to read
//     * @return number of bytes actually written
//     */
//    public int callProducer(ByteProducer byteProducer, int offset, int length) {
//        if (offset + length <= bufferSize) {
//            return byteProducer.write(entries, offset, length);
//        } else {
//            int wrappedSize = offset + length - bufferSize;
//            assert wrappedSize > 0;
//            assert wrappedSize < length;
//            final int numberOfBytesToWrite = length - wrappedSize;
//            int result = byteProducer.write(entries, offset, numberOfBytesToWrite);
//            if (result == numberOfBytesToWrite)
//                result += byteProducer.write(entries, 0, wrappedSize);
//            return result;
//        }
//    }

    ByteRingReader createByteRingReader() {
        return new ByteRingReader(entries);
    }

    public int getCapacity() {
        return bufferSize;
    }

}
