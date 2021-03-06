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
public enum PosReqResult implements org.f1x.api.message.types.IntEnum {
	VALID_REQUEST(0),
	INVALID_OR_UNSUPPORTED_REQUEST(1),
	NO_POSITIONS_FOUND_THAT_MATCH_CRITERIA(2),
	NOT_AUTHORIZED_TO_REQUEST_POSITIONS(3),
	REQUEST_FOR_POSITION_NOT_SUPPORTED(4),
	OTHER(99);

	private final int code;

	PosReqResult (int code) {
		this.code  = code;
	}

	public int getCode() { return code; }

	public static PosReqResult parse(String s) {
		switch(s) {
			case "0" : return VALID_REQUEST;
			case "1" : return INVALID_OR_UNSUPPORTED_REQUEST;
			case "2" : return NO_POSITIONS_FOUND_THAT_MATCH_CRITERIA;
			case "3" : return NOT_AUTHORIZED_TO_REQUEST_POSITIONS;
			case "4" : return REQUEST_FOR_POSITION_NOT_SUPPORTED;
			case "99" : return OTHER;
		}
		return null;
	}

}