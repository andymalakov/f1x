package org.f1x.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Test_ObjectPool {

    private ObjectPool<Integer> objectPool;
    private final ObjectFactory<Integer> integerFactory = new ObjectFactory<Integer>() {

        int number = 1;

        @Override
        public Integer create() {
            return number++;
        }

    };

    @Before
    public void setup() {
        objectPool = new ObjectPool<>(64, integerFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeAndCapacity() {
        int expectedCapacity = 100;

        objectPool = new ObjectPool<>(expectedCapacity, integerFactory);
        Assert.assertEquals(expectedCapacity, objectPool.capacity());
        Assert.assertEquals(expectedCapacity, objectPool.size());

        expectedCapacity = -1;
        objectPool = new ObjectPool<>(expectedCapacity, integerFactory);
    }

    @Test
    public void testElementStorage() {
        Assert.assertEquals(objectPool.capacity(), objectPool.size());

        for (int expectedSize = objectPool.capacity(); expectedSize > 0; expectedSize--) {
            Assert.assertEquals(expectedSize, objectPool.size());
            Assert.assertTrue(objectPool.borrow() != null);
        }

        Assert.assertEquals(0, objectPool.size());
        Assert.assertTrue(objectPool.borrow() == null);

        for (int expectedSize = 1; expectedSize <= objectPool.capacity(); expectedSize++) {
            objectPool.release(0);
            Assert.assertEquals(expectedSize, objectPool.size());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testFull() {
        objectPool.release(0);
    }

    @Test
    public void testToArray() {
        Integer[] array;
        array = objectPool.toArray(new Integer[objectPool.capacity()]);
        for (Integer num : array)
            Assert.assertTrue(num != null);

        while (objectPool.borrow() != null) ;

        objectPool.toArray(new Integer[0]);

        try {
            objectPool.toArray(new Integer[1]);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // skip
        }
    }

}
