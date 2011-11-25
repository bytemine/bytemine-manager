/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


/**
 * Watches the socket input stream
 * @author Daniel Rauer
 */
package net.bytemine.openvpn;

import java.util.*;
import java.io.*;
import java.net.Socket;

public class StatusWatcher extends TimerTask {

    private Socket connectionSocket;

    public StatusWatcher(Socket socket) {
        this.connectionSocket = socket;
    }


    public final void run() {
        try {
            InputStream stream = this.connectionSocket.getInputStream();
            StringBuffer buf = new StringBuffer();

            int c;

            while ((c = stream.read()) != -1) {
                buf.append((char) c);
            }
            String sT = buf.toString().trim();
            System.out.println(sT);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}