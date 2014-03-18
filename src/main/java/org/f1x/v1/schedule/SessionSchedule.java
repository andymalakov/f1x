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

package org.f1x.v1.schedule;

/**
 * FIX Session schedule.
 * This interface is responsible for questions:
 * <ol>
 * <li>When is the right time start new session or resume current one?</li>
 * <li>When is the current session must end?</li>
 * </ol>
 */
public interface SessionSchedule {
    /**
     * If Schedule allows FIX session at current time, this method returns immediately. Otherwise this method blocks until it is time to establish new FIX session.
     * @param lastConnectionTimestamp previous connection timestamp, or <code>-1</code> this is a first time connection or timestamp of the last connection is unknown.
     * @return Session end time (in Java 'epoch' time format) with a twist: result sign is used to indicate if new connection will continue previously running session (+) or it will open a new FIX session according to this FIX schedule (-).
     *         That is negative result can be used that FIX sequence numbers may require reset. In both cases, code like <code>Thread.sleep(Math.abs(result))</code> will sleep until it is time to finish this session.
     *
     */
    long waitForSessionStart(long lastConnectionTimestamp)
        throws InterruptedException;
}
