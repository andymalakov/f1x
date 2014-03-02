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

package org.f1x.log.file;

import org.f1x.SessionIDBean;
import org.f1x.util.AsciiUtils;
import org.f1x.util.StoredTimeSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;

public class Test_FileMessageLog {

    private static final String DATE = "2013123";
    private static final String TIME = "21:54:46.000";

    private static final byte[] INBOUND = AsciiUtils.getBytes("8=FIX.4.4\u00019=65\u000135=1\u000134=5\u000149=SERVER\u000152=20131223-15:41:22.154\u000156=CLIENT\u0001112=TEST\u000110=159\u0001");
    private static final byte[] OUTBOUND = AsciiUtils.getBytes("8=FIX.4.4\u00019=65\u000135=0\u000134=2\u000149=CLIENT\u000152=20131223-15:41:17.888\u000156=SERVER\u0001112=TEST\u000110=173\u0001");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLoggingFormat() throws IOException {

        File logFile = folder.newFile("log");
        SimpleFileMessageLog log = new SimpleFileMessageLog(logFile, StoredTimeSource.makeFromUTCTimestamp(DATE+'-'+TIME), 5000);


        log.log(true, wrap(INBOUND, 2), 2, OUTBOUND.length);
        log.log(false, wrap(OUTBOUND, 2), 2, OUTBOUND.length);

        log.close();

        LineNumberReader reader = new LineNumberReader(new FileReader(logFile));
        Assert.assertEquals(TIME + " IN   " +  new String (INBOUND), reader.readLine());
        Assert.assertEquals(TIME + " OUT  " +  new String (OUTBOUND), reader.readLine());
    }

    private static byte[] wrap(byte[] arr, int wrapperSize) {
        byte [] result = new byte[arr.length + 2*wrapperSize];
        Arrays.fill(result, (byte)'x');
        System.arraycopy(arr, 0, result, wrapperSize, arr.length);
        return result;
    }
}
