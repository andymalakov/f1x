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

import java.io.*;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TestUtils {

    public static DateFormat LOCAL_TIMESTAMP_FORMAT = createDateFormat(TimestampFormatter.DATE_TIME_FORMAT, TimeZone.getDefault());
    public static DateFormat UTC_TIMESTAMP_FORMAT = createUTCDateFormat(TimestampFormatter.DATE_TIME_FORMAT);
    public static DateFormat UTC_DATE_ONLY_FORMAT = createUTCDateFormat(TimestampFormatter.DATE_ONLY_FORMAT);
    public static DateFormat UTC_TIME_ONLY_FORMAT = createUTCDateFormat(TimeOfDayFormatter.FORMAT);

    public static DateFormat createUTCDateFormat(String format) {
        return createDateFormat(format, TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat createDateFormat(String format, TimeZone tz) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(tz);
        sdf.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
        return sdf;
    }

    public static byte[] wrap(byte[] arr, int wrapperSize) {
        byte [] result = new byte[arr.length + 2*wrapperSize];
        Arrays.fill(result, (byte) 'x');
        System.arraycopy(arr, 0, result, wrapperSize, arr.length);
        return result;
    }

    public static long parseUTCTimestamp(String time) {
        try {
            return UTC_TIMESTAMP_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            throw new AssertionFailedError("Error parsing time: " + time + ": " + e.getMessage());
        }
    }

    public static long parseLocalTimestamp(String time) {
        try {
            return LOCAL_TIMESTAMP_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            throw new AssertionFailedError("Error parsing time: " + time + ": " + e.getMessage());
        }
    }

    public static String formatLocalTimestamp(long timestamp) {
        return LOCAL_TIMESTAMP_FORMAT.format(new Date(timestamp));
    }

    public static String readText(File logFile) {

        try (LineNumberReader reader = new LineNumberReader(new FileReader(logFile))) {
            StringBuilder sb = new StringBuilder();
            while(true) {

                String line = reader.readLine();
                if (line == null)
                    break;

                if (sb.length() > 0)
                    sb.append('\n');
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Can't read file", e);
        }
    }

    public static String readSmallFile(File logFile, int maxLen) {
        byte [] buffer = new byte [maxLen];

        try (FileInputStream is = new FileInputStream(logFile)) {
            int bytesRead = is.read(buffer, 0, buffer.length);
            return new String (buffer, 0, bytesRead, "US-ASCII");
        } catch (IOException e) {
            throw new RuntimeException("Can't read file", e);
        }
    }
}
