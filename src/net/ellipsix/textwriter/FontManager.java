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
    
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        
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
        log(mode);

        if (mode != null && mode.equalsIgnoreCase("fonts")) {
            String refresh = request.getParameter("refresh");
            boolean refreshReqd = false;
            if (refresh != null && Boolean.parseBoolean(refresh)) {
                refreshReqd = true;
                FontCollection.initFontArray(true);
            }
            String dirName = request.getParameter("dir");
            if (dirName != null) {
                refreshReqd = true;
                File dir = new File(dirName);
                out.println("Examining directory: <b>" + dir.getAbsolutePath() + "</b><br><br>");
                HashMap<String, String> alist = FontCollection.searchFonts(dir);
                out.println("Status of font update:");
                for (String fn : alist.keySet()) {
                    out.println("<br>File " + fn + ": " +  alist.get(fn));
                }
            }
            else {
                out.println("<i>null directory name</i>");
            }
            if (refreshReqd) {
                FontCollection.refreshFontNames();
            }
            out.println("<hr>");
        }
        out.flush();
        out.close();
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
        return "Short description";
    }
    
}
