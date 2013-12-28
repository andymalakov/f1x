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

public class Test_IntEnumLookup {
    enum Color implements IntEnum {
        Red(1),
        Green(3),
        Blue(5);

        /** Boilerplate code */
        private final int code;
        Color(int code) {
            this.code = code;
        }
        public int getCode() { return code; }
    }

    @Test
    public void test() {
        IntEnumLookup<Color> decoder = new IntEnumLookup<>(Color.class);
        assertEquals (Color.Red, decoder.get(1));
        assertEquals (Color.Green, decoder.get(3));
        assertEquals (Color.Blue, decoder.get(5));

        assertIllegalArgument(decoder, 0);
        assertIllegalArgument(decoder, 2);
        assertIllegalArgument(decoder, 4);
        assertIllegalArgument(decoder, 6);
        assertIllegalArgument(decoder,  0xFF);
    }

    private static void assertIllegalArgument(IntEnumLookup decoder, int arg) {
        try {
            decoder.get(arg);
            fail ("Failed to detect invalid argument: " + arg);
        } catch (IllegalArgumentException expected) { }
    }

    @SuppressWarnings("unused")
    enum Duplicate implements IntEnum {
        One('1'),
        Two('1');

        /** Boilerplate code */
        private final int code;
        Duplicate(int code) {
            this.code = code;
        }
        public int getCode() { return code; }
    }

    @Test
    public void testDuplicate() {
        try {
            new IntEnumLookup<>(Duplicate.class);
            fail ("Failed to detect duplicate codes");
        } catch (IllegalArgumentException expected) { }
    }
}
