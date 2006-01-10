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
public class ImageDisplay extends HttpServlet {
    static final Color transparent = new Color(0, 0, 0, 0);
    static final Color purple = new Color(0xaa, 0, 0xaa, 255);
    
    long id;
    int clid;
    SeanchloGaelicParser textParser;
    
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        id = System.currentTimeMillis();
        clid = System.identityHashCode(getClass().getClassLoader());
        log("Initializing ImageDisplay; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + System.identityHashCode(getServletContext()));
        
        ServletContext ctx = config.getServletContext();
        
        WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)ctx.getAttribute("imagemap");
        if (images == null) {
            ctx.setAttribute("imagemap", new WeakHashMap<Integer, BufferedImage>());
        }
        
        AtomicInteger imageIdx = (AtomicInteger)ctx.getAttribute("imageindex");
        if (imageIdx == null) {
            ctx.setAttribute("imageindex", new AtomicInteger());
        }
        
        textParser = new SeanchloGaelicParser();
        textParser.addCharacterCodeMap(new SeanchloCharCodeMap("seanchlo", SeanchloCharCodeMap.unicodeGlyphCoords));
        textParser.addCharacterCodeMap(new AccentedVowelCodeMap("Unicode", AccentedVowelCodeMap.unicodeGlyphCoords));
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        log("Destroying ImageDisplay; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + System.identityHashCode(getServletContext()));
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String mode = request == null ? null : request.getParameter("mode");
        log("Processing request: mode=" + mode);
        
        String fontNm = "Bunchl\u00f3 GC";
        Integer fontSz = 16;
        String text = "";
        int style = 0;
        String bg = "transparent";
        String fg = "black";

        if (mode != null && mode.equalsIgnoreCase("image")) {
            String ptmp = request.getParameter("font");
            if (ptmp != null) {
                fontNm = ptmp;
            }
            ptmp = request.getParameter("size");
            if (ptmp != null) {
                fontSz = Integer.parseInt(ptmp);
            }
            text = request.getParameter("text");
            if (text == null) {
                text = "";
            }
            
            if (text.length() > 0) {
                String bold = request.getParameter("bold");
                String italic = request.getParameter("italic");
                if (bold != null && bold.equalsIgnoreCase("true")) {
                    style |= Font.BOLD;
                }
                if (italic != null && italic.equalsIgnoreCase("true")) {
                    style |= Font.ITALIC;
                }

                String bgtmp = request.getParameter("bgcolor");
                String fgtmp = request.getParameter("fgcolor");

                Color bgcolor = null, fgcolor = null;

                if (bgtmp != null) {
                    bgcolor = parseColor(bgtmp);
                    if (bgcolor == null) {
                        bgcolor = parseColor(bg);
                    }
                    else {
                        bg = bgtmp;
                    }
                }
                
                if (fgtmp != null) {
                    fgcolor = parseColor(fgtmp);
                    if (fgcolor == null) {
                        fgcolor = parseColor(fg);
                    }
                    else {
                        fg = fgtmp;
                    }
                }

                Font renderFont = FontCollection.retrieve(getServletContext()).getFont(fontNm, style, fontSz);
                Set<FontCollection.TaggedFont.FontAttribute> fontAttrs = 
                  FontCollection.retrieve(getServletContext()).getFontAttributes(fontNm);
                String[] fontSpecNames = new String[fontAttrs.size() + 2];
                int i = 0;
                fontSpecNames[i++] = renderFont.getFontName();
                fontSpecNames[i++] = renderFont.getFamily();
                for (FontCollection.TaggedFont.FontAttribute attr : fontAttrs) {
                    fontSpecNames[i++] = attr.getName();
                }
                
                log("Input: " + text + " in font " + renderFont.toString());
                text = textParser.prepForFont(text, fontSpecNames);
                log("Output: " + text);

                int imgId = renderText(text, renderFont, bgcolor, fgcolor);
                
                PrintWriter out = response.getWriter();
                
                if (request.getRequestURI().endsWith("ImageDisplay")) {
                    // output some starting HTML
                    out.println("<html><head><title>Ellipsix Programming TextWriter</title>");
                    out.println("<link rel='stylesheet' type='text/css' href='/ellipsix.css'>");
                    out.println("</head><body><div id='Banner'>&nbsp;</div>");
                    out.println("<div id=\"Menu\"><table><tr>");
                    out.println("<td><a href=\"http://www.ellipsix.net/index.php\">Home</a></td>");
                    out.println("<td><a href=\"http://www.ellipsix.net/products.php\">Products</a></td>");
                    out.println("<td><a href=\"http://www.ellipsix.net/links.php\">Links</a></td>");
                    out.println("<td><a href=\"http://www.ellipsix.net/contact.php\">Contact</a></td>");
                    out.println("<td><a href=\"http://www.ellipsix.net/about.php\">About Ellipsix</a></td>");
                    out.println("</tr></table></div><div id='Body'><div id='Content'>");
                }

                out.println("Rendition of the input text:<br>");
                out.println("<img src='" + request.getContextPath() + "/image?imageid=" + imgId + "'>");
                out.println("<p>To save this image file to your computer, right-click on the text");
                out.println("and choose the option &quotSave Image As&quot; or &quot;Save As&quot; from the menu.");
                out.println("To copy the URL of the image to your system clipboard, right-click");
                out.println("the text and select the &quot;Copy Image Location&quot; or &quot;Copy Image URL&quot;");
                out.println("option. <i><u>Please note that the URL will only be valid for a limited");
                out.println("amount of time.</u></i></p>");
                out.println("<hr>");
                out.flush();

                if (request.getRequestURI().endsWith("ImageDisplay")) {
                    // output the ending HTML
                    out.println("</div><div id='Footer'>");
                    out.println("<p>&copy;2005 <a href=\"mailto:contact@ellipsix.net\">Ellipsix Programming</a>;");
                    out.println("created by David Zaslavsky.</p>");
                    out.println("<p>This software service is provided by Ellipsix Programming for");
                    out.println("public use on an as-is basis. Please report any errors to");
                    out.println("<a href=\"mailto:contact@ellipsix.net\">contact@ellipsix.net</a>.</p>");
                    out.println("</div></div></body></html>");
                }
            }
        }
        else {
            String imageId = request.getParameter("imageid");
            if (imageId == null) {
                log("Note: null image id");
                
                response.sendError(HttpServletResponse.SC_NO_CONTENT);

                return;
            }
            response.setContentType("image/png");

            int index = Integer.parseInt(imageId);
            WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)getServletContext().getAttribute("imagemap");

            ServletOutputStream out = response.getOutputStream();
            BufferedImage img = images.get(index);
            ImageIO.write(img, "PNG", out);
            log("Writing image #" + index);
            out.flush();
            out.close();
        }
    }
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Generates, stores, and outputs image data";
    }
    
    
    public int renderText(String text, Font font, Color bgColor, Color fgColor) {
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
            log("Error in trimming image: " + rfe.getMessage());
            // usually means that the text boundary was bigger than the canvas
            // ignore for now
        }
        
        WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)getServletContext().getAttribute("imagemap");
        
        AtomicInteger imageIdx = (AtomicInteger)getServletContext().getAttribute("imageindex");
        int index = imageIdx.getAndIncrement();
        images.put(index, image);
        
        log("created image #" + index + " with text '" + text + "'");
        
        return index;
    }

    Parser<Color> cParse = new JavaColorNameParser(new HexColorParser(new RGB10ColorParser(new HSB10ColorParser())));
    
    public Color parseColor(String input) {
        input = input.toLowerCase();

        if (input.equals("transparent")) {
            return transparent;
        }
        else if (input.equals("purple")) {
            return purple;
        }
        else {
            return cParse.parse(input);
        }
    }
}
