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
package org.f1x.api.message;

/** Extends standard java.lang.Appendable with with a few additional methods. In addition standard methods do not throw java.io.IOException.  */
public interface AppendableValue extends Appendable {

    /**
     * Appends the specified character sequence to this <tt>AppendableValue</tt>.
     *
     * NOTE: CharSequence may contain ASCII characters only.
     *
     * <p> Depending on which class implements the character sequence
     * <tt>csq</tt>, the entire sequence may not be appended.  For
     * instance, if <tt>csq</tt> is a {@link java.nio.CharBuffer} then
     * the subsequence to append is defined by the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If <tt>csq</tt> is
     *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
     *         appended to this AppendableValue.
     *
     * @return  A reference to this <tt>AppendableValue</tt>
     */
    @Override
    AppendableValue append(CharSequence csq);

    /**
     * Appends a subsequence of the specified character sequence to this  <tt>AppendableValue</tt>.
     *
     * NOTE: CharSequence may contain ASCII characters only.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq, start,
     * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
     * exactly the same way as the invocation
     *
     *
     * <pre>
     *     out.append(csq.subSequence(start, end)) </pre>
     *
     * @param  csq The character sequence from which a subsequence will be
     *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
     *         will be appended as if <tt>csq</tt> contained the four
     *         characters <tt>"null"</tt>.
     *
     * @param  start The index of the first character in the subsequence
     *
     * @param  end The index of the character following the last character in the subsequence
     *
     * @return  A reference to this <tt>AppendableValue</tt>
     */
    @Override
    AppendableValue append(CharSequence csq, int start, int end);

    /**
     * Appends the specified ASCII character to this <tt>AppendableValue</tt>.
     *
     */
    @Override
    AppendableValue append(char c);

    AppendableValue append(byte c);
    AppendableValue append(int value);
    AppendableValue append(long value);
    AppendableValue append(double value);

    /** Appends FIX tag separator (ASCII SOH character). */
    void end();
}
