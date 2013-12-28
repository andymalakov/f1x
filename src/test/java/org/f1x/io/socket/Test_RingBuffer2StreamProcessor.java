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

package org.f1x.io.socket;


import static org.junit.Assert.*;
import org.junit.Test;
import org.f1x.io.disruptor.TestExceptionHandler;

import java.io.ByteArrayOutputStream;

public class Test_RingBuffer2StreamProcessor {

    private static final byte [] BYTES = "abcdefghijklmnopqrstuvwxyz".getBytes();

    @Test
    public void test() {
        assertEquals("abc", p(0,3));
        assertEquals("abcdefghijklmnopqrstuvwxyz", p(0, BYTES.length));
        assertEquals("abcdefghijklmnopqrstuvwxy", p(0, BYTES.length-1));
        assertEquals("bcdefghijklmnopqrstuvwxyz", p(1, BYTES.length-1));
        assertEquals("bcdefghijklmnopqrstuvwxyza", p(1, BYTES.length));
        assertEquals("cdefghijklmnopqrstuvwxyzab", p(2, BYTES.length));
        assertEquals("zabcdefghijklmnopqrstuvwxy", p(BYTES.length-1, BYTES.length));
    }

    @Test
    public void testBadCase() {
        try {
            assertEquals("you shall not pass", p(BYTES.length, BYTES.length));
        } catch(AssertionError e) {
            // expected
        }
    }

    private static String p (int offset, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        RingBuffer2StreamProcessor processor = new RingBuffer2StreamProcessor(baos, new TestExceptionHandler());

        processor.process(BYTES, offset, length, BYTES.length);
        return new String (baos.toByteArray());
    }
}
