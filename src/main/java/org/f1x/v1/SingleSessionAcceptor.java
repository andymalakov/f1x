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
import org.f1x.api.SessionID;

import java.io.IOException;
import java.net.Socket;

public class SingleSessionAcceptor extends ServerSocketSessionAcceptor {
    private final SessionID sessionID;
    private final FixSessionAcceptor acceptor;

    public SingleSessionAcceptor(String bindAddr, int bindPort, FixVersion fixVersion, SessionID sessionID, FixAcceptorSettings settings) {
        super(bindAddr, bindPort);
        this.sessionID = sessionID;
        this.acceptor = new FixSessionAcceptor(fixVersion, settings) {};
    }

    public SingleSessionAcceptor(String bindAddr, int bindPort, SessionID sessionID, FixSessionAcceptor acceptor) {
        super(bindAddr, bindPort);
        this.sessionID = sessionID;
        this.acceptor = acceptor;
    }

    @Override
    protected boolean processInboundConnection(Socket socket) throws IOException {
        acceptor.connect(socket, sessionID);
        acceptor.run();
        return true;
    }
}
