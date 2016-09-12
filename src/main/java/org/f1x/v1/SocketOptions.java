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

public class SocketOptions {
    private boolean isSocketKeepAlive = false;
    private boolean isSocketTcpNoDelay = true;
    private int socketTimeout = 0;
    private int socketRecvBufferSize = 64*1024;
    private int socketSendBufferSize = 64*1024;

    public boolean isSocketKeepAlive() {
        return isSocketKeepAlive;
    }

    public void setSocketKeepAlive(boolean socketKeepAlive) {
        isSocketKeepAlive = socketKeepAlive;
    }

    public void setSocketTcpNoDelay(boolean socketTcpNoDelay) {
        isSocketTcpNoDelay = socketTcpNoDelay;
    }


    public boolean isSocketTcpNoDelay() {
        return isSocketTcpNoDelay;
    }

    public int getSocketRecvBufferSize() {
        return socketRecvBufferSize;
    }

    public void setSocketRecvBufferSize(int socketRecvBufferSize) {
        this.socketRecvBufferSize = socketRecvBufferSize;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    public void setSocketSendBufferSize(int socketSendBufferSize) {
        this.socketSendBufferSize = socketSendBufferSize;
    }


    /** @return Specifies SO_TIMEOUT in milliseconds, zero means infinity */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

}
