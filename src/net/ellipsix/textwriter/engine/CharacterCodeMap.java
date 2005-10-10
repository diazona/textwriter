/*
 * CharacterCodeMap.java
 *
 * Created on September 13, 2005, 11:04 PM
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

/**
 * Represents a mapping of logical character identifiers to actual glyphs in a certain
 * font.
 * <p>A <i>logical character identifier</i> (LCI) is a tag of some kind which
 * identifies the essential aspects of a character as perceived by a reader,
 * regardless of specific details which may be font dependent. The code points
 * defined in the Unicode specification are typical logical character identifiers,
 * for example <code>0x00E1</code> (a lowercase a with an acute accent &#151;
 * <b>&aacute;</b>).</p>
 * <p>Some fonts are not Unicode-compliant, or use glyphs not represented in the
 * Unicode specification, so the Unicode standard character equivalents will not be
 * helpful when working with them. A class implementing <code>CharacterCodeMap</code>
 * remedies this by defining a set of LCIs corresponding to some subset of the
 * characters in that font, and storing them together with their respective actual
 * glyph indices for that font.</p>
 * <p>The set of logical character identifiers defined by a given implementation of
 * <code>CharacterCodeMap</code> should be clearly documented. In order to increase
 * interoperability, Unicode code points should be used as LCIs wherever possible,
 * LCIs defined in existing code should preferably be duplicated when writing new
 * code that deals with the same characters. In general, it should not be necessary
 * to create an implementation of <code>CharacterCodeMap</code> that uses a predefined
 * LCI.</p>
 * @author David Zaslavsky
 */
public interface CharacterCodeMap {
    /**
     * Indicates whether or not this implementation of <code>CharacterCodeMap</code>
     * defines the given <code>int</code> LCI.
     * @param lci the logical character identifier
     * @return <code>true</code> if this instance of <code>CharacterCodeMap</code> may contain
     * a mapping for the LCI
     */
    public abstract boolean isProvidedLCI(int lci);
    /**
     * Returns the glyph coordinate corresponding to the given LCI in this instance of
     * <code>CharacterCodeMap</code>. If there is no glyph coordinate mapped to the
     * given LCI, either because the LCI is not defined by this <code>CharacterCodeMap</code>
     * or simply because no mapping was assigned, this method should return -1.
     * @param lci the logical character identifier to retrieve
     * @return the coordinate of the glyph in this map's font which corresponds to the given LCI,
     * or -1 if the LCI is not found
     */
    public abstract int getCharacter(int lci);
    
    /**
     * Returns the name of the font family or font class for which this
     * <code>CharacterCodeMap</code>'s data are valid. A font class is an identifying
     * name for a group of fonts which have the same glyph coordinates for the logical
     * character identifiers defined by this <code>CharacterCodeMap</code> (and perhaps
     * in general). &quot;Unicode&quot; is a valid font class name that applies to many
     * fonts. The logic of determining whether a font falls in a given class is left up
     * to the application.
     * @return the font or font class name
     */
    public abstract String getFontName();
}
