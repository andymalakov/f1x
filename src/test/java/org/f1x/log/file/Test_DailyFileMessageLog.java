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

import org.f1x.log.AsIsLogFormatter;
import org.f1x.util.TestUtils;
import org.f1x.util.TimeSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Test_DailyFileMessageLog extends AbstractMessageLogTest {


    @Test (timeout = 30000)
    public void test() throws InterruptedException {

        final TestTimeSource timeSource = new TestTimeSource("20140303-23:01:02.999");
        final OutputStreamFactory streamFactory = new BufferedOutputStreamFactory(DailyFileMessageLogFactory.DEFAULT_FILE_BUFFER_SIZE, true);

        final AtomicInteger rolloverCount = new AtomicInteger();
        final int flushPeriod = 0; // ensures that timesource.sleep() is called only by DailyFileMessageLog.OutputStreamRollover
        DailyFileMessageLog log = new DailyFileMessageLog(SESSION_ID, logDir, streamFactory, null, timeSource, TimeZone.getDefault()) {
            @Override
            protected void onRollover() {
                rolloverCount.incrementAndGet();
            }
        };
        log.start(SESSION_ID, timeSource, flushPeriod);


        log(log, "Message1");
        log(log, "Message2");
        log(log, "Message3");

        timeSource.signalWakeUp();

        // wait for rollover
        while (rolloverCount.get() < 1) {
            Thread.yield();
        }

        log(log, "Message4");
        log(log, "Message5");
        log(log, "Message6");


        log.close();

        File [] logFiles = logDir.listFiles();
        Assert.assertNotNull(logFiles);
        Assert.assertEquals(2, logFiles.length);

        Assert.assertEquals("SERVER-CLIENT-20140303.log", logFiles[0].getName());
        String file1Content = TestUtils.readText(logFiles[0]);
        Assert.assertEquals("Message1Message2Message3", file1Content);


        Assert.assertEquals("SERVER-CLIENT-20140304.log", logFiles[1].getName());
        String file2Content = TestUtils.readText(logFiles[1]);
        Assert.assertEquals("Message4Message5Message6", file2Content);

    }


    private static class TestTimeSource implements TimeSource {
        final ReentrantLock lock = new ReentrantLock();
        final Condition wakeUpSignal = lock.newCondition();
        final Condition sleeperIsWaiting = lock.newCondition();
        final long now;

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
                sleeperIsWaiting.signal();
                wakeUpSignal.await();
            } finally {
                lock.unlock();
            }
        }

        void signalWakeUp() throws InterruptedException {
            lock.lock();
            try {
                sleeperIsWaiting.await();
                wakeUpSignal.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
