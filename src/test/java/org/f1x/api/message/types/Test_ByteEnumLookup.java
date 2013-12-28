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

package org.f1x.api.message.types;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Test_ByteEnumLookup {

    enum Color implements ByteEnum {
        Red('1'),
        Green('3'),
        Blue('5');

        /** Boilerplate code */
        private final byte code;
        Color(char code) {
            this.code  = (byte) code;
        }
        public byte getCode() { return code; }
    }

    @Test
    public void test() {
        ByteEnumLookup<Color> decoder = new ByteEnumLookup<>(Color.class);
        assertEquals (Color.Red, decoder.get((byte)'1'));
        assertEquals (Color.Green, decoder.get((byte)'3'));
        assertEquals (Color.Blue, decoder.get((byte)'5'));

        assertIllegalArgument(decoder, (byte)0);
        assertIllegalArgument(decoder, (byte)'0');
        assertIllegalArgument(decoder, (byte)'2');
        assertIllegalArgument(decoder, (byte)'4');
        assertIllegalArgument(decoder, (byte)'6');
        assertIllegalArgument(decoder, (byte) 0xFF);
    }

    private static void assertIllegalArgument(ByteEnumLookup decoder, byte arg) {
        try {
            decoder.get(arg);
            fail ("Failed to detect invalid argument: (byte) 0x" + Integer.toHexString(arg));
        } catch (IllegalArgumentException expected) { }
    }

    @SuppressWarnings("unused")
    enum Duplicate implements ByteEnum {
        One('1'),
        Two('1');

        /** Boilerplate code */
        private final byte code;
        Duplicate(char code) {
            this.code  = (byte) code;
        }
        public byte getCode() { return code; }
    }

    @Test
    public void testDuplicate() {
        try {
            new ByteEnumLookup<>(Duplicate.class);
            fail ("Failed to detect duplicate codes");
        } catch (IllegalArgumentException expected) { }
    }
}
