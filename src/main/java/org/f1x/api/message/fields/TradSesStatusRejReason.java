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
public enum TradSesStatusRejReason implements org.f1x.api.message.types.ByteEnum {
	UNKNOWN_OR_INVALID_TRADINGSESSIONID((byte)'1');

	private final byte code;

	TradSesStatusRejReason (byte code) {
		this.code  = code;
	}

	public byte getCode() { return code; }

	public static TradSesStatusRejReason parse(String s) {
		switch(s) {
			case "1" : return UNKNOWN_OR_INVALID_TRADINGSESSIONID;
		}
		return null;
	}

}