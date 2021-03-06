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
public enum AdvTransType implements org.f1x.api.message.types.StringEnum {
	NEW("N"),
	CANCEL("C"),
	REPLACE("R");

	private final String code;

	AdvTransType (String code) {
		this.code  = code;
		bytes = code.getBytes();
	}

	public String getCode() { return code; }

	private final byte[] bytes;
	public byte[] getBytes() { return bytes; }


	public static AdvTransType parse(String s) {
		switch(s) {
			case "N" : return NEW;
			case "C" : return CANCEL;
			case "R" : return REPLACE;
		}
		return null;
	}

}