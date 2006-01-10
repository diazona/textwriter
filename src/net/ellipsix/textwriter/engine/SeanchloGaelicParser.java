/*
 * SeanchloGaelicParser.java
 *
 * Created on September 20, 2005, 2:35 AM
 */

package net.ellipsix.textwriter.engine;

import java.awt.Font;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static net.ellipsix.textwriter.engine.AccentedVowelCodeMap.*;
import static net.ellipsix.textwriter.engine.SeanchloCharCodeMap.*;

/**
 *
 * @author David Zaslavsky
 */
public class SeanchloGaelicParser implements InputParser {
    HashMap<String, HashSet<CharacterCodeMap>> charmaps;
    
    /** Creates a new instance of SeanchloGaelicParser */
    public SeanchloGaelicParser() {
        super();
        charmaps = new HashMap<String, HashSet<CharacterCodeMap>>();
    }

    public void addCharacterCodeMap(CharacterCodeMap cmap) {
        HashSet<CharacterCodeMap> clist = charmaps.get(cmap.getFontName());
        if (clist == null) {
            clist = new HashSet<CharacterCodeMap>();
            charmaps.put(cmap.getFontName(), clist);
        }
        clist.add(cmap);
    }

    public void removeCharacterCodeMap(CharacterCodeMap cmap) {
        HashSet<CharacterCodeMap> clist = charmaps.get(cmap.getFontName());
        if (clist != null) {
            clist.remove(cmap);
        }
    }
    
    public String prepForFont(String input, Font fnt) {
        return prepForFont(input, fnt.getFontName(), fnt.getFamily());
    }

    static enum CharParseState {NONE, FADA, GRAVE, DOT};
    
    private int getCharacter(LinkedList<CharacterCodeMap> cmaps, int lci, int def) {
        for (CharacterCodeMap cmap : cmaps) {
            if (cmap.isProvidedLCI(lci)) {
                return cmap.getCharacter(lci);
            }
        }
        return def;
    }
    
    public String prepForFont(String input, String... fontSpecs) {
        LinkedList<CharacterCodeMap> cmaps = new LinkedList<CharacterCodeMap>();
        HashSet<CharacterCodeMap> tmp;
        for (String spc : fontSpecs) {
            if ((tmp = charmaps.get(spc)) != null) {
                cmaps.addAll(tmp);
            }
        }
        // Any maps added with an empty name are used always
        if ((tmp = charmaps.get("")) != null) {
            cmaps.addAll(tmp);
        }
        if (cmaps.size() == 0) {
            return input;
        }
        
        // Plan:
        // -replace \'a with a-fada etc.
        // -replace \.b with dotted b etc.
        // -replace & with tyronian symbol
        // -replace r (except \r) with seanchlo r
        // -replace s (except \s) with seanchlo s
        StringBuilder sb = new StringBuilder();
        CharParseState state = CharParseState.NONE;
        CharParseState nextState = CharParseState.NONE;
        boolean escaped = false;
        boolean nextEscaped = false;
        
        for (char c : input.toCharArray()) {
            if (c == '\\') {
                if (escaped) {
                    sb.append('\\');
                    nextEscaped = false;
                }
                else {
                    nextEscaped = true;
                }
            }
            else {
                nextState = CharParseState.NONE;
                nextEscaped = false;
            }
            switch (c) {
                case '\\':
                    break;
                /*case 'n':
                    if (escaped) {
                        sb.append('\n');
                        nextState = CharParseState.NONE;
                    }
                    else {
                        sb.append(c);
                    }
                    break;*/
                case '`': // back accent
                    if (escaped) {
                        nextState = CharParseState.GRAVE;
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case '\'':
                    if (escaped) {
                        nextState = CharParseState.FADA;
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case '.':
                    if (escaped) {
                        nextState = CharParseState.DOT;
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case '&':
                    if (!escaped) {
                        sb.appendCodePoint(getCharacter(cmaps, TYRONIAN, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                // TODO: maybe make the dotted consonants the default, and allow
                // backslashes to override
                case 'b':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_b, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'B':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_B, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'c':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_c, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'C':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_C, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'd':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_d, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'D':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_D, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'f':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_f, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'F':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_F, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'g':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_g, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'G':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_G, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'm':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_m, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'M':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_M, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'p':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_p, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'P':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_P, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'r':
                    if (escaped) {
                        sb.appendCodePoint(getCharacter(cmaps, SEANCHLO_r, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 's':
                    if (state == CharParseState.DOT) {
                        if (escaped) {
                            System.err.println("dss");
                            sb.appendCodePoint(getCharacter(cmaps, DOTTED_SEANCHLO_s, c));
                        }
                        else {
                            System.err.println("ds");
                            sb.appendCodePoint(getCharacter(cmaps, DOTTED_s, c));
                        }
                    }
                    else {
                        if (escaped) {
                            System.err.println("ss");
                            sb.appendCodePoint(getCharacter(cmaps, SEANCHLO_s, c));
                        }
                        else {
                            System.err.println("s");
                            sb.append(c);
                        }
                    }
                    break;
                case 'S':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_S, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 't':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_t, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'T':
                    if (state == CharParseState.DOT) {
                        sb.appendCodePoint(getCharacter(cmaps, DOTTED_T, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                    
                case 'a':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, a_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'A':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, A_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'e':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, e_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'E':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, E_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'i':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, i_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'I':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, I_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'o':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, o_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'O':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, O_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'u':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, u_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                case 'U':
                    if (state == CharParseState.FADA) {
                        sb.appendCodePoint(getCharacter(cmaps, U_ACUTE, c));
                    }
                    else {
                        sb.append(c);
                    }
                    break;
                default:
                    sb.append(c);
            }
            System.err.println(c + " " + state + " " + escaped);
            state = nextState;
            escaped = nextEscaped;
        }
        return sb.toString();
    }
}
