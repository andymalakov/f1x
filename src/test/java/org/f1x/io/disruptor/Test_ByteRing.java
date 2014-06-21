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

package org.f1x.io.disruptor;

import com.lmax.disruptor.*;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Test_ByteRing {

//    private static final String LOGON = "8=FIX.4.4\u00019=83\u000135=5\u000134=1\u000149=XXXXXXXXXX\u000150=XXXXXXXXXX\u000152=20131013-21:10:01.513\u000156=CITIFX\u000157=CITIFX\u000110=056\u0001";
//    private static final int LOGON_MSG_SIZE = LOGON.length();
    private final Executor executor = Executors.newCachedThreadPool();
    private static final int QUEUE_SIZE = 1024;
//TODO: Uncomment
//    @Test
//    public void testStdout() throws InterruptedException {
//        ByteRing ring = new ByteRing (QUEUE_SIZE, new BlockingWaitStrategy());
//
//        RingBufferBlockProcessor handler = new StdoutMessageHandler();
//        MessageProcessorPool processorPool = new MessageProcessorPool (ring, ring.newBarrier(), new TestExceptionHandler(), handler);
//        ring.addGatingSequences(processorPool.getWorkerSequences());
//        processorPool.start(executor);
//
//        ByteRingProducer producer = new ByteRingProducer(ring, new PlaybackByteProducer(LOGON));
//        executor.execute(producer);
//
//        Thread.sleep(500);
//    }



    //TODO: Use Test_MemoryExchangeThroughput version
    public static void measureThroughput (MessageCounter messageCounter, String testName) throws InterruptedException {
        long lastSeenMessageCount = 0;
        long lastTime = System.currentTimeMillis();
        while (true) {
            Thread.sleep(15000);

            final long messageCount = messageCounter.getMessageCount();
            final long now = System.currentTimeMillis();
            long throughput = (messageCount - lastSeenMessageCount) / ((now - lastTime)/1000);
            System.out.println("Test " + testName + " processed " + throughput + " messages/sec");
            lastSeenMessageCount = messageCount;
            lastTime = now;
        }
    }

    //    private static class CountingMessageHandler implements RingBufferBlockProcessor, MessageCounter {
//        private volatile long messageCount;
//
//        @Override
//        public void process(byte[] buffer, int bufferSize, int offset, int length) {
//            messageCount++;
//        }
//
//        @Override
//        public long getMessageCount() {
//            return messageCount;
//        }
//
//    }


//    public static void main (String [] args) throws InterruptedException {
//        new Test_ByteRing().testPerformance();
//    }
}
