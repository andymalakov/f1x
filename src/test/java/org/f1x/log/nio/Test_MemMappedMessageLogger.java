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

package org.f1x.log.nio;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;

public class Test_MemMappedMessageLogger {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    private static final int MAX_FILE_SIZE = 64;

    @Test
    public void test() throws IOException {
        File logFile = folder.newFile("log");
        MemMappedMessageLogger logger = new MemMappedMessageLogger (logFile, MAX_FILE_SIZE);


        for (int i=1; i <= 10; i++) {
            byte [] msg = makeBytes(i);
            logger.log(i%2==0, msg, 0, msg.length);
        }

        logger.close();

        String content = readText(logFile);
        int tailIndex = content.indexOf(new String(MemMappedMessageLogger.TAIL));
        if (tailIndex < 0)
            tailIndex = content.indexOf(new String(MemMappedMessageLogger.EOF));
        if (tailIndex > 0)
            content = content.substring(0, tailIndex);
        Assert.assertEquals(new String(">Message#6\n<Message#7\n>Message#8\n<Message#9\n>Message#10\n"), content);

    }

    private static String readText(File logFile) {
        StringBuilder sb = new StringBuilder();

        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(logFile));
            while(true) {

                String line = reader.readLine();
                if (line == null)
                    break;

                if (sb.length() > 0)
                    sb.append("\n");
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't read file", e);
        }
        return sb.toString();
    }

    private static byte[] makeBytes(int msgId) {
        return ("Message#" + msgId).getBytes();
    }
}
