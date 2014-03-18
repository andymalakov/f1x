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

import java.io.Closeable;

/**
 * Message logger. Implementation must handle logInbound() and logOutbound() methods to be called concurrently
 */
public interface MessageLog extends Closeable {
    /**
     * @param isInbound message direction (true if message is inbound)
     * @param buffer    Buffer containing FIX message to log
     * @param offset    message offset in the buffer
     * @param length    message length in bytes
     */
    void log(boolean isInbound, byte[] buffer, int offset, int length);
}
