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

import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionEventListener;
import org.f1x.api.session.SessionState;
import org.f1x.io.InputChannel;
import org.f1x.io.InputStreamChannel;
import org.f1x.io.OutputChannel;
import org.f1x.io.OutputStreamChannel;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public abstract class FixSocketCommunicator extends FixCommunicator {

    public FixSocketCommunicator(FixVersion fixVersion, FixSettings settings, SessionEventListener eventListener) {
        super(fixVersion, settings, eventListener);
    }

    protected void connect (Socket socket) throws IOException {
        if (LOGGER.isInfoEnabled()) {
            SocketAddress address = socket.getRemoteSocketAddress();
            LOGGER.info().append("Connected to ").append(address).commit();
        }

        socket.setKeepAlive(getSettings().isSocketKeepAlive());
        socket.setTcpNoDelay(getSettings().isSocketTcpNoDelay());
        socket.setSendBufferSize(getSettings().getSocketSendBufferSize());
        socket.setReceiveBufferSize(getSettings().getSocketRecvBufferSize());

        setSessionState(SessionState.SocketConnected);
        connect(getInputChannel(socket), getOutputChannel(socket));
    }

    protected InputChannel getInputChannel (Socket socket) throws IOException {
        return new InputStreamChannel(socket.getInputStream());
    }

    protected OutputChannel getOutputChannel (Socket socket) throws IOException {
        return new OutputStreamChannel(socket.getOutputStream());
    }

}
