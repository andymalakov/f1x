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
package org.f1x.v1;

import org.f1x.api.session.SessionID;
import org.f1x.util.AsciiUtils;
import org.f1x.v1.SessionIDByteReferences;
import org.junit.Assert;
import org.junit.Test;

public class Test_SessionIDByteReferences {

    @Test
    public void testEmptySessionID() {
        SessionIDByteReferences sessionID = new SessionIDByteReferences();
        assertSessionID("", "", "", "", sessionID);
    }

    @Test
    public void testCompletelyFilledSessionID() throws Exception {
        byte[] array = AsciiUtils.getBytes("SC-SS-TC-TS");
        SessionIDByteReferences sessionID = new SessionIDByteReferences();
        sessionID.setSenderCompId(array, 0, 2);
        sessionID.setSenderSubId(array, 3, 2);
        sessionID.setTargetCompId(array, 6, 2);
        sessionID.setTargetSubId(array, 9, 2);
        assertSessionID("SC", "SS", "TC", "TS", sessionID);
    }

    private static void assertSessionID(String senderCompID, String senderSubID, String targetCompID, String targetSubID, SessionID sessionID) {
        Assert.assertEquals(senderCompID, sessionID.getSenderCompId().toString());
        Assert.assertEquals(senderSubID, sessionID.getSenderSubId().toString());
        Assert.assertEquals(targetCompID, sessionID.getTargetCompId().toString());
        Assert.assertEquals(targetSubID, sessionID.getTargetSubId().toString());
    }

}
