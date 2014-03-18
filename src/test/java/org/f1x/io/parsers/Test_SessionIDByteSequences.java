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
import org.f1x.SessionIDBean;
import org.f1x.api.session.SessionID;
import org.f1x.util.TestUtils;
import org.junit.Test;

public class Test_SessionIDByteSequences {

    @Test
    public void test () {
        SessionID s1 = new SessionIDBean("SenderCompID", "SenderSubID", "TargetCompID", "TargetSubID");


        SessionIDByteSequences s2 = new SessionIDByteSequences(32);
        s2.setSenderCompId (TestUtils.AsciiCharSequence2Bytes(s1.getSenderCompId()), 0, s1.getSenderCompId().length());
        s2.setSenderSubId(TestUtils.AsciiCharSequence2Bytes(s1.getSenderSubId()), 0, s1.getSenderSubId().length());
        s2.setTargetCompId(TestUtils.AsciiCharSequence2Bytes(s1.getTargetCompId()), 0, s1.getTargetCompId().length());
        s2.setTargetSubId(TestUtils.AsciiCharSequence2Bytes(s1.getTargetSubId()), 0, s1.getTargetSubId().length());


        Assert.assertEquals(s1.hashCode(), s2.hashCode());
        Assert.assertEquals(s1, s2);
    }
}
