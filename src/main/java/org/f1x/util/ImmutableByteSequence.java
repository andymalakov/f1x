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
* Byte sequence to CharSequence adapter (ASCII characters only)
*/
public class ImmutableByteSequence implements CharSequence {
    private final byte [] buffer;

    public ImmutableByteSequence(byte[] buffer, int offset, int length) {
        this.buffer = new byte [length];
        System.arraycopy(buffer, offset, this.buffer, 0, length);
    }

    public ImmutableByteSequence(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    @Override
    public int length() {
        return buffer.length;
    }

    @Override
    public char charAt(int index) {
        return (char) buffer[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new ImmutableByteSubSequence(buffer, start, end-start);
    }

    @Override
    public boolean equals (Object obj) {
        if (obj instanceof  CharSequence) {
            CharSequence other = (CharSequence)obj;

            final int length = this.length();
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
        return ImmutableByteSubSequence.hashCode(buffer, 0, buffer.length);
    }

    @Override
    public final String toString () {
        return new StringBuilder (this).toString ();
    }

    static class ImmutableByteSubSequence implements CharSequence {
        private final byte [] buffer;
        private final int offset;
        private final int length;

        ImmutableByteSubSequence(byte [] buffer, int offset, int length) {
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
            return (char) buffer[index+offset];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ImmutableByteSubSequence(buffer, offset+start, end-start);
        }

        @Override
        public boolean equals (Object obj) {
            if (obj instanceof  CharSequence) {
                CharSequence other = (CharSequence)obj;

                final int length = this.length();
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
            return hashCode(buffer, offset, length);
        }

        static int hashCode (byte [] array, int offset, int length) {
            int result = 0;
            for (int i=0; i < length; i++)
                result = 31 * result + array[offset+i];
            return result;
        }


        @Override
        public final String toString () {
            return new StringBuilder (this).toString();
        }
    }
}
