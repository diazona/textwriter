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
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Contains the system's collection of fonts and shared font-related attributes.
 * The collection is internally synchronized so that instances of <code>FontCollection</code>
 * are safe for access by multiple threads.
 * @author David Zaslavsky
 */
public class FontCollection {
    /**
     * A font associated with zero or more attributes.
     *
     * There are no attribute values; all the information an attribute conveys is
     * whether a particular font has it or not.
     */
    public static class TaggedFont {
        // The font
        private Font fnt;
        // The attributes
        private HashSet<String> attributes;
        
        /**
         * Constructs a new {@code TaggedFont} with the given {@link Font} and
         * attributes.
         * @param fnt any {@link Font}
         * @param attrs the attributes
         * @throws IllegalArgumentException if {@code fnt} is {@code null}
         */
        TaggedFont(Font fnt, String... attrs) {
            this(fnt, Arrays.asList(attrs));
        }
        
        /**
         * Constructs a new {@code TaggedFont} with the given {@link Font} and
         * attributes.
         * @param fnt any {@link Font}
         * @param attrs the attributes
         * @throws IllegalArgumentException if {@code fnt} is {@code null}
         */
        TaggedFont(Font fnt, Collection<String> attrs) {
            this.fnt = fnt;
            if (fnt == null) {
                throw new IllegalArgumentException("Null font");
            }
            if (attrs == null) {
                attributes = Collections.emptySet();
            }
            else {
                attributes = Collections.unmodifiableSet(new HashSet<String>(attrs));
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
         * The returned {@code Set} is a read-only view created with
         * {@link Collections#unmodifiableSet(Set)}.
         * @return the set of attributes attached to this font
         */
        public Set<String> getAttributes() {
            return attributes;
        }
    }
    
    public static final FontCollection getInstance() {
        if (instance == null) {
            instance = new FontCollection();
        }
        return instance;
    }
    
    private static FontCollection instance = null;
    
    // A map of font names to their corresponding fonts
    private HashMap<String, TaggedFont> fonts = new HashMap<String, TaggedFont>();
    // Access lock for the font map
    private ReentrantReadWriteLock fontlock = new ReentrantReadWriteLock(false);
    
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
        return font.deriveFont(style, size);
    }
    
    /**
     * Returns a {@link java.util.List List} of {@code String} objects containing
     * the names of fonts in this collection. The list corresponds to the set of fonts
     * that was available the last time the font name list was refreshed.
     * @return a {@code List} containing the list of font names
     * @see #refreshFontNames()
     */
    public List<String> getAllFontNames() {
        return fontNames;
    }
    
    /**
     * Returns {@code true} if a font with the specified name is contained in the
     * collection. This determination is based on the actual set of fonts contained,
     * independent of the name list, so it is possible for <code>fontExists</code> to
     * return a value that is not contained in the list returned by {@link
     * #getAllFontNames()} (or vice versa) if the font name list has not been refreshed
     * since the last changes.
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
    public Set<String> getFontAttributes(String fontName) {
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
                if (fnt.contains("Unicode")) {
                    fonts.put(fnt, new TaggedFont(font, SYSTEM_FONT, UNICODE_FONT));
                }
                else {
                    fonts.put(fnt, new TaggedFont(font, SYSTEM_FONT));
                }
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
    public void loadFontsRecursive(File dir) {
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

    private static final Pattern delimiter = Pattern.compile("[:,\\s]");

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
     * assign attributes to the fonts in the directory as follows. Each line consists of
     * a list of words delimited by any of the characters {@code ':'}, {@code ','}, and/or
     * any whitespace character. (These are all treated interchangeably by the parser)
     * The first word on each line is a filename, and any remaining words are attributes
     * to be applied to any font(s) loaded from a file with that name.</p>
     * <p>As a special case, if the first character on a line is a delimiter character,
     * each word on the line will be considered an attribute, and those attributes will
     * be applied to all fonts loaded from the directory.</p>
     * @param file the file to load or directory to search
     * @return a <code>HashMap</code> containing status information for each font file in the
     * given directory
     */
    public void loadFonts(File file) {
        File[] list;
        
        Map<String, Set<String>> attrMap = new HashMap<String, Set<String>>();
        
        if (file.isDirectory()) {
            File dirAttrFile = new File(file, ".font.attributes");
            if (dirAttrFile.canRead()) { // parse the attributes file
                try {
                    BufferedReader br = new BufferedReader(new FileReader(dirAttrFile));
                    for (String s = br.readLine(); s != null; s = br.readLine()) {
                        s = s.trim();
                        if (s.length() == 0) {
                            continue;
                        }
                        String[] words = splitter.split(s);
                        String fontFilename;
                        int i; // index of first attribute in the list of words
                        if (delimiter.matcher(s.substring(0,1)).matches()) {
                            fontFilename = ""; // special value used to index the attributes applying to all fonts
                            i = 0;
                        }
                        else {
                            fontFilename = words[0];
                            i = 1;
                        }
                        Set<String> attrs = attrMap.get(fontFilename);
                        if (attrs == null) {
                            attrs = new HashSet<String>();
                            attrMap.put(fontFilename, attrs);
                        }
                        while (i++ < words.length) {
                            attrs.add(words[i]);
                        }
                    }
                }
                catch (IOException ioe) {
                    // if there's an error reading it, pretend it doesn't exist
                }
            }
            // list all TTF files
            list = dir.listFiles(new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return filename.toLowerCase().endsWith(".ttf");
                }
            });
        }
        else {
            list = new File[] {file};
        }

        Set<String> genericAttrs = attrMap.get("");
        fontlock.writeLock().lock();
        try {
            for (File fnt : list) {
                // TODO: does it make sense to catch exceptions in here?
                Font font = Font.createFont(Font.TRUETYPE_FONT, fnt);
                if (!fonts.containsKey(font.getFamily())) {
                    // if the font was not already loaded, determine its attributes
                    Set<String> fontAttrs = attrMap.get(fnt.getName());
                    if (fontAttrs == null) {
                        if (font.getFamily().contains("Unicode")) {
                            fontAttrs = new HashSet<String>();
                            fontAttrs.add(UNICODE_FONT);
                            fontAttrs.addAll(genericAttrs);
                        }
                        else {
                            fontAttrs = genericAttrs;
                        }
                    }
                    else {
                        fontAttrs.addAll(genericAttrs);
                        if (font.getFamily().contains("Unicode")) {
                            fontAttrs.add(UNICODE_FONT);
                        }
                    }
                    fonts.put(font.getFamily(), new TaggedFont(font, fontAttrs));
                }
            }
        }
        finally {
            fontlock.writeLock().unlock();
        }
    }
}
