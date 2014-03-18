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

import org.f1x.api.FixSettings;
import org.f1x.api.FixVersion;
import org.f1x.api.session.SessionID;
import org.f1x.api.session.SessionStatus;
import org.f1x.io.EmptyInputChannel;
import org.f1x.io.InputChannel;
import org.f1x.io.OutputChannel;
import org.f1x.io.socket.ConnectionInterceptor;
import org.f1x.util.TimeSource;

import java.io.IOException;

class TestFixCommunicator extends FixCommunicator {
    private final SessionID sessionID;

    public TestFixCommunicator(SessionID sessionID, TimeSource timeSource, InputChannel in, OutputChannel out) {
        super(FixVersion.FIX44, new FixSettings(), timeSource);

        this.sessionID = sessionID;

        connect (in, out);
        setSessionStatus(SessionStatus.ApplicationConnected);
    }


    public TestFixCommunicator(SessionID sessionID, TimeSource timeSource) {
        super(FixVersion.FIX44, new FixSettings(), timeSource);
        this.sessionID = sessionID;
    }

    @Override
    public void setConnectionInterceptor(ConnectionInterceptor connectionInterceptor) {
    }

    @Override
    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    protected void processInboundLogon(boolean isSequenceNumberReset) throws IOException {
    }

    @Override
    public void run() {
    }
}
