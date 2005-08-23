/*
 * FontManager.java
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

/**
 * Contains
 * @author David Zaslavsky
 */
public class FontCollection {
    static HashMap<String, Font> fonts = new HashMap<String, Font>();
    static ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    static List<String> fontNames;
    static ReentrantLock fNamelock = new ReentrantLock(false);
    
    /** Creates a new instance of FontManager */
    private FontCollection() {
    }
    
    static {
        initFontArray(false);
        searchFonts(new File(".")); // TODO: rework this to use the servlet init() method
        refreshFontNames();
    }
    
    public static List<String> getAllFontNames() {
        return fontNames;
    }
    
    public static boolean fontExists(String fontName) {
        fontlock.readLock().lock();
        try {
            return fonts.containsKey(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
    }
    
    public static Font getFont(String fontName, int style, float size) {
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
    public static void initFontArray(boolean clear) {
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
    public static HashMap<String, String> searchFonts(File dir) {
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
        }
        finally {
            fontlock.writeLock().unlock();
        }
        
        return alist;
    }

    // refreshes the list of font names
    public static void refreshFontNames() {
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
