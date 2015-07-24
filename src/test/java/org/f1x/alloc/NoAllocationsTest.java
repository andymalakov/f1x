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
package org.f1x.alloc;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;
import org.f1x.SessionIDBean;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.tools.SimpleFixAcceptor;
import org.f1x.tools.SimpleFixInitiator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This test verifies that there are no allocations in during session.
 * NOTE: you must run JVM with  -javaagent:allocation.jar option
 */
@Ignore
public class NoAllocationsTest /*extends TestCommon */{

    private static final String INITIATOR_SENDER_ID = "INITIATOR";
    private static final String ACCEPTOR_SENDER_ID = "ACCEPTOR";

    private static class TestObject {

        public TestObject(long orderId) {
            if (System.currentTimeMillis() == orderId)
                System.err.println("Never"); // prevent hotspot from optimizing this out
        }
    }


    private static SimpleFixInitiator createInitiator(int port) {
        return new SimpleFixInitiator("localhost", port, new SessionIDBean(ACCEPTOR_SENDER_ID, INITIATOR_SENDER_ID)) {
            @Override
            public void sendNewOrder(long orderId) throws IOException {
                super.sendNewOrder(orderId);
                new TestObject(orderId);
            }
        };
    }

    private static SimpleFixAcceptor createAcceptor(int port) {
        return new SimpleFixAcceptor(port, new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID));
    }


    @Test(timeout = 120000)
    public void simpleMessageLoop() throws InterruptedException, IOException {

        final CountDownLatch connected = new CountDownLatch(1);
        final SessionEventListener eventListener = new SessionEventListener() {

            @Override
            public void onStatusChanged(SessionID sessionID, SessionStatus oldStatus, SessionStatus newStatus) {
                if (newStatus == SessionStatus.ApplicationConnected)
                    connected.countDown();
            }
        };

        final SimpleFixAcceptor acceptor = createAcceptor(7890);
        final SimpleFixInitiator initiator = createInitiator(7890);

        initiator.setEventListener(eventListener);

        final Thread acceptorThread = new Thread(acceptor, "Acceptor");
        acceptorThread.start();

        final Thread initiatorThread = new Thread(initiator, "Initiator");
        initiatorThread.start();

        if ( ! connected.await(15, TimeUnit.SECONDS))
            Assert.fail("Connection failed");

        long  orderId = 1;
        AllocationDetector allocationDetector = AllocationDetector.create();
        for(int i=1; i <= 10; i++) {
            initiator.sendNewOrder(orderId++);
        }
        Thread.sleep(1000);
        allocationDetector.enabled = true;
        for(int i=1; i <= 1000; i++) {
            initiator.sendNewOrder(orderId++);
        }
        Thread.sleep(1000);
        allocationDetector.enabled = false;
        if ( ! allocationDetector.isTestObjectAllocatonsDetected)
            Assert.fail("Allocation detector is not working properly. Check that you added -agent:allocation.jar JVM argument");

        if ( ! allocationDetector.allocs.isEmpty())
            Assert.fail("There were " + allocationDetector.allocs.size() + " allocations");
        initiator.disconnect("End of test");
        initiator.close();
        acceptor.close();
    }


    @After
    public void disableTracing() {
        //TODO: disable tracing
    }

    private static class AllocationDetector implements Sampler {
        boolean isTestObjectAllocatonsDetected = false;
        final List<Class> allocs = Collections.synchronizedList(new ArrayList<Class>(10000));
        volatile boolean enabled;

        static AllocationDetector create () {
            AllocationDetector result = new AllocationDetector();
            AllocationRecorder.addSampler(result);
            return result;
        }

        @Override
        public void sampleAllocation(int count, String desc, Object newObj, long size) {
            //System.out.println("I just allocated the object " + newObj + " of type " + desc + " whose size is " + size);
            if (enabled) {
                Class allocatedClass = newObj.getClass();
                if (allocatedClass == TestObject.class)
                    isTestObjectAllocatonsDetected = true;
                else
                    allocs.add(allocatedClass);
            }
        }
    }
}
