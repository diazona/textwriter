/*
 * AccentedVowelCodeMap.java
 *
 * Created on December 9, 2005, 1:27 AM
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
 * Defines the set of LCIs corresponding to the accented vowels.
 *
 * @author David Zaslavsky
 */
// An instance of this can be created for a specific font name or font class name
// It will contain the character codes for each of the accented vowels
public class AccentedVowelCodeMap implements CharacterCodeMap , Serializable {
    // These numbers represent sorted order by Unicode code point
    public static final int A_GRAVE      = 0x00c0;
    public static final int A_ACUTE      = 0x00c1;
    public static final int A_CIRCUMFLEX = 0x00c2;
    public static final int A_TILDE      = 0x00c3;
    public static final int A_DIARESIS   = 0x00c4;
    public static final int E_GRAVE      = 0x00c8;
    public static final int E_ACUTE      = 0x00c9;
    public static final int E_CIRCUMFLEX = 0x00ca;
    public static final int E_DIARESIS   = 0x00cb;
    public static final int I_GRAVE      = 0x00cc;
    public static final int I_ACUTE      = 0x00cd;
    public static final int I_CIRCUMFLEX = 0x00ce;
    public static final int I_DIARESIS   = 0x00cf;
    public static final int O_GRAVE      = 0x00d2;
    public static final int O_ACUTE      = 0x00d3;
    public static final int O_CIRCUMFLEX = 0x00d4;
    public static final int O_TILDE      = 0x00d5;
    public static final int O_DIARESIS   = 0x00d6;
    public static final int U_GRAVE      = 0x00d9;
    public static final int U_ACUTE      = 0x00da;
    public static final int U_CIRCUMFLEX = 0x00db;
    public static final int U_TILDE      = 0x00dc;
    public static final int U_DIARESIS   = 0x00dd;
    public static final int a_GRAVE      = 0x00e0;
    public static final int a_ACUTE      = 0x00e1;
    public static final int a_CIRCUMFLEX = 0x00e2;
    public static final int a_TILDE      = 0x00e3;
    public static final int a_DIARESIS   = 0x00e4;
    public static final int e_GRAVE      = 0x00e8;
    public static final int e_ACUTE      = 0x00e9;
    public static final int e_CIRCUMFLEX = 0x00ea;
    public static final int e_DIARESIS   = 0x00eb;
    public static final int i_GRAVE      = 0x00ec;
    public static final int i_ACUTE      = 0x00ed;
    public static final int i_CIRCUMFLEX = 0x00ee;
    public static final int i_DIARESIS   = 0x00ef;
    public static final int o_GRAVE      = 0x00f2;
    public static final int o_ACUTE      = 0x00f3;
    public static final int o_CIRCUMFLEX = 0x00f4;
    public static final int o_TILDE      = 0x00f5;
    public static final int o_DIARESIS   = 0x00f6;
    public static final int u_GRAVE      = 0x00f9;
    public static final int u_ACUTE      = 0x00fa;
    public static final int u_CIRCUMFLEX = 0x00fb;
    public static final int u_TILDE      = 0x00fc;
    public static final int u_DIARESIS   = 0x00fd;
    // TODO: add macron, breve, ogonek
    
    // This is just the same list as above
    public static final int[] unicodeGlyphCoords = {
        0x00c0, 0x00c1, 0x00c2, 0x00c3, 0x00c4, 0x00c8, 0x00c9, 0x00ca, 
          0x00cb, 0x00cc, 0x00cd, 0x00ce, 0x00cf, 0x00d2, 0x00d3, 0x00d4,
          0x00d5, 0x00d6, 0x00d9, 0x00da, 0x00db, 0x00dc, 0x00dd,
        0x00e0, 0x00e1, 0x00e2, 0x00e3, 0x00e4, 0x00e8, 0x00e9, 0x00ea, 
          0x00eb, 0x00ec, 0x00ed, 0x00ee, 0x00ef, 0x00f2, 0x00f3, 0x00f4,
          0x00f5, 0x00f6, 0x00f9, 0x00fa, 0x00fb, 0x00fc, 0x00fd
    };
    
    // this array stores the bitwise negation of the coordinate, so that the
    // default array value (0) represents an invalid character (-1)
    int[] glyphCoords;
    String fontName;

    /** Creates a new instance of AccentedVowelCodeMap */
    public AccentedVowelCodeMap(String fontName) {
        super();
        this.fontName = fontName;
    }
    
    /** Creates a new instance of SeanchloCharCodeMap */
    public AccentedVowelCodeMap(String fontName, int... charvals) {
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
