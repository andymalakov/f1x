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
package org.f1x.api;

import org.f1x.v1.SocketOptions;

/** Defines FIX configuration parameter common for FIX Initiator and FIX Acceptor */
public class FixSettings extends SocketOptions {

    /** Max buffer size for outbound message assembler */
    private int maxOutboundMessageSize = 2048;
    /** Max buffer size for inbound message (used by Socket read) */
    private int maxInboundMessageSize = 8192;

    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    private int doubleFormatterPrecision;

    /** FIX Heartbeat interval in seconds */
    private int heartBeatIntervalSec = 30;

    /** If it > 0 then heartbeat check is performed each heartbeat check interval milliseconds.
     *  If not then heartbeat check is disabled.
     * */
    private int heartbeatCheckIntervalMs;

    /** Directory where FIX Log files will be stored */
    private String logDirectory;

    /** When <code>true</code> each LOGON will be sent with ResetSeqNum(141)=Y and sequence numbers/message store will be reset at the beginning of each connection */
    private boolean resetSequenceNumbersOnEachLogon;

    /** FIX 4.4 Has new mechanism to synchronize sequence numbers using tag NextExpectedMsgSeqNum(789). When this flag is enabled, tag NextExpectedMsgSeqNum(789) will be transmitted FIX Logon message */
    private boolean logonWithNextExpectedMsgSeqNum;
    private boolean sendRequiresConnect;


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


    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    public int getDoubleFormatterPrecision() {
        return doubleFormatterPrecision;
    }

    public void setDoubleFormatterPrecision(int doubleFormatterPrecision) {
        this.doubleFormatterPrecision = doubleFormatterPrecision;
    }

    /** Directory where FIX Log files will be stored */
    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    /** FIX Heartbeat interval in seconds (Default is 30) */
    public int getHeartBeatIntervalSec() {
        return heartBeatIntervalSec;
    }

    public void setHeartBeatIntervalSec(int heartBeatIntervalSec) {
        this.heartBeatIntervalSec = heartBeatIntervalSec;
    }

    public int getHeartbeatCheckIntervalMs() {
        return heartbeatCheckIntervalMs;
    }

    public void setHeartbeatCheckIntervalMs(int heartbeatCheckIntervalMs) {
        this.heartbeatCheckIntervalMs = heartbeatCheckIntervalMs;
    }

    /** When <code>true</code> each LOGON will be sent with ResetSeqNum(141)=Y and sequence numbers/message store will be reset at the beginning of each connection */
    public boolean isResetSequenceNumbersOnEachLogon() {
        return resetSequenceNumbersOnEachLogon;
    }

    public void setResetSequenceNumbersOnEachLogon(boolean resetSequenceNumbersOnEachLogon) {
        this.resetSequenceNumbersOnEachLogon = resetSequenceNumbersOnEachLogon;
    }

    /** When set, tag NextExpectedMsgSeqNum will be transmitted FIX Logon message */
    public boolean isLogonWithNextExpectedMsgSeqNum() {
        return logonWithNextExpectedMsgSeqNum;
    }

    public void setLogonWithNextExpectedMsgSeqNum(boolean logonWithNextExpectedMsgSeqNum) {
        this.logonWithNextExpectedMsgSeqNum = logonWithNextExpectedMsgSeqNum;
    }

    /** When this flag is enabled FixCommunicator.send() fails if session is not connected.
     * If this flag is disabled communicator will advance sequence number and store outbound message into FIX log if connection is down.
     * This will introduce outbound sequence number gap that other side may request to fill after reconnect. */
    public boolean isSendRequiresConnect() {
        return sendRequiresConnect;
    }

    public void setSendRequiresConnect(boolean sendRequiresConnect) {
        this.sendRequiresConnect = sendRequiresConnect;
    }
}
