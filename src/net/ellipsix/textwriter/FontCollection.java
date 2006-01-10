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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletContext;

/**
 * Contains the system's collection of fonts and shared font-related attributes.
 * The collection is internally synchronized so that instances of <code>FontCollection</code>
 * are safe for access by multiple threads.
 * @author David Zaslavsky
 */
// TODO: read/write lock for font attributes
public class FontCollection {
    public static class TaggedFont {
        public static class FontAttribute {
            String name;
            String info;
            public FontAttribute(String name) {
                this(name, null);
            }
            public FontAttribute(String name, String info) {
                super();
                this.name = name;
                this.info = info;
            }
            public String getName() {
                return name;
            }
            public String getInfo() {
                return info;
            }
            public String toString() {
                return name + ":" + info;
            }
        }
        
        Font fnt;
        HashSet<FontAttribute> attributes;
        
        public TaggedFont(Font fnt) {
            this.fnt = fnt;
            attributes = new HashSet<FontAttribute>();
        }
        
        public Font getFont() {
            return fnt;
        }
        
        public void setAttributes(Set<FontAttribute> attrs) {
            attributes.addAll(attrs);
        }
        
        public void setAttribute(FontAttribute attr) {
            attributes.add(attr);
        }
        
        public void clearAttributes(Set<FontAttribute> attrs) {
            attributes.removeAll(attrs);
        }
        
        public void clearAttribute(FontAttribute attr) {
            attributes.remove(attr);
        }
        
        public Set getAttributes() {
            return Collections.unmodifiableSet(attributes);
        }
    }
    
    /**
     * The default list of offered font sizes.
     */
    static final int[] defSizeList = {8, 9, 10, 11, 12, 13, 14, 15, 16,
            18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46,
            48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 76, 80,
            84, 90, 96, 100, 108};
    /**
     * The list of font sizes offered by this <code>FontCollection</code>.
     */
    int[] sizeList = null;

    /**
     * A map of font names to their corresponding fonts. This map includes all the fonts
     * managed by this <code>FontCollection</code>.
     */
    HashMap<String, TaggedFont> fonts = new HashMap<String, TaggedFont>();
    /**
     * Access lock for the font map.
     */
    ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    /**
     * The list of font names managed by this <code>FontCollection</code>.
     */
    List<String> fontNames;
    /**
     * Access lock for the font name list.
     */
    ReentrantLock fNamelock = new ReentrantLock(false);
    
    /**
     * The name used as an attribute key in a <code>ServletContext</code> to identify a
     * <code>FontCollection</code>.
     */
    static final String CONTEXT_ID = "fonts";
    
    HashMap<String, TaggedFont.FontAttribute> indexedAttributes;
    
    public static final TaggedFont.FontAttribute SYSTEM_FONT = new TaggedFont.FontAttribute("system");
    public static final TaggedFont.FontAttribute UNICODE_FONT = new TaggedFont.FontAttribute("Unicode");
    
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
        indexedAttributes = new HashMap<String, TaggedFont.FontAttribute>();
        searchFonts(new File("."));
        addSystemFonts(false);
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
        indexedAttributes = new HashMap<String, TaggedFont.FontAttribute>();
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
                HashMap<String, String> results = searchFontsRecursive(d, null);
                for (String fn : results.keySet()) {
                    ctx.log(fn + ": " + results.get(fn));
                }
            }
        }
        
        // add any system fonts which were not there already
        if (addSystemFonts) {
            ctx.log("Adding system fonts");
            addSystemFonts(false);
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
    
    public TaggedFont.FontAttribute getIndexedAttribute(String name) {
        return getIndexedAttribute(name, null);
    }
    
    public TaggedFont.FontAttribute getIndexedAttribute(String name, String info) {
        TaggedFont.FontAttribute newAttr = indexedAttributes.get(name);
        if (newAttr == null) {
            newAttr = new TaggedFont.FontAttribute(name, info);
            indexedAttributes.put(name, newAttr);
        }
        return newAttr;
    }
    
    public Set<String> getIndexedAttributeNames() {
        return indexedAttributes.keySet();
    }
    
    /**
     * Returns a font of the given family, style, and size. If the font family name
     * given is not managed by this <code>FontCollection</code>, then this method returns
     * <code>null</code>.
     * @param fontName the family name of the desired font
     * @param style an integer indicating the style of the desired font
     * @param size a <code>float</code> indicating the size, in points, of the desired font
     * @return a <code>Font</code> object representing the desired font
     * @see Font#deriveFont(int,float)
     */
    public Font getFont(String fontName, int style, float size) {
        TaggedFont tfont;
        fontlock.readLock().lock();
        try {
            tfont = fonts.get(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
        
        if (tfont == null) {
            return null;
        }
        Font font = tfont.getFont();
        if (font == null) {
            return null;
        }
        return font.deriveFont(style, size);
    }
    
    public Set<TaggedFont.FontAttribute> getFontAttributes(String fontName) {
        TaggedFont tfont;
        fontlock.readLock().lock();
        try {
            tfont = fonts.get(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
        
        if (tfont == null) {
            return null;
        }
        return tfont.getAttributes();
    }

    /**
     * Adds the fonts offered by the graphics system to the list of fonts managed by
     * this <code>FontCollection</code>.
     * @param clear whether to clear out the existing list of fonts before adding the system fonts
     * @see GraphicsEnvironment#getAvailableFontFamilyNames()
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
                if (fonts.containsKey(fnt)) {
                    continue;
                }
                TaggedFont tf = new TaggedFont(new Font(fnt, Font.PLAIN, 1));
                tf.setAttribute(SYSTEM_FONT);
                if (fnt.contains("Unicode")) {
                    tf.setAttribute(UNICODE_FONT);
                }
                fonts.put(fnt, tf);
            }
        }
        finally {
            fontlock.writeLock().unlock();
        }
    }
    
    /**
     * Adds any fonts in the given directory and any directories under it to the
     * list of fonts managed by this <code>FontCollection</code>.
     * <p>The <code>HashMap</code> returned from this method contains as its keys the
     * names of the font files in the given directory and its subdirectories.
     * For each font file name, the associated value is a human-readable string
     * indicating the status of that font file: whether the font contained in it
     * was added to the list, or the font conflicted with one already in the list
     * and was not added, or the file was corrupted and could not be read as a font,
     * etc.</p>
     * @param dir the directory to search
     * @param results a <code>HashMap</code> to store the results in, or <code>null</code>
     * @return a <code>HashMap</code> containing status information for each font file in the
     * given directory, which will be the same as the parameter <code>results</code> if
     * that parameter was non-null
     */
    public HashMap<String, String> searchFontsRecursive(File dir, HashMap<String, String> results) {
        if (results == null) {
            results = new HashMap<String, String>();
        }
        File[] dirList = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        results.putAll(searchFonts(dir));
        for (File d : dirList) {
            searchFontsRecursive(d, results);
        }
        return results;
    }

    /**
     * Adds any fonts in the given directory to the list of fonts managed by this
     * <code>FontCollection</code>. If the given <code>File</code> is an actual
     * file, rather than a directory, then the given <code>File</code> itself will
     * be checked, and if it is a font file, it will be added. This method does not
     * recurse into subdirectories of the given directory.
     * <p>If the parameter is a directory and there exists a file named
     * <code>.font.attributes</code> in it, then a font attribute will be created
     * for each non-empty line of the file, with a name corresponding to the contents
     * of that line, and will be tagged to each font added from the directory.
     * If any line contains a double colon (&quot;<code>::</code>&quot;)
     * then only the part of the line after the first such double colon is used for
     * the name of the attribute. (Occurences of the double colon sequence
     * after the first on a line are not treated specially.) The part of the line
     * before the double colon is parsed as a file name, and only a font imported
     * from the file with that name (if any) will have that attribute added to it.</p>
     * <p>The <code>HashMap</code> returned from this method contains as its keys the
     * names of the font files in the given directory. For each font file name, the
     * associated value is a human-readable string indicating the status of that font
     * file: whether the font contained in it was added to the list, or the font
     * conflicted with one already in the list and was not added, or the file was
     * corrupted and could not be read as a font, etc.</p>
     * @param dir the directory to search
     * @return a <code>HashMap</code> containing status information for each font file in the
     * given directory
     */
    public HashMap<String, String> searchFonts(File dir) {
        HashMap<String, String> alist = new HashMap<String, String>();
        File[] list;
        
        HashSet<TaggedFont.FontAttribute> attrs = new HashSet<TaggedFont.FontAttribute>();
        LinkedList<String> spAttrs = new LinkedList<String>();
        attrs.add(new TaggedFont.FontAttribute("source", dir.getAbsolutePath()));
        
        if (dir.isDirectory()) {
            File dirAttrFile = new File(dir, ".font.attributes");
            if (dirAttrFile.canRead()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(dirAttrFile));
                    for (String s = br.readLine(); s != null; s = br.readLine()) {
                        s = s.trim();
                        if (s.length() == 0) {
                            continue;
                        }
                        if (s.contains("::")) {
                            spAttrs.add(s);
                        }
                        else {
                            attrs.add(getIndexedAttribute(s));
                        }
                    }
                }
                catch (IOException ioe) {
                }
            }
            
            list = dir.listFiles(new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return filename.toLowerCase().endsWith(".ttf");
                }
            });
        }
        else {
            list = new File[] {dir};
        }
        
        fontlock.writeLock().lock();
        try {
            for (File fnt : list) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fnt);
                    if (fonts.containsKey(font.getFamily())) {
                        alist.put(fnt.getName(), "already added " + font.getFamily());
                    }
                    else {
                        String ff = font.getFamily();
                        alist.put(fnt.getName(), "added " + ff);
                        
                        TaggedFont tf = new TaggedFont(font);
                        tf.setAttributes(attrs);
                        if (ff.contains("Unicode")) {
                            tf.setAttribute(UNICODE_FONT);
                        }
                        ListIterator<String> spIterator = spAttrs.listIterator();
                        while (spIterator.hasNext()) {
                            String spA = spIterator.next();
                            if (spA.startsWith(fnt.getName())) {
                                int splitIdx = spA.indexOf("::", fnt.getName().length());
                                tf.setAttribute(getIndexedAttribute(spA.substring(splitIdx + 2)));
                                spIterator.remove();
                            }
                        }
                        fonts.put(font.getFamily(), tf);
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

    /**
     * Recreates the cached list of font names managed by this <code>FontCollection</code>.
     * This should be called after any operation or sequence of operations which changes
     * the actual list of fonts managed by this <code>FontCollection</code>.
     */
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
     * Returns the suggested list of sizes offered by this <code>FontCollection</code>.
     * It is possible to obtain fonts from this <code>FontCollection</code> with sizes
     * which are not contained in this list.
     * @return the list of sizes
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
