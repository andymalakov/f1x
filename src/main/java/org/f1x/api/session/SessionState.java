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

    void setLastLogonTimestamp(long newValue);

    /**
     * @return last logon timestamp or -1 if unknown
     */
    long getLastLogonTimestamp();


    void setNextSenderSeqNum(int newValue);

    int getNextSenderSeqNum();

    int consumeNextSenderSeqNum();


    void setNextTargetSeqNum(int newValue);

    int getNextTargetSeqNum();

    int consumeNextTargetSeqNum();

    void resetNextTargetSeqNum(int newValue) throws InvalidFixMessageException;

    void resetNextSeqNums();

}
