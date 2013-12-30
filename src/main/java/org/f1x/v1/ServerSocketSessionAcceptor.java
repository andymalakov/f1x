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

import org.f1x.io.socket.BindInterceptor;
import org.f1x.io.socket.ConnectionInterceptor;
import org.gflogger.GFLog;
import org.gflogger.GFLogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class ServerSocketSessionAcceptor implements Runnable {

    protected static final GFLog LOGGER = GFLogFactory.getLog(MultiSessionAcceptor.class);

    private BindInterceptor bindInterceptor;
    private ConnectionInterceptor connectionInterceptor;
    private final int bindPort;
    private final String bindAddress;

    private volatile boolean active = true;
    private volatile ServerSocket ss = null;

    /**
     *
     * @param bindAddress the port number, or <code>0</code> to use a port number that is automatically allocated
     * @param bindPort the local InetAddress the server will bind to (pass <code>null</code> to accept connections on any/all local addresses)
     */
    public ServerSocketSessionAcceptor(String bindAddress, int bindPort) {
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
    }

    public void setConnectionInterceptor(ConnectionInterceptor connectionInterceptor) {
        this.connectionInterceptor = connectionInterceptor;
    }

    public void setBindInterceptor(BindInterceptor bindInterceptor) {
        this.bindInterceptor = bindInterceptor;
    }

    /** Dispatch inbound socket connections to FixAcceptorThreads */
    @Override
    public void run() {
        LOGGER.info().append("FIX Acceptor started on port ").append(bindPort).commit();
        try {
            if (bindAddress != null)
                ss = new ServerSocket (bindPort, 50, InetAddress.getByName(bindAddress));
            else
                ss = new ServerSocket (bindPort);

            if (bindInterceptor != null)
                bindInterceptor.onBind(ss);
            acceptInboundConnections(ss);
        } catch (Throwable e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
        }
    }

    protected void acceptInboundConnections(ServerSocket ss) {
        while (active) {
            try {
                final Socket socket = ss.accept();
                if (connectionInterceptor != null && ! connectionInterceptor.onNewConnection(socket)) {
                    socket.close();
                    continue;
                }

                if (processInboundConnection (socket))
                    socket.close(); // otherwise acceptor spawns a thread that will be responsible for this connection

            } catch (Throwable e) {
                LOGGER.warn().append("Error in acceptor loop (ignoring)").append(e).commit();
            }
        }
    }

    /** @return true to close client socket (false if override takes care of the socket) */
    protected abstract boolean processInboundConnection(Socket socket) throws IOException;

    public void close() {
        active = false;

        ServerSocket ss = this.ss;
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException e) {
                LOGGER.warn().append("Error closing server socket").append(e).commit();
            }
        }
    }

}
