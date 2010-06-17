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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * A text rendering request as received from the Textwriter frontend.
 *
 * @author David Zaslavsky
 */
public class RenderRequest {
    private static final Logger logger = Logger.getLogger("net.ellipsix.textwriter");

    private String text;
    private Font font;
    private Color background;
    private Color foreground;
    private BufferedImage image;

    public static RenderRequest parse(BufferedReader r) throws IOException {
        logger.finest("Reading font information");
        String fontName = r.readLine();
        int fontSize = r.read();
        int bold = r.read();
        int italic = r.read();
        int style = (bold == 0 ? 0 : Font.BOLD) | (italic == 0 ? 0 : Font.ITALIC);
        String background = r.readLine();
        String foreground = r.readLine();
        int nLines = r.read();
        logger.finest("Reading " + nLines + " lines of text");
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
        logger.finest("Getting font");
        Font font = FontCollection.getInstance().getFont(fontName, fontSize, style);
        return new RenderRequest(text, font, parseColor(background), parseColor(foreground));
    }

    public RenderRequest(String text, Font font, Color background, Color foreground) {
        this.text = text;
        this.font = font;
        this.background = background;
        this.foreground = foreground;
        this.image = renderText(text, font, background, foreground);
    }

    public void write(OutputStream out) throws IOException {
        ImageIO.write(image, "PNG", out);
    }

    public void write(File file) throws IOException {
        ImageIO.write(image, "PNG", file);
    }

    /**
     * Creates an image with the given text using the given rendering parameters.
     *
     * @param text the text to draw in the image
     * @param font the font to use to draw the text
     * @param bgColor the background color
     * @param fgColor the foreground color
     */
    public static BufferedImage renderText(String text, Font font, Color bgColor, Color fgColor) {
        logger.finest("Rendering text");
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
            // usually means that the text boundary was bigger than the canvas
            logger.throwing("RenderRequest", "renderText", rfe);
            // ignore for now
        }
        logger.finest("Created image");
        return image;
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
    protected static Color parseColor(String color) {
        int red = Integer.parseInt(color.substring(0, 2), 16);
        int green = Integer.parseInt(color.substring(2, 4), 16);
        int blue = Integer.parseInt(color.substring(4, 6), 16);
        int alpha = Integer.parseInt(color.substring(6, 8), 16);
        return new Color(red, green, blue, alpha);
    }
}
