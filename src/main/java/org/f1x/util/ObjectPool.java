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
 * Fixed-size reference objectPool. Thread safe.
 *
 * @param <E> type of elements
 */
public final class ObjectPool<E> {

    private final Object[] objects;

    private int count;

    public ObjectPool(int capacity, ObjectFactory<E> objectFactory) {
        if (capacity < 0)
            throw new IllegalArgumentException("capacity < 0");

        this.objects = new Object[capacity];
        fill(capacity, objectFactory);
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

    public void release(E object) {
        if (object == null)
            throw new NullPointerException("object == null");

        synchronized (this) {
            if (isFull())
                throw new IllegalStateException("No free space");
            objects[count++] = object;
        }
    }

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
        return count == 0;
    }

    public int capacity() {
        return objects.length;
    }

    public synchronized int size() {
        return count;
    }

    private boolean isFull() {
        return count == objects.length;
    }

}
