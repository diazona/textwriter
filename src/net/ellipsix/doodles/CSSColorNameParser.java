/*
 * CSSColorNameParser.java
 *
 * Created on September 13, 2005, 9:53 PM
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

package net.ellipsix.doodles;

import java.awt.Color;
import java.util.HashMap;
import net.ellipsix.Parser;

/**
 *
 * @author David Zaslavsky
 */
public class CSSColorNameParser implements Parser<Color> {
    // Here are the 16 accepted CSS color names
    static final HashMap<String, Color> cssColors = new HashMap<String, Color>(16, 1.0f);

    static {
        cssColors.put("black", new Color(0x000000));
        cssColors.put("white", new Color(0xFFFFFF));
        cssColors.put("red", new Color(0xFF0000));
        cssColors.put("yellow", new Color(0xFFFF00));
        cssColors.put("lime", new Color(0x00FF00));
        cssColors.put("aqua", new Color(0x00FFFF));
        cssColors.put("blue", new Color(0x0000FF));
        cssColors.put("fuschia", new Color(0xFF00FF));
        cssColors.put("gray", new Color(0x808080));
        cssColors.put("silver", new Color(0xC0C0C0));
        cssColors.put("maroon", new Color(0x800000));
        cssColors.put("olive", new Color(0x808000));
        cssColors.put("green", new Color(0x008000));
        cssColors.put("teal", new Color(0x008080));
        cssColors.put("navy", new Color(0x000080));
        cssColors.put("purple", new Color(0x800080));
    }
    
    Parser<Color> parent;
    
    /** Creates a new instance of CSSColorNameParser */
    public CSSColorNameParser() {
        super();
    }
    
    /** Creates a new instance of CSSColorNameParser */
    public CSSColorNameParser(Parser<Color> backup) {
        super();
        parent = backup;
    }
        

    public Color parse(String input) {
        input = input.toLowerCase();
        Color col = cssColors.get(input);
        if (col != null) {
            return col;
        }
        else if (parent != null) {
            return parent.parse(input);
        }
        else {
            return null;
        }
    }
}
