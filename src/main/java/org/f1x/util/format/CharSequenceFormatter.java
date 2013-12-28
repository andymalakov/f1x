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

package org.f1x.util.format;

public final class CharSequenceFormatter {

    public static int format(CharSequence source, byte[] buffer, int offset) {
        for (int i=0; i < source.length(); i++) {
            int b = source.charAt(i);
            if ((b & 0xFFFFFF00) != 0)
                throw new IllegalArgumentException("ASCII only");
            buffer[offset++] = (byte)b;
        }
        return offset;
    }

    /**
     * @param  srcStart The index of the first character in the subsequence
     * @param  srcEnd The index of the character following the last character in the subsequence
     */
    public static int format(CharSequence source, int srcStart, int srcEnd, byte[] buffer, int offset) {
        for (int i=srcStart; i < srcEnd; i++) {
            int b = source.charAt(i);
            if ((b & 0xFFFFFF00) != 0)
                throw new IllegalArgumentException("ASCII only");
            buffer[offset++] = (byte)b;
        }
        return offset;
    }
}
