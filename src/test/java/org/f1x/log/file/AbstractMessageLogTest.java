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
import org.f1x.log.MessageLog;
import org.f1x.util.AsciiUtils;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractMessageLogTest {
    protected static SessionID SESSION_ID = new SessionIDBean("SERVER", "CLIENT");

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    public File logDir;

    @Before
    public void cleanLogDir() throws IOException {
        //tmpDir.delete();

        logDir = tmpDir.newFolder("log");
        Assert.assertTrue("Can't create log directory " + logDir.getAbsolutePath(), logDir.exists());

        File [] files = logDir.listFiles();
        Assert.assertTrue("Can't emptry log directory " + logDir.getAbsolutePath(), files == null || files.length == 0);

    }

    protected static void log (MessageLog log, String text) {
        byte [] m = AsciiUtils.getBytes(text);

        log.log(true, TestUtils.wrap(m, 3), 3, m.length);
    }

//    protected static void logSameCharNTimes (MessageLog log, char fillChar, int count) {
//        byte [] m = new byte[count];
//        Arrays.fill(m, (byte) fillChar);
//        log.log(true, wrap(m, 3), 3, m.length);
//    }

    protected static String fillString (char fillChar, int count) {
        byte [] m = new byte[count];
        Arrays.fill(m, (byte) fillChar);
        return new String (m);
    }


    protected static void assertLogContent (String expectedContent, File logFile) {
        String actualContent = TestUtils.readSmallFile(logFile, 128);
        Assert.assertEquals("Content of " + logFile.getName(), expectedContent, actualContent);
    }

}
