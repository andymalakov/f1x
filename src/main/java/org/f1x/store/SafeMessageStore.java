package org.f1x.store;

import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

public final class SafeMessageStore implements MessageStore {

    private static final GFLog LOGGER = GFLogFactory.getLog(SafeMessageStore.class);

    private MessageStore delegate;

    public SafeMessageStore(MessageStore delegate) {
        if (delegate == null)
            throw new NullPointerException("delegate is null");

        this.delegate = delegate;
    }

    @Override
    public void put(int seqNum, byte[] message, int offset, int length) {
        try {
            delegate.put(seqNum, message, offset, length);
        } catch (Throwable e) {
            LOGGER.warn().append("Error putting message in message store: ").append(e).commit();
        }
    }

    @Override
    public void clean() {
        try {
            delegate.clean();
        } catch (Throwable e) {
            LOGGER.warn().append("Error cleaning message store: ").append(e).commit();
        }
    }

    @Override
    public int get(int seqNum, byte[] buffer) {
        try {
            return delegate.get(seqNum, buffer);
        } catch (Throwable e) {
            LOGGER.warn().append("Error getting message from message store: ").append(e).commit();
            return -1;
        }
    }

    @Override
    public MessageStoreIterator iterator(int fromSeqNum, int toSeqNum) {
        try {
            return new SafeMessageStoreIterator(delegate.iterator(fromSeqNum, toSeqNum));
        } catch (Throwable e) {
            LOGGER.warn().append("Error getting message store iterator: ").append(e).commit();
            return EmptyMessageStore.EmptyMessageStoreIterator.getInstance();
        }
    }

    public static final class SafeMessageStoreIterator implements MessageStoreIterator {

        private final MessageStoreIterator delegate;

        public SafeMessageStoreIterator(MessageStoreIterator delegate) {
            if (delegate == null)
                throw new NullPointerException("delegate is null");

            this.delegate = delegate;
        }

        @Override
        public int next(byte[] buffer) {
            try {
                return delegate.next(buffer);
            } catch (Throwable e) {
                LOGGER.warn().append("Error getting message from message store by iterator: ").append(e).commit();
                return -1;
            }
        }

    }

}
