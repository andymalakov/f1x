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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class ServerSocketSessionAcceptor implements Runnable {

    protected static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);

    private final FixAcceptorSettings settings;

    private final int bindPort;

    private volatile boolean active = true;

    public ServerSocketSessionAcceptor(int bindPort, FixAcceptorSettings settings) {
        this.bindPort = bindPort;
        this.settings = settings;
    }

    /** Dispatch inbound socket connections to FixAcceptorThreads */
    @Override
    public void run() {
        LOGGER.info().append("FIX Acceptor started on port ").append(bindPort).commit();
        try {
            ServerSocket ss = new ServerSocket (bindPort);
            ss.setSoTimeout(settings.getSocketTimeout());
            acceptInboundConnections(ss);
        } catch (Throwable e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
        }
    }

    protected void acceptInboundConnections(ServerSocket ss) {
        while (active) {
            try {
                Socket socket = ss.accept ();
                if (processInboundConnection (socket))
                    socket.close();
            } catch (Throwable e) {
                LOGGER.warn().append("Error in acceptor loop (ignoring)").append(e).commit();
            }
        }
    }

    /** @return true to close client socket (false if override takes care of the socket) */
    protected abstract boolean processInboundConnection(Socket socket) throws IOException;

    public void close() {
        active = false;
//        ServerSocketSessionAcceptor.this.interrupt();
    }

}
