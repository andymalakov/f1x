package org.f1x.store;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Test_InMemoryMessageStore {

    private static final int STORE_SIZE = 1 << 6;

    private InMemoryMessageStore store;

    @Before
    public void init() {
        store = new InMemoryMessageStore (STORE_SIZE);
    }

    @Test
    public void simple() {
        put(1, "MSG1");
        put(2, "MSG2");
        put(3, "MSG3");

        //System.out.println(store.dump());

        assertNotFound(4);
        assertGet(3, "MSG3");
        assertGet(3, "MSG3");
        assertGet(1, "MSG1");
        assertGet(2, "MSG2");
        assertNotFound(0);
    }

    @Test
    public void testEmpty() {
        assertNotFound(123);
        put(123, "MSG");
        assertNotFound(122);
        assertGet(123, "MSG");
        assertNotFound(124);
        store.clean();
        assertNotFound(123);
    }

    @Test
    public void reset() {
        put(1, "MSG1");
        put(2, "MSG2");
        put(3, "MSG3");

        store.clean();

        assertNotFound(1);
        assertNotFound(2);
        assertNotFound(3);

        put(1, "MSG1");
        put(2, "MSG2");
        put(3, "MSG3");

        assertGet(3, "MSG3");
        assertGet(1, "MSG1");
        assertGet(2, "MSG2");
    }

    @Test
    public void wrappedStore() {
        final String FORMAT = "MSG%07d";

        final int msgSize = String.format(FORMAT, 0).length();
        final int n = 3*STORE_SIZE / (2*msgSize);

        for (int i=1; i < n; i++) {
            String msg = String.format(FORMAT, i);
            put(i, msg);
        }
        System.out.println(store.dump());
        assertNotFound(n);
        assertGet(n-1, String.format(FORMAT, n-1));
        assertGet(n-2, String.format(FORMAT, n-2));
        assertGet(n-3, String.format(FORMAT, n-3));
        assertNotFound(n-4);
    }

    @Test
    public void testIterator1 () {
        put(1, "MSG1");
        put(2, "MESSAGE2");
        put(3, "MSG3");

        assertIterator (1, 3, "MSG1,MESSAGE2,MSG3");
        assertIterator (1, 1, "MSG1");
        assertIterator (2, 2, "MESSAGE2");
        assertIterator (3, 3, "MSG3");

        assertIterator (1, 2, "MSG1,MESSAGE2");
        assertIterator (2, 3, "MESSAGE2,MSG3");
    }


    @Test
    public void testIterator2 () {
        put(10, "1");
        put(20, "2");
        put(30, "#3");
        put(40, "4");
        put(50, "5");

        System.out.println(store.dump());

        assertIterator (10, 50, "1,2,#3,4,5");
        assertIterator (1, 100, "1,2,#3,4,5");

        assertIterator (20, 40, "2,#3,4");
        assertIterator (30, 30, "#3");
        assertIterator (40, 40, "4");
        assertIterator (39, 41, "4");

    }

    /** Same as before but due to small buffer size we keep only last four  messages (M2, M3, M4, M5) */
    @Test
    public void testIteratorWithOverflow2 () {
        put(10, "M1");
        put(20, "M2");
        put(30, "MSG3");
        put(40, "M4");
        put(50, "M5");

        System.out.println(store.dump()); // MSG1 is overridden by MSG5

        assertIterator (10, 50, "M2,MSG3,M4,M5");
        assertIterator (1, 100, "M2,MSG3,M4,M5");

        assertIterator (20, 40, "M2,MSG3,M4");
        assertIterator (30, 30, "MSG3");
        assertIterator (40, 40, "M4");
        assertIterator (39, 41, "M4");

    }

    /** Same as before but due to small buffer size we keep only last three messages (M3, M4, M5) */
    @Test
    public void testIteratorWithOverflow3 () {
        put(10, "MSG1");
        put(20, "MSG2");
        put(30, "MESSAGE3");
        put(40, "MSG4");
        put(50, "MSG5");

        System.out.println(store.dump()); // MSG1 is overridden by MSG5

        assertIterator (10, 50, "MESSAGE3,MSG4,MSG5");
        assertIterator (1, 100, "MESSAGE3,MSG4,MSG5");

        assertIterator (20, 40, "MESSAGE3,MSG4");
        assertIterator (30, 30, "MESSAGE3");
        assertIterator (40, 40, "MSG4");
        assertIterator (39, 41, "MSG4");

    }

    @Test
    public void testEmptyIterators () {
        assertIterator (1, 1, "");
        assertIterator (1, 2, "");

        put(10, "MSG1");
        put(20, "MSG2");
        put(30, "MESSAGE4");
        put(40, "MSG4");
        put(50, "MSG5");

        assertIterator (1, 2, "");
        assertIterator (31, 32, "");
        assertIterator (51, 55, "");
    }

    @Test(expected = IllegalStateException.class)
    public void zeroAsSequenceNum() {
         put(0, "DUMMY");
    }

    @Test(expected = IllegalStateException.class)
    public void negativeSequenceNum() {
        put(-1, "DUMMY");
    }

    @Test(expected = IllegalStateException.class)
    public void outOfOrderSequenceNum() {
        put(2, "DUMMY");
        put(1, "DUMMY");
    }

    @Test(expected = IllegalStateException.class)
    public void outOfOrderSequenceNum1() {
        put(1, "DUMMY");
        put(1, "DUMMY");
    }

    private void put (int seqNum, String content) {
        put(seqNum, content.getBytes());
    }

    private void put (int seqNum, byte [] content) {
        store.put (seqNum, content, 0, content.length);
    }

    private void assertGet(int seqNum, String expectedContent) {
        byte [] buffer = new byte[256];
        if (store.get(seqNum, buffer) <= 0)
            Assert.fail("Can't find message with sequence number " + seqNum);
        String actualContent = new String (buffer, 0, sizeof(buffer));
        Assert.assertEquals(expectedContent, actualContent);
    }

    private void assertNotFound(int seqNum) {
        byte [] buffer = new byte[256];
        if (store.get(seqNum, buffer) > 0)
            Assert.fail("Was not supposed to find message with sequence number " + seqNum + " found: " + new String (buffer, 0, sizeof(buffer)));
    }

    private void assertIterator(int fromSeqNum, int toSeqNum, String expectedContent) {
        MessageStore.MessageStoreIterator iter = store.iterator(fromSeqNum, toSeqNum);
        byte [] buffer = new byte [256];
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (iter.next(buffer) <= 0)
                break;

            if (sb.length() > 0)
                sb.append(",");

            String item = new String (buffer, 0, sizeof(buffer));
            sb.append(item);
        }
        Assert.assertEquals("Range [" + fromSeqNum + ", " + toSeqNum +"]", expectedContent, sb.toString());
    }

    private static int sizeof(byte[] szBytes) {
        int result = 0;
        while(szBytes[result] != 0)
            result++;
        return result;
    }
}


