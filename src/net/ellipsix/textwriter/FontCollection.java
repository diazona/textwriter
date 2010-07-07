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
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains the system's collection of fonts and shared font-related attributes.
 * The collection is internally synchronized so that instances of <code>FontCollection</code>
 * are safe for access by multiple threads.
 * @author David Zaslavsky
 */
public class FontCollection {
    private static final Logger logger = Logger.getLogger("net.ellipsix.textwriter");

    /**
     * A font associated with zero or more key-value attributes.
     */
    public static class TaggedFont {
        // The font
        private Font fnt;
        // The attributes
        private Map<String,String> attributes;
        
        /**
         * Constructs a new {@code TaggedFont} with the given {@link Font} and
         * attributes.
         * @param fnt any {@link Font}
         * @param attrs the attributes
         * @throws IllegalArgumentException if {@code fnt} is {@code null}
         */
        TaggedFont(Font fnt, Map<String,String> attrs) {
            this.fnt = fnt;
            if (fnt == null) {
                throw new IllegalArgumentException("Null font");
            }
            if (attrs == null) {
                attributes = Collections.emptyMap();
            }
            else {
                attributes = Collections.unmodifiableMap(new HashMap<String,String>(attrs));
            }
        }
        
        /**
         * Returns the {@link Font} object.
         */
        public Font getFont() {
            return fnt;
        }
        
        /**
         * Returns the set of attributes attached to this font.
         * The returned {@code Map} is a read-only view created with
         * {@link Collections#unmodifiableMap(Map)}.
         * @return the map of attributes attached to this font
         */
        public Map<String,String> getAttributes() {
            return attributes;
        }
    }
    
    public static final FontCollection getInstance() {
        if (instance == null) {
            logger.finer("Creating FontCollection instance");
            instance = new FontCollection();
        }
        return instance;
    }
    
    private static FontCollection instance = null;
    
    // A map of font names to their corresponding fonts
    private HashMap<String, TaggedFont> fonts = new HashMap<String, TaggedFont>();
    // Access lock for the font map
    private ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    // A set of font names
    private Set<String> fontNames = Collections.unmodifiableSet(fonts.keySet());
    
    public static final String SYSTEM_FONT = "system";
    public static final String UNICODE_FONT = "Unicode";
    
    /**
     * Creates a new instance of {@code FontCollection} and initializes it with the
     * system fonts.
     */
    private FontCollection() {
        addSystemFonts(false);
    }

    /**
     * Returns a font of the given family, style, and size. If the font family name
     * given is not managed by this {@code FontCollection}, then this method returns
     * {@code null}.
     * @param fontName the family name of the desired font
     * @param style an integer indicating the style of the desired font
     * @param size a {@code float} indicating the size, in points, of the desired font
     * @return a {@code Font} object representing the desired font
     * @see Font#deriveFont(int,float)
     */
    public Font getFont(String fontName, int style, float size) {
        logger.entering("FontCollection", "getFont", new Object[] {fontName, style, size});
        TaggedFont tfont;
        fontlock.readLock().lock();
        try {
            tfont = fonts.get(fontName);
        }
        finally {
            fontlock.readLock().unlock();
        }
        
        if (tfont == null) {
            logger.config("No font of name " + fontName + " found");
            return null;
        }
        Font font = tfont.getFont();
        logger.finest("Found font " + font.toString());
        font = font.deriveFont(style, size);
        logger.finer("Returning font " + font.toString());
        return font;
    }
    
    /**
     * Returns a {@link Set} of {@code String} objects representing the names
     * of fonts in this collection.
     * @return a {@code Set} containing the list of font names
     */
    public Set<String> getAllFontNames() {
        return fontNames;
    }
    
    /**
     * Returns {@code true} if a font with the specified name is contained in the
     * collection.
     * @param fontName the name of the font to search for
     * @return {@code true} if a font with the given name is contained in the system, or
     *   {@code false} otherwise
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
     * Gets all the attributes for a given font.
     */
    public Map<String,String> getFontAttributes(String fontName) {
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
     * Gets the complete list of {@link TaggedFont} objects stored by this
     * {@code FontCollection}.
     */
    Collection<TaggedFont> getAllFonts() {
        return Collections.unmodifiableCollection(fonts.values());
    }

    /**
     * Adds the fonts offered by the graphics system to the list of fonts managed by
     * this {@code FontCollection}.
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
                Font font = new Font(fnt, Font.PLAIN, 1);
                Map<String,String> attrs = new HashMap<String,String>();
                attrs.put(SYSTEM_FONT, "1");
                if (fnt.contains("Unicode")) {
                    attrs.put(UNICODE_FONT, "1");
                }
                fonts.put(fnt, new TaggedFont(font, attrs));
            }
        }
        finally {
            fontlock.writeLock().unlock();
        }
    }
    
    /**
     * Adds any fonts in the given directory and any directories under it to the
     * list of fonts managed by this <code>FontCollection</code>.
     * @param dir the directory to search
     */
    public void loadFontsRecursive(File dir) throws FontFormatException, IOException {
        File[] dirList = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        loadFonts(dir);
        for (File d : dirList) {
            loadFontsRecursive(d);
        }
    }

    private static final Pattern fontAttrLine = Pattern.compile("(.+?)=(\".*?\"|.*?)((?::[^:]+?)*)");

    /**
     * Adds fonts from the given file or directory to the list of fonts managed by this
     * {@code FontCollection}.
     *
     * <p>If the given {@code File} is a normal (non-directory) file, it will be parsed as
     * a font file and added. If it is a directory, all files in it with the extension
     * <tt>.ttf</tt> will be added. This method does not recurse into subdirectories of
     * the given directory.</p>
     * <p>If the parameter is a directory and there exists a file named
     * <tt>.font.attributes</tt> in it, then the contents of the file will be used to
     * assign attributes to the fonts in the directory. Each line takes the form
     * <code>key=value:filename1:filename2:filename3...</code>
     * Each font in the list of filenames will receive the attribute <tt>key=value</tt>.
     * The value may be enclosed in double quotes, e.g. if it contains a colon. The
     * value may be the empty string but the key cannot be.</p>
     * <p>As a special case, if the list of filenames is empty, all fonts loaded from the
     * directory will receive the attribute.</p>
     * @param file the file to load or directory to search
     * @return a <code>HashMap</code> containing status information for each font file in the
     * given directory
     */
    public void loadFonts(File file) throws FontFormatException, IOException {
        File[] list;
        
        Map<String, Map<String,String>> attrMap = new HashMap<String, Map<String,String>>();
        
        if (file.isDirectory()) {
            File dirAttrFile = new File(file, ".font.attributes");
            if (dirAttrFile.canRead()) { // parse the attributes file
                try {
                    BufferedReader br = new BufferedReader(new FileReader(dirAttrFile));
                    for (String s = br.readLine(); s != null; s = br.readLine()) {
                        Matcher m = fontAttrLine.matcher(s);
                        if (!m.matches()) {
                            continue;
                        }
                        String key = m.group(1);
                        String value = m.group(2);
                        String[] filenames = m.group(3).split(":");
                        for (String fn : filenames) {
                            Map<String,String> attrs = attrMap.get(fn);
                            if (attrs == null) {
                                attrs = new HashMap<String,String>();
                                attrMap.put(fn, attrs);
                            }
                            attrs.put(key, value);
                        }
                    }
                }
                catch (IOException ioe) {
                    logger.throwing("FontCollection", "loadFonts", ioe);
                }
            }
            // list all TTF files
            list = file.listFiles(new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return filename.toLowerCase().endsWith(".ttf");
                }
            });
        }
        else {
            list = new File[] {file};
        }

        Map<String,String> genericAttrs = attrMap.get("");
        fontlock.writeLock().lock();
        try {
            for (File fnt : list) {
                logger.finest("Loading file " + fnt.getPath());
                // TODO: does it make sense to catch exceptions in here?
                Font font = Font.createFont(Font.TRUETYPE_FONT, fnt);
                if (!fonts.containsKey(font.getFamily())) {
                    // if the font was not already loaded, determine its attributes
                    Map<String,String> fontAttrs = attrMap.get(fnt.getName());
                    if (fontAttrs == null) {
                        if (font.getFamily().contains("Unicode")) {
                            fontAttrs = new HashMap<String,String>();
                            fontAttrs.put(UNICODE_FONT, "1");
                            fontAttrs.putAll(genericAttrs);
                        }
                        else {
                            fontAttrs = genericAttrs;
                        }
                    }
                    else {
                        fontAttrs.putAll(genericAttrs);
                        if (font.getFamily().contains("Unicode")) {
                            fontAttrs.put(UNICODE_FONT, "1");
                        }
                    }
                    logger.finest("Adding font " + font.getFamily() + "; attributes=" + fontAttrs);
                    fonts.put(font.getFamily(), new TaggedFont(font, fontAttrs));
                }
            }
        }
        finally {
            fontlock.writeLock().unlock();
        }
    }
}
