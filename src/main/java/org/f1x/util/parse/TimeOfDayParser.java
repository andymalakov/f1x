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

package org.f1x.util.parse;

import org.f1x.api.FixParserException;

import java.util.concurrent.TimeUnit;

/**
 * Formats time-of-day using  FIX UTCTimeOnly format (HH:mm:ss or HH:mm:ss.SSS).
 * For example, "23:59:59.999".
 * This class is thread safe.
 */
public final class TimeOfDayParser {

    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);
    private static final int MILLIS_PER_MINUTE = (int) TimeUnit.MINUTES.toMillis(1);

    /** @return number of milliseconds since midnight */
    public static int parseTimeOfDay(byte[] buffer, int valueOffset, int valueLength) {
        //HH:mm:ss.SSS
        //012345678901

        int hours =   NumbersParser.parsePositiveInt(buffer, valueOffset+0, 2);
        int minutes = NumbersParser.parsePositiveInt(buffer, valueOffset+3, 2);
        int seconds = NumbersParser.parsePositiveInt(buffer, valueOffset+6, 2);
        int millis = (valueLength > 8) ? NumbersParser.parsePositiveInt(buffer, valueOffset+9, 3) : 0;

        if (hours >= 24 || minutes >= 60 || seconds >= 60)
            throw new FixParserException("Invalid time of day");
        return hours*MILLIS_PER_HOUR + minutes*MILLIS_PER_MINUTE + seconds*1000 + millis;
    }

    /** Parses HH:MM:SS into array containing H,M,S elements */
    public static int [] parseTimeOfDay(byte[] buffer) {
        int [] result = new int [3];
        result [0] =   NumbersParser.parsePositiveInt(buffer, 0, 2);
        result [1] = NumbersParser.parsePositiveInt(buffer, 3, 2);
        result [2] = NumbersParser.parsePositiveInt(buffer, 6, 2);

        if (result [0] >= 24 || result [1] >= 60 || result [2] >= 60)
            throw new FixParserException("Invalid time of day");
        return result;
    }

}
