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

/** Implementation of CharSequence backed by a reference to *external* byte array. Similar to MutableByteSequence but does not copy external array into internal. */
public class ByteArrayReference implements CharSequence {
    private byte [] buffer;
    private int length;
    private int offset;

    public ByteArrayReference() {
    }

    public ByteArrayReference(byte[] buffer, int offset, int length) {
        set(buffer, offset, length);
    }

    public void set(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index >= length)
            throw new IndexOutOfBoundsException();
        return (char) buffer[offset + index];
    }

    public int copyTo (byte [] buffer, int offset) {
        System.arraycopy(this.buffer, this.offset, buffer, offset, this.length);
        return this.length;
    }


    /**
     * Returns a new <code>CharSequence</code> that is a subsequence of this sequence.
     * The subsequence starts with the <code>char</code> value at the specified index and
     * ends with the <code>char</code> value at index <tt>end - 1</tt>.  The length
     * (in <code>char</code>s) of the
     * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt>
     * then an empty sequence is returned. </p>
     *
     * @param   start   the start index, inclusive
     * @param   end     the end index, exclusive
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return new ByteArrayReference(buffer, offset + start, end-start);
    }

    @Override
    public boolean equals (Object obj) {
        if (obj instanceof  CharSequence) {
            CharSequence other = (CharSequence)obj;

            if (length != other.length ())
                return false;

            for (int i = 0; i < length; i++) {
                if (this.charAt (i) != other.charAt (i))
                    return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public int hashCode () {
        return ImmutableByteSequence.ImmutableByteSubSequence.hashCode(buffer, 0, length);
    }

    @Override
    public final String toString () {
        return new StringBuilder (this).toString ();
    }

}
