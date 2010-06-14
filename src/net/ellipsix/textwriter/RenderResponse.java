/* RenderResponse.java */

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
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author David Zaslavsky
 */
public class RenderResponse {
    private BufferedImage image;

    public RenderResponse(RenderRequest req) {
        image = renderText(req.text, req.font, req.background, req.foreground);
    }

    public void write(OutputStream out) {
        ImageIO.write(image, "PNG", out);
    }

    public void write(File file) {
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
