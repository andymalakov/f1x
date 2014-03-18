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

package org.f1x.v1;

import org.f1x.api.message.MessageParser;

public class MessageParser2String {

    public static String convert (MessageParser parser) {
        StringBuilder sb =  new StringBuilder(256);
        while (parser.next()) {
            sb.append (parser.getTagNum());
            sb.append ('=');
            sb.append (parser.getCharSequenceValue());
            sb.append ((char)1);
        }
        return sb.toString();
    }
}
