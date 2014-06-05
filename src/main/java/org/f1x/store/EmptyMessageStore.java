package org.f1x.store;

public final class EmptyMessageStore implements MessageStore {

    private static final MessageStore INSTANCE = new EmptyMessageStore();

    private EmptyMessageStore() {
    }

    public static MessageStore getInstance() {
        return INSTANCE;
    }

    @Override
    public void put(int seqNum, byte[] message, int offset, int length) {
       // empty
    }

    @Override
    public void clean() {
       // empty
    }

    @Override
    public int get(int seqNum, byte[] buffer) {
        return -1;
    }

    @Override
    public MessageStoreIterator iterator(int fromSeqNum, int toSeqNum) {
        return EmptyMessageStoreIterator.getInstance();
    }


    private static final class EmptyMessageStoreIterator implements MessageStoreIterator {

        private static final MessageStoreIterator INSTANCE = new EmptyMessageStoreIterator();

        private EmptyMessageStoreIterator() {
        }

        @Override
        public int next(byte[] buffer) {
            return -1;
        }

        public static MessageStoreIterator getInstance() {
            return INSTANCE;
        }

    }


}
