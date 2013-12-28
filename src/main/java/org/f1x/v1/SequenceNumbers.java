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

import java.util.concurrent.atomic.AtomicInteger;

final class SequenceNumbers {
    private final AtomicInteger inboundSeqNumber = new AtomicInteger(0); //last used sequence number
    private final AtomicInteger outboundSeqNumber = new AtomicInteger(0); //last used sequence number

    public void reset() {
        outboundSeqNumber.set(0);
        inboundSeqNumber.set(0);
    }

    public int consumeOutbound() {
        return outboundSeqNumber.incrementAndGet();
    }

    public void resetInbound(int inboundSeqNum) throws InvalidFixMessageException {
        if (inboundSeqNum < inboundSeqNumber.get())
            throw InvalidFixMessageException.RESET_BELOW_CURRENT_SEQ_LARGE;

        inboundSeqNumber.set(inboundSeqNum);
    }
}
