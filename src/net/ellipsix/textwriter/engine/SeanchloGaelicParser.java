/*
 * SeanchloGaelicParser.java
 *
 * Created on September 20, 2005, 2:35 AM
 */

package net.ellipsix.textwriter.engine;

import java.awt.Font;
import java.util.HashMap;

/**
 *
 * @author David Zaslavsky
 */
public class SeanchloGaelicParser implements InputParser<SeanchloCharCodeMap> {
    HashMap<String, SeanchloCharCodeMap> charmaps;
    
    /** Creates a new instance of SeanchloGaelicParser */
    public SeanchloGaelicParser() {
        super();
        charmaps = new HashMap<String, SeanchloCharCodeMap>();
    }

    public void addCharacterCodeMap(SeanchloCharCodeMap cmap) {
        charmaps.put(cmap.getFontName(), cmap);
    }

    public String prepForFont(String input, Font fnt) {
        return prepForFont(input, fnt.getFamily());
    }

    public String prepForFont(String input, String fontname) {
        SeanchloCharCodeMap cmap = charmaps.get(fontname);
        // TODO: perhaps add something to search for a font class if the font
        // name is not found
        if (cmap == null) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        // Plan:
        // -replace \'a with a-fada etc.
        // -replace \.b with dotted b etc.
        // -replace & with tyronian symbol
        // -replace r (except \r) with seanchlo r
        // -replace s (except \s) with seanchlo s
        int index = -1;
        while ((index = sb.indexOf("\\'", index + 1)) >= 0) {
            //sb.replace(index); // something
        }
    }

    public void removeCharacterCodeMap(SeanchloCharCodeMap cmap) {
    }
    
}
