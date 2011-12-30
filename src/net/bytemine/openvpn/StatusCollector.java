/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author fkr
 */
public class StatusCollector extends Thread {

    private Server server;
    private int interval = 10;

    private Socket connectionSocket;

    public StatusCollector(Server server) {
        this.server = server;
        this.connectionSocket = null;
    }

    public void connect() throws Exception {
        try {
            this.connectionSocket = new Socket(server.getHostname(), server.getPort());
        } catch (UnknownHostException e) {
            throw new Exception(e);
        }
    }

    public void disconnect() throws Exception {
        try {
            this.connectionSocket.close();
        } catch (IOException e) {
            throw new Exception(e);
        }
    }


    public void run() {

        TimerTask watcherTask = new StatusWatcher(this.connectionSocket);
        Timer timer = new Timer();
        // repeat every $interval seconds
        timer.schedule(watcherTask, new Date(), this.interval * 1000);

    }


    /**
     * @return the intervall
     */
    public int getIntervall() {
        return interval;
    }

    /**
     * @param intervall the intervall to set
     */
    public void setIntervall(int intervall) {
        this.interval = intervall;
    }
}
