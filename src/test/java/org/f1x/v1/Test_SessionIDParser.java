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

import org.f1x.util.AsciiUtils;
import org.junit.Assert;
import org.junit.Test;

public class Test_SessionIDParser {

    @Test
    public void testEmptySessionID() throws Exception {
        String message = "8=FIX.4.4|9=82|35=A|34=1|52=20140101-10:10:10.100|98=0|108=30|141=Y|383=8192|10=080|";
        assertSessionID(false, "", "", "", "", message);
    }

    @Test
    public void testCompletelyFilledSessionID() throws Exception {
        String message = "8=FIX.4.4|9=82|35=A|34=1|49=SC|50=SS|52=20140101-10:10:10.100|56=TC|57=TS|98=0|108=30|141=Y|383=8192|10=080|";
        assertSessionID(false, "SC", "SS", "TC", "TS", message);
        assertSessionID(true, "TC", "TS", "SC", "SS", message);
    }

    private static void assertSessionID(boolean opposite, String senderCompID, String senderSubID, String targetCompID, String targetSubID, String message) {
        message = message.replace('|', '\u0001');
        byte[] messageBytes = AsciiUtils.getBytes(message);
        SessionIDByteReferences sessionID = new SessionIDByteReferences();
        if (opposite)
            SessionIDParser.parseOpposite(messageBytes, 0, messageBytes.length, sessionID);
        else
            SessionIDParser.parse(messageBytes, 0, messageBytes.length, sessionID);

        Assert.assertEquals(senderCompID, sessionID.getSenderCompId().toString());
        Assert.assertEquals(senderSubID, sessionID.getSenderSubId().toString());
        Assert.assertEquals(targetCompID, sessionID.getTargetCompId().toString());
        Assert.assertEquals(targetSubID, sessionID.getTargetSubId().toString());
    }

}
