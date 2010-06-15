/* TextwriterDaemon.java */

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

import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

/**
 * The main Textwriter daemon process that accepts connections and
 * reads and writes data.
 * @author David Zaslavsky
 */
public class TextwriterDaemon {
    // I don't use an Enum for these because the values have to be synced with the Python side
    public static final int RENDER_MODE = 0;
    public static final int FONT_LIST_MODE = 1;
    public static final int FONT_ADD_MODE = 2;

    public static void main(String[] args) {
        try {
            ServerSocket ssock = new ServerSocket(0);
            FontCollection fc = FontCollection.getInstance();
            while (true) {
                Socket sock = ssock.accept();
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();
                try {
                    int mode = in.read();
                    BufferedReader bfin = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    switch (mode) {
                        case FONT_LIST_MODE:
                            // list all fonts known to the system
                            Set<String> fontNames = fc.getAllFontNames();
                            BufferedWriter bfout = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                            for (String s : fontNames) {
                                bfout.write(s + "\n");
                            }
                            bfout.flush();
                            break;
                        case FONT_ADD_MODE:
                            // add a new font from a file or directory
                            String filename = bfin.readLine();
                            File file = new File(filename);
                            if (file.canRead() || file.isDirectory()) {
                                try {
                                    fc.loadFonts(file);
                                    out.write(0); // success
                                }
                                catch (IOException ioe) {
                                    out.write(1);
                                }
                                catch (FontFormatException ffe) {
                                    out.write(1);
                                }
                            }
                            else {
                                out.write(1); // failure
                            }
                            break;
                        case RENDER_MODE:
                            // render text
                            RenderRequest req = RenderRequest.parse(bfin);
                            req.write(sock.getOutputStream());
                            break;
                        default:
                            break;
                    }
                }
                catch (IOException ioe) {
                    // TODO: do something
                }
                finally {
                    sock.close();
                }
            }
        }
        catch (IOException ioe) {
            System.exit(1);
        }
    }
}
