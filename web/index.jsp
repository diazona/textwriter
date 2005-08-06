<%@taglib prefix="jstlc" uri="http://java.sun.com/jsp/jstl/core"%>

<%-- Import all required classes --%>

<%@page import="java.awt.Color"%>
<%@page import="java.awt.Font"%>
<%@page import="java.awt.FontMetrics"%>
<%@page import="java.awt.Graphics2D"%>
<%@page import="java.awt.GraphicsEnvironment"%>
<%@page import="java.awt.RenderingHints"%>
<%@page import="java.awt.geom.Rectangle2D"%>
<%@page import="java.awt.Toolkit"%>
<%@page import="java.awt.font.TextLayout"%>
<%@page import="java.awt.image.BufferedImage"%>
<%@page import="java.awt.image.RasterFormatException"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FilenameFilter"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.WeakHashMap"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.concurrent.locks.ReentrantReadWriteLock"%>
<%@page import="javax.imageio.ImageIO"%>

<%!
    static volatile int index = 0;
    static WeakHashMap<Integer, BufferedImage> images = new WeakHashMap<Integer, BufferedImage>();
    static HashMap<String, Font> fonts = new HashMap<String, Font>();
    static ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    static String[] fontNames;
    static ReentrantReadWriteLock fNamelock = new ReentrantReadWriteLock(false);
    static final Color transparent = new Color(0, 0, 0, 0);
    static final int[] sizeList = {8, 9, 10, 11, 12, 13, 14, 15, 16,
            18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46,
            48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 76, 80,
            84, 90, 96, 100, 108};
    
    static {
        initFontArray(false);
        searchFonts(new File("."));
        refreshFontNames();
    }
    
    // initializes the font list from the GraphicsEnvironment
    static void initFontArray(boolean clear) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontsArr = ge.getAvailableFontFamilyNames();
        fontlock.writeLock().lock();
        if (clear) {
            fonts.clear();
        }
        for (String fnt : fontsArr) {
            fonts.put(fnt, new Font(fnt, Font.PLAIN, 1));
        }
        fontlock.writeLock().unlock();
    }
    
    // adds fonts found in the given directory to the list
    static HashMap<String, String> searchFonts(File dir) {
        HashMap<String, String> alist = new HashMap<String, String>();
        File[] list = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String filename) {
                return filename.toLowerCase().endsWith(".ttf");
            }
        });
        fontlock.writeLock().lock();
        for (File fnt : list) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fnt);
                if (fonts.containsKey(font.getFamily())) {
                    alist.put(fnt.getName(), "already added");
                }
                else {
                    alist.put(fnt.getName(), "added " + font.getFamily());
                    fonts.put(font.getFamily(), font);
                }
            }
            catch (Exception e) {
                alist.put(fnt.getName(), "exception occurred: " + e.getMessage());
            }
        }
        fontlock.writeLock().unlock();
        return alist;
    }
    
    // refreshes the list of font names
    static void refreshFontNames() {
        fontlock.readLock().lock();
        String[] dummy = new String[0];
        String[] newFontNamesArr = fonts.keySet().toArray(dummy);
        fontlock.readLock().unlock();
        Arrays.sort(newFontNamesArr);
        fNamelock.writeLock().lock();
        fontNames = newFontNamesArr;
        fNamelock.writeLock().unlock();
    }
    
    // creates an image with the given text in the given font
    public int renderText(String text, Font font) {
        BufferedImage image = new BufferedImage(font.getSize() * text.length() + 2, font.getSize() + 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
        Rectangle2D bounds = layout.getBounds();
        // fill the background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, (int)(bounds.getWidth() + 1), (int)(bounds.getHeight() + 1));
        // draw the text
        g.setColor(Color.BLACK);
        layout.draw(g, -(float)bounds.getX(), -(float)bounds.getY());
        try {
            image = image.getSubimage(0, 0, (int)(bounds.getWidth() + 1), (int)(bounds.getHeight() + 1));
        }
        catch (RasterFormatException rfe) {
            // ignore
        }
        images.put(++index, image);
        index %= Integer.MAX_VALUE / 2;
        return index;
    }
%>

<%
    String mode = request == null ? null : request.getParameter("mode");
    if (mode != null && mode.equalsIgnoreCase("image")) {
        response.setContentType("image/png");
        int imageId = Integer.parseInt(request.getParameter("imageId"));
        BufferedImage img = images.get(imageId);
        OutputStream rout = response.getOutputStream();
        ImageIO.write(img, "PNG", rout);
        rout.flush();
    }
    else { // HTML mode
        %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Ellipsix Programming TextWriter</title>
        <style type="text/css">
        <!--
         #Footer {font-size: x-small; font-style: italic; text-align: center}
        -->
        </style>
        <link rel='stylesheet' type='text/css' href='/ellipsix.css'>
    </head>
    <body>
        <div id='Banner'>&nbsp;</div>
        <%-- Copied from /include/menu.php --%>
        <div id="Menu">
            <table>
                <tr>
                    <td><a href="http://www.ellipsix.net/index.php">Home</a></td>
                    <td><a href="http://www.ellipsix.net/products.php">Products</a></td>
                    <td><a href="http://www.ellipsix.net/links.php">Links</a></td>
                    <td><a href="http://www.ellipsix.net/contact.php">Contact</a></td>
                    <td><a href="http://www.ellipsix.net/about.php">About Ellipsix</a></td>
                </tr>
            </table>
        </div>
        <%-- end /include/menu.php --%>
        <div id='Body'>
            <div id='Content'>
                <p class="intro">Welcome to Ellipsix Programming!</p>
                <p>This software service is provided by Ellipsix Programming for
                public use on an as-is basis. Please report any errors to 
                <a href="mailto:contact@ellipsix.net">contact@ellipsix.net</a>.</p>
                <hr>
                <%-- This section of code handles searching for new fonts --%>
                <%
                    if (mode != null && mode.equalsIgnoreCase("fonts")) {
                        String refresh = request.getParameter("refresh");
                        boolean refreshReqd = false;
                        if (refresh != null && Boolean.parseBoolean(refresh)) {
                            refreshReqd = true;
                            initFontArray(true);
                        }
                        String dirName = request.getParameter("dir");
                        if (dirName != null) {
                            refreshReqd = true;
                            File dir = new File(dirName);
                            %>
                            Examining directory: <b><%= dir.getAbsolutePath() %></b><br><br>
                            <%
                            HashMap<String, String> alist = searchFonts(dir);
                            %>
                            Status of font update:
                            <%
                            for (String fn : alist.keySet()) {
                                %>
                                <br>File <%= fn %>: <%= alist.get(fn) %>
                                <%
                            }
                        }
                        else {
                            %>
                            <i>null directory name</i>
                            <%
                        }
                        if (refreshReqd) {
                            refreshFontNames();
                        }
                        %>
                        <hr>
                        <%
                    }
                %>
                <%-- This next section acts to handle requests and display the image --%>
                <%
                    String fontNm = "Gaeilge 2";
                    Integer fontSz = 16;
                    String text = "";
                    int style = 0;
                    if (mode != null && mode.equalsIgnoreCase("html")) {
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

                            fontlock.readLock().lock();
                            Font baseFont = fonts.get(fontNm);
                            fontlock.readLock().unlock();
                            Font renderFont = baseFont.deriveFont(style, fontSz);

                            int imgId = renderText(text, renderFont);
                            %>
                            Rendition of "<%= text %>":<br>
                            <img src='<%= request.getRequestURL().toString() %>?mode=image&imageId=<%= imgId %>'>
                            <p>To save this image file to your computer, right-click on the text
                            and choose the option "Save Image As" or "Save As" from the menu.
                            To copy the URL of the image to your system clipboard, right-click
                            the text and select the "Copy Image Location" or "Copy Image URL"
                            option. <i><u>Please note that the URL will only be valid for a limited
                            amount of time.</u></i></p>
                            <hr>
                            <%
                        }
                    }
                %>
                <%-- This section implements the submission form --%>
                <p>To render a text, type it into the text box below and choose the
                attributes you would like.</p>
                <form method="POST" action="index.jsp">
                    <input type="hidden" name="mode" value="html">
                    <input type="text" name="text" value="<%= text %>"><br>
                    <select name="font">
                        <%
                            boolean selected = false;
                            for (String str : fontNames) {
                                if (!selected && str.equalsIgnoreCase(fontNm)) {
                                    selected = true;
                                    %>
                                    <option selected><%= str %></option>
                                    <%
                                }
                                else {
                                    %>
                                    <option><%= str %></option>
                                    <%
                                }
                            }
                        %>
                    </select><br>
                    <select name="size">
                        <%
                            for (int size : sizeList) {
                                if (fontSz.compareTo(size) == 0) {
                                    %>
                                    <option selected><%= size %></option>
                                    <%
                                }
                                else {
                                    %>
                                    <option><%= size %></option>
                                    <%
                                }
                            }
                        %>
                    </select><br>
                    <input type="checkbox" name="bold" value="true"<%= (style & Font.BOLD) > 0 ? " checked" : "" %>>Bold<br>
                    <input type="checkbox" name="italic" value="true"<%= (style & Font.ITALIC) > 0 ? " checked" : "" %>>Italic<br>
                    <%--<select name="bgcolor">
                        <option>
                    </select>--%>
                    <input type="submit">
                </form>
                <hr>
                <h3>Feature list:</h3>
                <p>Version 0.2 (upcoming)</p>
                <ul>
                    <li class="active">Add background color and transparency options</li>
                    <li class="active">Add additional instruction text</li>
                </ul>
                <p>Version 0.1</p>
                <ul>
                    <li class="complete">Added persistence of field values between requests</li>
                    <li class="complete">Enabled text anti-aliasing</li>
                    <li class="complete">Added ~20 fonts to default distribution</li>
                    <li class="complete">Added ability to reset font list</li>
                </ul>
                <p>Version 0.1b</p>
                <ul>
                    <li class="complete">Fixed bug with fada display on lowercase letters</li>
                    <li class="complete">Added some instruction text</li>
                    <li class="complete">Added Ellipsix header</li>
                </ul>
                <p>Version 0.1a</p>
                <ul>
                    <li class="complete">Internal test version</li>
                </ul>
                <p><u>Future releases:</u></p>
                <ul>
                    <li class="feature">Fix bug with fadas on capital letters</li>
                </ul>
            </div>
            <div id='Footer'>
                <p>&copy;2005 <a href="mailto:contact@ellipsix.net">Ellipsix Programming</a>;
                created by David Zaslavsky.</p>
            </div>
        </div>
    </body>
</html>
    <%
    }
%>
