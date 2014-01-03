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
public enum TrdRegTimestampType implements org.f1x.api.message.types.ByteEnum {
	EXECUTION_TIME((byte)'1'),
	TIME_IN((byte)'2'),
	TIME_OUT((byte)'3'),
	BROKER_RECEIPT((byte)'4'),
	BROKER_EXECUTION((byte)'5');

	private final byte code;

	TrdRegTimestampType (byte code) {
		this.code  = code;
	}

	public byte getCode() { return code; }

	public static TrdRegTimestampType parse(String s) {
		switch(s) {
			case "1" : return EXECUTION_TIME;
			case "2" : return TIME_IN;
			case "3" : return TIME_OUT;
			case "4" : return BROKER_RECEIPT;
			case "5" : return BROKER_EXECUTION;
		}
		return null;
	}

}