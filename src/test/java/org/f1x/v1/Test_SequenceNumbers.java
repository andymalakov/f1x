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


import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.fail;

public class Test_SequenceNumbers {

    private SequenceNumbers seqNum = new SequenceNumbers();

    @Test
    public void test() throws InvalidFixMessageException {
        Assert.assertEquals(1, seqNum.getNextInbound());
        Assert.assertEquals(1, seqNum.getNextOutbound());

        Assert.assertEquals(1, seqNum.consumeOutbound());
        Assert.assertEquals(2, seqNum.consumeOutbound());
        Assert.assertEquals(3, seqNum.consumeOutbound());

        Assert.assertEquals(4, seqNum.getNextOutbound());


        Assert.assertEquals(1, seqNum.consumeInbound());
        Assert.assertEquals(2, seqNum.consumeInbound());
        Assert.assertEquals(3, seqNum.consumeInbound());

        Assert.assertEquals(4, seqNum.getNextInbound());

        try {
            seqNum.resetInbound(3);
            fail("Failed to detect sequence number decrease");
        } catch(Exception expected) {
        }

        seqNum.resetInbound(4);
        Assert.assertEquals(4, seqNum.getNextInbound());

        seqNum.resetInbound(5);
        Assert.assertEquals(5, seqNum.getNextInbound());
    }

}
