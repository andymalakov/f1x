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


    @Test
    public void test() throws InterruptedException {

        TestTimeSource timeSource = new TestTimeSource("20140303-23:01:02.999");
        TestStreamOutputFactory streamFactory = new TestStreamOutputFactory();


        DailyFileMessageLogFactory logFactory = new DailyFileMessageLogFactory(logDir, streamFactory);
        logFactory.setTimeSource(timeSource);
        logFactory.setTimeZone(TimeZone.getDefault());
        logFactory.setLogFormatter(new AsIsLogFormatter());
        DailyFileMessageLog log = (DailyFileMessageLog)logFactory.create(SESSION_ID);


        log(log, "Message1");
        log(log, "Message2");
        log(log, "Message3");

        timeSource.signalWakeUp();

        while (streamFactory.closedStreamsCount.get() < 1) {
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

    private static class TestStreamOutputFactory extends BufferedOutputStreamFactory {

        final AtomicInteger closedStreamsCount = new AtomicInteger();

        public TestStreamOutputFactory() {
            super(4096, false);
        }

        @Override
        public OutputStream create(File file) {
            return new OutputStreamWrapper (super.create(file));
        }

        // intercept OutputStream.close()
        private class OutputStreamWrapper extends OutputStream {
            final OutputStream delegate;

            private OutputStreamWrapper(OutputStream delegate) {
                this.delegate = delegate;
            }

            @Override
            public void write(int b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void close() throws IOException {
                delegate.close();
                closedStreamsCount.incrementAndGet();
            }
        }
    }

    private static class TestTimeSource implements TimeSource {
        final ReentrantLock lock = new ReentrantLock();
        final Condition wakeUpSignal = lock.newCondition();
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
                wakeUpSignal.await();
            } finally {
                lock.unlock();
            }
        }

        void signalWakeUp() {
            lock.lock();
            try {
                wakeUpSignal.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}
