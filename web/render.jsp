<%--
  The content of this file is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.
 
  This file is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this file; if not, write to
 
  Free Software Foundation, Inc.
  59 Temple Place, Suite 330
  Boston, MA 02111-1307 USA
 
  or download the license from the Free Software Foundation website at
 
  http://www.gnu.org/licenses/gpl.html
--%>

<%@taglib prefix="jstlc" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="incl" tagdir="/WEB-INF/tags"%>

<%-- Import all required classes --%>

<%@page import="java.awt.Font"%>
<%@page import="java.util.Set"%>
<%@page import="net.ellipsix.textwriter.FontCollection"%>

<%!
    long id;
    int clid;
    int cid;
    
    public void jspInit() {
        id = System.currentTimeMillis();
        clid = System.identityHashCode(getClass().getClassLoader());
        cid = System.identityHashCode(getServletContext());
        
        log("Initializing render.jsp; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + cid);
    }
    
    public void jspDestroy() {
        log("Destroying render.jsp; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + cid);
    }
%>

<%
    String mode = request == null ? "" : request.getParameter("mode");
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
        <incl:menu/>
        <%-- Copied from /include/menu.php --%>
        <%--
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
        --%>
        <%-- end /include/menu.php --%>
        <div id='Body'>
            <div id='Content'>
                <p class="intro">Welcome to Ellipsix Programming!</p>
                <%
                    if (!FontCollection.retrieve(application).fontExists("Bunchl\u00f3 GC")) {
                        log("Detected font absence; servlet instance id=" + id);
                        /*for (String str : FontCollection.getAllFontNames()) {
                            log("Existing font: " + str);
                        }*/
                        log("Reconstructed request: " + request.getMethod() + " " + 
                            request.getRequestURL() + (request.getQueryString() == null ?
                            "" : ("?" + request.getQueryString())));
                        %>
                        <p><b>Important notice for users of TextWriter:</b>
                        In both of the major web applications on this site, there has been
                        a consistent problem of certain data which is stored on the server
                        disappearing for no apparent reason. I have taken steps to try to
                        fix this, but it appears that something else has gone wrong. To aid
                        in debugging, I'd appreciate it if you would
                        <span class="important">please email me</span> at your earliest
                        convenience and include in your message the following things:<ul>
                            <li>the time and date when you first noticed the problem, as
                            exactly as you can determine it</li>
                            <li>the exact URL in your web browser's address bar</li>
                            <li>whether you were in the middle of browsing this site or came
                            here from a link on another website</li>
                            <li>the <i>instance ID</i>, which is <%= id %></li>
                            <li>the <i>loader ID</i>, which is <%= clid %></li>
                        </ul>The email address to use is the usual
                        <a href="mailto:contact@ellipsix.net">contact@ellipsix.net</a>
                        Thanks for your cooperation.</p>
                        <p id="Closing">:) David</span></p>
                        <hr>
                        <%
                    }
                %>
                <%-- This section of code handles searching for new fonts --%>
                <%
                    if (mode != null && mode.equalsIgnoreCase("fonts")) {
                        out.flush();
                        application.getNamedDispatcher("FontManager").include(request, response);
                    }
                %>
                <%-- This next section acts to handle requests and display the image --%>
                <%
                    // defaults
                    String fontNm = "Bunchl\u00f3 GC";
                    Integer fontSz = 16;
                    String text = "";
                    int style = 0;
                    String bg = "transparent";
                    String fg = "black";

                    if (mode != null && mode.equalsIgnoreCase("image")) {
                        out.flush();
                        application.getNamedDispatcher("ImageDisplay").include(request, response);

                        fontNm = request.getParameter("font");
                        fontSz = Integer.parseInt(request.getParameter("size"));
                        text = request.getParameter("text");

                        String bold = request.getParameter("bold");
                        String italic = request.getParameter("italic");
                        if (bold != null && bold.equalsIgnoreCase("true")) {
                            style |= Font.BOLD;
                        }
                        if (italic != null && italic.equalsIgnoreCase("true")) {
                            style |= Font.ITALIC;
                        }

                        bg = request.getParameter("bgcolor");
                        fg = request.getParameter("fgcolor");
                    }
                %>
                <%-- This section implements the submission form --%>
                <p>To render a text, type it into the text box below and choose the
                attributes you would like.<br>
                <font size=-1><i>See the notes below about special characters and
                acceptable color input.</i></font>
                <form method="POST" action="render.jsp">
                    <input type="hidden" name="mode" value="image">
                    Text: <input type="text" name="text" value="<%= text %>"><br>
                    Font:
                    <select name="font">
                        <%
                            boolean selected = false;
                            FontCollection ftc = FontCollection.retrieve(application);
                            for (String name : ftc.getAllFontNames()) {
                                String disp;
                                Set<FontCollection.TaggedFont.FontAttribute> attrs = ftc.getFontAttributes(name);
                                if (attrs != null && attrs.size() > 0) {
                                    StringBuilder attrlist = new StringBuilder(" (");
                                    for (FontCollection.TaggedFont.FontAttribute a : attrs) {
                                        if (a.getName() != "source") {
                                            attrlist.append(a.getName()).append(",");
                                        }
                                    }
                                    if (attrlist.length() > 2) {
                                        attrlist.deleteCharAt(attrlist.length() - 1).append(")");
                                        disp = name + attrlist.toString();
                                    }
                                    else {
                                        disp = name;
                                    }
                                }
                                else {
                                    disp = name;
                                }
                                if (!selected && name.equalsIgnoreCase(fontNm)) {
                                    selected = true;
                                    %>
                                    <option selected value="<%= name %>"><%= disp %></option>
                                    <%
                                }
                                else {
                                    %>
                                    <option value="<%= name %>"><%= disp %></option>
                                    <%
                                }
                            }
                        %>
                    </select><br>
                    Size:
                    <select name="size">
                        <%
                            int[] sizeList = FontCollection.retrieve(getServletContext()).getFontSizeList();
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
                    <input type="submit" value="Render">
                </form>
                <hr>
                <p><font color="#ff3a03"><b>NEW:</b></font> The fonts in the list are
                labeled with attributes, which are listed in parentheses after the
                font name, in no particular order. The attributes are there to give
                both you (the user) and the computer information about the capabilities
                of the different fonts, especially regarding which accented characters
                each one supports.</p>
                <p><font color="#ff3a03"><b>NEW:</b></font> TextWriter now allows you to put
                certain special characters in your input text by using the following
                escape sequences:<ul>
                    <li><b>\'</b> places an acute accent (fada) over the next letter
                    if it is a vowel. For example, giving as input the text
                    <code>this is \'a t\'est</code> produces a rendition of the
                    string <i>this is &aacute; t&eacute;st</i>. This only works for
                    the fonts which are labeled &quot;Unicode&quot;</li>
                    <li><b>\.</b> places a dot over the next letter if it is a
                    consonant which accepts a dot: b, c, d, f, g, m, p, s, or t
                    (or their capitalized equivalents).  This only works for
                    the fonts which are labeled &quot;Unicode&quot;</li>
                    <li><b>\r</b> substitutes a normal seanchl&oacute; lowercase r
                    in place of the normal miniature capital R, if you are
                    using a seanchl&oacute;-capable Unicode font (any font with the
                    attributes &quot;seanchlo&quot; and &quot;Unicode&quot;).</li>
                    <li><b>\s</b> substitutes a normal seanchl&oacute; lowercase s
                    in place of the normal miniature capital S, if you are
                    using a seanchl&oacute;-capable Unicode font (any font with the
                    attributes &quot;seanchlo&quot; and &quot;Unicode&quot;).</li>
                    <li><b>\&</b> substitutes a standard ampersand in your text
                    in place of the Tyronian sign usually used in seanchl&oacute;,
                    if you areusing a seanchl&oacute;-capable Unicode font (any font
                    with the attributes &quot;seanchlo&quot; and &quot;Unicode&quot;).</li>
                </ul></p>
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
                    <%-- Note that these values are defined in ImageDisplay.parseColor(String) --%>
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
                        <li><b>magenta</b> = <i>0xFF00FFFF</i></li>
                        <li><b>purple</b> = <i>0xAA00AAFF</i></li>
                    </ul>Color designations are not case-sensitive.</li>
                </ul></p>
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
