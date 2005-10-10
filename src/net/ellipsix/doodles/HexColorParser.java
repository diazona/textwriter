/*
 * HexColorParser.java
 *
 * Created on September 13, 2005, 9:27 PM
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.ellipsix.Parser;
import net.ellipsix.WLogManager;

/**
 *
 * @author David Zaslavsky
 */
public class HexColorParser implements Parser<Color> {
    static Pattern pattern;
    
    static {
        try {
            if (pattern == null) {
                // recognizes a string of six or eight consecutive hex digits, with
                // an optional 0x or # in front
                pattern = Pattern.compile("(?:0x|\\#)?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})?");
            }
        }
        catch (PatternSyntaxException pse) {
            // should not happen - suggests corrupted class file
            WLogManager.fatal(pse);
        }
    }

    
    Parser<Color> parent;
    
    /** Creates a new instance of HexColorParser */
    public HexColorParser() {
        super();
    }

    /** Creates a new instance of HexColorParser */
    public HexColorParser(Parser<Color> backup) {
        super();
        parent = backup;
    }

    public Color parse(String str) {
        str = str.toLowerCase();
        int red, green, blue, alpha = 255;
        
        Matcher matcher = pattern.matcher(str);
        try {
            if (matcher.matches()) {
                red = Integer.parseInt(matcher.group(1), 16);
                green = Integer.parseInt(matcher.group(2), 16);
                blue = Integer.parseInt(matcher.group(3), 16);
                String g4 = matcher.group(4);
                if (g4 != null) {
                    alpha = Integer.parseInt(g4, 16);
                }
                return new Color(red, green, blue, alpha);
            }
        }
        catch (NumberFormatException nfe) {
            // continue on
        }
        if (parent == null) {
            return null;
        }
        else {
            return parent.parse(str);
        }
    }
    
}
