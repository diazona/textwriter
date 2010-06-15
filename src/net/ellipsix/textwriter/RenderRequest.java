/* RenderRequest.java */

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

package net.ellipsix.textwriter;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;

/**
 * A text rendering request as received from the Textwriter frontend.
 *
 * @author David Zaslavsky
 */
public class RenderRequest {
    /**
     * Parses a color from a 8-character hex digit specification.
     *
     * This is almost the same thing that {@link Color#decode(String)} does,
     * except that this method handles an alpha channel.
     *
     * @param color the string specification of the color, which must contain
     *  exactly 8 hexadecimal digits
     * @return the Color object corresponding to the input string
     */
    protected static Color parseColor(String color) {
        int red = Integer.parseInt(color.substring(0, 2), 16);
        int green = Integer.parseInt(color.substring(2, 4), 16);
        int blue = Integer.parseInt(color.substring(4, 6), 16);
        int alpha = Integer.parseInt(color.substring(6, 8), 16);
        return new Color(red, green, blue, alpha);
    }

    String text;
    Font font;
    Color background;
    Color foreground;

    public static RenderRequest parse(BufferedReader r) {
        String fontName = r.readLine();
        int fontSize = r.read();
        int style = r.read();
        String background = r.readLine();
        String foreground = r.readLine();
        int nLines = r.read();
        String text;
        if (nLines == 1) {
            text = r.readLine();
        }
        else {
            StringBuilder sb = new StringBuilder();
            while (nLines-- > 0)
                sb.append(r.readLine() + "\n");
            text = sb.toString();
        }
        Font font = FontCollection.getInstance().getFont(fontName, fontSize, style);
        return new RenderRequest(text, font, parseColor(background), parseColor(foreground));
    }

    private RenderRequest(String text, Font font, Color background, Color foreground) {
        this.text = text;
        this.font = font;
        this.background = background;
        this.foreground = foreground;
    }
}
