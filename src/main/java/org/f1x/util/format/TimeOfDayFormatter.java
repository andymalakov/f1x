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

package org.f1x.util.format;

import java.util.concurrent.TimeUnit;

/**
 * Formats timestamp using  FIX UTCTimeOnly format (HH:mm:ss.SSS).
 * For example, "23:59:59.999".
 * This class is thread safe.
 */
public class TimeOfDayFormatter {
    private final static long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    public static final String FORMAT = "HH:mm:ss.SSS";
    public static final int LENGTH = FORMAT.length();

    private TimeOfDayFormatter() {}

    /**
     * @param timestamp Java time from System.currentTimeMillis().
     * @param buffer buffer for formatted time (Output will look like "01:23:45"). Must accommodate 12 bytes of formatted value.
     * @param offset offset in output buffer
     * @return offset + output length
     */
    public static int format (final long timestamp, final byte [] buffer, int offset) {
        if (timestamp < 0)
            throw new IllegalArgumentException();

        final int secondsInDay = (int) ((timestamp / 1000) % SECONDS_IN_DAY);
        offset = IntFormatter.format2digits(secondsInDay / 3600, buffer, offset); // hours
        buffer [offset++] = ':';
        offset = IntFormatter.format2digits((secondsInDay / 60) % 60, buffer, offset); // minutes
        buffer [offset++] = ':';
        offset = IntFormatter.format2digits(secondsInDay % 60, buffer, offset); // seconds

        buffer [offset++] = '.';
        offset = IntFormatter.format3digits((int) (timestamp % 1000), buffer, offset); // milliseconds

        return offset;
    }
}
