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

import org.f1x.log.AsIsLogFormatter;
import org.f1x.log.MessageLog;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class Test_RollingFileMessageLog extends AbstractMessageLogTest {

    private final static int FILE_COUNTER = 4;
    private final static int BYTES_PER_FILE = 64;
    private final static int FLUSH_PERIOD_MILLISECONDS = 10;
    private final int APPROX_MSG_SIZE = 11;


    private RollingFileMessageLogFactory logFactory;


    @Before
    public void createLogFactory () {
        logFactory = new RollingFileMessageLogFactory(logDir, FILE_COUNTER, BYTES_PER_FILE) {
            {
                setFlushPeriod(FLUSH_PERIOD_MILLISECONDS);
                setLogFormatter(new AsIsLogFormatter());
            }
        };
    };


    @Test
    public void testNoRollover() throws InterruptedException, IOException {
        MessageLog log = logFactory.create(SESSION_ID);

        final int N = 32;
        StringBuilder expectedContent = new StringBuilder (N*APPROX_MSG_SIZE);

        for (int i=0; i < N; i++) {
            String message = "Msg#"+i+';';
            log.log(true, message.getBytes(), 0, message.length());
            expectedContent.append(message);

            Thread.sleep(2*FLUSH_PERIOD_MILLISECONDS); // give asynchronous flusher a chance to start next file
        }
        log.close();

        // due to asynchronous nature of rollover logger there is no guarantee that we will get exact number of log files
        StringBuilder actualContent = readStoredContent();

        Assert.assertEquals("Content", expectedContent.toString(), actualContent.toString());

    }

    @Test
    public void testContentRollover() throws InterruptedException, IOException {
        MessageLog log = logFactory.create(SESSION_ID);

        final int N = 49;

        for (int i=0; i < N; i++) {
            String message = "Message#"+i+';';
            log.log(true, message.getBytes(), 0, message.length());

            Thread.sleep(2*FLUSH_PERIOD_MILLISECONDS); // give asynchronous flusher a chance to start next file
        }
        log.close();

        // due to asynchronous nature of rollover logger there is no guarantee that we will get exact number of log files
        StringBuilder actualContent = readStoredContent();

        String expectedContent = "Message#48;Message#30;Message#31;Message#32;Message#33;Message#34;Message#35;Message#36;Message#37;Message#38;Message#39;Message#40;Message#41;Message#42;Message#43;Message#44;Message#45;Message#46;Message#47;";
        Assert.assertEquals("Content", expectedContent, actualContent.toString());

    }

    private StringBuilder readStoredContent() {
        File[] logFiles = logDir.listFiles();
        Assert.assertTrue("Several log files", logFiles.length > 1);

        StringBuilder actualContent = new StringBuilder (FILE_COUNTER*(BYTES_PER_FILE+APPROX_MSG_SIZE));
        for (int i=0; i < FILE_COUNTER; i++) {
            Assert.assertEquals("SERVER-CLIENT."+ (i+1) + ".log", logFiles[i].getName());
            String fileContent = TestUtils.readText(logFiles[i]);
            actualContent.append(fileContent);
        }
        return actualContent;
    }
//
//    private static final int MSG_LEN = 8;
//    private static int populate(byte[] content) {
//        int offset = 0;
//        int messageId = 0;
//
//        int numberOfMessages = 0;
//        try {
//            while (true) {
//                content[offset++] = 'M';   // 1
//                content[offset++] = 's';   // 2
//                content[offset++] = 'g';   // 3
//                content[offset++] = '#';   // 4
//                offset = IntFormatter.format3digits(++messageId, content, offset); // + 7
//                content[offset++] = ';'; // 8
//                numberOfMessages++;
//            }
//
//        } catch (IndexOutOfBoundsException | IllegalArgumentException stop) {
//            assert numberOfMessages > 0;
//        }
//        return numberOfMessages;
//    }


}
