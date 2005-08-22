/*
 * ImageDisplay.java
 *
 * Created on August 21, 2005, 1:20 AM
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
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.imageio.ImageIO;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 * @author David Zaslavsky
 * @version
 */
public class ImageDisplay extends HttpServlet {
    static final Color transparent = new Color(0, 0, 0, 0);
    
    static Pattern hexPattern;
    static Pattern rgbPattern;
    static Pattern hsbPattern;
    
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        ServletContext ctx = config.getServletContext();
        
        WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)ctx.getAttribute("imagemap");
        if (images == null) {
            ctx.setAttribute("imagemap", new WeakHashMap<Integer, BufferedImage>());
        }
        
        AtomicInteger imageIdx = (AtomicInteger)ctx.getAttribute("imageindex");
        if (imageIdx == null) {
            ctx.setAttribute("imageindex", new AtomicInteger());
        }

        try {
            if (hexPattern == null) {
                // recognizes a string of six or eight consecutive hex digits, with
                // an optional 0x in front
                hexPattern = Pattern.compile("(?:0x)?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})?");
            }
            if (rgbPattern == null) {
                // recognizes a possibly parenthesized grouping of three or four
                // integer coordinates separated by an optional comma and/or whitespace
                String rgbgroup = "(\\d{0,3})";
                String rgbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + rgbgroup + ")";
                rgbPattern = Pattern.compile("\\(?\\s*" + rgbgroup + rgbrep + rgbrep + rgbrep + "?\\s*\\)?");
            }
            if (hsbPattern == null) {
                // recognizes a possibly parenthesized grouping of three or four
                // decimal coordinates separated by an optional comma and/or whitespace
                String hsbgroup = "([01]\\.\\d+)";
                String hsbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + hsbgroup + ")";
                hsbPattern = Pattern.compile("\\(?\\s*" + hsbgroup + hsbrep + hsbrep + hsbrep + "?\\s*\\)?");
            }
        }
        catch (PatternSyntaxException pse) {
            log("pattern error: " + pse.getMessage(), pse);
        }
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String mode = request == null ? null : request.getParameter("mode");
        log(mode);
        
        String fontNm = "Bunchl\u00f3 GC";
        Integer fontSz = 16;
        String text = "";
        int style = 0;
        String bg = "transparent";
        String fg = "black";

        if (mode != null && mode.equalsIgnoreCase("image")) {
            fontNm = request.getParameter("font");
            fontSz = Integer.parseInt(request.getParameter("size"));
            text = request.getParameter("text");
            
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

                Font renderFont = FontCollection.getFont(fontNm, style, fontSz);

                int imgId = renderText(text, renderFont, bgcolor, fgcolor);
                
                PrintWriter out = response.getWriter();
                out.println("Rendition of " +  text + ":<br>");
                out.println("<img src='" + request.getContextPath() + "/ImageDisplay?imageid=" + imgId + "'>");
                out.println("<p>To save this image file to your computer, right-click on the text");
                out.println("and choose the option &quotSave Image As&quot; or &quot;Save As&quot; from the menu.");
                out.println("To copy the URL of the image to your system clipboard, right-click");
                out.println("the text and select the &quot;Copy Image Location&quot; or &quot;Copy Image URL&quot;");
                out.println("option. <i><u>Please note that the URL will only be valid for a limited");
                out.println("amount of time.</u></i></p>");
                out.println("<hr>");
            }
        }
        else {
            response.setContentType("image/png");

            String imageId = request.getParameter("imageid");
            if (imageId == null) {
                log("null image id");
                response.sendError(response.SC_BAD_REQUEST);
                return;
            }
            int index = Integer.parseInt(imageId);
            WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)getServletContext().getAttribute("imagemap");

            ServletOutputStream out = response.getOutputStream();
            BufferedImage img = images.get(index);
            ImageIO.write(img, "PNG", out);
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
        return "Outputs image data which has been generated and stored by the program";
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
            // usually means that the text boundary was bigger than the canvas
            // ignore for now
        }
        
        WeakHashMap<Integer, BufferedImage> images = (WeakHashMap<Integer, BufferedImage>)getServletContext().getAttribute("imagemap");
        
        AtomicInteger imageIdx = (AtomicInteger)getServletContext().getAttribute("imageindex");
        int index = imageIdx.getAndIncrement();
        images.put(index, image);
        
        return index;
    }

    public Color parseColor(String input) {
        input = input.toLowerCase();
        int red, green, blue, alpha = 255;

        Matcher matcher = hexPattern.matcher(input);
        try {
            if (matcher.matches()) {
                red = Integer.parseInt(matcher.group(1), 16);
                green = Integer.parseInt(matcher.group(2), 16);
                blue = Integer.parseInt(matcher.group(3), 16);
                String str = matcher.group(4);
                if (str != null) {
                    alpha = Integer.parseInt(str, 16);
                }
                return new Color(red, green, blue, alpha);
            }
        }
        catch (NumberFormatException nfe) {
        }

        matcher = rgbPattern.matcher(input);
        try {
            if (matcher.matches()) {
                red = Integer.parseInt(matcher.group(1), 10);
                green = Integer.parseInt(matcher.group(2), 10);
                blue = Integer.parseInt(matcher.group(3), 10);
                String str = matcher.group(4);
                if (str != null) {
                    alpha = Integer.parseInt(str, 10);
                }
                return new Color(red, green, blue, alpha);
            }
        }
        catch (NumberFormatException nfe) {
        }

        float hue, saturation, value, alphaf;

        matcher = hsbPattern.matcher(input);
        try {
            if (matcher.matches()) {
                hue = Float.parseFloat(matcher.group(1));
                saturation = Float.parseFloat(matcher.group(2));
                value = Float.parseFloat(matcher.group(3));
                String str = matcher.group(4);
                if (str != null) {
                    alphaf = Float.parseFloat(str);
                    if (alphaf > 1.0) {
                        alphaf = 1.0f;
                    }
                    alpha = (int)(alphaf * 255);
                }
                return new Color((Color.HSBtoRGB(hue, saturation, value) & ((1 << 24) - 1)) | (alpha << 24), true);
            }
        }
        catch (NumberFormatException nfe) {
        }

        if (input.equals("transparent")) {
            return transparent;
        }
        else if (input.equals("black")) {
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
        else {
            return null;
        }
    }
}
