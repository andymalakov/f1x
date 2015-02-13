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

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Test_ByteSequences {
    @Test
    public void testEmpty () {
        MutableByteSequence bs = new MutableByteSequence(10);

        assertEquals(0, bs.length());
        assertOutOfRange(bs, 0);
        assertOutOfRange(bs, 1);

        assertEquals(0, bs.hashCode());
        assertTrue(bs.equals(""));
        assertFalse(bs.equals("abc"));
        assertEquals("", bs.toString());
    }

    @Test
    public void testSingleByteSequence () {
        MutableByteSequence bs = new MutableByteSequence(10);
        bs.set(new byte [] {0x01, 0x02, 0x03}, 1, 1);

        assertEquals(1, bs.length());
        assertEquals(0x02, bs.charAt(0));
        assertOutOfRange(bs, 1);

        assertEquals(0x02, bs.hashCode());
        assertTrue(bs.equals("\002"));
        assertFalse(bs.equals("abc"));
        assertEquals("\002", bs.toString());


        bs.set(new byte [] {0x01, 0x02, 0x03}, 2, 1);
        assertEquals(0x03, bs.charAt(0));
        assertEquals("\003", bs.toString());



        CharSequence cs = bs.subSequence(0, 1);
        assertEquals(1, bs.length());
        assertEquals(0x03, bs.charAt(0));
        assertOutOfRange(bs, 1);
        assertEquals(bs.hashCode(), cs.hashCode());
        assertTrue(bs.equals(cs));
        assertTrue(cs.equals(bs));
        assertEquals(bs.toString(), cs.toString());
    }

    @Test
    public void testFewBytesSequence () throws UnsupportedEncodingException {
        MutableByteSequence bs = new MutableByteSequence(10);
        bs.set("abc".getBytes("ASCII"), 0, 3);

        assertEquals(3, bs.length());
        assertEquals('a', bs.charAt(0));
        assertEquals('b', bs.charAt(1));
        assertEquals('c', bs.charAt(2));
        assertOutOfRange(bs, 3);

        assertEquals("abc".hashCode(), bs.hashCode());
        assertTrue(bs.equals("abc"));
        assertFalse(bs.equals("ABC"));
        assertEquals("abc", bs.toString());


        CharSequence cs3 = bs.subSequence(0, 3);
        assertEquals(3, cs3.length());
        assertEquals('a', cs3.charAt(0));
        assertOutOfRange(cs3, 3);
        assertEquals(bs.hashCode(), cs3.hashCode());
        assertTrue(bs.equals(cs3));
        assertTrue(cs3.equals(bs));
        assertEquals(bs.toString(), cs3.toString());


        CharSequence cs2 = bs.subSequence(0, 2);
        assertEquals(2, cs2.length());
        assertEquals('a', cs2.charAt(0));
        assertOutOfRange(cs2, 2);
        assertEquals("ab", cs2.toString());

        CharSequence cs1 = bs.subSequence(1, 2);
        assertEquals(1, cs1.length());
        assertEquals('b', cs1.charAt(0));
        assertOutOfRange(cs1, 1);
        assertEquals("b", cs1.toString());
    }

//    @Test
//    public void hashIdentity () {
//        assertIdentical("", bytestr(""));
//        assertIdentical("a", bytestr("a"));
//        assertIdentical("abc", bytestr("abc").subSequence(0, 3));
//        assertIdentical("abc", bytestr("abc").subSequence(0, 3).subSequence(0, 3));
//
//        assertIdentical("abc".substring(1,3), bytestr("abc").subSequence(1, 3).subSequence(0, 2));
//    }
//
//    private static void assertIdentical(String s, CharSequence bs) {
//        assertEquals(s.hashCode(), bs.hashCode());
//        assertTrue(s.equals(bs));
//        assertTrue(bs.equals(s));
//    }

    private CharSequence bytestr(String text) {
        try {
            return new ImmutableByteSequence(text.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
        }
        return null; // never
    }

    static void assertOutOfRange (CharSequence cs, int index) {
        try {
            cs.charAt(index);
            Assert.fail ("Character access beyond length supposed to trigger IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
        }
    }
}
