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

/** Converts an input string into the proper character sequence for rendering
 * in a given font.
 *
 * @author David Zaslavsky
 */
public interface InputParser<C extends CharacterCodeMap> {
    public abstract void addCharacterCodeMap(C cmap);
    public abstract void removeCharacterCodeMap(C cmap);
    public abstract String prepForFont(String input, Font fnt);
    public abstract String prepForFont(String input, String fontname);
}
