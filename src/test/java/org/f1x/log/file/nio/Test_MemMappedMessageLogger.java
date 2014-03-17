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
package org.f1x.log.file.nio;


import org.f1x.log.file.AbstractMessageLogTest;
import org.f1x.util.TestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class Test_MemMappedMessageLogger extends AbstractMessageLogTest {

    /** Buffer fits N log messages exactly */
    @Test
    public void testEven() throws IOException {
        File logFile = tmpDir.newFile("mem.log");
        MemMappedMessageLogger logger = new MemMappedMessageLogger (logFile, 16);

        for (int i=1; i <= 4; i++) {
            String msg = String.format("M%02d", i); // logger adds \n char to each
            logger.log(true, msg.getBytes(), 0, msg.length());
        }

        logger.close();
        assertLogContent ("M01\nM02\nM03\nM04\n", logFile);
    }

    /** Buffer fits N log messages exactly */
    @Test
    public void testEvenOverflow() throws IOException {
        File logFile = tmpDir.newFile("mem.log");
        MemMappedMessageLogger logger = new MemMappedMessageLogger (logFile, 16);

        for (int i=1; i <= 5; i++) {
            String msg = String.format("M%02d", i); // logger adds \n char to each
            logger.log(true, msg.getBytes(), 0, msg.length());
        }

        logger.close();
        assertLogContent ("M05\nM02\nM03\nM04\n", logFile);
    }


    /** Buffer fits N log messages exactly */
    @Test
    public void testEvenOverflow1() throws IOException {
        File logFile = tmpDir.newFile("mem.log");
        MemMappedMessageLogger logger = new MemMappedMessageLogger (logFile, 16);

        for (int i=1; i <= 8; i++) {
            String msg = String.format("M%02d", i); // logger adds \n char to each
            logger.log(true, msg.getBytes(), 0, msg.length());
        }

        logger.close();
        assertLogContent ("M05\nM06\nM07\nM08\n", logFile);
    }

    /** Buffer overflows in the middle of the message */
    @Test
    public void testOdd() throws IOException {
        File logFile = tmpDir.newFile("mem.log");
        MemMappedMessageLogger logger = new MemMappedMessageLogger (logFile, 32);

        for (int i=1; i <= 10; i++) {
            String msg = "Message#" + i;
            logger.log(true, msg.getBytes(), 0, msg.length());
        }

        logger.close();
        assertLogContent ("e#10\n\nMessage#8\nMessage#9\nMessag", logFile);
    }


}
