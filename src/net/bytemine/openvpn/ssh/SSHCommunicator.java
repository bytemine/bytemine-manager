/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.openvpn.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.gui.ControlCenterTab;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusMessage;
import net.bytemine.manager.i18n.ResourceBundleMgmt;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


/**
 * Communicator-Thread for communicating with one ssh session
 *
 * @author Daniel Rauer
 */
public class SSHCommunicator implements Runnable {

    private static Logger logger = Logger.getLogger(SSHCommunicator.class.getName());
    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private Session session;
    private String wrapperCommand;
    private ControlCenterTab ccTab;
    private Hashtable<String, Channel> channelPool = new Hashtable<String, Channel>();
    private String channelToOpen;
    private String channelToClose;
    private String currentChannelNumber;
    private Vector<String> openChannels = new Vector<String>();
    private boolean successfulInit = false;
    private boolean allClose = false;

    private InputStream in;
    private OutputStream out;
    private Channel channel;
    private SSHParser parser;

    public SSHCommunicator(Session session, String command, ControlCenterTab ccTab) {
        this.session = session;
        this.wrapperCommand = command;
        this.ccTab = ccTab;
    }


    public void run() {
        initialize();

        if (successfulInit)
            read();
    }


    /**
     * opens a secure shell channel. Retrieves In- and Outputstreams
     */
    private void connect() {
        try {
            channel = this.session.openChannel(SSHConstants.CHANNEL_TYPE_SHELL);
            addChannel(SSHConstants.CHANNEL_COMMAND, channel);

            channel.setInputStream(null);
            channel.setOutputStream(null);

            in = channel.getInputStream();
            out = channel.getOutputStream();
            channel.connect();
            successfulInit = true;

            return;
        } catch (JSchException e) {
            logger.log(Level.SEVERE, "error opening ssh shell channel", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error getting in- and output stream from channel", e);
        }

        String message = rb.getString("ssh.status.error_init");
        ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));

        successfulInit = false;

    }


    /**
     * Open a shell channel, get in- and outputstreams.
     * Initialize UT communication
     */
    private void initialize() {
        logger.info("initializing ssh communication with " + this.session.getHost());

        connect();

        // send empty command to avoid problems with terminal type
        sendCommand("");

        sendCommand(this.wrapperCommand);
        return;
    }


    /**
     * Open the given channel
     *
     * @param channelNumber
     */
    public void openChannel(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <00< open chNr
        String openStr = "<" + SSHConstants.CHANNEL_COMMAND + "<" + " " + SSHConstants.COMMAND_OPEN + " " + channelNumber;
        sendCommand(openStr);

        channelToOpen = currentChannelNumber;
    }


    /**
     * close the given channel
     *
     * @param channelNumber
     */
    public void closeChannel(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <00< close chNr
        String closeStr = "<" + SSHConstants.CHANNEL_COMMAND + "<" +
                " " + SSHConstants.COMMAND_CLOSE +
                " " + channelNumber;
        sendCommand(closeStr);

        channelToClose = currentChannelNumber;
    }


    /**
     * close all open channels
     */
    public void closeAllChannels() {
        allClose = true;
        Vector<String> openChannelsCopy = new Vector<String>();
        openChannelsCopy.addAll(openChannels);
        if (!openChannelsCopy.isEmpty()) {
            int i = 0;
            for (Iterator<String> iterator = openChannelsCopy.iterator(); iterator.hasNext();) {
                String channelNr = (String) iterator.next();
                closeChannel(channelNr);

                while (channelToClose != null && !channel.isClosed()) {
                    if (i == 100)
                        break;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    i++;
                }
            }
        }
        ccTab.closeTabAndSession();
    }


    /**
     * get the openvpn server status information
     *
     * @param channelNumber
     */
    public void callStatus(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <chNr< status
        String statusStr = "<" + channelNumber + "<" + " " + SSHConstants.COMMAND_STATUS;
        sendCommand(statusStr);
    }


    /**
     * start logging
     *
     * @param channelNumber
     */
    public void callLog(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <chNr< log on
        String logStr = "<" + currentChannelNumber + "<" + " " + SSHConstants.COMMAND_LOG_ON;
        sendCommand(logStr);
    }


    /**
     * Sends a command entered by the user
     *
     * @param channelNumber
     * @param command       The users command
     */
    public void sendUserCommand(String channelNumber, String command) {
        parser.tellOutputToGUI(true);
        currentChannelNumber = channelNumber;
        // <chNr< <command>
        String logStr = "<" + currentChannelNumber + "<" + " " + command;
        sendCommand(logStr);
    }


    /**
     * quit logging
     *
     * @param channelNumber
     */
    public void endLog(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <chNr< log on
        String logStr = "<" + currentChannelNumber + "<" + " " + SSHConstants.COMMAND_LOG_OFF;
        sendCommand(logStr);
    }


    /**
     * get verion information
     *
     * @param channelNumber
     */
    public void callVersion(String channelNumber) {
        currentChannelNumber = channelNumber;
        // <chNr< log on
        String logStr = "<" + currentChannelNumber + "<" + " " + SSHConstants.COMMAND_VERSION;
        sendCommand(logStr);
    }


    /**
     * kill the user with the given username
     *
     * @param channelNumber The channelNumber
     * @param username      The users username
     */
    public void killUser(String channelNumber, String username) {
        currentChannelNumber = channelNumber;
        // <chNr< kill <username>
        String killStr = "<" + currentChannelNumber + "<" + " " + SSHConstants.COMMAND_KILL + " " + username;
        sendCommand(killStr);
    }


    /**
     * read constantly from the server
     */
    private void read() {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        parser = new SSHParser();
        while (true) {
            try {
                line = br.readLine();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "error reading from ssh socket", e);
            }
            if (line != null) {
                parser.parse(line, this);
            }

            if (channel.isClosed()) {
                logger.info("channel is closed");
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        if (!allClose)
            // session was disconnected without user interaction
            reconnect();
    }


    /**
     * shows a dialog asking the user to reconnect
     */
    private void reconnect() {
        boolean reconnect = Dialogs.showSSHReconnectDialog(ManagerGUI.mainFrame);
        while (reconnect) {
            // re-try connecting
            try {
                session = ccTab.sshConnection.reconnectSession();
                reconnect = false;
                ccTab.setSshSession(session);
                ccTab.closeAllChannelPanel();

                // successful
                return;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "error reconnecting ssh session", e);
                reconnect = Dialogs.showSSHReconnectDialog(ManagerGUI.mainFrame);
            }
        }
        // no further reconnect wanted by user, close and disconnect
        ccTab.closeTabAndSession();
    }


    /**
     * Send the given command to the server
     *
     * @param command
     */
    private void sendCommand(String command) {
        logger.info("--->" + command);
        ccTab.displayDebugMessage("--->" + command);
        try {
            out.write(command.getBytes());
            out.write(SSHConstants.NEWLINE.getBytes());
            out.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error sending ssh command: " + command, e);
            String message = rb.getString("ssh.status.error_send");
            ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));
        }
    }


    public void addChannel(String name, Channel channel) {
        channelPool.put(name, channel);
    }

    public void removeChannel(String name) {
        channelPool.remove(name);
    }

    public Channel getChannel(String name) {
        return channelPool.get(name);
    }


    public Session getSession() {
        return session;
    }


    public ControlCenterTab getCcTab() {
        return ccTab;
    }


    public String getChannelToOpen() {
        return channelToOpen;
    }


    public void setChannelToOpen(String channelToOpen) {
        this.channelToOpen = channelToOpen;
    }


    public String getChannelToClose() {
        return channelToClose;
    }


    public void setChannelToClose(String channelToClose) {
        this.channelToClose = channelToClose;
    }

    public void addOpenChannel(String ch) {
		openChannels.add(ch);
	}
	
	public void removeOpenChannel(String ch) {
		openChannels.remove(ch);
	}
	
	
}
