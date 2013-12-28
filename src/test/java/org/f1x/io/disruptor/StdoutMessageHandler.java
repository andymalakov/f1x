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

class StdoutMessageHandler implements RingBufferBlockProcessor {
    private static final char SOH = 1;
    private final StringBuilder sb = new StringBuilder();


    @Override
    public int process(byte[] buffer, int offset, int length, int bufferSize) {
        sb.setLength(0);
        sb.append('[');
        for (int i=0; i < length; i++) {
            char c = (char) buffer[(offset+i) % bufferSize];
            if (c == SOH)
                c = '|';
            sb.append(c);
        }
        sb.append(']');
        System.out.println(sb.toString());
        return length;
    }

    @Override
    public void close() {
    }
}
