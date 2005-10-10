/*
 * HSB10ColorParser.java
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
public class HSB10ColorParser implements Parser<Color> {
    static Pattern pattern;
    
    static {
        try {
            if (pattern == null) {
                // recognizes a possibly parenthesized grouping of three or four
                // decimal coordinates separated by an optional comma and/or whitespace
                String hsbgroup = "([01]\\.\\d+)";
                String hsbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + hsbgroup + ")";
                pattern = Pattern.compile("\\(?\\s*" + hsbgroup + hsbrep + hsbrep + hsbrep + "?\\s*\\)?");
            }
        }
        catch (PatternSyntaxException pse) {
            // should not happen - suggests corrupted class file
            WLogManager.fatal(pse);
        }
    }

    
    Parser<Color> parent;
    
    /** Creates a new instance of HSB10ColorParser */
    public HSB10ColorParser() {
        super();
    }

    /** Creates a new instance of HSB10ColorParser */
    public HSB10ColorParser(Parser<Color> backup) {
        super();
        parent = backup;
    }

    public Color parse(String str) {
        str = str.toLowerCase();
        float hue, saturation, value, alphaf;
        
        Matcher matcher = pattern.matcher(str);
        try {
            if (matcher.matches()) {
                int alpha = 255;
                hue = Float.parseFloat(matcher.group(1));
                saturation = Float.parseFloat(matcher.group(2));
                value = Float.parseFloat(matcher.group(3));
                String g4 = matcher.group(4);
                if (g4 != null) {
                    alphaf = Float.parseFloat(g4);
                    if (alphaf > 1.0) {
                        alphaf = 1.0f;
                    }
                    alpha = (int)(alphaf * 255);
                }
                return new Color((Color.HSBtoRGB(hue, saturation, value) & ((1 << 24) - 1)) | (alpha << 24), true);
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
