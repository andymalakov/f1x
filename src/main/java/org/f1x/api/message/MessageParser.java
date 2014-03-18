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

import org.f1x.util.ByteArrayReference;

/** Not thread safe */
public interface MessageParser {

    boolean next ();

    /** @return current tag number (processed by last call to {@link #next()} */
    int getTagNum();

    byte getByteValue();

    /** @return value of current tag interpreted as INT32 number. */
    int getIntValue();

    /** @return value of current tag interpreted as INT64 number. */
    long getLongValue();

    /** @return value of current tag interpreted as double number. Some loss of precision may occur when converting fixed-point number to floating point result. */
    double getDoubleValue();

    /** @return value of current tag as CharSequence. Note: caller must save result because returned object will be reused for other tags. (Flyweight pattern). */
    CharSequence getCharSequenceValue();

    void getByteSequence(ByteArrayReference seq);

    /** @return value of current tag as String (WARNING: Allocates memory!) */
    String getStringValue ();

    /** Appends current value to given string builder */
    void getStringBuilder (StringBuilder appendable);

    /** @return value of current tag interpreted as Boolean ('Y' = true; 'N' = false}. */
    boolean getBooleanValue ();

    /** @return value of current tag interpreted as UTC Time and Date combination (in either YYYYMMDD-HH:MM:SS (whole seconds) or YYYYMMDD-HH:MM:SS.sss (milliseconds) format) */
    long getUTCTimestampValue ();

    /** @return number of millisecond since midnight if current tag contains time-of-day or UTCTimeOnly (in either HH:MM:SS (whole seconds) or HH:MM:SS.sss (milliseconds) format) */
    int getUTCTimeOnly ();

    /** @return value of current tag interpreted as UTC Date (in YYYYMMDD format) */
    long getUTCDateOnly ();

    /** @return value of current tag interpreted as LocalMktDate (in YYYYMMDD format) */
    long getLocalMktDate ();

    /** @return true if value of the last processed tag equals to given byte array */
    boolean isValueEquals(byte[] constant);

}


