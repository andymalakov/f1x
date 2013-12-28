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

package org.f1x.io.socket;

import java.io.IOException;
import java.net.Socket;

/**
 *
 */
public class DefaultInboundConnectionInterceptor implements InboundConnectionInterceptor {

    private final FixNetworkingOptions networkingOptions;

    public DefaultInboundConnectionInterceptor() {
        this (new FixNetworkingOptions()); // load defaults
    }

    public DefaultInboundConnectionInterceptor(FixNetworkingOptions networkingOptions) {
        this.networkingOptions = networkingOptions;
    }


    @Override
    public boolean onNewConnection(Socket socket) throws IOException {
        socket.setTcpNoDelay(networkingOptions.isTcpNoDelay());
        socket.setKeepAlive(networkingOptions.isKeepAlive());
        return true;
    }
}
