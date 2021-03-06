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
public enum OwnerType implements org.f1x.api.message.types.IntEnum {
	INDIVIDUAL_INVESTOR(1),
	PUBLIC_COMPANY(2),
	PRIVATE_COMPANY(3),
	INDIVIDUAL_TRUSTEE(4),
	COMPANY_TRUSTEE(5),
	PENSION_PLAN(6),
	CUSTODIAN_UNDER_GIFTS_TO_MINORS_ACT(7),
	TRUSTS(8),
	FIDUCIARIES(9),
	NETWORKING_SUB_ACCOUNT(10),
	NON_PROFIT_ORGANIZATION(11),
	CORPORATE_BODY(12),
	NOMINEE(13);

	private final int code;

	OwnerType (int code) {
		this.code  = code;
	}

	public int getCode() { return code; }

	public static OwnerType parse(String s) {
		switch(s) {
			case "1" : return INDIVIDUAL_INVESTOR;
			case "2" : return PUBLIC_COMPANY;
			case "3" : return PRIVATE_COMPANY;
			case "4" : return INDIVIDUAL_TRUSTEE;
			case "5" : return COMPANY_TRUSTEE;
			case "6" : return PENSION_PLAN;
			case "7" : return CUSTODIAN_UNDER_GIFTS_TO_MINORS_ACT;
			case "8" : return TRUSTS;
			case "9" : return FIDUCIARIES;
			case "10" : return NETWORKING_SUB_ACCOUNT;
			case "11" : return NON_PROFIT_ORGANIZATION;
			case "12" : return CORPORATE_BODY;
			case "13" : return NOMINEE;
		}
		return null;
	}

}