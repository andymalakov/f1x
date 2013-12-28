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

package org.f1x.util.log;

/** Garbage free logger  */
public interface Log {
    //TODO: (Use byte[] formatters)
    Log timestamp ();
    Log append (CharSequence text);
    Log append (int value);
    Log append (long value);
    Log append (double value);
    Log append (boolean value);
    Log appendUTCTimestamp (long timestamp);
    Log append (Object value);
    void flush();
}
