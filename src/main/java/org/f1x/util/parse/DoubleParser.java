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

package org.f1x.util.parse;

@Deprecated // keeping for reference
public class DoubleParser {
   /**
    * Parses this <code>CharSequence</code> as a <code>double</code>.
    *
    * @param  chars the character sequence to parse.
    * @return the double number represented by this character sequence.
    * @throws NumberFormatException if the character sequence does not contain
    *         a parsable <code>double</code>.
    *
    * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a> (Codehaus Plexus)
	* @version 4.6, June 22, 2003*
    */
   public static double parseDouble(CharSequence chars)
           throws NumberFormatException {
       try {
           int length = chars.length();
           double result = 0.0;
           int exp = 0;

           boolean isNegative = (chars.charAt(0) == '-') ? true : false;
           int i = (isNegative || (chars.charAt(0) == '+')) ? 1 : 0;


           // Reads decimal number.
           boolean fraction = false;
           while (true) {
               char c = chars.charAt(i);
               if ( (c == '.') && (!fraction)) {
                   fraction = true;
               } else if ((c == 'e') || (c == 'E')) {
                   break;
               } else if ((c >= '0') && (c <= '9')) {
                   result = result * 10 + (c - '0');
                   if (fraction) {
                       exp--;
                   }
               } else {
                   throw new NumberFormatException(
                       "For input characters: \"" + chars.toString() + "\"");
               }
               if (++i >= length) {
                   break;
               }
           }
           result = isNegative ? - result : result;

           // Reads exponent (if any).
           if (i < length) {
               i++;
               boolean negE = (chars.charAt(i) == '-') ? true : false;
               i = (negE || (chars.charAt(i) == '+')) ? i+1 : i;
               int valE = 0;
               while (true) {
                   char c = chars.charAt(i);
                   if ((c >= '0') && (c <= '9')) {
                       valE = valE * 10 + (c - '0');
                       if (valE > 10000000) { // Hard-limit to avoid overflow.
                           valE = 10000000;
                       }
                   } else {
                       throw new NumberFormatException(
                           "For input characters: \"" + chars.toString() +
                            "\"");
                   }
                   if (++i >= length) {
                       break;
                   }
               }
               exp += negE ? -valE : valE;
           }

           // Returns product decimal number with exponent.
           return multE(result, exp);

       } catch (IndexOutOfBoundsException e) {
           throw new NumberFormatException(
                 "For input characters: \"" + chars.toString() + "\"");
       }
   }


    /**
     * Parses this <code>CharSequence</code> as a <code>double</code>.
     *
     * @return the double number represented by this character sequence.
     * @throws NumberFormatException if the character sequence does not contain
     *         a parsable <code>double</code>.
     *
     * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a> (Codehaus Plexus)
     * @version 4.6, June 22, 2003*
     */
    public static double parseDouble(byte [] buffer, int offset, int length)
            throws NumberFormatException
    {
        try {
            double result = 0.0;
            int exp = 0;

            boolean isNegative = (buffer[offset] == '-') ? true : false;
            int i = (isNegative || (buffer[offset] == '+')) ? offset+1 : offset;

            // Reads decimal number.
            boolean fraction = false;
            while (true) {
                byte c = buffer[i];
                if ( (c == '.') && (!fraction)) {
                    fraction = true;
                } else if ((c == 'e') || (c == 'E')) {
                    break;
                } else if ((c >= '0') && (c <= '9')) {
                    result = result * 10 + (c - '0');
                    if (fraction) {
                        exp--;
                    }
                } else {
                    throw new NumberFormatException("For input characters: invalid character at position " + (i-offset));
                }
                if (++i >= length) {
                    break;
                }
            }
            result = isNegative ? - result : result;

            // Reads exponent (if any).
            if (i < length) {
                i++;
                boolean negE = (buffer[i] == '-') ? true : false;
                i = (negE || (buffer[i] == '+')) ? i+1 : i;
                int valE = 0;
                while (true) {
                    byte c = buffer[i];
                    if ((c >= '0') && (c <= '9')) {
                        valE = valE * 10 + (c - '0');
                        if (valE > 10000000) { // Hard-limit to avoid overflow.
                            valE = 10000000;
                        }
                    } else {
                        throw new NumberFormatException("For input characters: invalid character at position " + (i-offset));
                    }
                    if (++i >= length) {
                        break;
                    }
                }
                exp += negE ? -valE : valE;
            }

            // Returns product decimal number with exponent.
            return multE(result, exp);

        } catch (IndexOutOfBoundsException e) {
            throw new NumberFormatException();
        }
    }

	/**
      * Returns the product of the specified value with <code>10</code> raised
      * at the specified power exponent.
      *
      * @param  value the value.
      * @param  E the exponent.
      * @return <code>value * 10^E</code>
      */
     private static final double multE(double value, int E) {
         if (E >= 0) {
            if (E <= 308) {
                 // Max: 1.7976931348623157E+308
                return value * DOUBLE_POW_10[E];
             } else {
                 value *= 1E21; // Exact multiplicand.
                 E = Math.min(308, E-21);
                 return value * DOUBLE_POW_10[E];
             }
         } else {
             if (E >= -308) {
                 return value / DOUBLE_POW_10[-E];
             } else {
                 // Min: 4.9E-324
                 value /= 1E21; // Exact divisor.
                 E = Math.max(-308, E+21);
                 return value / DOUBLE_POW_10[-E];
             }
         }
     }

     // Note: Approximation for exponents > 21. This may introduce round-off
     //       errors (e.g. 1E23 represented as "9.999999999999999E22").
     private static final double[] DOUBLE_POW_10 = new double[] {

       1E000, 1E001, 1E002, 1E003, 1E004, 1E005, 1E006, 1E007, 1E008, 1E009,
       1E010, 1E011, 1E012, 1E013, 1E014, 1E015, 1E016, 1E017, 1E018, 1E019,
       1E020, 1E021, 1E022, 1E023, 1E024, 1E025, 1E026, 1E027, 1E028, 1E029,
       1E030, 1E031, 1E032, 1E033, 1E034, 1E035, 1E036, 1E037, 1E038, 1E039,
       1E040, 1E041, 1E042, 1E043, 1E044, 1E045, 1E046, 1E047, 1E048, 1E049,
       1E050, 1E051, 1E052, 1E053, 1E054, 1E055, 1E056, 1E057, 1E058, 1E059,
       1E060, 1E061, 1E062, 1E063, 1E064, 1E065, 1E066, 1E067, 1E068, 1E069,
       1E070, 1E071, 1E072, 1E073, 1E074, 1E075, 1E076, 1E077, 1E078, 1E079,
       1E080, 1E081, 1E082, 1E083, 1E084, 1E085, 1E086, 1E087, 1E088, 1E089,
       1E090, 1E091, 1E092, 1E093, 1E094, 1E095, 1E096, 1E097, 1E098, 1E099,

       1E100, 1E101, 1E102, 1E103, 1E104, 1E105, 1E106, 1E107, 1E108, 1E109,
       1E110, 1E111, 1E112, 1E113, 1E114, 1E115, 1E116, 1E117, 1E118, 1E119,
       1E120, 1E121, 1E122, 1E123, 1E124, 1E125, 1E126, 1E127, 1E128, 1E129,
       1E130, 1E131, 1E132, 1E133, 1E134, 1E135, 1E136, 1E137, 1E138, 1E139,
       1E140, 1E141, 1E142, 1E143, 1E144, 1E145, 1E146, 1E147, 1E148, 1E149,
       1E150, 1E151, 1E152, 1E153, 1E154, 1E155, 1E156, 1E157, 1E158, 1E159,
       1E160, 1E161, 1E162, 1E163, 1E164, 1E165, 1E166, 1E167, 1E168, 1E169,
       1E170, 1E171, 1E172, 1E173, 1E174, 1E175, 1E176, 1E177, 1E178, 1E179,
       1E180, 1E181, 1E182, 1E183, 1E184, 1E185, 1E186, 1E187, 1E188, 1E189,
       1E190, 1E191, 1E192, 1E193, 1E194, 1E195, 1E196, 1E197, 1E198, 1E199,

       1E200, 1E201, 1E202, 1E203, 1E204, 1E205, 1E206, 1E207, 1E208, 1E209,
       1E210, 1E211, 1E212, 1E213, 1E214, 1E215, 1E216, 1E217, 1E218, 1E219,
       1E220, 1E221, 1E222, 1E223, 1E224, 1E225, 1E226, 1E227, 1E228, 1E229,
       1E230, 1E231, 1E232, 1E233, 1E234, 1E235, 1E236, 1E237, 1E238, 1E239,
       1E240, 1E241, 1E242, 1E243, 1E244, 1E245, 1E246, 1E247, 1E248, 1E249,
       1E250, 1E251, 1E252, 1E253, 1E254, 1E255, 1E256, 1E257, 1E258, 1E259,
       1E260, 1E261, 1E262, 1E263, 1E264, 1E265, 1E266, 1E267, 1E268, 1E269,
       1E270, 1E271, 1E272, 1E273, 1E274, 1E275, 1E276, 1E277, 1E278, 1E279,
       1E280, 1E281, 1E282, 1E283, 1E284, 1E285, 1E286, 1E287, 1E288, 1E289,
       1E290, 1E291, 1E292, 1E293, 1E294, 1E295, 1E296, 1E297, 1E298, 1E299,

       1E300, 1E301, 1E302, 1E303, 1E304, 1E305, 1E306, 1E307, 1E308 };
}
