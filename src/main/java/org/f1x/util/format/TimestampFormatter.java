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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Formats timestamp using UTCTimestamp (yyyyMMdd-HH:mm:ss.SSS) and UTCDateOnly (yyyyMMdd) formats.
 * For example, "19981231-23:59:59.999" and "19981231".
 * NOTE: This class is NOT thread safe.
 */
public class TimestampFormatter {
    public static final String DATE_TIME_FORMAT = "yyyyMMdd-HH:mm:ss.SSS";
    public static final String DATE_ONLY_FORMAT = "yyyyMMdd";
    public static final int DATE_TIME_LENGTH = 21;
    public static final int DATE_ONLY_LENGTH = 8;
    private static final  long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    private final Calendar calendar;

    public static TimestampFormatter createUTCTimestampFormatter() {
        return new TimestampFormatter(TimeZone.getTimeZone("UTC"));
    }

    public static TimestampFormatter createLocalTimestampFormatter() {
        return new TimestampFormatter(TimeZone.getDefault());
    }

    private TimestampFormatter(TimeZone tz) {
        calendar = Calendar.getInstance(tz);
    }

    /**
     * @param timestamp the difference, measured in milliseconds, between the given moment of time and midnight, January 1, 1970 UTC.
     * @param buffer buffer for formatted timestamp (Output will look like "01:23:45"). Must accommodate 8 bytes of formatted value.
     * @param offset offset in the buffer
     * @return  offset + length of resulting string (21 bytes)
     */
    public int formatDateTime(final long timestamp, final byte[] buffer, int offset) {
        calendar.setTimeInMillis(timestamp);

        offset = IntFormatter.format4digits(calendar.get(Calendar.YEAR), buffer, offset);
        offset = IntFormatter.format2digits(calendar.get(Calendar.MONTH)+1, buffer, offset);
        offset = IntFormatter.format2digits(calendar.get(Calendar.DAY_OF_MONTH), buffer, offset);
        buffer[offset++] = '-';


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

    /**
     * @param timestamp the difference, measured in milliseconds, between the given moment of time and midnight, January 1, 1970 UTC.
     * @param buffer buffer for formatted timestamp (Output will look like "01:23:45"). Must accommodate 8 bytes of formatted value.
     * @param offset offset in the buffer
     * @return  offset + length of resulting string (21 bytes)
     */
    public int formatDateOnly (final long timestamp, final byte [] buffer, int offset) {
        calendar.setTimeInMillis(timestamp);

        offset = IntFormatter.format4digits(calendar.get(Calendar.YEAR), buffer, offset);
        offset = IntFormatter.format2digits(calendar.get(Calendar.MONTH)+1, buffer, offset);
        offset = IntFormatter.format2digits(calendar.get(Calendar.DAY_OF_MONTH), buffer, offset);

        return offset;
    }
}
