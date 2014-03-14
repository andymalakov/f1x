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

package org.f1x.log.file;

import org.f1x.SessionIDBean;
import org.f1x.api.session.SessionID;
import org.junit.Assert;
import org.junit.Test;

public class Test_SimpleFileNameGenerator {

    private SimpleFileNameGenerator fng = new SimpleFileNameGenerator();

    @Test
    public void test() {
        assertFileName("Client-Server.log", new SessionIDBean("Client", "Server"));
        assertFileName("Client-Server.log", new SessionIDBean("Client", "Trader", "Server", "Broker"));
        assertFileName("CLIENT%09A-SERVER+B.log", new SessionIDBean("CLIENT\tA", "SERVER B"));
    }

    private void assertFileName(String expectedFileName, SessionID sessionID) {
        String actualFileName = fng.getLogFile(sessionID);
        Assert.assertEquals("file name", expectedFileName, actualFileName);
    }
}
