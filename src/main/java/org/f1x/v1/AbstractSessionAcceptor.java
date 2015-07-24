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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class AbstractSessionAcceptor implements Runnable {

    protected static final GFLog LOGGER = GFLogFactory.getLog(AbstractSessionAcceptor.class);

    protected final String host;
    protected final int port;
    protected final int incomingConnectionQueueSize;

    protected ServerSocket serverSocket;
    protected volatile boolean active = true;

    /**
     * @param connectionQueueSize the maximum queue length for incoming connection indications
     *                                    (a request to connect). If a connection indication arrives
     *                                    when the queue is full, the connection is refused.
     */
    public AbstractSessionAcceptor(String host, int port, int connectionQueueSize) {
        this.host = host;
        this.port = port;
        this.incomingConnectionQueueSize = connectionQueueSize;
    }

    @Override
    public void run() {
        LOGGER.info().append("FIX Acceptor started on port ").append(port).commit();
        try {
            initialize();
            service();
        } catch (Exception e) {
            LOGGER.error().append("Terminating FIX Acceptor due to error").append(e).commit();
        } finally {
            shutdown();
        }
    }

    protected synchronized void initialize() throws IOException {
        serverSocket = new ServerSocket(port, incomingConnectionQueueSize, InetAddress.getByName(host));
    }

    protected void service() {
        while (active && !serverSocket.isClosed()) {
            Socket socket = acceptConnection();
            if (socket != null)
                processConnection(socket);
        }
    }

    protected void shutdown() {
        close();
        serverSocket = null;
    }

    protected Socket acceptConnection() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            if (!serverSocket.isClosed())
                LOGGER.warn().append("Error accepting inbound connection: ").append(e.getMessage()).append(e).commit();
        }

        return socket;
    }

    protected abstract void processConnection(Socket socket);

    public synchronized void close() {
        if (active) {
            active = false;
            closeServerSocket(serverSocket);
        }
    }

    protected void closeServerSocket(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.warn().append("Error closing server socket: ").append(e).commit();
            }
        }
    }

    protected void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.warn().append("Error closing socket: ").append(e).commit();
        }
    }
}
