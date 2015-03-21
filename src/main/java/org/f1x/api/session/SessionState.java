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
package org.f1x.api.session;

import org.f1x.v1.InvalidFixMessageException;

/**
 * Session state that requires persistence.
 */
public interface SessionState {

    void setNextSenderSeqNum(int newValue);
    int getNextSenderSeqNum();
    /** "increment and get" for {@link #getNextSenderSeqNum() SenderSeqNum} */
    int consumeNextSenderSeqNum();


    void setNextTargetSeqNum(int newValue);
    int getNextTargetSeqNum();
    /** "increment and get" for {@link #getNextTargetSeqNum() TargetSeqNum} */
    int consumeNextTargetSeqNum();


    void resetNextSeqNums();

    /**
     * @param newValue time of the last disconnect after successful connection to other counter-party. Measured in in milliseconds (-1 if unknown)
     */
    void setLastConnectionTimestamp(long newValue);

    /**
     * @return time of the last last disconnect after successful connection to other counter-party. Measured in in milliseconds (-1 if unknown)
     */
    long getLastConnectionTimestamp();


    /**
     * @param newValue time of the last message received by our side. Measured in in milliseconds (-1 if unknown)
     */
    void setLastReceivedMessageTimestamp(long newValue);

    /**
     * @return time of the last message received by our side. Measured in in milliseconds (-1 if unknown)
     */
    long getLastReceivedMessageTimestamp();

    /**
     * @param newValue time of the last message send from our side. Measured in in milliseconds (-1 if unknown)
     */
    void setLastSentMessageTimestamp(long newValue);

    /**
     * @return time of the last message send from our side. Measured in in milliseconds (-1 if unknown)
     */
    long getLastSentMessageTimestamp();

    /** Flushes session state (if necessary). Depending on implementation this may happen periodically or on disconnect.
     * Some implementations (in-memory and memory-mapped-file) simply ignore this call. */
    void flush();

}
