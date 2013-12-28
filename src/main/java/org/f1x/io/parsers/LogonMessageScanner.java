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

package org.f1x.io.parsers;

import org.f1x.api.FixParserException;
import org.f1x.util.AsciiUtils;

import java.util.Arrays;

/**
 * Micro-parser to extract session identity from LOGON message. Thread-safe.
 */
public abstract class LogonMessageScanner<Cookie> {

    private static final byte [] HEADER_START = AsciiUtils.getBytes("8=FIX.");
    private static final int HEADER_START_LENGTH = HEADER_START.length;
    private static final byte SOH = 1;
    private static final byte EQUALS = '=';
    private static final int BODY_LENGTH_TAG_NUM = 9;

    public void scan (byte [] buffer, int start, int len, Cookie cookie) throws FixParserException {
        if (len == 0)
            throw new IllegalArgumentException();

        if (len < HEADER_START_LENGTH || ! AsciiUtils.equals(HEADER_START, buffer, start, HEADER_START_LENGTH))
            throw new FixParserException("Invalid Logon message: message should begin with standard FIX prefix");


        start += HEADER_START_LENGTH + 4; // X.X|

        int bodyLength = 0;
        int currentTagNum = 0;
        int currentTagValueStart = 0;
        boolean parsingTagNum = true;
        final int cnt = len + start;
        for (int i=start; i < cnt; i++) {
            byte b = buffer[i];
            if (parsingTagNum) {
                if (bodyLength > 0 && i >= bodyLength)
                    return; // reached the end

                if (b == EQUALS) {
                    if (onTagNumber(currentTagNum, cookie)) {
                        currentTagValueStart = i+1;
                    }

                    parsingTagNum = false;
                } else {
                    currentTagNum = 10*currentTagNum + parseDigit(i, b);
                }
            } else {
                if (b == SOH) {
                    if (currentTagValueStart > 0) {
                        if ( ! onTagValue(currentTagNum, buffer, currentTagValueStart, i - currentTagValueStart, cookie))
                            return;

                        currentTagValueStart = -1;
                    }
                    if (currentTagNum == BODY_LENGTH_TAG_NUM)
                        bodyLength += i; // number of characters in the message following the BodyLength field up to, and including, the delimiter immediately preceding the CheckSum tag ("10=")
                    parsingTagNum = true;
                    currentTagNum = 0;
                } else {
                    if (currentTagNum == BODY_LENGTH_TAG_NUM) {
                        bodyLength = 10*bodyLength + parseDigit(i, b);
                    }
                }
            }
        }
        throw new FixParserException("Invalid Logon message: message exceeds " + len + " bytes or missing BodyLength(9) tag");

    }

    /** @return true to process value of this tag (get onTagValue callaback) or false to skip this tag */
    protected abstract boolean onTagNumber(int tagNum, Cookie cookie) throws FixParserException;

    /** @return true to continue parsing message or false to skip the rest tags */
    protected abstract boolean onTagValue(int tagNum, byte [] message, int tagValueStart, int tagValueLen, Cookie cookie) throws FixParserException;

    private static int parseDigit(int i, byte b) throws FixParserException {
        int digit = b - '0';
        if (digit < 0 || digit > 9)
            throw new FixParserException("Invalid Logon message: non-numeric character appears in tag number (position " + i + ')');
        return digit;
    }

}
