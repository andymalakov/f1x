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
package org.f1x.api;

public class FixInitiatorSettings extends FixSettings {

    private long connectInterval = 15000;
    private long logonInterval = 5000;
    private long errorRecoveryInterval = 5000;
    private boolean isResetSequenceNumbers = true;

    public long getConnectInterval() {
        return connectInterval;
    }

    public void setConnectInterval(long connectInterval) {
        this.connectInterval = connectInterval;
    }

    public long getLogonInterval() {
        return logonInterval;
    }

    public void setLogonInterval(long logonInterval) {
        this.logonInterval = logonInterval;
    }

    public long getErrorRecoveryInterval() {
        return errorRecoveryInterval;
    }

    public void setErrorRecoveryInterval(long errorRecoveryInterval) {
        this.errorRecoveryInterval = errorRecoveryInterval;
    }

    public boolean isResetSequenceNumbers() {
        return isResetSequenceNumbers;
    }

    public void setResetSequenceNumbers(boolean resetSequenceNumbers) {
        isResetSequenceNumbers = resetSequenceNumbers;
    }
}
