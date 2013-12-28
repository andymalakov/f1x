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

package org.f1x.v1;

import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;
import org.f1x.api.SessionID;
import org.f1x.util.Stack;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

public abstract class MultiSessionAcceptor extends Thread {
//    protected static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);
//
//    protected static interface FixAcceptorFactory {
//        FixAcceptorFactory create(String name);
//    }
//    private final int bindPort;
//    private final SessionID sessionID;
//
//    private volatile boolean active = true;
//
//
////    private final Executor executor;
////    private final Stack<FixAcceptorThread> threadPool;  //TODO: Use Executor's thread pool ?
//
//    public MultiSessionAcceptor(int bindPort, SessionID sessionID, FixAcceptorSettings settings, Executor executor, FixAcceptorThreadFactory factory) {
//        super("FIX Acceptor on " + bindPort);
//
//        this.bindPort = bindPort;
//        this.sessionID = sessionID;
//
////        this.executor = executor;
////        this.threadPool = new Stack<>(settings.getMaxNumberOfConnections());
////        for (int i = 0; i < settings.getMaxNumberOfConnections(); i++) {
////            threadPool.add(factory.create("AcceptorThread#" + i));
////        }
//    }
//
//    /** Dispatch inbound socket connections to FixAcceptorThreads */
//    @Override
//    public void run() {
//        LOGGER.info().append("FIX Acceptor started on port ").append(bindPort).commit();
//        try {
//            ServerSocket ss = new ServerSocket (bindPort);
//            acceptInboundConnections (ss);
//        } catch (Throwable e) {
//            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
//        }
//    }
//
//    protected void acceptInboundConnections(ServerSocket ss) {
//        while (active) {
//            try {
//                Socket socket = ss.accept ();
//                if ( ! processInboundConnection (socket))
//                    socket.close();
//            } catch (Throwable e) {
//                LOGGER.warn().append("Error in acceptor loop (ignoring)").append(e).commit();
//            }
//        }
//    }
//
////    /** @return false to close client socket */
////    protected abstract boolean processInboundConnection(Socket socket);
////
////    protected abstract boolean processInboundConnection1(Socket socket) {
////        FixAcceptorThread acceptor = threadPool.remove(); //TODO: how to return it back later?
////        if (acceptor != null) {
////            acceptor.connect(sessionID, socket);
////            executor.execute(acceptor);
////        } else {
////            socket.close();
////            LOGGER.info().append("No more connection (rejecting connection)").commit();
////        }
////        //To change body of created methods use File | Settings | File Templates.
////    }
//
////    protected abstract boolean processInboundConnection1(Socket socket) {
////        FixAcceptorThread acceptor = threadPool.remove(); //TODO: how to return it back later?
////        if (acceptor != null) {
////            acceptor.connect(sessionID, socket);
////            executor.execute(acceptor);
////        } else {
////            socket.close();
////            LOGGER.info().append("No more connection (rejecting connection)").commit();
////        }
////        //To change body of created methods use File | Settings | File Templates.
////    }
//
//
//    public void close() {
//        active = false;
//        MultiSessionAcceptor.this.interrupt();
//    }
//
}
