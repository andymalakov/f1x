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
import java.io.OutputStream;

public final class OutputStreamChannel implements OutputChannel {
    private final OutputStream os;

    public OutputStreamChannel(OutputStream os) {
        this.os = os;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        os.write(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
