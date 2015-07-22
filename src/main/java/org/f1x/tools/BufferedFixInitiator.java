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

package org.f1x.tools;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import org.f1x.SessionIDBean;
import org.f1x.api.FixInitiatorSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.io.OutputChannel;
import org.f1x.io.RingBufferStreamChannel;
import org.f1x.io.disruptor.ByteRing;
import org.f1x.io.disruptor.MessageProcessorPool;
import org.f1x.io.disruptor.RingBufferBlockProcessor;
import org.f1x.io.socket.RingBuffer2StreamProcessor;
import org.f1x.v1.FixSessionInitiator;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Extension of FixInitiator that uses ring buffer for asynchronous send.
 */
public class BufferedFixInitiator extends FixSessionInitiator {
    private final Executor executor;
    private final ByteRing ring;
    private MessageProcessorPool processorPool;

    private final ExceptionHandler exceptionHandler = new ExceptionHandler () {
        @Override
        public void handleEventException(Throwable e, long l, Object o) {
            disconnect("Error " + e.getMessage());
        }

        @Override
        public void handleOnStartException(Throwable e) {
            disconnect("Error " + e.getMessage());
        }

        @Override
        public void handleOnShutdownException(Throwable e) {
            disconnect("Error " + e.getMessage());
        }
    };

    public BufferedFixInitiator(String host, int port, FixVersion fixVersion, SessionID sessionID, int queueSize, FixInitiatorSettings settings) {
        super(host, port, fixVersion, sessionID, settings);

        ring = new ByteRing (queueSize, new BlockingWaitStrategy()); //TODO: Use BusySpinWaitStrategy?
        executor = Executors.newCachedThreadPool();
    }


    @Override
    protected OutputChannel getOutputChannel(Socket socket) throws IOException {
        if (processorPool != null)
            throw new IllegalStateException("Previous processor pool still uses Ring Buffer");


        //ring.reset();

        //TODO: Not the best thing to create all that each time we reconnect?
        RingBufferBlockProcessor socketSender = new RingBuffer2StreamProcessor(socket.getOutputStream(), exceptionHandler);

        //TODO: RingBufferBlockProcessor logger = BufferLogger.createLogger(new File("d:\\fixlog.bin"), 8192, exceptionHandler);

        processorPool = new MessageProcessorPool (ring, ring.newBarrier(), exceptionHandler, socketSender); //,logger

        ring.addGatingSequences(processorPool.getWorkerSequences());
        processorPool.start(executor);

        return new RingBufferStreamChannel(ring);
    }

    @Override
    public void disconnect(CharSequence cause) {
        super.disconnect(cause);

        if (processorPool != null) {
            processorPool.halt(); //TODO: special mode in which we do processor.drainAndHalt() ?
            processorPool = null;
        }

        //TODO: Handle data that is still in the buffer
    }

    public static void main (String [] args) throws InterruptedException, IOException {
        final String host = (args.length > 0) ? args[0] : "192.168.1.105";
        final int port = (args.length > 1) ? Integer.parseInt(args[1]) : 2508;
        final int queueSize = 64*1024;
        new BufferedFixInitiator(host, port, FixVersion.FIX44, new SessionIDBean("CLIENT", "SERVER"), queueSize, new FixInitiatorSettings()).run();
    }


}
