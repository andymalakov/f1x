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

import org.junit.Assert;
import org.f1x.api.session.SessionID;
import org.f1x.util.AsciiUtils;
import org.junit.Test;

public class Test_DefaultLogonSessionParser {

    @Test
    public void simple() {
        DefaultLogonSessionParser parser = new DefaultLogonSessionParser();


        String message = "8=FIX.4.4|9=82|35=A|34=1|49=CLIENT|52=20140101-10:10:10.100|56=SERVER|98=0|108=30|141=Y|383=8192|10=080|";
        message = message.replace('|', '\u0001');
        byte [] messageBytes = AsciiUtils.getBytes(message);
        SessionID sessionID = parser.parse(messageBytes, 0, messageBytes.length);

        Assert.assertNotNull(sessionID);
        Assert.assertEquals("CLIENT",   sessionID.getSenderCompId().toString());
        Assert.assertEquals("",         sessionID.getSenderSubId().toString());
        Assert.assertEquals("SERVER",   sessionID.getTargetCompId().toString());
        Assert.assertEquals("",         sessionID.getTargetSubId().toString());
    }
}
