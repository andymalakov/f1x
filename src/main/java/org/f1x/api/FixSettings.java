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

public class FixSettings extends SocketOptions {
    private boolean logInboundMessages = false;
    private boolean logOutboundMessages = false;

    /** Max buffer size for outbound message assembler */
    private int maxOutboundMessageSize = 2048;
    /** Max buffer size for inbound message (used by Socket read) */
    private int maxInboundMessageSize = 8192;

    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    private int doubleFormatterPrecision;

    /** FIX Heartbeat interval in seconds */
    private int heartBeatIntervalSec = 30;

    private String logDirectory;
    private boolean resetSequenceNumbersOnEachLogon;
    private boolean logonWithNextExpectedMsgSeqNum;

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


//    public boolean isLogInboundMessages() {
//        return logInboundMessages;
//    }
//
//    public void setLogInboundMessages(boolean logInboundMessages) {
//        this.logInboundMessages = logInboundMessages;
//    }
//
//    public boolean isLogOutboundMessages() {
//        return logOutboundMessages;
//    }
//
//    public void setLogOutboundMessages(boolean logOutboundMessages) {
//        this.logOutboundMessages = logOutboundMessages;
//    }

    /** Defines how floating point numbers will be formatted in FIX messages. This parameter sets maximum number of digits after decimal point (e.g. 3). Truncated part will be rounded. */
    public int getDoubleFormatterPrecision() {
        return doubleFormatterPrecision;
    }

    public void setDoubleFormatterPrecision(int doubleFormatterPrecision) {
        this.doubleFormatterPrecision = doubleFormatterPrecision;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    /** FIX Heartbeat interval in seconds */
    public int getHeartBeatIntervalSec() {
        return heartBeatIntervalSec;
    }

    public void setHeartBeatIntervalSec(int heartBeatIntervalSec) {
        this.heartBeatIntervalSec = heartBeatIntervalSec;
    }

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
}
