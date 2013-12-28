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

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Parses timestamp using UTCTimestamp (yyyyMMdd-HH:mm:ss or yyyyMMdd-HH:mm:ss.SSS) and UTCDateOnly (yyyyMMdd) formats.
 * For example, "19981231-23:59:59.999" and "19981231".
 * NOTE: This class is NOT thread safe.
 */
public final class TimestampParser {
    private final Calendar calendar;

    private TimestampParser(TimeZone timeZone) {
        this.calendar = Calendar.getInstance(timeZone);
    }


    public static TimestampParser createUTCTimestampParser() {
        return new TimestampParser(TimeZone.getTimeZone("UTC"));
    }

    public static TimestampParser createLocalTimestampParser() {
        return new TimestampParser(TimeZone.getDefault());
    }


    public long getUTCTimestampValue (byte [] buffer, int offset, int length) {
        //TODO: Validate ':' characters

        //yyyyMMdd-HH:mm:ss.SSS
        //012345678901234567890
        calendar.set (Calendar.YEAR,  NumbersParser.parsePositiveInt(buffer, offset, 4));
        calendar.set (Calendar.MONTH, NumbersParser.parsePositiveInt(buffer, offset+4, 2) - 1);
        calendar.set (Calendar.DAY_OF_MONTH, NumbersParser.parsePositiveInt(buffer, offset+6, 2));
        calendar.set (Calendar.HOUR_OF_DAY, NumbersParser.parsePositiveInt(buffer, offset+9, 2));
        calendar.set (Calendar.MINUTE, NumbersParser.parsePositiveInt(buffer, offset+12, 2));
        calendar.set (Calendar.SECOND, NumbersParser.parsePositiveInt(buffer, offset+15, 2));
        if (length > 17)
            calendar.set (Calendar.MILLISECOND, NumbersParser.parsePositiveInt(buffer, offset+18, 3));
        else
            calendar.set (Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public long getUTCDateOnly(byte [] buffer, int offset, int length) {
        //TODO: Validate ':' characters

        calendar.set (Calendar.YEAR,  NumbersParser.parsePositiveInt(buffer, offset, 4));
        calendar.set (Calendar.MONTH, NumbersParser.parsePositiveInt(buffer, offset+4, 2) - 1);
        calendar.set (Calendar.DAY_OF_MONTH, NumbersParser.parsePositiveInt(buffer, offset+6, 2));
        calendar.set (Calendar.HOUR_OF_DAY, 0);
        calendar.set (Calendar.MINUTE, 0);
        calendar.set (Calendar.SECOND, 0);
        calendar.set (Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


}
