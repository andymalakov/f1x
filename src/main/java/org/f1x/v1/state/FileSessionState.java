package org.f1x.v1.state;

import org.f1x.v1.InvalidFixMessageException;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Thread safe.
 * Offset       Description
 * 0            last logon timestamp
 * 17           next sender seq num
 * 26           next target seq num
 */
public class FileSessionState extends AbstractSessionState {

    protected static final GFLog LOGGER = GFLogFactory.getLog(FileSessionState.class);

    protected static final int SIZE_OF_BYTE = 1;
    protected static final int SIZE_OF_INT = 4;
    protected static final int SIZE_OF_LONG = 8;

    protected static final int NUM_OF_STORAGE_BYTES = LongTransaction.getSize() + IntTransaction.getSize() + IntTransaction.getSize();
    protected static final int LAST_LOGON_TIMESTAMP_OFFSET = 0;
    protected static final int NEXT_SENDER_SEQ_NUM_OFFSET = LongTransaction.getSize();
    protected static final int NEXT_TARGET_SEQ_NUM_OFFSET = NEXT_SENDER_SEQ_NUM_OFFSET + IntTransaction.getSize();

    protected final LongTransaction lastLogonTimestamp;
    protected final IntTransaction nextSenderSeqNum;
    protected final IntTransaction nextTargetSeqNum;

    final MappedByteBuffer buffer; // only for tests

    public FileSessionState(String filePath) throws IOException {
        this(Paths.get(filePath));
    }

    public FileSessionState(File file) throws IOException {
        this(file.toPath());
    }

    public FileSessionState(Path file) throws IOException {
        boolean justCreated = check(file);

        this.buffer = map(file, justCreated);
        this.lastLogonTimestamp = new LongTransaction(buffer, LAST_LOGON_TIMESTAMP_OFFSET);
        this.nextSenderSeqNum = new IntTransaction(buffer, NEXT_SENDER_SEQ_NUM_OFFSET);
        this.nextTargetSeqNum = new IntTransaction(buffer, NEXT_TARGET_SEQ_NUM_OFFSET);

        if (justCreated)
            setDefaults();
    }

    @Override
    public final void setLastConnectionTimestamp(long newValue) {
        synchronized (lastLogonTimestamp) {
            lastLogonTimestamp.write(newValue);
        }
    }

    @Override
    public long getLastConnectionTimestamp() {
        synchronized (lastLogonTimestamp) {
            return lastLogonTimestamp.read();
        }
    }

    @Override
    public final void setNextSenderSeqNum(int newValue) {
        synchronized (nextSenderSeqNum) {
            nextSenderSeqNum.write(newValue);
        }
    }

    @Override
    public int getNextSenderSeqNum() {
        synchronized (nextSenderSeqNum) {
            return nextSenderSeqNum.read();
        }
    }

    @Override
    public int consumeNextSenderSeqNum() {
        synchronized (nextSenderSeqNum) {
            int currentValue = nextSenderSeqNum.read();
            nextSenderSeqNum.write(currentValue + 1);
            return currentValue;
        }
    }

    @Override
    public final void setNextTargetSeqNum(int newValue) {
        synchronized (nextTargetSeqNum) {
            nextTargetSeqNum.write(newValue);
        }
    }

    @Override
    public int getNextTargetSeqNum() {
        synchronized (nextTargetSeqNum) {
            return nextTargetSeqNum.read();
        }
    }

    @Override
    public int consumeNextTargetSeqNum() {
        synchronized (nextTargetSeqNum) {
            int currentValue = nextTargetSeqNum.read();
            nextTargetSeqNum.write(currentValue + 1);
            return currentValue;
        }
    }

    @Override
    public void resetNextTargetSeqNum(int newValue) throws InvalidFixMessageException {
        synchronized (nextTargetSeqNum) {
            int currentValue = nextTargetSeqNum.read();
            if (newValue <= currentValue)
                throw InvalidFixMessageException.RESET_BELOW_CURRENT_SEQ_LARGE;

            nextTargetSeqNum.write(newValue);
        }
    }

    protected final void setDefaults() {
        setLastConnectionTimestamp(-1);
        setNextSenderSeqNum(1);
        setNextTargetSeqNum(1);
    }

    protected static void setToZero(MappedByteBuffer buffer) {
        for (int index = 0; index < buffer.capacity(); index++)
            buffer.put(index, (byte) 0);
    }

    /**
     * Checks the file path. Creates the file and parent directories if needed.
     * @return true if the file has just been created otherwise false
     */
    protected static boolean check(Path file) throws IOException {
        boolean exists = Files.exists(file);
        if (exists) {
            if (!Files.isRegularFile(file))
                throw new FileNotFoundException(file + " is not file");

            // TODO: check content
        } else {
            Path dir = file.getParent();
            if (dir != null && !Files.exists(dir))
                createDirectories(file, dir);

            createFile(file);
        }

        return !exists;
    }

    protected static void createFile(Path file) throws IOException {
        try {
            Files.createFile(file);
        } catch (IOException e) {
            LOGGER.error().append("During creating file: ").append(file).append(" error occurred: ").append(e).commit();
            throw e;
        }
    }

    protected static void createDirectories(Path file, Path dir) throws IOException {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error().append("During creating parent directories for file: ").append(file).append(" error occurred: ").append(e).commit();
            throw e;
        }
    }

    protected static MappedByteBuffer map(Path file, boolean setToZero) throws IOException {
        try {
            FileChannel channel = null;
            try {
                channel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
                MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, NUM_OF_STORAGE_BYTES);
                if (setToZero)
                    setToZero(buffer);

                return buffer;
            } finally {
                if (channel != null)
                    channel.close();
            }
        } catch (Throwable e) {
            LOGGER.error().append("During opening file: ").append(file).append(" error occurred: ").append(e).commit();
            throw e;
        }
    }

    /**
     * Definitions:
     * 1. readCell - cell from which to read (with 0 or 1 index)
     * 2. writeCell - cell to which to write (with 0 or 1 index)
     * 3. index - refers to read cell (0 or 1)
     * <p/>
     * Transactions:
     * 1. read transaction:
     * a) read index
     * b) read from readCell
     * 2. write transaction:
     * a) read index
     * b) write to writeCell
     * c) modify index
     * <p/>
     * Buffer:
     * 1. cell 0 (read or write cell)
     * 2. cell 1 (read or write cell)
     * 3. index (0 or 1)
     */
    protected static abstract class Transaction {

        protected final MappedByteBuffer buffer;
        protected final int[] cellIndexes;
        protected final int indexIndex;

        protected Transaction(MappedByteBuffer buffer, int cell0Index, int cell1Index, int indexIndex) {
            this.buffer = buffer;
            this.cellIndexes = new int[]{cell0Index, cell1Index};
            this.indexIndex = indexIndex;
            readIndex(); // check file format
        }

        protected final byte readIndex() {
            byte index = buffer.get(indexIndex);
            if (index != 0 && index != 1)
                throw new IllegalArgumentException("Invalid file format, index value must be 0 or 1 and not: " + index);

            return index;
        }

        protected void writeIndex(byte index) {
            buffer.put(indexIndex, index);
        }

    }

    protected static class IntTransaction extends Transaction {

        protected static final int SIZE = SIZE_OF_INT + SIZE_OF_INT + SIZE_OF_BYTE;

        protected IntTransaction(MappedByteBuffer buffer, int offset) {
            super(buffer, offset, offset + SIZE_OF_INT, offset + 2 * SIZE_OF_INT);
        }

        protected int read() {
            int indexToReadCell = readIndex();
            int readCellIndex = cellIndexes[indexToReadCell];
            return buffer.getInt(readCellIndex);
        }

        protected void write(int value) {
            byte indexToReadCell = readIndex();
            byte indexToWriteCell = (byte) (indexToReadCell == 0 ? 1 : 0);
            int writeCellIndex = cellIndexes[indexToWriteCell];
            buffer.putInt(writeCellIndex, value);
            writeIndex(indexToWriteCell);
        }

        protected static int getSize() {
            return SIZE;
        }

    }

    protected static class LongTransaction extends Transaction {

        protected static final int SIZE = SIZE_OF_LONG + SIZE_OF_LONG + SIZE_OF_BYTE;

        protected LongTransaction(MappedByteBuffer buffer, int offset) {
            super(buffer, offset, offset + SIZE_OF_LONG, offset + 2 * SIZE_OF_LONG);
        }

        protected long read() {
            int indexToReadCell = readIndex();
            int readCellIndex = cellIndexes[indexToReadCell];
            return buffer.getLong(readCellIndex);
        }

        protected void write(long value) {
            byte indexToReadCell = readIndex();
            byte indexToWriteCell = (byte) (1 - indexToReadCell);
            int writeCellIndex = cellIndexes[indexToWriteCell];
            buffer.putLong(writeCellIndex, value);
            writeIndex(indexToWriteCell);
        }

        protected static int getSize() {
            return SIZE;
        }

    }

}
