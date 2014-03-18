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

package org.f1x.io.parsers;

import org.f1x.api.session.SessionID;
import org.f1x.util.MutableByteSequence;

/**
 * Mutable implementation of SessionID. Used for initial session lookup.
 */
public class SessionIDByteSequences extends SessionID {

    private final MutableByteSequence senderCompId;
    private final MutableByteSequence senderSubId;
    private final MutableByteSequence targetCompId;
    private final MutableByteSequence targetSubId;

    public SessionIDByteSequences(int maximumCompIDByteCount) {
        this.senderCompId = new MutableByteSequence(maximumCompIDByteCount);
        this.senderSubId = new MutableByteSequence(maximumCompIDByteCount);
        this.targetCompId = new MutableByteSequence(maximumCompIDByteCount);
        this.targetSubId = new MutableByteSequence(maximumCompIDByteCount);
    }

    @Override
    public CharSequence getSenderCompId() {
        return senderCompId;
    }

    public void setSenderCompId(byte [] buffer, int offset, int length) {
        senderCompId.set(buffer, offset, length);
    }

    @Override
    public CharSequence getSenderSubId() {
        return senderSubId;
    }

    public void setSenderSubId(byte [] buffer, int offset, int length) {
        senderSubId.set(buffer, offset, length);
    }

    @Override
    public CharSequence getTargetCompId() {
        return targetCompId;
    }

    public void setTargetCompId(byte [] buffer, int offset, int length) {
        targetCompId.set(buffer, offset, length);
    }

    @Override
    public CharSequence getTargetSubId() {
        return targetSubId;
    }

    public void setTargetSubId(byte [] buffer, int offset, int length) {
        targetSubId.set(buffer, offset, length);
    }

    public void clear(){
        senderCompId.clear();
        senderSubId.clear();
        targetCompId.clear();
        targetSubId.clear();
    }

}
