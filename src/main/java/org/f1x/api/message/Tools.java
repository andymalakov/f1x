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

import org.f1x.api.message.fields.MsgType;

public class Tools {
    public static boolean equals(MsgType msgType, CharSequence value) {
        return equals(msgType.getCode(), value);
    }

    public static boolean equals(CharSequence s1, CharSequence s2) {
        int         len1 = s1.length ();
        int         len2 = s2.length ();

        if (len1 != len2)
            return false;

        for (int ii = 0; ii < len1; ii++) {
            if (s1.charAt (ii) != s2.charAt (ii))
                return false;
        }

        return true;
    }

    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    /**
     * The CheckSum integrity check is calculated by summing the binary value of each character from the "8" of "8=" up to
     *  and including the SOH character immediately preceding the CheckSum tag field and comparing the least significant
     *  eight bits of the calculated value to the CheckSum value
     */
    public static int calcCheckSum(final byte [] buffer, final int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += buffer[i];
        }
        return sum & 0xFF;
    }
}
