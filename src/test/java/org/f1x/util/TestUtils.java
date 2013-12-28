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

import junit.framework.AssertionFailedError;
import org.f1x.util.format.TimeOfDayFormatter;
import org.f1x.util.format.TimestampFormatter;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TestUtils {

    public static DateFormat UTC_TIMESTAMP_FORMAT = createUTCDateFormat(TimestampFormatter.DATE_TIME_FORMAT);
    public static DateFormat UTC_DATE_ONLY_FORMAT = createUTCDateFormat(TimestampFormatter.DATE_ONLY_FORMAT);
    public static DateFormat UTC_TIME_ONLY_FORMAT = createUTCDateFormat(TimeOfDayFormatter.FORMAT);

    public static DateFormat createUTCDateFormat(String format) {
        return createUTCDateFormat(format, TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat createUTCDateFormat(String format, TimeZone tz) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(tz);
        sdf.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
        return sdf;
    }

    public static long parseUTCTimestamp(String time) {
        try {
            return UTC_TIMESTAMP_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            throw new AssertionFailedError("Error parsing time: " + time + ": " + e.getMessage());
        }
    }
}
