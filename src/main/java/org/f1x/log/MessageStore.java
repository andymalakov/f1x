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

package org.f1x.log;

import org.f1x.util.ByteArrayReference;

/**
 * Goals:
 * a) Fullfill RESEND requests (usually last N messages before disconnect)
 * b) Help process Reject/BusinessReject messages
 */
public interface MessageStore {
    void record (int seqNum, byte [] message, int offset, int length);
    void reset();
    ByteArrayReference get(int seqNum);
}
