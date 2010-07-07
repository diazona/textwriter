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
import java.io.ByteArrayOutputStream;
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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main Textwriter daemon process that accepts connections and
 * reads and writes data.
 * @author David Zaslavsky
 */
public class TextwriterDaemon implements Runnable {
    // I don't use an Enum for these because the values have to be synced with the Python side
    public static final int RENDER_MODE = 0;
    public static final int FONT_LIST_MODE = 1;
    public static final int FONT_ADD_MODE = 2;
    
    public static final Charset TRANSFER_CHARSET = Charset.forName("UTF-8");
    public static final FontCollection fc = FontCollection.getInstance();

    private static final Logger logger = Logger.getLogger("net.ellipsix.textwriter");
    
    public static void main(String[] args) {
        logger.fine("Starting Textwriter");
        ExecutorService executor = Executors.newFixedThreadPool(5); // TODO: configure this value
        try {
            logger.finer("Opening ServerSocket on port 47521");
            ServerSocket ssock = new ServerSocket(47251);
            while (true) {
                Socket sock = ssock.accept();
                logger.finest("Got a connection");
                executor.execute(new TextwriterDaemon(sock));
            }
        }
        catch (IOException ioe) {
            logger.throwing("TextwriterDaemon", "main", ioe);
            System.exit(1);
        }
    }
    
    private Socket sock;
            
    private TextwriterDaemon(Socket sock) {
        this.sock = sock;
    }
            
    public void run() {
        logger.info("Starting Textwriter thread");
        try{
            BufferedReader bfin = new BufferedReader(new InputStreamReader(sock.getInputStream(), TRANSFER_CHARSET));
            OutputStream out = sock.getOutputStream();
            BufferedWriter bfout = new BufferedWriter(new OutputStreamWriter(out, TRANSFER_CHARSET));
            readloop:
            while (true) {
                // read a request
                int mode = bfin.read();
                switch (mode) {
                    case FONT_LIST_MODE:
                        // list all fonts known to the system
                        logger.fine("Listing fonts");
                        Collection<FontCollection.TaggedFont> fonts = fc.getAllFonts();
                        for (FontCollection.TaggedFont tf : fonts) {
                            bfout.write(tf.getFont().getFamily() + "\n");
                            Map<String,String> attrs = tf.getAttributes();
                            for (String k : attrs.keySet()) {
                                String v = attrs.get(k);
                                bfout.write(k + "=" + v + "\n");
                            }
                        }
                        bfout.write("\n");
                        bfout.flush();
                        break;
                    case FONT_ADD_MODE:
                        // add a new font from a file or directory
                        logger.fine("Adding fonts");
                        String filename = bfin.readLine();
                        File file = new File(filename);
                        if (file.canRead() || file.isDirectory()) {
                            try {
                                if (file.isDirectory()) { // TODO: maybe incorporate this choice into loadFontsRecursive()
                                    fc.loadFontsRecursive(file);
                                }
                                else {
                                    fc.loadFonts(file);
                                }
                                logger.finer("Loaded fonts from " + file.getPath());
                                out.write(0); // success
                            }
                            catch (IOException ioe) {
                                logger.throwing("TextwriterDaemon", "run", ioe);
                                out.write(1);
                            }
                            catch (FontFormatException ffe) {
                                logger.throwing("TextwriterDaemon", "run", ffe);
                                out.write(1);
                            }
                        }
                        else {
                            logger.info("Unreadable file " + file.getPath());
                            out.write(1); // failure
                        }
                        out.flush();
                        break;
                    case RENDER_MODE:
                        // render text
                        logger.fine("Rendering text");
                        RenderRequest req = RenderRequest.parse(bfin);
                        logger.finest("Successfully parsed request");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        req.write(baos);
                        int count = baos.size();
                        // write four bytes for the image size
                        logger.finest("Writing 4 bytes");
                        out.write(count >> 24);
                        out.write(count >> 16);
                        out.write(count >> 8);
                        out.write(count);
                        // write the image data
                        logger.finest("Writing " + count + " bytes of image data");
                        baos.writeTo(out);
                        out.flush();
                        logger.finest("Done writing image data");
                        break;
                    case -1:
                        // EOF
                        logger.info("EOF on socket");
                        break readloop;
                    default:
                        logger.info("Invalid mode " + String.valueOf(mode));
                        break;
                }
            }
        }
        catch (IOException ioe) {
            logger.throwing("TextwriterDaemon", "run", ioe);
        }
        // quit and return
    }
    
    protected void finalize() {
        try {
            sock.close();
        }
        catch (IOException ioe) {
            logger.throwing("TextwriterDaemon", "finalize", ioe);
        }
    }
}
