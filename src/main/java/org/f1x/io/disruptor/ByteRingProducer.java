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

package org.f1x.io.disruptor;

import org.f1x.io.parsers.BodyLengthParser;
import org.f1x.util.ByteRingReader;

@Deprecated
public class ByteRingProducer implements Runnable {
    private static final int SIZE_OF_INT32 = 4;

    //8=FIX.X.X|9=A|35=?|34=?|10=123|
    //123456789012345678901234567890
    private static final int MINIMUM_FIX_MESSAGE_SIZE = 30; // minimum size of valid message (we only need to be sure this block contains MsgLen field

    // We encode length using 4 bytes (maximum size of *single* FIX message is limited to 4Gb)
    private final ByteRing ring;
    private final RingBufferBlockProcessor byteProducer;
    private final ByteRingReader reader;

    public ByteRingProducer(ByteRing ring, RingBufferBlockProcessor byteProducer) {
        this.ring = ring;
        this.reader = ring.createByteRingReader();
        this.byteProducer = byteProducer;
    }


    @Override
    public void run() {
        while (true) {  //TODO: Nicer break condition
            readSingleMessage();
        }
    }

    private void readSingleMessage () {
        // Step 1: Claim space for the prefix that will contain parsed message length and the first (MINIMUM_FIX_MESSAGE_SIZE) bytes of the message
        final int blockSize = MINIMUM_FIX_MESSAGE_SIZE + SIZE_OF_INT32;
        final long high = ring.next(blockSize);
        final long low = high - blockSize + 1;

        final int messageOffset = ring.index(low + SIZE_OF_INT32);
        ring.processBlock(low + SIZE_OF_INT32, MINIMUM_FIX_MESSAGE_SIZE, byteProducer);


        // Step 2: Parse BodyLength(9) to figure out length of entire message
        reader.reset(messageOffset, MINIMUM_FIX_MESSAGE_SIZE);
        final int remainingMessageSize = BodyLengthParser.getRemainingMessageSize(reader);

        // Step 3: Store total message length
        ring.writeInt(low, MINIMUM_FIX_MESSAGE_SIZE + remainingMessageSize);

        // Step 4: Claim space for the rest of the message
        final long high1 = ring.next(remainingMessageSize);
        final long low1 = high1 - remainingMessageSize + 1;

        ring.processBlock(low1, remainingMessageSize, byteProducer);

        ring.publish(high); // publish whole message with 4-byte length prefix
    }




}
