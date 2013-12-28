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

public final class Stack<E> {

    private final Object [] elements;
    private int count;

    public Stack (int maxSize) {
        elements = new Object [maxSize];
    }

    public void add (E e) {
        if (isFull())
            throw new IllegalStateException();
        elements[count++] = e;
    }

    @SuppressWarnings("unchecked")
    public E remove () {
        if (isEmpty())
            return null;
        return (E) elements [--count];
    }

    public boolean isEmpty () {
        return count == 0;
    }

    private boolean isFull () {
        return count == elements.length;
    }
}