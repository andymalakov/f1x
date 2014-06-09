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

package org.f1x.io;

import java.io.IOException;


public class TextOutputChannel implements OutputChannel {

    protected StringBuilder sb = new StringBuilder();

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        sb.append (new String (buffer, offset, length));
    }

    @Override
    public void close() throws IOException {
        sb.setLength(0);
    }

    @Override
    public String toString() {
        String result = sb.toString().replaceAll("\u0001", "|");
        sb.setLength(0);
        return result;
    }
}
