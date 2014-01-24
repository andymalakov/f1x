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

import org.f1x.log.MessageLog;

import java.io.IOException;

public final class LoggingOutputChannel implements OutputChannel {
    private final MessageLog logger;
    private final OutputChannel delegate;

    public LoggingOutputChannel(MessageLog logger, OutputChannel delegate) {
        this.logger = logger;
        this.delegate = delegate;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        try {
            delegate.write(buffer, offset, length);
        } finally { //TODO: Should we do this in case of exception
            logger.logOutbound(buffer, 0, length); // latency first
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
