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

package org.f1x.io.disruptor;

/**
 * Interface for Message consumer
 */
public interface RingBufferBlockProcessor {
    /**
     * Called to handle single FIX message
     * @param buffer ring buffer containing the message
     * @param ringBufferSize size of ring buffer (message may be wrapped)
     * @param offset offset of the first message byte in the buffer
     * @param length message length (in bytes)
     * @return number of bytes actually processed
     */
    int process(byte[] buffer, int offset, int length, int ringBufferSize);

    void close();
}
