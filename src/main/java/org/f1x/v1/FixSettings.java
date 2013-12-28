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

public class FixSettings {
    private boolean logInboundMessages = false;
    private boolean logOutboundMessages = false;

    /** Max buffer size for outbound message assembler */
    private int maxOutboundMessageSize = 2048;
    /** Max buffer size for inbound message (used by Socket read) */
    private int maxInboundMessageSize = 8192;

    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    private int doubleFormatterPrecision;

    private boolean isSocketKeepAlive = true;
    private boolean isSocketTcpNoDelay = true;

    private int socketRecvBufferSize = 64*1024;
    private int socketSendBufferSize = 64*1024;

    private String logDirectory;

    /** Max buffer size for inbound message (used by Socket read) */
    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public void setMaxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
    }


    /** Max buffer size for outbound message assembler */
    public int getMaxOutboundMessageSize() {
        return maxOutboundMessageSize;
    }

    public void setMaxOutboundMessageSize(int maxOutboundMessageSize) {
        this.maxOutboundMessageSize = maxOutboundMessageSize;
    }


    public boolean isLogInboundMessages() {
        return logInboundMessages;
    }

    public void setLogInboundMessages(boolean logInboundMessages) {
        this.logInboundMessages = logInboundMessages;
    }

    public boolean isLogOutboundMessages() {
        return logOutboundMessages;
    }

    public void setLogOutboundMessages(boolean logOutboundMessages) {
        this.logOutboundMessages = logOutboundMessages;
    }

    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    public int getDoubleFormatterPrecision() {
        return doubleFormatterPrecision;
    }

    public void setDoubleFormatterPrecision(int doubleFormatterPrecision) {
        this.doubleFormatterPrecision = doubleFormatterPrecision;
    }

    public boolean isSocketKeepAlive() {
        return isSocketKeepAlive;
    }

    public boolean isSocketTcpNoDelay() {
        return isSocketTcpNoDelay;
    }

    public int getSocketRecvBufferSize() {
        return socketRecvBufferSize;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }
}
