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
 *
 * @author David Zaslavsky
 */
// An instance of this can be created for a specific font name or font class name
// It will contain the character codes for each of the dotted consonants and the & sign
public class SeanchloCharCodeMap implements CharacterCodeMap , Serializable {
    // These numbers represent sorted order by Unicode code point
    public static final int DOTTED_C = 0;
    public static final int DOTTED_c = 1;
    public static final int DOTTED_G = 2;
    public static final int DOTTED_g = 3;
    public static final int SEANCHLO_s = 4;
    public static final int SEANCHLO_r = 5;
    public static final int DOTTED_B = 6;
    public static final int DOTTED_b = 7;
    public static final int DOTTED_D = 8;
    public static final int DOTTED_d = 9;
    public static final int DOTTED_F = 10;
    public static final int DOTTED_f = 11;
    public static final int DOTTED_M = 12;
    public static final int DOTTED_m = 13;
    public static final int DOTTED_P = 14;
    public static final int DOTTED_p = 15;
    public static final int DOTTED_S = 16;
    public static final int DOTTED_s = 17;
    public static final int DOTTED_T = 18;
    public static final int DOTTED_t = 19;
    public static final int TYRONIAN = 20;
    
    // These are the Unicode code points for the seanchlo characters, in sorted order
    public static final int[] unicodeGlyphCoords = {
        0x010a, 0x010b, 0x0120, 0x0121, 0x017f, 0x027c, 0x1e0a, 0x1e0b,
          0x1e1e, 0x1e1f, 0x1e02, 0x1e03, 0x1e40, 0x1e41, 0x1e56, 0x1e57,
          0x1e60, 0x1e61, 0x1e6a, 0x1e6b, 0x204a
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
