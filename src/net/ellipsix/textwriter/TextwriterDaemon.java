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

import java.io.InputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The main Textwriter daemon process that accepts connections and
 * reads and writes data.
 * @author David Zaslavsky
 */
public class TextwriterDaemon {
    public static final int FONT_LIST_MODE = 1;
    public static final int RENDER_MODE = 2;
    public static void main(String[] args) {
        ServerSocket ssock = new ServerSocket(0);
        while (true) {
            Socket sock = ssock.accept();
            InputStream in = sock.getInputStream();
            int mode = in.read();
            switch (mode) {
                case FONT_LIST_MODE:
                    break;
                case RENDER_MODE:
                    RenderRequest req = RenderRequest.parse();
                    RenderResponse rsp = new RenderResponse(req);
                    rsp.write(sock.getOutputStream());
                    break;
                default:
                    break;
            }
            sock.close();
        }
    }
}
