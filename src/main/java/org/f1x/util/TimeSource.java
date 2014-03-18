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

package org.f1x.util;

/** Real time operations that allow mockup implementation in unit testing */
public interface TimeSource {
    /**
     * Similar to <code>System.currentTimeMillis</code>.
     *
     * @return  the difference, measured in milliseconds, between
     *          the current time and midnight, January 1, 1970 UTC.
     */
    long currentTimeMillis();

    /**
     * Similar to <code>Thread.sleep()</code>
     * @param  millis the length of time to sleep in milliseconds
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     */
    void sleep(long millis) throws InterruptedException;
}
