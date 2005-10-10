/*
 * FontCollection.java
 *
 * Created on August 21, 2005, 1:29 AM
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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletContext;

/**
 * Contains the system's collection of fonts and shared font-related attributes.
 * The collection is internally synchronized so that instances of <code>FontCollection</code>
 * are safe for access by multiple threads.
 * @author David Zaslavsky
 */
// TODO: provide a mechanism for organizing fonts into categories
public class FontCollection {
    static final int[] defSizeList = {8, 9, 10, 11, 12, 13, 14, 15, 16,
            18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46,
            48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 76, 80,
            84, 90, 96, 100, 108};
    int[] sizeList = null;

    HashMap<String, Font> fonts = new HashMap<String, Font>();
    ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    List<String> fontNames;
    ReentrantLock fNamelock = new ReentrantLock(false);
    
    static final String CONTEXT_ID = "fonts";
    
    /**
     * Retrieves the <code>FontCollection</code> associated with the given
     * <code>ServletContext</code>. If there is no preexisting <code>FontCollection</code>,
     * a new one is created and added to the <code>ServletContext</code>.
     * @param ctx the <code>ServletContext</code> representing the application instance which uses the <code>FontCollection</code>
     * @return the <code>FontCollection</code> for the web application
     */
    public static FontCollection retrieve(ServletContext ctx) {
        FontCollection fc = (FontCollection)ctx.getAttribute(CONTEXT_ID);
        if (fc == null) {
            fc = new FontCollection(ctx);
            ctx.setAttribute(CONTEXT_ID, fc);
            ctx.log("Installing FontCollection " + System.identityHashCode(fc) + " on context ID = " + System.identityHashCode(ctx));
        }
        return fc;
    }
    
    /**
     * Creates a new instance of <code>FontCollection</code> and initializes it with the
     * system fonts and any fonts from the working directory.
     */
    public FontCollection() {
        addSystemFonts(false);
        searchFonts(new File("."));
        refreshFontNames();
    }
    
    /**
     * Creates a new instance of <code>FontCollection</code> based on the given
     * <code>ServletContext</code>. This constructor is equivalent to calling
     * <code>FontCollection(ServletContext, boolean)</code> with a second parameter of
     * <code>true</code>.
     * @param ctx the <code>ServletContext</code> to initialize from
     */
    public FontCollection(ServletContext ctx) {
        this(ctx, true);
    }
    
    /**
     * Creates a new instance of <code>FontCollection</code> based on the given
     * <code>ServletContext</code>.
     * <p>If the <code>addSystemFonts</code> parameter is <code>true</code>, the font
     * list will be populated with the fonts available from the
     * <code>GraphicsEnvironment</code>, which are normally whichever fonts are installed
     * on the system. Also, if the initialization parameter <code>datadir</code>
     * is present in the context, &quot;/twfonts/&quot; will be appended to it and the
     * directory named by the resulting string (if it exists) will be examined for
     * fonts in the manner of <code>searchFonts(String)</code>.</p>
     * @param addSystemFonts whether or not to add the fonts from the <code>GraphicsEnvironment</code>
     * @param ctx the <code>ServletContext</code> to initialize from
     * @see #searchFonts(String)
     */
    public FontCollection(ServletContext ctx, boolean addSystemFonts) {
        if (addSystemFonts) {
            addSystemFonts(false);
        }
        
        // add font directories
        String fontdirsParam = ctx.getInitParameter("fontdirs");
        if (fontdirsParam != null) {
            String[] fontdirs = fontdirsParam.split("\\:");
            for (String fontdir : fontdirs) {
                ctx.log("Examining font directory: " + fontdir);
                HashMap<String, String> results = searchFonts(new File(fontdir));
                for (String fn : results.keySet()) {
                    ctx.log(fn + ": " + results.get(fn));
                }
            }
        }
        
        // add default font directory [datadir]/twfonts/
        fontdirsParam = ctx.getInitParameter("datadir") + "/twfonts/";
        
        String[] fontdirs = fontdirsParam.split("\\:");
        for (String fontdir : fontdirs) {
            ctx.log("Examining font directory: " + fontdir);
            File d = new File(fontdir);
            if (d.exists()) {
                HashMap<String, String> results = searchFonts(d);
                for (String fn : results.keySet()) {
                    ctx.log(fn + ": " + results.get(fn));
                }
            }
        }
        
        refreshFontNames();
        
        // initialize font size list
        String sizesParam = ctx.getInitParameter("sizelist");
        if (sizesParam != null) {
            String[] sizesS = sizesParam.split("\\,\\s?");
            int[] sizes = new int[sizesS.length];
            for (int i = 0; i < sizesS.length; i++) {
                sizes[i] = Integer.parseInt(sizesS[i]);
            }
        }
    }
    
    /**
     * Returns a {@link java.util.List List} of <code>String</code> objects containing
     * the names of fonts in this collection. The list corresponds to the set of fonts
     * that was available the last time the font name list was refreshed.
     * @return a <code>List</code> containing the list of font names
     * @see #refreshFontNames()
     */
    public List<String> getAllFontNames() {
        return fontNames;
    }
    
    /**
     * Returns <code>true</code> if a font with the specified name is contained in the
     * collection. This determination is based on the actual set of fonts contained,
     * independent of the name list, so it is possible for <code>fontExists</code> to
     * return a value that is not contained in the list returned by {@link
     * #getAllFontNames()} (or vice versa) if the font name list has not been refreshed
     * since the last changes.
     * @param fontName the name of the font to search for
     * @return <code>true</code> if a font with the given name is contained in the system, or <code>false</code> otherwise
     */
    public boolean fontExists(String fontName) {
        fontlock.readLock().lock();
        try {
            return fonts.containsKey(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
    }
    
    /**
     * 
     * @param fontName 
     * @param style 
     * @param size 
     * @return 
     */
    public Font getFont(String fontName, int style, float size) {
        Font font;
        fontlock.readLock().lock();
        try {
            font = fonts.get(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
        
        if (font == null) {
            return null;
        }
        return font.deriveFont(style, size);
    }
    
    // initializes the font list from the GraphicsEnvironment
    /**
     * 
     * @param clear 
     */
    public void addSystemFonts(boolean clear) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontsArr = ge.getAvailableFontFamilyNames();
        
        fontlock.writeLock().lock();
        try {
            if (clear) {
                fonts.clear();
            }
            for (String fnt : fontsArr) {
                fonts.put(fnt, new Font(fnt, Font.PLAIN, 1));
            }
        }
        finally {
            fontlock.writeLock().unlock();
        }
    }

    // adds fonts found in the given directory to the list
    /**
     * 
     * @param dir 
     * @return 
     */
    public HashMap<String, String> searchFonts(File dir) {
        HashMap<String, String> alist = new HashMap<String, String>();
        File[] list = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String filename) {
                return filename.toLowerCase().endsWith(".ttf");
            }
        });
        
        fontlock.writeLock().lock();
        try {
            for (File fnt : list) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fnt);
                    if (fonts.containsKey(font.getFamily())) {
                        alist.put(fnt.getName(), "already added " + font.getFamily());
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
        }
        finally {
            fontlock.writeLock().unlock();
        }
        
        return alist;
    }

    // refreshes the list of font names
    public void refreshFontNames() {
        List<String> newFontNameList;
        fontlock.readLock().lock();
        try {
            newFontNameList = new ArrayList<String>(fonts.keySet());
        }
        finally {
            fontlock.readLock().unlock();
        }
        
        Collections.sort(newFontNameList);
        
        fNamelock.lock();
        try {
            fontNames = Collections.unmodifiableList(newFontNameList);
        }
        finally {
            fNamelock.unlock();
        }
    }
    
    /**
     * 
     * @return 
     */
    public int[] getFontSizeList() {
        if (sizeList != null) {
            return sizeList;
        }
        else {
            return defSizeList;
        }
    }
}
