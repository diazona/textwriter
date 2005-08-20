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
<%@page import="java.util.Date"%>
<%@page import="java.util.WeakHashMap"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.concurrent.locks.ReentrantReadWriteLock"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.util.regex.PatternSyntaxException"%>
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
    static String logs = "";
    static long id = System.currentTimeMillis();

    static {
        logs += new Date() + ": static initializer (id=" + id + ")<br>\n";
        initFontArray(false);
        searchFonts(new File(".")); // TODO: rework this to use the servlet init() method
        refreshFontNames();
    }

    // initializes the font list from the GraphicsEnvironment
    static void initFontArray(boolean clear) {
        logs += new Date() + ": reinitializing font array (clear=" + clear + ")<br>\n";
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
        logs += new Date() + ": searching font directory (dir=" + dir.getAbsolutePath() + ")<br>\n";
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
        logs += new Date() + ": recreating font list<br>\n";
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
    public int renderText(String text, Font font, Color bgColor, Color fgColor) {
        BufferedImage image = new BufferedImage(font.getSize() * text.length() + 2, font.getSize() + 2, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
        Rectangle2D bounds = layout.getBounds();
        // fill the background
        g.setColor(bgColor);
        g.fillRect(0, 0, (int)(bounds.getWidth() + 1), (int)(bounds.getHeight() + 1));
        // draw the text
        g.setColor(fgColor);
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

    static Pattern hexPattern = null;
    static Pattern rgbPattern = null;
    static Pattern hsbPattern = null;

    static {
        try {
            // recognizes a string of six or eight consecutive hex digits, with
            // an optional 0x in front
            hexPattern = Pattern.compile("(?:0x)?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})?");
            // recognizes a possibly parenthesized grouping of three or four
            // integer coordinates separated by an optional comma and/or whitespace
            String rgbgroup = "(\\d{0,3})";
            String rgbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + rgbgroup + ")";
            rgbPattern = Pattern.compile("\\(?\\s*" + rgbgroup + rgbrep + rgbrep + rgbrep + "?\\s*\\)?");
            // recognizes a possibly parenthesized grouping of three or four
            // decimal coordinates separated by an optional comma and/or whitespace
            String hsbgroup = "([01]\\.\\d+)";
            String hsbrep = "(?:(?:\\s*\\,\\s*|\\s+)" + hsbgroup + ")";
            hsbPattern = Pattern.compile("\\(?\\s*" + hsbgroup + hsbrep + hsbrep + hsbrep + "?\\s*\\)?");
        }
        catch (PatternSyntaxException pse) {
            System.err.println("pattern error: " + pse.getMessage());
        }
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
        String showLogs = request == null ? null : request.getParameter("logstr");
        if (showLogs != null && Boolean.parseBoolean(showLogs)) {
            out.print(logs);
        }
        %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Ellipsix Programming TextWriter</title>
        <style type="text/css">
        <!--
         #Footer {font-size: x-small; font-style: italic; text-align: center}
         .important {color: red}
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
                <p><b>Important notice for users of TextWriter:</b>
                In both of the major web applications on this site, there has been
                a consistent problem of certain data which is stored on the server
                disappearing for no apparent reason. As yet I don't know what
                is causing this or how to fix it. In order to understand
                this, I'd like all users to <span class="important">check what the
                default font is when you first access the web page</span> - that is,
                the font that is initially selected on the list.  It should be
                Bunchl&oacute; GC. If it is, then everything's fine, but if not,
                <span class="important">please email me right away</span> and
                include in your message the following things:<ul>
                    <li>what shows up as the default font for you</li>
                    <li>the time and date when you first noticed the problem, as
                    exactly as you can determine it</li>
                    <li>the exact URL in your web browser's address bar</li>
                    <li>whether you were in the middle of browsing this site or came
                    here from a link on another website</li>
                    <li>this number: <%= id %>
                </ul>The email address to use is the usual
                <a href="mailto:contact@ellipsix.net">contact@ellipsix.net</a>
                Thanks for your cooperation.</p>
                <p id="Closing">:) David</span></p>
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
                    String fontNm = "Bunchl\u00f3 GC";
                    Integer fontSz = 16;
                    String text = "";
                    int style = 0;
                    String bg = "transparent";
                    String fg = "black";

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

                            fontlock.readLock().lock();
                            Font baseFont = fonts.get(fontNm);
                            fontlock.readLock().unlock();
                            Font renderFont = baseFont.deriveFont(style, fontSz);

                            int imgId = renderText(text, renderFont, bgcolor, fgcolor);
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
                    Text: <input type="text" name="text" value="<%= text %>"><br>
                    Font:
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
                    Size:
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
                    </select>
                    <input type="checkbox" name="bold" value="true"<%= (style & Font.BOLD) > 0 ? " checked" : "" %>>Bold
                    <input type="checkbox" name="italic" value="true"<%= (style & Font.ITALIC) > 0 ? " checked" : "" %>>Italic<br>
                    Background color: <input type="text" name="bgcolor" value="<%= bg %>"><br>
                    Text color: <input type="text" name="fgcolor" value="<%= fg %>"><br>
                    <font size=-1><i>See the note below about acceptable color input</i></font><br>
                    <input type="submit" value="Render">
                </form>
                <hr>
                <p>Colors may be input either as names or as numerical values. Several
                different formats are possible:<ul>
                    <li>Hexadecimal: <b>0x</b>000000<i>00</i><br>The <b>0x</b> at the
                    beginning is optional. The first two digits represent red, the
                    second two represent green, the third two represent blue, and
                    the last two, which are optional, give the alpha channel. If the
                    alpha digits are absent, they are assumed to be FF (opaque). Any
                    sequence of six or eight consecutive hexadecimal digits without
                    whitespace will be parsed as this form.</li>
                    <li>RGBA point: <b>rgb(</b>0, 0, 0<i>, 0</i><b>)</b><br>Each coordinate
                    is a decimal integer from 0-255. The first coordinate is red, the
                    second is green, the third is blue, and the last is alpha. Any
                    combination of whitespace with or without a comma may be used as
                    a separator. The last component (along with its preceding
                    whitespace/comma) may be omitted, in which case it will be
                    assumed as 255 (opaque).</li>
                    <li>HSBA point: <b>hsb(</b>0.0, 0.0, 0.0<i>, 0.0</i><b>)</b><br>
                    Each coordinate is a floating-point value between 0.0 and 1.0,
                    which must include a decimal point. The first coordinate represents
                    hue, the second represents saturation, the third represents
                    brightness, and the optional last component represents alpha.
                    As with rgb, the last component (along with its preceding
                    whitespace/comma) may be omitted, in which case it will be
                    assumed as 1.0 (opaque).</li>
                    <li>Color name: Acceptable names with their hexadecimal equivalents
                    are listed below.<ul>
                        <li><b>transparent</b> = <i>0x00000000</i></li>
                        <li><b>white</b> = <i>0xFFFFFFFF</i></li>
                        <li><b>light gray</b> = <i>0xC0C0C0FF</i></li>
                        <li><b>gray</b> = <i>0x808080FF</i></li>
                        <li><b>dark gray</b> = <i>0x404040FF</i></li>
                        <li><b>black</b> = <i>0x000000FF</i></li>
                        <li><b>red</b> = <i>0xFF0000FF</i></li>
                        <li><b>pink</b> = <i>0xFFAFAFFF</i></li>
                        <li><b>orange</b> = <i>0xFFC800FF</i></li>
                        <li><b>yellow</b> = <i>0xFFFF00FF</i></li>
                        <li><b>green</b> = <i>0x00FF00FF</i></li>
                        <li><b>cyan</b> = <i>0x00FFFFFF</i></li>
                        <li><b>blue</b> = <i>0x0000FFFF</i></li>
                        <li><b>magenta</b> = <i>0xFF00FF</i></li>
                    </ul>Names are not case-sensitive.</li>
                </ul></p>
                <hr>
                <h3>Feature list:</h3>
                <p>Version 0.2.1</p>
                <ul>
                    <li class="complete">Added temporary warning about font problem asking for
                    help from users</li>
                    <li class="complete">Added elementary logging facility</li>
                    <li class="complete">Removed fonts from distribution of web app</li>
                </ul>
                <p>Version 0.2</p>
                <ul>
                    <li class="complete">Added background color and transparency options</li>
                    <li class="complete">Added additional instruction text</li>
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
                    <li class="feature">Rewrite JSP to eliminate use of static initializers</li>
                    <li class="feature">Fix bug with fadas on capital letters</li>
                </ul>
            </div>
            <div id='Footer'>
                <p>&copy;2005 <a href="mailto:contact@ellipsix.net">Ellipsix Programming</a>;
                created by David Zaslavsky.</p>
                <p>This software service is provided by Ellipsix Programming for
                public use on an as-is basis. Please report any errors to
                <a href="mailto:contact@ellipsix.net">contact@ellipsix.net</a>.</p>
            </div>
        </div>
    </body>
</html>
    <%
    }
%>
