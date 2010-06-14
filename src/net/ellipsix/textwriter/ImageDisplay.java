/*
 * ImageDisplay.java
 *
 * Created on August 21, 2005, 1:20 AM
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

package net.ellipsix.textwriter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.*;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import javax.servlet.*;
import javax.servlet.http.*;
import net.ellipsix.Parser;
import net.ellipsix.doodles.HSB10ColorParser;
import net.ellipsix.doodles.HexColorParser;
import net.ellipsix.doodles.JavaColorNameParser;
import net.ellipsix.doodles.RGB10ColorParser;
import net.ellipsix.textwriter.engine.AccentedVowelCodeMap;
import net.ellipsix.textwriter.engine.SeanchloCharCodeMap;
import net.ellipsix.textwriter.engine.SeanchloGaelicParser;

/** Creates, stores, and renders image data for TextWriter.
 *
 * @author David Zaslavsky
 */
// TODO: create a system of vector images using RenderableImage
// TODO: create a custom "glyph-vector-layouter"
public class ImageDisplay {
    // TODO: develop some equivalent for this
/*        textParser = new SeanchloGaelicParser();
        textParser.addCharacterCodeMap(new SeanchloCharCodeMap("seanchlo", SeanchloCharCodeMap.unicodeGlyphCoords));
        textParser.addCharacterCodeMap(new AccentedVowelCodeMap("Unicode", AccentedVowelCodeMap.unicodeGlyphCoords));*/
    
    

    /**
     * Produces the font corresponding to the given specs.
     *
     * @param fontName the name of the font family
     * @param fontSize the desired font size
     * @param bold should be {@code "true"} to get a bold font,
     *  anything else otherwise
     * @param italic should be {@code "true"} to get an italic font,
     *  anything else otherwise
     */
    protected Font parseFont(String fontName, int fontSize, boolean bold, boolean italic) throws IOException {
        int style = 0;
        if (bold != null && bold.equalsIgnoreCase("true")) {
            style |= Font.BOLD;
        }
        if (italic != null && italic.equalsIgnoreCase("true")) {
            style |= Font.ITALIC;
        }
        return FontCollection.getFont(fontName, Integer.parseInt(fontSize), style);
    }
    
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
    protected Color parseColor(String color) {
        int red = Integer.parseInt(color.substring(0, 2), 16);
        int green = Integer.parseInt(color.substring(2, 4), 16);
        int blue = Integer.parseInt(color.substring(4, 6), 16);
        int alpha = Integer.parseInt(color.substring(6, 8), 16);
        return new Color(red, green, blue, alpha);
    }
        
        if (text.length() > 0) {

            String bgtmp = request.getParameter("bgcolor");
            String fgtmp = request.getParameter("fgcolor");

            Set<FontCollection.TaggedFont.FontAttribute> fontAttrs = 
                FontCollection.retrieve(getServletContext()).getFontAttributes(fontNm);
            String[] fontSpecNames = new String[fontAttrs.size() + 2];
            int i = 0;
            fontSpecNames[i++] = renderFont.getFontName();
            fontSpecNames[i++] = renderFont.getFamily();
            for (FontCollection.TaggedFont.FontAttribute attr : fontAttrs) {
                fontSpecNames[i++] = attr.getName();
            }
            
            text = textParser.prepForFont(text, fontSpecNames);

            BufferedImage img = renderText(text, renderFont, bgcolor, fgcolor);
        }
    }

    /**
     * Creates an image with the given text using the given rendering parameters.
     *
     * @param text the text to draw in the image
     * @param font the font to use to draw the text
     * @param bgColor the background color
     * @param fgColor the foreground color
     */
    public BufferedImage renderText(String text, Font font, Color bgColor, Color fgColor) {
        // create the image
        BufferedImage image = new BufferedImage(font.getSize() * text.length() + 2, font.getSize() + 2, BufferedImage.TYPE_4BYTE_ABGR);
        
        // get a graphics object
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // get the text boundary
        TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
        Rectangle2D bounds = layout.getBounds();
        
        // fill the background
        g.setColor(bgColor);
        g.fillRect(0, 0, (int)(bounds.getWidth() + 1), (int)(bounds.getHeight() + 1));
        
        // draw the text
        g.setColor(fgColor);
        layout.draw(g, -(float)bounds.getX(), -(float)bounds.getY());
        
        // and trim
        try {
            image = image.getSubimage(0, 0, (int)(bounds.getWidth() + 1), (int)(bounds.getHeight() + 1));
        }
        catch (RasterFormatException rfe) {
//             log("Error in trimming image: " + rfe.getMessage());
            // usually means that the text boundary was bigger than the canvas
            // ignore for now
        }

//         log("created image with text '" + text + "'");
        return image;
    }
}
