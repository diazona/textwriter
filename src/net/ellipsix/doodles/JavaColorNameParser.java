/*
 * JavaColorNameParser.java
 *
 * Created on September 13, 2005, 9:49 PM
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
import net.ellipsix.Parser;

/**
 *
 * @author David Zaslavsky
 */
public class JavaColorNameParser implements Parser<Color> {
    Parser<Color> parent;
    
    /** Creates a new instance of JavaColorNameParser */
    public JavaColorNameParser() {
        super();
    }
    
    /** Creates a new instance of JavaColorNameParser */
    public JavaColorNameParser(Parser<Color> backup) {
        super();
        parent = backup;
    }
        

    public Color parse(String input) {
        input = input.toLowerCase();
        if (input.equals("black")) {
            return Color.black;
        }
        else if (input.equals("white")) {
            return Color.white;
        }
        else if (input.equals("red")) {
            return Color.red;
        }
        else if (input.equals("green")) {
            return Color.green;
        }
        else if (input.equals("blue")) {
            return Color.blue;
        }
        else if (input.equals("dark grey")) {
            return Color.darkGray;
        }
        else if (input.equals("gray")) {
            return Color.gray;
        }
        else if (input.equals("light gray")) {
            return Color.lightGray;
        }
        else if (input.equals("magenta")) {
            return Color.magenta;
        }
        else if (input.equals("cyan")) {
            return Color.cyan;
        }
        else if (input.equals("yellow")) {
            return Color.yellow;
        }
        else if (input.equals("orange")) {
            return Color.orange;
        }
        else if (input.equals("pink")) {
            return Color.pink;
        }
        else if (parent != null) {
            return parent.parse(input);
        }
        else {
            return null;
        }
    }
}
