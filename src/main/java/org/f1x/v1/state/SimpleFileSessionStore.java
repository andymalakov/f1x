package org.f1x.v1.state;

import org.f1x.api.session.SessionID;
import org.f1x.util.TimeSource;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Session Store backed by file (updated periodically or at the end of each connection)
 */
public class SimpleFileSessionStore extends MemorySessionState {

    private static final GFLog LOGGER = GFLogFactory.getLog(SimpleFileSessionStore.class);

    private final RandomAccessFile raf;
    private final Flusher flusher;

    public SimpleFileSessionStore (File file) {
        this(file, null, 0);
    }

    /**
     * @param file file to hold session state
     * @flushPeriod How often session should flush the changes (in milliseconds). Zero or negative value disables periodic flush (in which case state is flushed on disconnect).
     */
    public SimpleFileSessionStore (File file, TimeSource timeSource, int flushPeriod) {
        final boolean existingFile = file.exists();
        try {
            raf = new RandomAccessFile(file, "rw");
            if (existingFile) {
                long lastConnTimestamp = raf.readLong();
                int senderCompId = raf.readInt();
                int targetCompId = raf.readInt();
                if (senderCompId < 1 || targetCompId < 1)
                    throw new RuntimeException("Invalid session state loaded");

                boolean isComplete = raf.readBoolean();
                if ( ! isComplete)
                    throw new IllegalStateException("Session State was lost (sequence reset is required)");

                setLastConnectionTimestamp(lastConnTimestamp);
                setNextSenderSeqNum(senderCompId);
                setNextTargetSeqNum(targetCompId);

            }
            store(false); // Mark as incomplete
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        flusher = (flushPeriod > 0) ? new Flusher(file, timeSource, flushPeriod) : null;
    }

    private void store (boolean isComplete) throws IOException {
        raf.seek(0);
        raf.writeLong(getLastConnectionTimestamp());
        raf.writeInt(getNextSenderSeqNum());
        raf.writeInt(getNextTargetSeqNum());
        raf.writeBoolean(isComplete);
        raf.getFD().sync();
    }

    @Override
    public void flush() {
        try {
            store(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    @Override //TODO
    public void close() {
        flusher.interrupt();
        flush();
    }

    protected class Flusher extends Thread {   //TODO: Replace by alloc-free version of ScheduledExecutorService ?
        private final TimeSource timeSource;
        protected final int flushPeriod;

        protected Flusher(File file, TimeSource timeSource, int flushPeriod) {
            super("State flusher for " + file.getName());
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY - 1);
            this.timeSource = timeSource;
            this.flushPeriod = flushPeriod;
        }

        @Override
        public void run () {
            while (true) {
                try {
                    timeSource.sleep(flushPeriod);
                    store (false);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    LOGGER.error().append("Error writing FIX log").append(e).commit();
                }
            }

        }

    }
}
