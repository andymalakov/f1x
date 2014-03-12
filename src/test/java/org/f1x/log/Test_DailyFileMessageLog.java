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

package org.f1x.log;

import org.f1x.SessionIDBean;
import org.f1x.api.session.SessionID;
import org.f1x.log.MessageLog;
import org.f1x.log.file.DailyFileMessageLog;
import org.f1x.log.file.DailyFileMessageLogFactory;
import org.f1x.util.AsciiUtils;
import org.f1x.util.StoredTimeSource;
import org.f1x.util.TestUtils;
import org.f1x.util.TimeSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Test_DailyFileMessageLog {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test() throws InterruptedException {
        SessionID sessionID = new SessionIDBean("SERVER", "CLIENT");
        TestTimeSource timeSource = new TestTimeSource("20140303-23:01:02.999");

        File logDir = folder.newFolder("log");
        DailyFileMessageLogFactory logFactory = new DailyFileMessageLogFactory(logDir, DailyFileMessageLogFactory.DEFAULT_FILE_BUFFER_SIZE);
        logFactory.setTimeSource(timeSource);
        logFactory.setTimeZone(TimeZone.getDefault());
        DailyFileMessageLog log = (DailyFileMessageLog)logFactory.create(sessionID);


        log(log, "Message1");
        log(log, "Message2");
        log(log, "Message3");

        final OutputStream os = log.os;
        timeSource.signalContentStored();
        while (log.os == os) {
            Thread.yield();
        }

        log(log, "Message4");
        log(log, "Message5");
        log(log, "Message6");


        log.close();

        File [] logFiles = logDir.listFiles();
        Assert.assertEquals(2, logFiles.length);

        Assert.assertEquals("SERVER-CLIENT-20140302.log", logFiles[0].getName());
        String file1Content = TestUtils.readText(logFiles[0]);
        Assert.assertEquals("Message1Message2Message3", file1Content);


        Assert.assertEquals("SERVER-CLIENT-20140303.log", logFiles[1].getName());
        String file2Content = TestUtils.readText(logFiles[1]);
        Assert.assertEquals("Message4Message5Message6", file2Content);

    }

    private void log (MessageLog log, String text) {
        byte [] m = AsciiUtils.getBytes(text);
        log.log(true, m, 0, m.length);
    }

    static class TestTimeSource implements TimeSource {
        final ReentrantLock lock = new ReentrantLock();
        final Condition contentStored = lock.newCondition();
        long now;

        TestTimeSource(String timestamp) {
            now = TestUtils.parseLocalTimestamp(timestamp);
        }

        @Override
        public long currentTimeMillis() {
            return now;
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            lock.lock();
            try {
                contentStored.await();
            } finally {
                lock.unlock();
            }
        }

        void signalContentStored() {
            lock.lock();
            try {
                contentStored.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
