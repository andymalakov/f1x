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
package org.f1x.util;

/**
 * Fixed-size object pool. Thread safe.
 *
 * @param <E> type of objects in the pool.
 */
public final class ObjectPool<E> {

    private final Object[] objects;

    private int count;

    public ObjectPool(int maxSize, ObjectFactory<E> objectFactory) {
        if (maxSize < 0)
            throw new IllegalArgumentException("maxSize < 0");

        this.objects = new Object[maxSize];
        fill(maxSize, objectFactory);
    }

    private void fill(int capacity, ObjectFactory<E> objectFactory) {
        count = capacity;
        for (int index = 0; index < capacity; index++) {
            E object = objectFactory.create();
            if (object == null)
                throw new NullPointerException("Object factory created null");

            objects[index] = object;
        }
    }

    /** Recycles object previously borrowed using {@link #borrow()} */
    public void release(E object) {
        if (object == null)
            throw new NullPointerException("object == null");

        synchronized (this) {
            if (isFull())
                throw new IllegalStateException("No free space");
            objects[count++] = object;
        }
    }

    /**
     * @return object borrowed from pool, or <code>null</code> if pool is empty.
     * Caller must return borrowed object after use using {@link ObjectPool#release(Object)}.
     */

    @SuppressWarnings("unchecked")
    public synchronized E borrow() {
        if (isEmpty())
            return null;
        E element = (E) objects[--count];
        objects[count] = null;
        return element;
    }

    public synchronized E[] toArray(E[] to) {
        if (to.length != size())
            throw new IllegalArgumentException("length of array is not equal to object pool size");

        System.arraycopy(objects, 0, to, 0, size());
        return to;
    }


    private boolean isEmpty() {
        assert Thread.holdsLock(this);
        return count == 0;
    }

    public int capacity() {
        return objects.length;
    }

    public synchronized int size() {
        return count;
    }

    private boolean isFull() {
        assert Thread.holdsLock(this);
        return count == objects.length;
    }

}
