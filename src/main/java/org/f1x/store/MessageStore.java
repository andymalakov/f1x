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

package org.f1x.store;

import org.f1x.util.MutableByteSequence;

/**
 * Repository of outbound messages. Implementations may keep last N messages or last M kilobytes of outbound traffic.
 *
 * Thread-safe.
 *
 * Goals:
 * a) Fullfill RESEND requests (usually last N messages before disconnect)
 * b) Help process Reject/BusinessReject messages by retrieving previously sent messages that were rejected.
 */
public interface MessageStore {

    /** Stores message with given sequence number from provided buffer */
    void put (int seqNum, byte [] message, int offset, int length);

    /** Cleans message store (usually after FIX sequence numbers reset) */
    void clean();

    /**
     * Retrieves a single outbound message with given sequence number into provided result buffer.
     *
     * @return message sequence number or -1 if message with given sequence number is not found.
     * @throws ArrayIndexOutOfBoundsException if provided buffer is not enough to fit the message
     */
    int get(int seqNum, byte[] buffer);

    /**
     * Retrieves a group of messages with given sequence number range (inclusive). Result is ordered by sequence number. Gaps are possible.
     * Some implementations keep only M latest messages, in which case result will start will the oldest message that is still in the store.
     * Moreover, store can be modified during iteration and wipe out some of the messages (in which case iterator will also return gaps).
     * @return never null. Returned instance is for single thread use (not thread-safe)
     */
    MessageStoreIterator iterator(int fromSeqNum, int toSeqNum);

    /** See {@link MessageStore#iterator(int, int)} */
    interface MessageStoreIterator {
        /**
         * Retrieves a single message from range specified during iterator creation.
         * @return message sequence number or -1 if there are no more messages
         * @throws ArrayIndexOutOfBoundsException if provided buffer is not enough to fit the message
         */
        int next( byte[] buffer);
    }
}
