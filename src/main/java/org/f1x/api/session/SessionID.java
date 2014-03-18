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

package org.f1x.api.session;

/**
 * Identity of FIX Session
 */
public abstract class SessionID {

    public abstract CharSequence getSenderCompId();

    public abstract CharSequence getSenderSubId();

    public abstract CharSequence getTargetCompId();

    public abstract CharSequence getTargetSubId();

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof SessionID) {
            SessionID sessionID = (SessionID) obj;
            return equal(getSenderCompId(), sessionID.getSenderCompId()) && equal(getTargetCompId(), sessionID.getTargetCompId()) &&
                    equal(getSenderSubId(), sessionID.getSenderSubId()) && equal(getTargetSubId(), sessionID.getTargetSubId());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return hashCode(getSenderCompId()) + hashCode(getTargetCompId()) +
                hashCode(getSenderSubId()) + hashCode(getTargetSubId());
    }

    private static boolean equal(CharSequence sequence1, CharSequence sequence2) {
        if (sequence1 == sequence2)
            return true;

        if (sequence1 == null)
            return sequence2.length() == 0;
        else if(sequence2 == null)
            return sequence1.length() == 0;

        int length1 = sequence1.length();
        int length2 = sequence2.length();
        if (length1 != length2)
            return false;

        for (int index = 0; index < length1; index++)
            if (sequence1.charAt(index) != sequence2.charAt(index))
                return false;

        return true;
    }

    private static int hashCode(CharSequence sequence) {
        if (sequence == null)
            return 0;

        int h = 0;
        int length = sequence.length();
        for (int index = 0; index < length; index++)
            h = 31 * h + sequence.charAt(index);

        return h;
    }

}
