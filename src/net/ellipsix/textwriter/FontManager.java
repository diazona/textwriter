/*
 * FontManager.java
 *
 * Created on August 21, 2005, 2:04 PM
 */

package net.ellipsix.textwriter;

import java.io.*;
import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 * @author David Zaslavsky
 * @version
 */
public class FontManager extends HttpServlet {
    long id;
    int clid;
    
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        id = System.currentTimeMillis();
        clid = System.identityHashCode(getClass().getClassLoader());
        log("Initializing FontManager; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + System.identityHashCode(getServletContext()));
        
        ServletContext ctx = config.getServletContext();
        String fontdirsParam = ctx.getInitParameter("fontdirs");
        if (fontdirsParam != null) {
            String[] fontdirs = fontdirsParam.split("\\:");
            for (String fontdir : fontdirs) {
                log("Examining font directory: " + fontdir);
                HashMap<String, String> results = FontCollection.retrieve(ctx).searchFonts(new File(fontdir));
                for (String fn : results.keySet()) {
                    log(fn + ": " + results.get(fn));
                }
            }
        }
        
        fontdirsParam = ctx.getInitParameter("datadir") + "/twfonts/";
        
        String[] fontdirs = fontdirsParam.split("\\:");
        for (String fontdir : fontdirs) {
            log("Examining font directory: " + fontdir);
            File d = new File(fontdir);
            if (d.exists()) {
                HashMap<String, String> results = FontCollection.retrieve(ctx).searchFonts(d);
                for (String fn : results.keySet()) {
                    log(fn + ": " + results.get(fn));
                }
            }
        }
        
        FontCollection.retrieve(ctx).refreshFontNames();
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        log("Destroying FontManager; servlet instance ID = " + id + "; class loader ID = " + clid +
                "; context ID = " + System.identityHashCode(getServletContext()));
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        String mode = request == null ? null : request.getParameter("mode");
        log("Processing request: mode=" + mode);

        if (request.getRequestURI().endsWith("FontManager")) {
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
        
        String refresh = request.getParameter("refresh");
        boolean refreshReqd = false;
        if (refresh != null && Boolean.parseBoolean(refresh)) {
            refreshReqd = true;
            FontCollection.retrieve(getServletContext()).initFontArray(true);
        }
        String dirName = request.getParameter("dir");
        if (dirName != null) {
            refreshReqd = true;
            File dir = new File(dirName);
            out.println("Examining directory: <b>" + dir.getAbsolutePath() + "</b><br><br>");
            HashMap<String, String> alist = FontCollection.retrieve(getServletContext()).searchFonts(dir);
            out.println("Status of font update:");
            for (String fn : alist.keySet()) {
                out.println("<br>File " + fn + ": " +  alist.get(fn));
            }
        }
        else {
            out.println("<p>No directory specified</p>");
        }
        if (refreshReqd) {
            FontCollection.retrieve(getServletContext()).refreshFontNames();
        }
        out.println("<p><a href='render.jsp'>Return to TextWriter</a></p>");
        out.println("<hr>");

        if (request.getRequestURI().endsWith("FontManager")) {
            // output the ending HTML
            out.println("</div><div id='Footer'>");
            out.println("<p>&copy;2005 <a href=\"mailto:contact@ellipsix.net\">Ellipsix Programming</a>;");
            out.println("created by David Zaslavsky.</p>");
            out.println("<p>This software service is provided by Ellipsix Programming for");
            out.println("public use on an as-is basis. Please report any errors to");
            out.println("<a href=\"mailto:contact@ellipsix.net\">contact@ellipsix.net</a>.</p>");
            out.println("</div></div></body></html>");
        }

        out.flush();
    }
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Allows remote access to and management of the font set maintained on the system";
    }
}
