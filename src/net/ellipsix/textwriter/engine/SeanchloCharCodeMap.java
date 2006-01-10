/*
 * SeanchloCharCodeMap.java
 *
 * Created on September 10, 2005, 11:46 PM
 */

/*
 * The content of this file is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this file; if not, write to
 *
 * Free Software Foundation, Inc.
 * 59 Temple Place, Suite 330
 * Boston, MA 02111-1307 USA
 *
 * or download the license from the Free Software Foundation website at
 *
 * http://www.gnu.org/licenses/gpl.html
 */

package net.ellipsix.textwriter.engine;

import java.io.Serializable;
import java.util.Arrays;

/** 
 * Defines the set of LCIs corresponding to the dotted consonants and other
 * nonstandard characters used in seanchlo print. 
 *
 * @author David Zaslavsky
 */
// An instance of this can be created for a specific font name or font class name
// It will contain the character codes for each of the dotted consonants and the & sign
public class SeanchloCharCodeMap implements CharacterCodeMap , Serializable {
    // These numbers represent sorted order by Unicode code point
    public static final int DOTTED_C = 0x010a;
    public static final int DOTTED_c = 0x010b;
    public static final int DOTTED_G = 0x0120;
    public static final int DOTTED_g = 0x0121;
    public static final int SEANCHLO_s = 0x017f;
    public static final int SEANCHLO_r = 0x027c;
    public static final int DOTTED_B = 0x1e02;
    public static final int DOTTED_b = 0x1e03;
    public static final int DOTTED_D = 0x1e0a;
    public static final int DOTTED_d = 0x1e0b;
    public static final int DOTTED_F = 0x1e1e;
    public static final int DOTTED_f = 0x1e1f;
    public static final int DOTTED_M = 0x1e40;
    public static final int DOTTED_m = 0x1e41;
    public static final int DOTTED_P = 0x1e56;
    public static final int DOTTED_p = 0x1e57;
    public static final int DOTTED_S = 0x1e60;
    public static final int DOTTED_s = 0x1e61;
    public static final int DOTTED_T = 0x1e6a;
    public static final int DOTTED_t = 0x1e6b;
    public static final int DOTTED_SEANCHLO_s = 0x1e9b;
    public static final int TYRONIAN = 0x204a;
    
    // These are the Unicode code points for the seanchlo characters, in sorted order
    // This is just the same list as above
    // TODO: change this to use symbolic names in the array declaration
    public static final int[] unicodeGlyphCoords = {
        0x010a, 0x010b, 0x0120, 0x0121, 0x017f, 0x027c, 0x1e02, 0x1e03, 
          0x1e0a, 0x1e0b, 0x1e1e, 0x1e1f, 0x1e40, 0x1e41, 0x1e56, 0x1e57,
          0x1e60, 0x1e61, 0x1e6a, 0x1e6b, 0x1e9b, 0x204a
    };
    
    // this array stores the bitwise negation of the coordinate, so that the
    // default array value (0) represents an invalid character (-1)
    int[] glyphCoords;
    String fontName;

    /** Creates a new instance of SeanchloCharCodeMap */
    public SeanchloCharCodeMap(String fontName) {
        super();
        this.fontName = fontName;
    }
    
    /** Creates a new instance of SeanchloCharCodeMap */
    public SeanchloCharCodeMap(String fontName, int... charvals) {
        this(fontName);

        glyphCoords = new int[unicodeGlyphCoords.length];
        for (int i = 0; i < glyphCoords.length && i < charvals.length; i++) {
            glyphCoords[i] = ~charvals[i];
        }
    }
    
    public void setCharacter(int lci, int glyphCoord) {
        // the lci is the Unicode code point of the desired character
        int index = Arrays.binarySearch(unicodeGlyphCoords, lci);
        if (index < 0) {
            throw new IllegalArgumentException("Undefined logical character identifier: " + lci);
        }
        if (glyphCoords == null) {
            glyphCoords = new int[unicodeGlyphCoords.length];
        }
        glyphCoords[index] = ~glyphCoord;
    }
    
    public int getCharacter(int lci) {
        // the lci is the Unicode code point of the desired character
        int index = Arrays.binarySearch(unicodeGlyphCoords, lci);
        if (index < 0) {
            throw new IllegalArgumentException("Undefined logical character identifier: " + lci);
        }
        if (glyphCoords == null || glyphCoords[index] == 0) {
            return -1;
        }
        return ~glyphCoords[index];
    }

    public boolean isProvidedLCI(int lci) {
        return Arrays.binarySearch(unicodeGlyphCoords, lci) >= 0;
    }

    public String getFontName() {
        return fontName;
    }
}
