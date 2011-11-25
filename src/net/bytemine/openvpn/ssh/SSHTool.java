/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Server;
import net.bytemine.manager.exception.VisualException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

/**
 * is able to send commands via ssh
 *
 * @author Daniel Rauer
 */
public class SSHTool {

    private static Logger logger = Logger.getLogger(SSHTool.class.getName());
//    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private Server server;
    private Session sshSession;
    private SSHConnector sshConnector;


    public SSHTool(Server server) throws ConnectException, Exception {
        this.server = server;

        sshConnector = new SSHConnector(this.server);
    }
    
    
    public SSHTool(Server server, String username, String keyfile, int authtype) throws ConnectException, Exception {
        this.server = server;

        sshConnector = new SSHConnector(this.server,username,keyfile,authtype);
    }


    private Session getSession() throws ConnectException {
        sshSession = SSHSessionPool.getInstance().getSession(server.getHostname());
        if (sshSession == null || !sshSession.isConnected())
            this.sshSession = sshConnector.createSessionImmediately();
        return sshSession;
    }
    
    public void exec(String command) throws Exception {
        
        try {
            logger.info("SSHTool.exec start with command: " + command);

            // exec 'scp -t rfile' remotely
            String exec = command;
            Channel channel = getSession().openChannel("exec");
            ((ChannelExec) channel).setCommand(exec);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            ((ChannelExec)channel).setErrStream(baos);
            ((ChannelExec)channel).setXForwarding(true);
            
            channel.connect();

            checkAck(in);
            
            String err = baos.toString();
            if (err.length() > 0)
                throw new Exception(err);

            out.close();
            channel.disconnect();
            
            logger.info("SSHTool.exec end");
        } catch (Exception e) {
            new VisualException(e);
            throw e;
        }
    }
    
    public void disconnectSession() {
        int i = 1;
        while (this.sshSession != null && this.sshSession.isConnected() && i < 100) {
            this.sshSession.disconnect();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            i++;
        }
        SSHSessionPool.getInstance().removeSession(server.getHostname());
    }


    /**
     * Checks if the server accepts a connection
     *
     * @param in
     * @return 0 for success
     *         1 for an error
     *         2 for a fatal error
     * @throws IOException
     */
    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0 || b == -1)
            return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');

            if (b == 1)  {// error
                logger.severe("Error: " + sb.toString());
                throw new IOException(sb.toString());
            } else if (b == 2)  {// fatal error
                logger.severe("Fatal Error:" + sb.toString());
                throw new IOException(sb.toString());
            }
        }
        return b;
    }
}