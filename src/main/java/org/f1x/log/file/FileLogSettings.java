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

package org.f1x.log.file;

public class FileLogSettings {

    private String logDir;
    private int flushPeriod = 15000;
    private int fileBufferSize = 8192;
    private int maxLogSize = 0;
    private boolean daily;

    public FileLogSettings() {
        this.logDir = logDir;
    }

    public FileLogSettings(String logDir) {
        this.logDir = logDir;
    }

    /** @return directory where log files will reside */
    public String getLogDir() {
        return logDir;
    }

    /** @return directory where log files will reside */
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }


    /** Most loggers use buffers. This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing.  */
    public int getFlushPeriod() {
        return flushPeriod;
    }

    /** Most loggers use buffers. This setting determines how often logger flushes the buffer (in milliseconds). Negative or zero value disables periodic flushing. */
    public void setFlushPeriod(int flushPeriod) {
        this.flushPeriod = flushPeriod;
    }


    /** For stream-oriented loggers this setting defines buffer size. Default recommended value is 8192. */
    public int getFileBufferSize() {
        return fileBufferSize;
    }

    /** For stream-oriented loggers this setting defines buffer size. Default recommended value is 8192. */
    public void setFileBufferSize(int fileBufferSize) {
        this.fileBufferSize = fileBufferSize;
    }

    /**
     * This setting defines maximum size of log file. Leave at zero to disable this feature.
     * NOTE: logger is implemented using memory-mapped file. Specifying large value will cause page faults.
     */
    public int getMaxLogSize() {
        return maxLogSize;
    }

    public void setMaxLogSize(int maxLogSize) {
        this.maxLogSize = maxLogSize;
    }

    /** This settings enables logger that starts a new log file each midnight (local TZ) */
    public boolean isDaily() {
        return daily;
    }

    public void setDaily(boolean daily) {
        this.daily = daily;
    }
}
