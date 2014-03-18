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

package org.f1x;

import org.f1x.api.session.SessionID;

public class SessionIDBean extends SessionID {

    private String senderCompId;
    private String senderSubId;
    private String targetCompId;
    private String targetSubId;

    public SessionIDBean() {
    }

    public SessionIDBean(String senderCompId, String targetCompId) {
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
    }

    public SessionIDBean(String senderCompId, String senderSubId, String targetCompId, String targetSubId) {
        this.senderCompId = senderCompId;
        this.senderSubId = senderSubId;
        this.targetCompId = targetCompId;
        this.targetSubId = targetSubId;
    }

    public SessionIDBean(SessionID sessionID){
        this(sessionID.getSenderCompId().toString(), sessionID.getSenderSubId().toString(),
                sessionID.getTargetCompId().toString(), sessionID.getTargetSubId().toString());
    }

    @Override
    public String getSenderCompId() {
        return senderCompId;
    }

    public void setSenderCompId(String senderCompId) {
        this.senderCompId = senderCompId;
    }

    @Override
    public String getSenderSubId() {
        return senderSubId;
    }

    public void setSenderSubId(String senderSubId) {
        this.senderSubId = senderSubId;
    }

    @Override
    public String getTargetCompId() {
        return targetCompId;
    }

    public void setTargetCompId(String targetCompId) {
        this.targetCompId = targetCompId;
    }

    @Override
    public String getTargetSubId() {
        return targetSubId;
    }

    public void setTargetSubId(String targetSubId) {
        this.targetSubId = targetSubId;
    }

    public void clear(){
        setSenderCompId(null);
        setSenderSubId(null);
        setTargetCompId(null);
        setTargetSubId(null);
    }

    @Override
    public String toString() {
        return senderCompId + '-' + targetCompId;
    }

}
