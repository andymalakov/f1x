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
     * Usage example:
     * <pre>
     * long currentTime = System.currentTimeMillis();
     * SessionTimes sessionTimes = schedule.getCurrentSessionTimes()
     * if (currentTime > sessionTimes.getStart()) {
     *     ///start session immediately, set timer to stop session at sessionTimes.getEnd()
     * } else {
     *     ///set timer to start session at sessionTimes.getStart() and end it some time later at sessionTimes.getEnd()
     * }
     * </pre>
     *
     * @return exact times of the current session (if method is called during active session hours) or the next session (if method is called after session hours).
     * @param currentTime Current time which can be used by implementation to set calendar. For example, {@link org.f1x.util.TimeSource#currentTimeMillis()}.
     */
    SessionTimes getCurrentSessionTimes(long currentTime);
}
