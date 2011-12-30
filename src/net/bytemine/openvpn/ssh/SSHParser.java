/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import net.bytemine.manager.gui.ControlCenterTab;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusMessage;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.StringUtils;


/**
 * Parser to parse single lines from the SSHCommunicator
 *
 * @author Daniel Rauer
 */
public class SSHParser {

    private static Logger logger = Logger.getLogger(SSHParser.class.getName());
    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private int prefix = 0;
    private String channel;
    private String targetChannel;
    private String line;
    private int status = 0;
    private boolean channelSequence = false;
    private boolean statusSequence = false;
    private boolean routingSequence = false;
    private Vector<String> waitingForReadyStatusOnChannel = new Vector<String>();
    private Vector<String> waitingForOkStatusOnChannel = new Vector<String>();
    private Hashtable<String, String[]> availableChannels = new Hashtable<String, String[]>();
    private Hashtable<String, SSHStatusModel> statusModels = new Hashtable<String, SSHStatusModel>();
    private SSHCommunicator communicator;
    private Pattern channelPattern = Pattern.compile(SSHConstants.CHANNEL_PATTERN);
    private Matcher channelMatcher;

    private boolean tellOutputToGUI = false;
    public static String output;
    
    public SSHParser() {
    }


    /**
     * initializes parsing of the line
     *
     * @param newLine The line to parse
     * @param communicator A SSHCommunicator to talk to
     */
    public void parse(String newLine, SSHCommunicator communicator) {
        this.communicator = communicator;
        line = newLine;

        if (line.isEmpty())
            return;
        int i = 0;
        channel = null;
        targetChannel = null;

        // remove null characters
        line = line.replaceAll("\u0000", "");
        logger.info(line);
        communicator.getCcTab().displayDebugMessage(line);

        StringTokenizer t = new StringTokenizer(line, " ");
        while (t.hasMoreTokens()) {
            String token = t.nextToken();

            switch (i) {
                case 0:
                    detectPrefixAndChannel(token);
                    break;
                case 1:
                    detectKeywords(token);
                    break;
                case 3:
                    channelMatcher = channelPattern.matcher(token);
                    if ((status == SSHConstants.STATUS_CODE_OK
                            || status == SSHConstants.STATUS_CODE_FAIL
                            || status == SSHConstants.STATUS_CODE_WAIT)
                        && (!StringUtils.isEmptyOrWhitespaces(token) && channelMatcher.matches()))
                    // only care for messages like >00> FAIL <command> 04
                    targetChannel = token;
                    break;
            }
            i++;

            // line has undefined prefix, do not process
            if (prefix == SSHConstants.PREFIX_UNDEFINED) {
                break;
            }

            // error handling
            if (prefix == SSHConstants.PREFIX_ERROR) {
                detectError();
                break;
            }

            // after first token detect potential channel entry like
            // >00> 02 VPNM "mgmt udp-bridged"
            if (channelSequence
                    && prefix == SSHConstants.PREFIX_INPUT
                    && SSHConstants.CHANNEL_COMMAND.equals(channel)) {
                addAvailableChannel();
                break;
            }

            // status information -> client list
            if (statusSequence
                    && prefix == SSHConstants.PREFIX_INPUT) {
                evaluateClientList();
                break;
            }
        }

        // perhaps a channel is just closed or opened
        if (status == SSHConstants.STATUS_CODE_OK) {
            String channelToOpen = communicator.getChannelToOpen();
            String channelToClose = communicator.getChannelToClose();
            if (channelToOpen != null) {
                waitingForReadyStatusOnChannel.remove(channelToOpen);
                waitingForOkStatusOnChannel.remove(channelToOpen);
                // a channel has been successfully opened
                String message = rb.getString("ssh.status.channel") + " " + channelToOpen + " " + rb.getString("ssh.status.channel_opened");
                communicator.setChannelToOpen(null);
                communicator.addOpenChannel(channelToOpen);
                communicator.getCcTab().openChannelPanel(channelToOpen);
                ManagerGUI.addStatusMessage(new StatusMessage(message));
            }
            if (channelToClose != null) {
                waitingForReadyStatusOnChannel.remove(channelToClose);
                waitingForOkStatusOnChannel.remove(channelToOpen);
                // a channel has been successfully closed
                String message = rb.getString("ssh.status.channel") + " " + channelToClose + " " + rb.getString("ssh.status.channel_closed");
                communicator.setChannelToClose(null);
                communicator.removeOpenChannel(channelToClose);
                communicator.getCcTab().closeChannelPanel(channelToClose);
                ManagerGUI.addStatusMessage(new StatusMessage(message));
            }
        } else if (status == SSHConstants.STATUS_CODE_WAIT) {
            if (targetChannel != null) {
                // failed to open the channel
                waitingForOkStatusOnChannel.add(targetChannel);
                communicator.getCcTab().disableDisconnectButton(targetChannel);
            }
        }
    }


    /**
     * detect, which type of error occurred
     */
    private void detectError() {
        Pattern pOpen = Pattern.compile(SSHConstants.KEYWORD_PATTERN_OPEN_FAILED);
        Pattern pOpenTimeOut = Pattern.compile(SSHConstants.KEYWORD_PATTERN_OPEN_FAILED_TIME_OUT);
        Pattern pKill = Pattern.compile(SSHConstants.KEYWORD_PATTERN_KILL_FAILED);
        Pattern pInstance = Pattern.compile(SSHConstants.KEYWORD_PATTERN_INSTANCE_FAILED);
        Matcher mOpen = pOpen.matcher(line);
        Matcher mOpenTimeOut = pOpenTimeOut.matcher(line);
        Matcher mKill = pKill.matcher(line);
        Matcher mInstance = pInstance.matcher(line);
        if (mOpen.matches() || mOpenTimeOut.matches()) {
            targetChannel = StringUtils.extractDigitsFromString(line.substring(4));

            if (waitingForOkStatusOnChannel.contains(targetChannel))
                communicator.getCcTab().enableConnectButton(targetChannel);
            else
                communicator.getCcTab().toggleConnectButton(targetChannel);
            communicator.setChannelToOpen(null);

            waitingForReadyStatusOnChannel.remove(targetChannel);
            waitingForOkStatusOnChannel.remove(targetChannel);
            communicator.getCcTab().enableConnectButton(targetChannel);

            String message = rb.getString("ssh.status.channel") + " " + targetChannel + " " + rb.getString("ssh.status.error_opening");
            ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));
        } else if (mKill.matches()) {
            String targetChannel = StringUtils.extractDigitsFromString(line.substring(4));
            String username = StringUtils.extractBetweenQuotes(line);

            String message = rb.getString("ssh.status.channel") + " " + targetChannel + ": " +
                    rb.getString("ssh.status.user") + " '" + username + "' " +
                    rb.getString("ssh.status.error_killing");
            ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));
        } else if (mInstance.matches()) {
            String message = rb.getString("ssh.status.another_instance");
            ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));
        }
    }


    /**
     *  a new available channel was detected
     */
    private void addAvailableChannel() {
        StringTokenizer t = new StringTokenizer(line, " ");
        // empty line signals end of channel sequence
        if (t.countTokens() == 1) {
            if (!waitingForReadyStatusOnChannel.contains(channel))
                waitingForReadyStatusOnChannel.add(channel);
            channelSequence = false;
            tellAvailableChannelsToGUI();
            return;
        } else if (t.countTokens() == 2) {
            return;
        }

        t.nextToken();
        String[] entry = new String[3];
        entry[0] = t.nextToken();
        entry[1] = t.nextToken();

        if (t.hasMoreTokens()) {
            entry[2] = t.nextToken();
            while (t.hasMoreTokens())
                entry[2] += " " + t.nextToken();
        }
        entry[2] = entry[2].replaceAll("\"", "");
        availableChannels.put(entry[0], entry);
    }


    /**
     * a new client entry was detected
     */
    private void evaluateClientList() {
        if (line.indexOf(SSHConstants.KEYWORD_VIRTUAL_ADDRESS) > -1)
            routingSequence = true;
        
        // uninteresting, ignore these lines
        if (line.indexOf(SSHConstants.KEYWORD_UPDATED) > -1
                || line.indexOf(SSHConstants.KEYWORD_COMMON) > -1
                || line.indexOf(SSHConstants.KEYWORD_ROUTING_TABLE) > -1
                || line.indexOf(SSHConstants.KEYWORD_VIRTUAL_ADDRESS) > -1
                || line.indexOf(SSHConstants.KEYWORD_STATUS) > -1) {
            return;
        }
        // status sequence ended, tell client list to GUI
        if (line.indexOf(SSHConstants.KEYWORD_GLOBAL_STATS) > -1) {
            statusSequence = false;
            routingSequence = false;
            
            tellClientListToGUI();
            return;
        }

        // process client list 
        if (!routingSequence) {
            SSHStatusModel model = new SSHStatusModel(line);
            statusModels.put(model.getCommonName(), model);
        } 
        // process routing information
        else {
            // cut off prefix
            String lineBackup = line.substring(5);
            String cn = null;
            
            StringTokenizer t = new StringTokenizer(lineBackup, ",");
            if (t.countTokens() > 1) {
                // get only the second token, which is the common name 
                t.nextToken();
                cn = t.nextToken();
            }
            
            // get the correct model
            SSHStatusModel model = statusModels.get(cn);
            if (model != null)
                model.addRoutingInformation(line);
        }
    }


    /**
     * try to detect the prefix and the channel the message came from
     *
     * @param token the String token to get prefix and channel from.
     */
    private void detectPrefixAndChannel(String token) {
        if (token == null || token.length() < 4) {
            prefix = SSHConstants.PREFIX_UNDEFINED;
            channel = null;
            return;
        }

        char[] chars = token.toCharArray();
        channel = chars[1] + "" + chars[2];
        if (chars[0] == '<' && chars[3] == '<')
            prefix = SSHConstants.PREFIX_OUTPUT;
        else if (chars[0] == '>' && chars[3] == '>')
            prefix = SSHConstants.PREFIX_INPUT;
        else if (chars[0] == '_' && chars[3] == '_')
            prefix = SSHConstants.PREFIX_INPUT_NO_NEWLINE;
        else if (chars[0] == '!' && chars[3] == '!')
            prefix = SSHConstants.PREFIX_ERROR;
        else if (chars[0] == '.' && chars[3] == '.')
            prefix = SSHConstants.PREFIX_CLOSEDOWN;
        else {
            prefix = SSHConstants.PREFIX_UNDEFINED;
            channel = null;
        }
    }


    /**
     * try to detect a keyword in the token
     *
     * @param token The token to analyze
     */
    private void detectKeywords(String token) {
        if (token == null) {
            return;
        }

        Pattern pKill = Pattern.compile(SSHConstants.KEYWORD_PATTERN_KILL_FAILED);
        Matcher mKill = pKill.matcher(line);
        
        if (SSHConstants.KEYWORD_CHANNELS.equals(token)) {
            channelSequence = true;
        } else if (line.indexOf(SSHConstants.KEYWORD_STATUS) > -1) {
            // reset the status models
            statusModels = new Hashtable<String, SSHStatusModel>();
            statusSequence = true;
        } else if (line.indexOf(SSHConstants.KEYWORD_KILL_SUCCESS) > -1) {
            updateStatus();
        } else if (mKill.matches()) {
            tellKillUnsuccesfulMessageToGUI(line);
        } else if (line.indexOf(SSHConstants.KEYWORD_LOG) > -1) {
            tellNewLogMessageToGUI(line);
        } else if (line.indexOf(SSHConstants.KEYWORD_VERSION) > -1) {
            tellVersionToGUI(line);
        } else {
            detectStatus(token);
            if (tellOutputToGUI && line != null)
            	tellNewOutputMessageToGUI(line);
        }
    }


    /**
     * detect the status from the token
     *
     * @param token The token to analyze
     */
    private void detectStatus(String token) {
        if (SSHConstants.KEYWORD_OK.equals(token))
            status = SSHConstants.STATUS_CODE_OK;
        else if (SSHConstants.KEYWORD_READY.equals(token))
            status = SSHConstants.STATUS_CODE_READY;
        else if (SSHConstants.KEYWORD_WAIT.equals(token))
            status = SSHConstants.STATUS_CODE_WAIT;
        else if (SSHConstants.KEYWORD_FAIL.equals(token))
            status = SSHConstants.STATUS_CODE_FAIL;
        else
            status = SSHConstants.STATUS_CODE_UNDEFINED;
    }


    /**
     * cut off the >LOG: prefix from the line
     *
     * @param line The line to substring
     * @return The line without the log prefix
     */
    private String cutOffLogPrefix(String line) {
        try {
            line = line.substring(line.indexOf(SSHConstants.KEYWORD_LOG) + SSHConstants.KEYWORD_LOG.length());
        } catch (Exception e) {
        }
        return line;
    }


    /**
     * tell available channels to the GUI
     */
    private void tellAvailableChannelsToGUI() {
        ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(
                this.communicator.getSession().getHost());
        ccTab.displayAvailableChannels(availableChannels);
    }


    /**
     * tell a client list to the GUI
     */
    private void tellClientListToGUI() {
        Vector<String[]> clientList = new Vector<String[]>();
        for (Iterator<String> iterator = statusModels.keySet().iterator(); iterator.hasNext();) {
            String key = (String)iterator.next();
            SSHStatusModel sshStatusModel = (SSHStatusModel) statusModels.get(key);
            clientList.add(sshStatusModel.toStringArray());
        }

        
        ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(
                this.communicator.getSession().getHost());
        ccTab.updateClientTable(clientList, channel);
    }


    /**
     * Get a new status. Is called after a kill
     */
    private void updateStatus() {
        this.communicator.callStatus(channel);
    }

    
    /**
     * tell a new output message to the GUI
     * @param line A message
     */
    private void tellNewOutputMessageToGUI(String line) {
        ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(
                this.communicator.getSession().getHost());
        // cut off prefix
        
        line = cutOffLogPrefix(line);
        	
        ccTab.updateUserCommandDialog(line, channel);
    }
    
    
    /**
     * tell a new log message to the GUI
     * @param line A message
     */
    private void tellNewLogMessageToGUI(String line) {
        ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(
                this.communicator.getSession().getHost());
        // cut off prefix
        line = cutOffLogPrefix(line);

        ccTab.displayNewLogMessage(line, channel);
    }


    /**
     * tell the GUI, that a kill was unsuccessful
     *
     * @param line The line, where the username can be extracted
     */
    private void tellKillUnsuccesfulMessageToGUI(String line) {
        String username = StringUtils.extractBetweenQuotes(line);

        String message = rb.getString("ssh.status.channel") + " " + channel + ": " +
                rb.getString("ssh.status.user") + " '" + username + "' " +
                rb.getString("ssh.status.error_killing");
        ManagerGUI.addStatusMessage(new StatusMessage(message, StatusMessage.TYPE_ERROR));
    }


    /**
     * tell version information to the GUI
     *
     * @param line The line to get the information from
     */
    private void tellVersionToGUI(String line) {
        ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(
                this.communicator.getSession().getHost());
        // cut off prefix
        line = cutOffLogPrefix(line).trim();

        ccTab.displayVersion(line);
    }
    
    public void tellOutputToGUI(boolean status) {
        tellOutputToGUI = status;
    }


    /*
     * Getter and Setter
     */

    public Hashtable<String, String[]> getAvailableChannels() {
        return availableChannels;
    }

    public void setAvailableChannels(Hashtable<String, String[]> availableChannels) {
        this.availableChannels = availableChannels;
    }

    public static void main(String [] args) {
    }


}
