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

package org.f1x.api.message.fields;

// Generated by org.f1x.tools.DictionaryGenerator from QuickFIX dictionary
public enum AllocStatus implements org.f1x.api.message.types.ByteEnum {
	ACCEPTED((byte)'0'),
	BLOCK_LEVEL_REJECT((byte)'1'),
	ACCOUNT_LEVEL_REJECT((byte)'2'),
	RECEIVED((byte)'3'),
	INCOMPLETE((byte)'4'),
	REJECTED_BY_INTERMEDIARY((byte)'5');

	private final byte code;

	AllocStatus (byte code) {
		this.code  = code;
	}

	public byte getCode() { return code; }

	public static AllocStatus parse(String s) {
		switch(s) {
			case "0" : return ACCEPTED;
			case "1" : return BLOCK_LEVEL_REJECT;
			case "2" : return ACCOUNT_LEVEL_REJECT;
			case "3" : return RECEIVED;
			case "4" : return INCOMPLETE;
			case "5" : return REJECTED_BY_INTERMEDIARY;
		}
		return null;
	}

}