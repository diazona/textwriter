/*
 * InputParser.java
 *
 * Created on September 10, 2005, 11:45 PM
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

import java.awt.Font;

/**
 * Converts an input string into the proper character sequence for rendering
 * in a given font.
 * <p>An instance of <code>InputParser</code> maintains an ordered list of character
 * code maps, each of which maps some set of LCIs to code points. Each individual
 * map applies to only a single font or font class. During a conversion operation,
 * when the code point corresponding to some LCI is needed, the parser goes through
 * the list and checks each map which supports the desired font or font class to see
 * if it has a mapping for the given LCI. The first map in the list which has a
 * mapping for the required LCI in the font which is being used is queried for that
 * code point.</p>
 * @author David Zaslavsky
 */
public interface InputParser {
    
    /**
     * Adds mappings for a set of LCIs to this parser.
     * @param cmap the map to add
     */
    public abstract void addCharacterCodeMap(CharacterCodeMap cmap);
    /**
     * Removes a character code map from the list maintained by this parser.
     * @param cmap the map to remove
     */
    public abstract void removeCharacterCodeMap(CharacterCodeMap cmap);
    /**
     * Prepares a string for rendering in the given font.
     * <p>The input string will presumably contain character escapes or sequences
     * which correspond, according to rules which are implemented in this method,
     * to the LCIs offered by the character sets stored in the parser's maps. Each time
     * the parser encounters such a sequence, it obtains the relevant code point and
     * replaces the sequence with that code point. The output from this method will be
     * in the proper format for feeding to graphics routines.</p>
     * <p>This method is equivalent to calling <code>prepForFont(fnt.getFontName(),
     * fnt.getFamily())</code>.
     * @param input the input string containing escape sequences
     * @param fnt the font to produce output for
     * @return the string to pass on to the drawing methods
     */
    public abstract String prepForFont(String input, Font fnt);
    /**
     * Prepares a string for rendering in the given font.
     * <p>The input string will presumably contain character escapes or sequences
     * which correspond, according to rules which are implemented in this method,
     * to the LCIs offered by the character sets stored in the parser's maps. Each time
     * the parser encounters such a sequence, it obtains the relevant code point and
     * replaces the sequence with that code point. The output from this method will be
     * in the proper format for feeding to graphics routines.</p>
     * <p>This function allows multiple font class names to be specified. When it is
     * necessary to look up the code point for an LCI, the first class name specified
     * (which will typically be the name of a particular font) will be used first to
     * search for the code point. If that does not yield a result, then the second class
     * name specified (typically a font family) will be used to search, and so on until
     * a code point is found or the end of the list is reached. For fonts which comply
     * with Unicode encoding, it is generally a good idea to give "Unicode" as the last
     * parameter, so that any generic unicode LCI mappings will be searched if all
     * other mappings fail.</p>
     * @param input the input string containing escape sequences
     * @param fontSpecs a list of names and/or classes which describe the font, from most specific to
     * least specific
     * @return the string to pass on to the drawing methods
     */
    public abstract String prepForFont(String input, String... fontSpecs);
}
