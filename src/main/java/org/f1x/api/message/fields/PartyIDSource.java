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
public enum PartyIDSource implements org.f1x.api.message.types.ByteEnum {
	BIC((byte)'B'),
	GENERALLY_ACCEPTED_MARKET_PARTICIPANT_IDENTIFIER((byte)'C'),
	PROPRIETARY_CUSTOM_CODE((byte)'D'),
	ISO_COUNTRY_CODE((byte)'E'),
	SETTLEMENT_ENTITY_LOCATION((byte)'F'),
	MIC((byte)'G'),
	CSD_PARTICIPANT_MEMBER_CODE((byte)'H'),
	KOREAN_INVESTOR_ID((byte)'1'),
	TAIWANESE_QUALIFIED_FOREIGN_INVESTOR_ID_QFII_FID((byte)'2'),
	TAIWANESE_TRADING_ACCOUNT((byte)'3'),
	MALAYSIAN_CENTRAL_DEPOSITORY_NUMBER((byte)'4'),
	CHINESE_B_SHARE((byte)'5'),
	UK_NATIONAL_INSURANCE_OR_PENSION_NUMBER((byte)'6'),
	US_SOCIAL_SECURITY_NUMBER((byte)'7'),
	US_EMPLOYER_IDENTIFICATION_NUMBER((byte)'8'),
	AUSTRALIAN_BUSINESS_NUMBER((byte)'9'),
	AUSTRALIAN_TAX_FILE_NUMBER((byte)'A'),
	DIRECTED_BROKER((byte)'I');

	private final byte code;

	PartyIDSource (byte code) {
		this.code  = code;
	}

	public byte getCode() { return code; }

	public static PartyIDSource parse(String s) {
		switch(s) {
			case "B" : return BIC;
			case "C" : return GENERALLY_ACCEPTED_MARKET_PARTICIPANT_IDENTIFIER;
			case "D" : return PROPRIETARY_CUSTOM_CODE;
			case "E" : return ISO_COUNTRY_CODE;
			case "F" : return SETTLEMENT_ENTITY_LOCATION;
			case "G" : return MIC;
			case "H" : return CSD_PARTICIPANT_MEMBER_CODE;
			case "1" : return KOREAN_INVESTOR_ID;
			case "2" : return TAIWANESE_QUALIFIED_FOREIGN_INVESTOR_ID_QFII_FID;
			case "3" : return TAIWANESE_TRADING_ACCOUNT;
			case "4" : return MALAYSIAN_CENTRAL_DEPOSITORY_NUMBER;
			case "5" : return CHINESE_B_SHARE;
			case "6" : return UK_NATIONAL_INSURANCE_OR_PENSION_NUMBER;
			case "7" : return US_SOCIAL_SECURITY_NUMBER;
			case "8" : return US_EMPLOYER_IDENTIFICATION_NUMBER;
			case "9" : return AUSTRALIAN_BUSINESS_NUMBER;
			case "A" : return AUSTRALIAN_TAX_FILE_NUMBER;
			case "I" : return DIRECTED_BROKER;
		}
		return null;
	}

}