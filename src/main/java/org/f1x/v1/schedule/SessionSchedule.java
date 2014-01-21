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
     * Method waits until next session
     * @param lastSessionTimestamp previous connection timestamp, or <code>-1</code> this is a first time connection or last connection timestamp is unknown.
     * @return <code>true</code> if this is going to be a new session, <code>false</code> if we are going to continue [previously established] session.
     */
    boolean waitForSessionStart(long lastSessionTimestamp)
        throws InterruptedException;

    /** @return current/next session end time */
    long getSessionEndTime();
}
