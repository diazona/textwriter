/*
 * RGB10ColorParser.java
 *
 * Created on September 13, 2005, 9:40 PM
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
public class RGB10ColorParser implements Parser<Color> {
    static Pattern pattern;
    
    static {
        try {
            if (pattern == null) {
                // recognizes a possibly parenthesized grouping of three or four
                // integer coordinates separated by an optional comma and/or whitespace
                String rgbgroup = "(\\d{0,3})";
                String rgbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + rgbgroup + ")";
                pattern = Pattern.compile("\\(?\\s*" + rgbgroup + rgbrep + rgbrep + rgbrep + "?\\s*\\)?");
            }
        }
        catch (PatternSyntaxException pse) {
            // should not happen - suggests corrupted class file
            WLogManager.fatal(pse);
        }
    }

    
    Parser<Color> parent;
    
    /** Creates a new instance of RGB10ColorParser */
    public RGB10ColorParser() {
        super();
    }

    /** Creates a new instance of RGB10ColorParser */
    public RGB10ColorParser(Parser<Color> backup) {
        super();
        parent = backup;
    }

    public Color parse(String str) {
        str = str.toLowerCase();
        int red, green, blue, alpha = 255;
        
        Matcher matcher = pattern.matcher(str);
        try {
            if (matcher.matches()) {
                red = Integer.parseInt(matcher.group(1), 10);
                green = Integer.parseInt(matcher.group(2), 10);
                blue = Integer.parseInt(matcher.group(3), 10);
                String g4 = matcher.group(4);
                if (g4 != null) {
                    alpha = Integer.parseInt(g4, 10);
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
