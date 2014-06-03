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

import java.nio.charset.StandardCharsets;

public class AsciiUtils {

    public static byte[] getBytes(String asciiText) {
        return asciiText.getBytes(StandardCharsets.US_ASCII);
    }

    public static boolean equals(byte[] array1, byte[] array2, int offset2, int length) {
        for (int i=0; i < length; i++)
            if (array1[i] != array2[i+offset2])
                return false;

        return true;
    }

}
