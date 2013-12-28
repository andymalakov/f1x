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

package org.f1x.v0;

/**
 * Message format is invalid. For example, field is missing value, bad checksum, illegal character, unexpected message end.
 */
public class InvalidMessageFormatException extends Exception {
    public InvalidMessageFormatException(String message) {
        super(message);
    }
}
