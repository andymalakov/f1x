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

import java.lang.reflect.Array;

public class ByteEnumLookup<E extends ByteEnum> {
    private final E [] xlatTable;
    private final byte base;

    @SuppressWarnings("unchecked")
    public ByteEnumLookup(Class<E> cls) {
        E [] values = cls.getEnumConstants();
        if (values.length == 0)
            throw new IllegalArgumentException("Enum doesn't declare any values: " + cls.getName());

        byte min = Byte.MAX_VALUE, max = Byte.MIN_VALUE;
        for (E value : values) {
            byte b = value.getCode();
            if (min > b)
                min = b;
            if (max < b)
                max = b;
        }

        base = min;
        xlatTable = (E[]) Array.newInstance(cls, max - min + 1);
        for (E value : values) {
            int index = value.getCode() - min;
            if (xlatTable[index] != null)
                throw new IllegalArgumentException("Enum " + cls.getName() + " defines duplicate codes for " + value + " and " + xlatTable[index]);
            xlatTable[index] = value;
        }
    }

    public E get(byte code) {
        final int index = code - base;
        if (index >= 0 && index < xlatTable.length) {
            E result = xlatTable[index];
            if (result != null)
                return result;

        }
        throw new IllegalArgumentException("Undefined code '" + code + "' (0x" + Integer.toHexString(code) + ") for enum " + xlatTable[0].getClass().getSimpleName());
    }

}
