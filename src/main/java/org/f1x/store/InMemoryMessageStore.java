package org.f1x.store;

/**
 * Message store backed by byte[] array. Limited to N last bytes.
 */
public final class InMemoryMessageStore implements MessageStore {
    private static final int SIZE_OF_INT = 4;

    private final CircularBuffer buffer;
    private int lastSeqNum;

    //TODO: Initialize from message log
    //TODO: Don't bother storing admin messages?

    public InMemoryMessageStore(int bufferSize) {
        buffer = new CircularBuffer (bufferSize);
    }


    @Override
    public void clean() {
        synchronized (buffer) {
            buffer.clear();
            lastSeqNum = 0;
        }
    }

    @Override
    public void put(int seqNum, byte[] message, int offset, int length) {
        synchronized (buffer) {
            if (seqNum <= lastSeqNum)
                throw new IllegalStateException("Attempt to store message #" + seqNum + " which is less than expected " + (lastSeqNum+1));
            lastSeqNum = seqNum;
            // Store message using [LEN][Message][SEQ][LEN] format
            buffer.writeInt(length);
            buffer.writeByteArray(message, offset, length);
            buffer.writeInt(seqNum);
            buffer.writeInt(length);
        }
    }

    @Override
    public int get(int targetSeqNum, byte[] result) {
        synchronized (buffer) {
            if (targetSeqNum <= lastSeqNum) {
                long position = buffer.tail - 2*SIZE_OF_INT;
                final long limit = Math.max(0, buffer.tail - buffer.bufferSize);
                while (position > limit) {
                    // Reading [LEN][Message][SEQ][LEN] backwards
                    int seqNum = buffer.readInt(position);
                    int msgLen = buffer.readInt(position + SIZE_OF_INT);
                    position -= msgLen;
                    if (position > limit) {

                        if (seqNum == targetSeqNum) { // found it!
                            buffer.readByteArray(position, result, 0, Math.min(msgLen, result.length));
                            result[msgLen] = 0;
                            return seqNum;
                        } if (seqNum < targetSeqNum) {
                            break;
                        }
                        position -= 3*SIZE_OF_INT;
                    }
                }
            }
        }
        return -1; // not found
    }

    private class MessageStoreIter implements MessageStoreIterator {
        private int nextSeqNum;
        private final int toSeqNum;

        private MessageStoreIter(int fromSeqNum, int toSeqNum) {
            if (nextSeqNum > toSeqNum)
                throw new IllegalArgumentException();
            this.nextSeqNum = fromSeqNum;
            this.toSeqNum = toSeqNum;
        }

        @Override
        public int next(byte[] result) {

            if (nextSeqNum > toSeqNum)
                return -1;

            synchronized (buffer) {
                if (nextSeqNum > lastSeqNum)
                    return -1;

                long position = buffer.tail - 2*SIZE_OF_INT;
                final long limit = Math.max(0, buffer.tail - buffer.bufferSize);
                int lastScannedSeqNum = -1;
                while (position > limit) {
                    // reading [LEN][Message][SEQ][LEN] backwards
                    int seqNum = buffer.readInt(position);
                    int msgLen = buffer.readInt(position + SIZE_OF_INT);
                    position -= msgLen;
                    if (position > limit) { // do we have full message?
                        if (seqNum == nextSeqNum) { // found it!
                            buffer.readByteArray(position, result, 0, Math.min(msgLen, result.length));
                            result[msgLen] = 0;
                            nextSeqNum ++;
                            return seqNum;
                        } if (seqNum < nextSeqNum) { //overshot
                            break;
                        }
                        lastScannedSeqNum = seqNum;
                        position -= 3*SIZE_OF_INT;
                    }
                }

                if (lastScannedSeqNum > 0 && lastScannedSeqNum <= toSeqNum) {
                    nextSeqNum = lastScannedSeqNum;
                    return next(result);
                }

            }
            return -1; // not found
        }
    }

    @Override
    public MessageStoreIterator iterator(int fromSeqNum, int toSeqNum) {
        return new MessageStoreIter (fromSeqNum, toSeqNum);
    }


    /// Implementation

    private static final class CircularBuffer {
        private final int bufferSize;
        private final byte [] entries;
        private final int indexMask;

        private long tail = 0;

        CircularBuffer(int bufferSize) {
            if (bufferSize < 1)
                throw new IllegalArgumentException("bufferSize must not be less than 1");
            if (Integer.bitCount(bufferSize) != 1)
                throw new IllegalArgumentException("bufferSize must be a power of 2");

            this.bufferSize = bufferSize;
            this.entries = new byte[bufferSize];
            this.indexMask = bufferSize - 1;
        }

        /** Translates sequence number to ring buffer offset */
        private int index(long sequence) {
            return (int) sequence & indexMask;
        }

        /** Writes single byte specified by value parameter and advances offset */
        private int writeByte (int offset, int value) {
            assert offset >=0 && offset < bufferSize;
            entries[offset++] = (byte) (0xFF & value);
            if (offset == bufferSize)
                offset = 0;
            return offset;
        }

        /** Store given INT32 number in Big-Endian notation (Used to store message length) */
        void writeInt (int value) {
            int offset = index(tail);

            offset = writeByte (offset, value >>> 24);
            offset = writeByte (offset, value >>> 16);
            offset = writeByte (offset, value >>> 8);
            writeByte (offset, value);

            tail += 4;
        }

        void writeByteArray(byte [] src, int srcPos, int length) {
            final int index = index(tail);
            final int wrappedSize = index + length - bufferSize;
            if (wrappedSize <= 0) {
                System.arraycopy(src, srcPos, entries, index, length);
            } else {
                assert wrappedSize < length;
                final int numberOfBytesToWrite = length - wrappedSize;
                System.arraycopy(src, srcPos, entries, index, numberOfBytesToWrite);
                System.arraycopy(src, srcPos + numberOfBytesToWrite, entries, 0, wrappedSize);
            }
            tail += length;
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

        void readByteArray (long sequence, byte [] dst, int dstPos, int length) {
            assert sequence >= 0;
            int index = index(sequence);

            final int wrappedSize = index + length - bufferSize;
            if (wrappedSize <= 0) {
                System.arraycopy(entries, index, dst, dstPos, length);
            } else {
                assert wrappedSize < length;
                final int numberOfBytesToWrite = length - wrappedSize;
                System.arraycopy(entries, index, dst, dstPos, numberOfBytesToWrite);
                System.arraycopy(entries, 0, dst, dstPos + numberOfBytesToWrite, wrappedSize);
            }
        }

        void clear() {
            tail = 0;
        }


    }

    public String dump () {
        StringBuilder sb = new StringBuilder(1024);

        long position = buffer.tail - 2*SIZE_OF_INT;
        final long limit = Math.max(0, buffer.tail - buffer.bufferSize);
        while (position > limit) {
            int seqNum = buffer.readInt(position);
            int msgLen = buffer.readInt(position + SIZE_OF_INT);

            position -= msgLen;
            if (position > limit) {

                byte [] message = new byte [msgLen];
                buffer.readByteArray(position, message, 0, msgLen);

                sb.append("[SEQ:");
                sb.append(seqNum);
                sb.append(" MSG:");
                sb.append(new String (message));
                sb.append("] ");

                position -= 3*SIZE_OF_INT;
            }
        }

        return sb.toString();
    }

}
