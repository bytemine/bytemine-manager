/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Server;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.openvpn.ssh.SSHConnector;
import net.bytemine.openvpn.ssh.SSHSessionPool;


/**
 * is able to copy files via scp
 *
 * @author Daniel Rauer
 */
public class ScpTool {

    private static Logger logger = Logger.getLogger(ScpTool.class.getName());
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private Server server;
    private Session sshSession;
    private SSHConnector sshConnector;


    public ScpTool(Server server) throws ConnectException, Exception {
        this.server = server;

        sshConnector = new SSHConnector(this.server);
    }


    private Session getSession() throws ConnectException {
        sshSession = SSHSessionPool.getInstance().getSession(server.getHostname());
        if (sshSession == null || !sshSession.isConnected())
            this.sshSession = sshConnector.createSessionImmediately();
        return sshSession;
    }


    /**
     * Gets a file from a server via scp as OutputStream
     *
     * @param sourceFile The filename of the source file
     * @throws java.lang.Exception
     */
    public byte[] getFromServer(String sourceFile) throws ConnectException, Exception {

        try {
            logger.info("ScpTool.getFrom start");

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // exec 'scp -f rfile'
            String command = "scp -f \"" + sourceFile + "\"";
            Channel channel = getSession().openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();


            byte[] buf = new byte[1024];

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            while (true) {
                int c = checkAck(in);
                if (c != 'C')
                    break;

                // read '0644 '
                in.read(buf, 0, 5);

                long filesize = 0L;
                while (true) {
                    if (in.read(buf, 0, 1) < 0)
                        break;
                    if (buf[0] == ' ')
                        break;
                    filesize = filesize * 10L + (long) (buf[0] - '0');
                }


                String file = null;
                for (int i = 0; ; i++) {
                    in.read(buf, i, 1);
                    if (buf[i] == (byte) 0x0a) {
                        file = new String(buf, 0, i);
                        break;
                    }
                }
                file.toString();


                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                int pointer;
                while (true) {
                    if (buf.length < filesize)
                        pointer = buf.length;
                    else
                        pointer = (int) filesize;
                    pointer = in.read(buf, 0, pointer);
                    if (pointer < 0)
                        break;

                    os.write(buf, 0, pointer);

                    filesize -= pointer;

                    if (filesize == 0L)
                        break;
                }
                os.close();

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();
            }

            channel.disconnect();

            logger.info("ScpTool.getFrom end");
            return os.toByteArray();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new Exception(rb.getString("error.syncusers.scpFrom"));
        }
    }

    /**
     * Writes a String to a file on a server via scp
     *
     * @param content    The content to write
     * @param targetFile The filename of the target file
     * @throws java.lang.Exception
     */
    public void postToServer(String content, String targetFile) throws Exception {

        try {
            logger.info("ScpTool.postTo start");

            // exec 'scp -t rfile' remotely
            String command = "scp -t \"" + targetFile + "\"";
            Channel channel = getSession().openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            checkAck(in);

            long filesize = content.length();
            command = "C0644 " + filesize + " ";
            command += "file";
            command += "\n";
            out.write(command.getBytes());
            out.flush();

            checkAck(in);

            // send the content
            InputStream bais = new ByteArrayInputStream(content.getBytes());

            byte[] buf = new byte[1024];
            while (true) {
                int len = bais.read(buf, 0, buf.length);
                if (len <= 0)
                    break;
                out.write(buf, 0, len);
            }
            bais.close();
            bais = null;

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            checkAck(in);

            out.close();

            channel.disconnect();

            logger.info("ScpTool.postTo end");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
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
