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
import org.f1x.api.FixVersion;
import org.f1x.api.SessionID;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionState;
import org.f1x.tools.SimpleFixAcceptor;
import org.f1x.tools.SimpleFixInitiator;
import org.f1x.v1.FixAcceptorSettings;
import org.f1x.v1.FixSessionInitiator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This test verifies that there are no allocations in during session
 */
public class NoAllocationsTest {

    private static final String INITIATOR_SENDER_ID = "INITIATOR";
    private static final String ACCEPTOR_SENDER_ID = "ACCEPTOR";

    private static SimpleFixInitiator createInitiator(int port, SessionEventListener eventListener) {
        return new SimpleFixInitiator("localhost", port, new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID), eventListener);
    }

    private static SimpleFixAcceptor createAcceptor(int port) {
        return new SimpleFixAcceptor(port, new SessionIDBean(INITIATOR_SENDER_ID, ACCEPTOR_SENDER_ID));
    }


    @Test(timeout = 120000)
    public void simpleMessageLoop() throws InterruptedException, IOException {
        List<String> allocs = enableAllocationTracing(); //TODO: Something is wrong... no allocations are recorded :-(

        final CountDownLatch connected = new CountDownLatch(1);
        final SessionEventListener eventListener = new SessionEventListener() {

            @Override
            public void onStateChanged(SessionID sessionID, SessionState oldState, SessionState newState) {
                if (newState == SessionState.ApplicationConnected)
                    connected.countDown();
            }
        };

        final SimpleFixAcceptor acceptor = createAcceptor(7890);
        final SimpleFixInitiator initiator = createInitiator(7890, eventListener);


        final Thread acceptorThread = new Thread(acceptor, "Acceptor");
        acceptorThread.start();

        final Thread initiatorThread = new Thread(initiator, "Initiator");
        initiatorThread.start();

        if ( ! connected.await(15, TimeUnit.SECONDS))
            Assert.fail("Connection failed");

        for(int i=1; i <= 10000; i++) {
            initiator.sendNewOrder(i);
        }
        Thread.sleep(1000);
        initiator.disconnect("End of test");
        if ( ! allocs.isEmpty())
            Assert.fail("There were " + allocs.size() + " allocations");
    }

    private static List<String> enableAllocationTracing() {
        final List<String> allocs = Collections.synchronizedList(new ArrayList<String>(10000));
        AllocationRecorder.addSampler(new Sampler() {
            public void sampleAllocation(int count, String desc, Object newObj, long size) {
                System.out.println("I just allocated the object " + newObj + " of type " + desc + " whose size is " + size);
                allocs.add(newObj.getClass().getName());
            }
        });
        return allocs;
    }
}
