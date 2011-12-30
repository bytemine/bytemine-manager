/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.ControlCenterTab;
import net.bytemine.manager.gui.CustomJOptionPane;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusFrame;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.FileUtils;


/**
 * Establishes ssh connections
 *
 * @author Daniel Rauer
 */
public class SSHConnector {

    private static Logger logger = Logger.getLogger(SSHConnector.class.getName());
    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private Session sshSession = null;

    Server server;
    String username;
    String keyfile;
    String hostname;
    int port;
    int authType;
    JSch jsch;

    public SSHConnector(Server server) throws ConnectException {
        this.server = server;
        this.username = server.getUsername();
        this.keyfile = server.getKeyfilePath();
        this.hostname = server.getHostname();
        this.port = server.getSshPort();
        this.authType = server.getAuthType();
    }
    
    public SSHConnector(Server server, String username, String keyfile, int authType) throws ConnectException {
        this.server = server;
        this.username = username;
        this.keyfile = keyfile;
        this.hostname = server.getHostname();
        this.port = server.getSshPort();
        this.authType = authType;
    }


    /**
     * Creates a ssh session
     *
     * @throws java.net.ConnectException
     * @return Session
     */
    public Session createSessionImmediately() throws ConnectException {
        String logMessage = rb.getString("status.ssh.generalconnect.text1") +
                " " + hostname + ":" + port + " " +
                rb.getString("status.ssh.generalconnect.text2");

        try {
            jsch = new JSch();
            
            String knownHostsFilename = "";
            try {
                File knowHostsFile = null;
                knownHostsFilename = FileUtils.getDirectoryFromFilename(Configuration.getInstance().JDBC_PATH).toString()+File.separator+"known_hosts"; // append known_hosts filename to path
                knowHostsFile = new File(knownHostsFilename);
                if(!knowHostsFile.exists())
                    knowHostsFile.createNewFile();
                jsch.setKnownHosts(knownHostsFilename);
            } catch(Exception e) {
                logger.severe("Path to known_hosts is invalid, cannot access known_hosts-file: " + knownHostsFilename);
                throw new ConnectException(rb.getString("status.ssh.knownhosts_not_writable") + " " + knownHostsFilename);
            }

            JFrame parentFrame= ManagerGUI.mainFrame;

            // Auth with username and password
            if (authType == Server.AUTH_TYPE_PASSWORD) {
                logger.info("SSH auth with password");
                UserInfoWithPassword ui = new UserInfoWithPassword(parentFrame);
                sshSession = jsch.getSession(username, hostname, port);
                sshSession.setConfig("Language", ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage());
                sshSession.setConfig("StrictHostKeyChecking", "ask");
                sshSession.setUserInfo(ui);
            }

            // Auth with username and keyfile
            else if (authType == Server.AUTH_TYPE_KEYFILE) {
                logger.info("SSH auth with keyfile");
                UserInfo ui = new UserInfoWithKeyfile(parentFrame, keyfile);
                jsch.addIdentity(keyfile);
                sshSession = jsch.getSession(username, hostname, port);
                sshSession.setConfig("Language", ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage());
                sshSession.setConfig("StrictHostKeyChecking", "ask");
                sshSession.setUserInfo(ui);
            } else {
                logger.severe("Invalid authType");
                throw new ConnectException(logMessage);
            }
            sshSession.setTimeout(15000);
            sshSession.connect();

            SSHSessionPool.getInstance().addSession(sshSession.getHost(), sshSession);
            ControlCenterTab ccTab = ManagerGUI.getOpenCCTab(hostname);
            if (ccTab != null) {
                ccTab.setSshSession(sshSession);
                ccTab.detectAvailableChannels();
            }

            logger.info("ssh session established");
            return sshSession;
        } catch (JSchException je) {
            Throwable cause = je.getCause();
            if (cause == null) {
                logger.log(Level.SEVERE, je.getMessage(), je);
                if (je.getMessage().indexOf("invalid privatekey") > -1)
                    throw new ConnectException(rb.getString("status.ssh.invalidkey"));
                else if (je.getMessage().indexOf("timeout") > -1)
                    throw new ConnectException(rb.getString("status.ssh.timeout"));
                else
                    throw new ConnectException(rb.getString("status.ssh.invalidauth"));
            } else {
                logger.log(Level.SEVERE, cause.getMessage(), cause);
                if (cause.getClass() == UnknownHostException.class)
                    throw new ConnectException(rb.getString("status.ssh.unknownhost"));
                else if (cause.getClass() == ConnectException.class)
                    throw new ConnectException(rb.getString("status.ssh.noresponse"));
                else if (cause.getClass() == FileNotFoundException.class)
                    throw new ConnectException(rb.getString("status.ssh.keyfile_not_found"));
                else
                    throw new ConnectException(logMessage);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ConnectException(logMessage);
        }
    }


    /**
     * Creates a ssh session in a new thread
     *
     * @throws java.net.ConnectException
     */
    public void createSession() throws ConnectException {
        SwingWorker<String, Void> connectWorker = new SwingWorker<String, Void>() {
            private ConnectException exception = null;

            protected String doInBackground() {
                try {
                    createSessionImmediately();
                } catch (ConnectException e) {
                    exception = e;
                }
                return "";
            }

            protected void done() {
                if (exception != null)
                    new VisualException(exception);
            }
        };

        connectWorker.execute();

    }


    /**
     * disconnects the ssh session
     */
    public void disconnectSession() {
        if (sshSession.isConnected())
            sshSession.disconnect();
        SSHSessionPool.getInstance().removeSession(sshSession.getHost());

    }

    /**
     * connects the ssh session
     * @throws Exception
     */
    public void connectSession() throws Exception {
        if (!sshSession.isConnected())
            sshSession.connect();
        SSHSessionPool.getInstance().addSession(sshSession.getHost(), sshSession);
    }


    /**
     * reconnects the ssh session
     * @return Session
     * @throws Exception
     */
    public Session reconnectSession() throws Exception {
        SSHSessionPool.getInstance().removeSession(sshSession.getHost());

        sshSession = createSessionImmediately();
        return sshSession;
    }


    /**
     * Manages ssh authentication by displaying a
     * dialog for entering the password
     */
    private static class UserInfoWithPassword implements UserInfo, UIKeyboardInteractive {

        private JFrame parentFrame;
        private int counter = 0;

        public UserInfoWithPassword(JFrame parentFrame) {
            this.parentFrame = parentFrame;
        }

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            Object[] options = {rb.getString("status.msg.yes"), rb.getString("status.msg.no")};
            int foo = CustomJOptionPane.showOptionDialog(parentFrame,
                    str,
                    "Warning",
                    CustomJOptionPane.DEFAULT_OPTION,
                    CustomJOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            return foo == 0;
        }

        String passwd;
        JTextField passwordField = (JTextField) new JPasswordField(20);

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            counter++;
            // abort connection after specified false login attempts
            if (counter > SSHConstants.MAX_LOGIN_ATTEMPTS)
                return false;
            
            // reset the password field
            passwordField.setText("");
            
            // update status message
            StatusFrame sf= new StatusFrame();
            sf.updateStatus(rb.getString("status.msg.passphrase"));

            Object[] ob = {rb.getString("status.msg.password") + ":", passwordField};

            int result = CustomJOptionPane.showOptionDialog(parentFrame,
                    ob, message,
                    CustomJOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null,
                    null, 1);
            
            if (result == CustomJOptionPane.OK_OPTION) {
                passwd = passwordField.getText();
                return true;
            } else {
                return false;
            }
        }

        public void showMessage(String message) {
            CustomJOptionPane.showMessageDialog(parentFrame, message);
        }

        final GridBagConstraints gbc =
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0);
        private Container panel;

        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts = new JTextField[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if (echo[i]) {
                    texts[i] = new JTextField(20);
                } else {
                    texts[i] = new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if (CustomJOptionPane.showConfirmDialog(parentFrame, panel,
                    destination + ": " + name,
                    CustomJOptionPane.OK_CANCEL_OPTION,
                    CustomJOptionPane.QUESTION_MESSAGE) == CustomJOptionPane.OK_OPTION) {
                String[] response = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    response[i] = texts[i].getText();
                }
                return response;
            } else {
                return null;  // cancel

            }
        }
    }


    /**
     * Manages ssh authentication by displaying a
     * dialog for entering the passphrase of the key
     */
    private static class UserInfoWithKeyfile implements UserInfo, UIKeyboardInteractive {

        private JFrame parentFrame;
        private String keyfile;
        private int counter = 0;

        public UserInfoWithKeyfile(JFrame parentFrame, String keyfile) {
            this.parentFrame = parentFrame;
            this.keyfile = keyfile;
        }

        public String getPassword() {
            return null;
        }

        public boolean promptYesNo(String str) {
            Object[] options = {rb.getString("status.msg.yes"), rb.getString("status.msg.no")};
            int foo = CustomJOptionPane.showOptionDialog(parentFrame,
                    str,
                    "Warning",
                    CustomJOptionPane.DEFAULT_OPTION,
                    CustomJOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            return foo == 0;
        }

        String passphrase;
        JTextField passphraseField = (JTextField) new JPasswordField(20);
        
        public String getPassphrase() {
            return passphrase;
        }

        public boolean promptPassphrase(String message) {
            counter++;
            // abort connection after specified false login attempts
            if (counter > SSHConstants.MAX_LOGIN_ATTEMPTS)
                return false;
            
            // reset the password field
            passphraseField.setText("");
            
            // update status message
            StatusFrame sf = new StatusFrame();
            sf.updateStatus(rb.getString("status.msg.passphrase"));
            
            String text = rb.getString("status.msg.passphraseForKey") + ":";
            if (keyfile != null)
                text += "\n" + keyfile;
            Object[] ob = {text, passphraseField};
            
            int result = CustomJOptionPane.showOptionDialog(parentFrame,
                    ob, message,
                    CustomJOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null,
                    null, 1);
            
            if (result == CustomJOptionPane.OK_OPTION) {
                passphrase = passphraseField.getText();
                return true;
            } else {
                return false;
            }
        }

        public boolean promptPassword(String message) {
            return true;
        }

        public void showMessage(String message) {
            CustomJOptionPane.showMessageDialog(parentFrame, message);
        }

        final GridBagConstraints gbc =
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0);
        private Container panel;

        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {

            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts = new JTextField[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if (echo[i]) {
                    texts[i] = new JTextField(20);
                } else {
                    texts[i] = new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if (CustomJOptionPane.showConfirmDialog(parentFrame, panel,
                    destination + ": " + name,
                    CustomJOptionPane.OK_CANCEL_OPTION,
                    CustomJOptionPane.QUESTION_MESSAGE) == CustomJOptionPane.OK_OPTION) {
                String[] response = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    response[i] = texts[i].getText();
                }
                return response;
            } else {
                return null;  // cancel

            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server(
                "", "server_cn", "server_ou",
                "vpntest.bytemine.net",
                Server.AUTH_TYPE_KEYFILE,
                "manager",
                "/home/dra/.ssh/id_rsa", "", "",
                -1, -1, -1, 22, Server.SERVER_TYPE_REGULAR_OPENVPN, "command",
                -1, 0, 0, false, "", "vpnNetworkAddress", 24, 0, false, false, "", 
                "", "");
        SSHConnector conn = null;
        try {
            conn = new SSHConnector(server);
            conn.createSessionImmediately();
        } catch (ConnectException e) {
            e.printStackTrace();
        }
    }

}
