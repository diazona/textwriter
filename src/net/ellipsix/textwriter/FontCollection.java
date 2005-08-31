/*
 * FontCollection.java
 *
 * Created on August 21, 2005, 1:29 AM
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
 * Contains
 *
 * @author David Zaslavsky
 */
public class FontCollection {
    HashMap<String, Font> fonts = new HashMap<String, Font>();
    ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    List<String> fontNames;
    ReentrantLock fNamelock = new ReentrantLock(false);
    
    static final String CONTEXT_ID = "fonts";
    
    public static FontCollection retrieve(ServletContext ctx) {
        FontCollection fc = (FontCollection)ctx.getAttribute(CONTEXT_ID);
        if (fc == null) {
            fc = new FontCollection();
            ctx.setAttribute(CONTEXT_ID, fc);
            ctx.log("Installing FontCollection " + System.identityHashCode(fc) + " on context ID = " + System.identityHashCode(ctx));
        }
        return fc;
    }
    
    /** Creates a new instance of FontCollection */
    public FontCollection() {
        initFontArray(false);
        searchFonts(new File("."));
        refreshFontNames();
    }
    
    public List<String> getAllFontNames() {
        return fontNames;
    }
    
    public boolean fontExists(String fontName) {
        fontlock.readLock().lock();
        try {
            return fonts.containsKey(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
    }
    
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
    public void initFontArray(boolean clear) {
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
}
