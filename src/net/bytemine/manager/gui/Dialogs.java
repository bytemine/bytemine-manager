/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.action.ConfigurationAction;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.ValidatorAction;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ConfigurationQueries;
import net.bytemine.manager.db.LicenceQueries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.update.UpdateMgmt;
import net.bytemine.manager.utility.X509FileImporter;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.UserImport;
import net.bytemine.openvpn.UserImporter;
import net.bytemine.openvpn.ssh.SSHTool;
import net.bytemine.utility.DBUtils;
import net.bytemine.utility.DNUtil;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.StringUtils;
import net.miginfocom.swing.MigLayout;


/**
 * Generates user dialogs
 *
 * @author Daniel Rauer
 */
public class Dialogs {

    private static Logger logger = Logger.getLogger(Dialogs.class.getName());

    private static Popup popup;
    private static boolean skipFollowingDialogs = false;
    private static boolean x509ConfigurationSuccessfullySaved = false;
    
    
    /**
     * Shows a dialog for generating the root certificate
     *
     * @param frame The parent frame
     */
    public static void showRootCertDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.rootcert.answer_cancel"),
                rb.getString("dialog.rootcert.answer_yes"),
                rb.getString("dialog.rootcert.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.rootcert.text") + "\n" + rb.getString("dialog.rootcert.question"),
                rb.getString("dialog.rootcert.title"),
                CustomJOptionPane.YES_NO_CANCEL_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        // create root certificate
        if (n == CustomJOptionPane.YES_OPTION) {
            X509Utils.createRootCertificate();
            UserImport.incGeneratedCerts();
        }

        // import root certificate
        else if (n == CustomJOptionPane.NO_OPTION) {
            logger.info("importing root certificate");
            
            // create a file chooser
            final JFileChooser fc = LocalizedFileChooser.getLocalizedFileChooser();
            
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setFileHidingEnabled(false);
            fc.setAcceptAllFileFilterUsed(false);
            fc.addChoosableFileFilter(new CertificateFilter());
            
            fc.setApproveButtonText(rb.getString("fileChooser.rootCert.button"));
            fc.setDialogTitle(rb.getString("fileChooser.rootCert.title"));
            
            // responds to a button click:
            int returnVal = fc.showOpenDialog(frame);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                
                // try to import the certificate
                try {
                    X509FileImporter importer = new X509FileImporter(file);
                    importer.importRootCertAndKey();
                } catch (Exception e) {
                    new VisualException(e);
                    showRootCertDialog(frame);
                }
            } else {
                logger.info("file choosing canceled");
                showRootCertDialog(frame);
            }
        }

        // no root certificate will be created
        else if (n == CustomJOptionPane.CANCEL_OPTION) {
            logger.info("root import/generation skipped");
        }

        ManagerGUI.refreshX509Table();
    }


    /**
     * Shows a dialog for importing certificates and users
     *
     * @param frame The parent frame
     * @return true, if the import was skipped
     */
    public static boolean showImportDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.import.answer_yes"),
                rb.getString("dialog.import.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.import.text"),
                rb.getString("dialog.import.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        // no import
        if (n == CustomJOptionPane.NO_OPTION) {
            logger.info("import skipped");
            return true;
        }

        // import
        else if (n == CustomJOptionPane.YES_OPTION) {
            return false;
        }
        return false;
    }


    /**
     * Shows a dialog for deciding whether for users without certificates
     * shall be created one
     *
     * @param frame The parent frame
     * @return an integer with a code
     */
    public static int showCreateCertificatesDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.createcerts.answer_yes"),
                rb.getString("dialog.createcerts.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.createcerts.text"),
                rb.getString("dialog.createcerts.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        if (n == CustomJOptionPane.YES_OPTION)
            return UserImport.YES;
        else if (n == CustomJOptionPane.NO_OPTION)
            return UserImport.NO;

        return UserImport.NOT_SET;
    }


    /**
     * Shows a dialog for deciding whether not importable users shall be created
     * from the cn in the certificates
     *
     * @param frame The parent frame
     * @return an integer with a code
     */
    public static int showCreateUserFromCNDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.createusers.answer_yes"),
                rb.getString("dialog.createusers.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.createusers.text"),
                rb.getString("dialog.createusers.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        if (n == CustomJOptionPane.YES_OPTION)
            return UserImport.YES;
        else if (n == CustomJOptionPane.NO_OPTION)
            return UserImport.NO;

        return UserImport.NOT_SET;
    }
    
    
    /**
     * Shows a dialog to let the user decide whether saving changes changes or 
     * cancel them
     *
     * @param frame The parent frame
     * @return true, if the changes shall be saved
     */
    public static boolean showUnsavedDataDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        String rbPrefix = "dialog.unsaveddata.";
        
        Object[] options = {
                rb.getString(rbPrefix + "answer_yes"),
                rb.getString(rbPrefix + "answer_no")
        };
        
        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString(rbPrefix + "text"),
                rb.getString(rbPrefix + "title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        if (n == CustomJOptionPane.YES_OPTION)
            return true;
        else if (n == CustomJOptionPane.NO_OPTION)
            return false;
        return true;
    }


    /**
     * Host is not known to system. Ask user, if fingerprint is ok
     *
     * @param frame The parent frame
     * @return true, if the fingerprint is ok
     */
    public static boolean showKnownHostNewDialog(JFrame frame, String hostname, String fingerprint) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.knownhosts.answer_yes"),
                rb.getString("dialog.knownhosts.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.knownhosts.text_new") +
                        "\nServer: " + hostname +
                        "\nFingerprint " + fingerprint,
                rb.getString("dialog.knownhosts.title_new"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        if (n == CustomJOptionPane.YES_OPTION)
            return true;
        return false;
    }


    /**
     * Hosts fingerprint changed, has to be evaluated by user
     *
     * @param frame The parent frame
     * @return true, if fingerprint is ok
     */
    public static boolean showKnownHostChangedDialog(JFrame frame, String hostname, String fingerprint) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.knownhosts.answer_yes"),
                rb.getString("dialog.knownhosts.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.knownhosts.text_changed") +
                        "\nServer: " + hostname +
                        "\nFingerprint " + fingerprint,
                rb.getString("dialog.knownhosts.title_changed"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.WARNING_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.NO_OPTION)
            return true;
        return false;
    }


    /**
     * Asks, whether a mistrusted server is now trusted
     *
     * @param frame The parent frame
     * @return true, if server is trusted
     */
    public static boolean showKnownHostMistrustDialog(JFrame frame, String hostname, String fingerprint) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.knownhosts.answer_yes"),
                rb.getString("dialog.knownhosts.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.knownhosts.text_mistrusted") +
                        "\nServer: " + hostname +
                        "\nFingerprint " + fingerprint,
                rb.getString("dialog.knownhosts.title_mistrusted"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.WARNING_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.NO_OPTION)
            return true;
        return false;
    }


    /**
     * Hosts fingerprint changed, has to be evaluated by user
     *
     * @param frame The parent frame
     * @return true, if fingerprint is ok
     */
    public static boolean showSSHReconnectDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {
                rb.getString("dialog.ssh_reconnect.answer_yes"),
                rb.getString("dialog.ssh_reconnect.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.ssh_reconnect.text"),
                rb.getString("dialog.ssh_reconnect.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.WARNING_MESSAGE,
                null, // icon
                options,
                options[0]); //default button title

        if (n == CustomJOptionPane.YES_OPTION)
            return true;
        return false;
    }


    /**
     * Shows a dialog for choosing a language
     *
     * @param frame The parent frame
     */
    public static void showLanguageDialog(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {rb.getString("dialog.language.answer_de"),
                rb.getString("dialog.language.answer_en")
        };

        int n = CustomJOptionPane.showOptionDialog(frame,
                rb.getString("dialog.language.question"),
                rb.getString("dialog.language.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[0]); //default button

        if (n == CustomJOptionPane.NO_OPTION) {
            // english selected
            ResourceBundleMgmt.getInstance().setSelectedLanguage(Constants.LANGUAGE_CODE_ENGLISH);
            Configuration.getInstance().setLanguage(Constants.LANGUAGE_CODE_ENGLISH);
        } else {
            // german selected
            ResourceBundleMgmt.getInstance().setSelectedLanguage(Constants.LANGUAGE_CODE_GERMAN);
            Configuration.getInstance().setLanguage(Constants.LANGUAGE_CODE_GERMAN);
        }

    }


    /**
     * dialog for entering pkcs12 password
     *
     * @param parentFrame The parent frame
     * @param headline The title of the dialog
     * @return The entered password, or null on cancel
     */
    public static String showPKCS12PasswordDialog(JFrame parentFrame, String headline) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JLabel headlineLabel = new JLabel(headline);
        JLabel pwd1Label = new JLabel(rb.getString("dialog.pkcs12password.password"));
        JLabel pwd2Label = new JLabel(rb.getString("dialog.pkcs12password.repeat"));
        JPasswordField pwd1Field = new JPasswordField(30);
        JPasswordField pwd2Field = new JPasswordField(30);

        JPanel p = new JPanel(new MigLayout());
        p.add(headlineLabel, "span 2, wrap");
        p.add(pwd1Label);
        p.add(pwd1Field, "wrap");
        p.add(pwd2Label);
        p.add(pwd2Field, "wrap");
        
        int cancel = CustomJOptionPane.showOptionDialog(parentFrame,
                            p, rb.getString("dialog.pkcs12password.title"),
                            CustomJOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, null,
                            null, 2);
        
        if (cancel == CustomJOptionPane.CANCEL_OPTION)
            return null;
        
        String pwd1 = new String(pwd1Field.getPassword());
        String pwd2 = new String(pwd2Field.getPassword());

        String validateResult = ValidatorAction.validatePKCS12Password(pwd1, pwd2);
        if (validateResult == null) {
            // return password
            return pwd1;
        } else {
            // show error dialog
            CustomJOptionPane.showMessageDialog(parentFrame,
                    validateResult,
                    rb.getString("dialog.pkcs12password.errorTitle"),
                    CustomJOptionPane.ERROR_MESSAGE);
        }
        return showPKCS12PasswordDialog(parentFrame, headline);
    }


    /**
     * dialog for entering pkcs12 password
     *
     * @param parentFrame The parent frame
     */
    public static String showUserCommandDialog(JFrame parentFrame, String title) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JLabel headlineLabel = new JLabel(rb.getString("ccTab.command_label"));

        JTextField commandField = new JTextField(20);
        JPanel p = new JPanel(new MigLayout());
        p.add(headlineLabel, "span 2, wrap");
        p.add(commandField, "wrap");
        CustomJOptionPane.showMessageDialog(
                parentFrame, p, title, CustomJOptionPane.PLAIN_MESSAGE);

        return commandField.getText();
    }
    
    
    /**
     * dialog for entering a command while seeing its returning output
     *
     * @param parentFrame The parent frame
     * @param commandStr The displayed command
     * @param outputField The field containing the output
     *
    public static String showUserCommandOutputDialog(JFrame parentFrame, String commandStr, JTextArea outputField) {
        JLabel headlineLabel = new JLabel(commandStr);

        // input-field
        JTextField commandField = new JTextField(20);
        
        JScrollPane scroll = new JScrollPane(outputField);

        JPanel p = new JPanel(new MigLayout());
        p.add(headlineLabel, "span 2, wrap");
        p.add(commandField, "wrap");
        p.add(scroll, "height 150:300:350, growx, growy");

        int result = CustomJOptionPane.showOptionDialog(parentFrame,
                p, commandStr,
                CustomJOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null,
                null, 1);

        // 0=ok,	2=cancel
        if(result==2) {
        	return "";
        }
        
        return commandField.getText();
    }*/
    
    /**
     * resizable dialog for entering a command while seeing its returning output
     *
     * @param parentFrame The parent frame
     * @param commandStr The displayed command
     * @param outputField The field containing the output
     */
    public static String showUserCommandOutputDialog(JFrame parentFrame, String commandStr, JTextArea outputField, final Dimension commandOutputDialogSize) {
    	final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        final JDialog dialog = new JDialog(parentFrame, true);   
        dialog.setLayout(new BorderLayout());
        dialog.setTitle(commandStr);
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);
        
        Container inputPanel = dialog.getContentPane();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        Action sendAction = new AbstractAction("Send") {
			private static final long serialVersionUID = -6741799568173603317L;

			public void actionPerformed(ActionEvent e) {
            	commandOutputDialogSize.setSize( dialog.getSize() );
            	dialog.setVisible(false);
            }
        };
        
        // create input-Dialog
        final JTextField commandField = new JTextField();
        commandField.setAction(sendAction);
        JLabel headlineLabel = new JLabel(commandStr);
        JScrollPane scroll = new JScrollPane(outputField);
        
        
        final JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	commandOutputDialogSize.setSize( dialog.getSize() );
            	dialog.setVisible(false);
            }
        });
        final JButton cancelButton = new JButton(rb.getString("dialog.configuration.cancelButton"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	commandOutputDialogSize.setSize( dialog.getSize() );
            	commandField.setText("");
            	dialog.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel(new MigLayout("align center"));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        c.insets = new Insets(4,15,0,15);
        
        inputPanel.add(headlineLabel, c);
        c.gridwidth = 3;
        c.weightx = .5;
        c.weighty = 0;
        c.ipadx = 100;		// width
        c.ipady = 4;		// high
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 1;
        inputPanel.add(commandField, c);
        c.gridwidth = 3;
        c.weightx = .5;
        c.weighty = .5;
        c.ipadx = 100;		// width
        c.ipady = 250;		// high
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 2;
        inputPanel.add(scroll, c);
        c.gridwidth = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.ipadx = 100;		// width
        c.ipady = 4;		// high
        c.ipady = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridy = 3;
        inputPanel.add(buttonPanel, c);
        
        if (parentFrame != null) {
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        } else {
            dialog.setLocationRelativeTo(null);
        }
        
        inputPanel.setPreferredSize(commandOutputDialogSize);
        dialog.setContentPane(inputPanel);
        CssRuleManager.getInstance().format(dialog);
        dialog.pack();   
        dialog.setVisible(true);
        
    	return commandField.getText();
    }
    
    
    /**
     * dialog for displaying the OpenVPN version
     *
     * @param parentFrame The parent frame
     * @param version The version String returned from the server
     */
    public static void showOpenVPNVersionDialog(JFrame parentFrame, String version) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JLabel headlineLabel = new JLabel(rb.getString("ccTab.version_label") + ":");

        JPanel p = new JPanel(new MigLayout());
        p.add(headlineLabel, "span 2, wrap");
        p.add(new JLabel(version), "wrap");
        CustomJOptionPane.showMessageDialog(
                parentFrame, p, rb.getString("ccTab.version_title"), CustomJOptionPane.PLAIN_MESSAGE);
    }

    
    /**
     * dialog for displaying a OpenVpnStartDaemon-dialog
     *
     * @param parentFrame The parent frame
     * @param server      The server on which the daemon should be started
     */
    public static void showOpenVpnStartDaemonDialog(JFrame parentFrame, final Server server) throws Exception {
        
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        final JDialog dialog = new JDialog(parentFrame, true);   
        dialog.setLayout(new BorderLayout());
        
        dialog.setTitle(rb.getString("dialog.vpnstart.header"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);
        
        final Container inputPanel = dialog.getContentPane();
        inputPanel.setLayout(new MigLayout("insets 5, fillx, gap 5"));
        
        // create input-Dialog
        final JLabel usernameLabel = new JLabel(rb.getString("dialog.vpnstart.username"));
        usernameLabel.setFont(Constants.FONT_PLAIN);
        final JLabel commandLabel = new JLabel(rb.getString("dialog.vpnstart.command"));
        commandLabel.setFont(Constants.FONT_PLAIN);
        
        final JTextField usernameField = new JTextField(10);
        usernameField.setText("root");
        final JTextArea commandField = new JTextArea(6, 24);
        if (server.getServerType() == Server.SERVER_TYPE_BYTEMINE_APPLIANCE)
            commandField.setText(Constants.DEFAULT_OPENVPN_START_COMMAND_BOA);
        else
            commandField.setText(Constants.DEFAUlT_OPENVPN_START_COMMAND_NON_BOA);
        JScrollPane commandScroll = new JScrollPane(commandField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // auth-type
        final JRadioButton authTypePassword = new JRadioButton();
        final JRadioButton authTypeKeyfile = new JRadioButton();
        JLabel authTypeLabel = new JLabel(rb.getString("server.details.authType"));
        authTypeLabel.setFont(Constants.FONT_PLAIN);
        final JLabel authTypePasswordLabel = new JLabel(rb.getString("server.details.authType.password"));
        authTypePasswordLabel.setFont(Constants.FONT_PLAIN);
        final JLabel authTypeKeyfileLabel = new JLabel(rb.getString("server.details.authType.keyfile"));
        authTypeKeyfileLabel.setFont(Constants.FONT_PLAIN);
        final ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(authTypePassword);
        radioGroup.add(authTypeKeyfile);
        
        final JLabel keyfileLabel = new JLabel(rb.getString("server.details.keyfilepath"));
        if (authTypePassword.isSelected())
            keyfileLabel.setFont(Constants.FONT_PLAIN);
        else
            keyfileLabel.setFont(Constants.FONT_BOLD);
            
        final JTextField keyfileField = new JTextField(Constants.DEFAULT_SSH_KEYFILE_PATH , 10);
        
        final JButton filechooserButton = new JButton();
        filechooserButton.setIcon(
            ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton.setSize(30, 20);
        filechooserButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = ServerDetails.keyfileChooser(inputPanel);
                    if (filename != null)
                        keyfileField.setText(filename);
                }
            }
        });       
        
        keyfileField.setEnabled(false);
        
        if(server.getAuthType() == Server.AUTH_TYPE_PASSWORD)        
            authTypePassword.setSelected(true);
        else {
            authTypePassword.setSelected(false); 
            authTypeKeyfile.setSelected(true);
            keyfileField.setEnabled(true);
        }
        
        authTypePassword.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                keyfileField.setEnabled(false);
                //keyfileLabel.setFont(Constants.FONT_PLAIN);
                filechooserButton.setEnabled(false);
            }
        });
        authTypeKeyfile.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                keyfileField.setEnabled(true);
                //keyfileLabel.setFont(Constants.FONT_BOLD);
                filechooserButton.setEnabled(true);
            }
        });
        
        
        // buttons
        final JButton okButton = new JButton(rb.getString("dialog.vpnstart.option_ok"));
        okButton.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                
                int authtype = 0;
                
                if(authTypePassword.isSelected())
                    authtype = Server.AUTH_TYPE_PASSWORD;
                else if(authTypeKeyfile.isSelected())
                    authtype = Server.AUTH_TYPE_KEYFILE;
                
                SSHTool sshTool = null;
                
                try {
                    sshTool = new SSHTool(server, usernameField.getText(),
                                                         keyfileField.getText(),
                                                         authtype);
                    
                    sshTool.exec(commandField.getText().replace("\n", " "));
                    sshTool.disconnectSession();
                } catch (Exception ex) {
                    logger.warning("SSHTool.exec: "+ex.toString());
                }
                    
                if(sshTool != null)
                    sshTool.disconnectSession();
                
                dialog.setVisible(false);
            }
        });
        final JButton cancelButton = new JButton(rb.getString("dialog.vpnstart.option_cancel"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                commandField.setText("");
                dialog.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel(new MigLayout("align center"));       
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // TODO
        final JRadioButton startCmd = new JRadioButton();
        final JRadioButton restartCmd = new JRadioButton();
        JLabel vpnCmd = new JLabel(rb.getString("dialog.vpnstart.command_type"));
        vpnCmd.setFont(Constants.FONT_PLAIN);
        final JLabel startCmdLabel = new JLabel(rb.getString("dialog.vpnstart.start"));
        final JLabel restartCmdLabel = new JLabel(rb.getString("dialog.vpnstart.restart"));
        authTypeKeyfileLabel.setFont(Constants.FONT_PLAIN);
        startCmdLabel.setFont(Constants.FONT_PLAIN);
        restartCmdLabel.setFont(Constants.FONT_PLAIN);
        final ButtonGroup cmdGroup = new ButtonGroup();
        cmdGroup.add(startCmd);
        cmdGroup.add(restartCmd);
        
        startCmd.setSelected(true);
        restartCmd.setSelected(false);
        
        startCmd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (server.getServerType() == Server.SERVER_TYPE_BYTEMINE_APPLIANCE)
                    commandField.setText(Constants.DEFAULT_OPENVPN_START_COMMAND_BOA);
                else
                    commandField.setText(Constants.DEFAUlT_OPENVPN_START_COMMAND_NON_BOA);
            }
        });
        restartCmd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (server.getServerType() == Server.SERVER_TYPE_BYTEMINE_APPLIANCE)
                    commandField.setText("kill -HUP `head -1 /var/run/openvpnd.pid`");
                else
                    commandField.setText("restart vpn non boa");
            }
        });       
        // TODO
        
        
        inputPanel.add(new JLabel(),"wrap");
        inputPanel.add(usernameLabel, "align left");
        inputPanel.add(usernameField, "wrap");
        
        inputPanel.add(authTypeLabel, "align left");
        int row = 2;
        inputPanel.add(authTypePassword, "cell 1 "+row);
        inputPanel.add(authTypePasswordLabel, "cell 1 "+row);

        inputPanel.add(new JLabel());
        inputPanel.add(authTypeKeyfile, "cell 1 "+row);
        inputPanel.add(authTypeKeyfileLabel, "cell 1 "+row+", wrap");
        
        inputPanel.add(keyfileLabel, "align left");
        inputPanel.add(keyfileField, "align right, growx");
        inputPanel.add(filechooserButton, "width 30!, wrap");
        
        //TODO
        inputPanel.add(vpnCmd, "align left");
        int rowCmd = 4;
        inputPanel.add(startCmd, "cell 1 "+rowCmd);
        inputPanel.add(startCmdLabel, "cell 1 "+rowCmd);

        inputPanel.add(new JLabel());
        inputPanel.add(restartCmd, "cell 1 "+rowCmd);
        inputPanel.add(restartCmdLabel, "cell 1 "+rowCmd+", wrap");
        //TODO
        
        inputPanel.add(commandLabel, "align left");
        inputPanel.add(commandScroll, "align right, wrap");
        inputPanel.add(new JLabel("                "),"wrap");
        inputPanel.add(buttonPanel, "grow, push, span");
        
        if (parentFrame != null)
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        else
            dialog.setLocationRelativeTo(null);
        
        dialog.setContentPane(inputPanel);
        CssRuleManager.getInstance().format(dialog);
        dialog.pack();   
        dialog.setVisible(true);
    }
    
    
    /**
     * dialog for displaying a invalid OpenVPN-IP warning
     *
     * @param parentFrame The parent frame
     * @param ip The IP causing a warning
     */
    public static void showOpenVPNIpWarningDialog(JFrame parentFrame, String ip) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        final JDialog dialog = new JDialog(parentFrame, rb.getString("server.details.staticIp_warningTitle"), true);
        JPanel inputPanel = new JPanel(new MigLayout("align left"));
        
        JLabel headlineLabel = new JLabel("<html>" +
        		rb.getString("server.details.staticIp_warning1")+
        		ip+
        		rb.getString("server.details.staticIp_warning2")+
        		"<br>"+
        		rb.getString("server.details.staticIp_warning3")+
        		"<br></html>"
        );
        inputPanel.add(headlineLabel, "wrap");
        
        
        JLabel askAgainLabel = new JLabel(rb.getString("dialog.cctab.option_warning"));
        askAgainLabel.setFont(Constants.FONT_PLAIN);
        inputPanel.add(askAgainLabel);
        final JCheckBox askAgainCheckbox = new JCheckBox();
        askAgainCheckbox.setSelected(false);
        inputPanel.add(askAgainCheckbox);
        
        JButton okButton = new JButton(rb.getString("dialog.cctab.option_ok"));
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            	
            	if (askAgainCheckbox.isSelected())
                    Configuration.getInstance().setGuiShowOpenVPNIpWarningDialog(false);
            	
                dialog.dispose();
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(okButton);
        
        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        
        Point center = GuiUtils.getCenterPositionRelativeTo(dialog, parentFrame);
        dialog.setLocation(center);
        
        dialog.setVisible(true);
    }
    
    

    /**
     * dialog for x509 configurations
     *
     * @param parentFrame The parent frame
     * @return true, if the x509 configuration has been saved
     */
    public static boolean showX509ConfigurationDialog(final JFrame parentFrame) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        boolean rootExisting = Configuration.getInstance().isRootCertExisting();

        final JDialog dialog = new JDialog(parentFrame, true);
        dialog.setTitle(rb.getString("dialog.x509configuration.title"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);

        JPanel inputPanel = new JPanel(new MigLayout("fillx"));

        final JLabel rootHeadlineLabel = new JLabel(rb.getString("dialog.x509configuration.rootHeadline"));
        rootHeadlineLabel.setFont(Constants.FONT_BOLD);

        DNUtil dnUtil = new DNUtil();
        dnUtil.split(Configuration.getInstance().X509_ROOT_SUBJECT);
        final JLabel cLabel = new JLabel(rb.getString("dialog.x509configuration.c"));
        cLabel.setFont(Constants.FONT_PLAIN);
        final JTextField cField = new JTextField(dnUtil.getC(), 20);
        final JLabel stLabel = new JLabel(rb.getString("dialog.x509configuration.st"));
        stLabel.setFont(Constants.FONT_PLAIN);
        final JTextField stField = new JTextField(dnUtil.getSt(), 20);
        final JLabel lLabel = new JLabel(rb.getString("dialog.x509configuration.l"));
        lLabel.setFont(Constants.FONT_PLAIN);
        final JTextField lField = new JTextField(dnUtil.getL(), 20);
        final JLabel oLabel = new JLabel(rb.getString("dialog.x509configuration.o"));
        oLabel.setFont(Constants.FONT_PLAIN);
        final JTextField oField = new JTextField(dnUtil.getO(), 20);
        final JLabel ouLabel = new JLabel(rb.getString("dialog.x509configuration.ou"));
        ouLabel.setFont(Constants.FONT_PLAIN);
        final JTextField ouField = new JTextField(dnUtil.getOu(), 20);
        final JLabel cnLabel = new JLabel(rb.getString("dialog.x509configuration.cn"));
        cnLabel.setFont(Constants.FONT_PLAIN);
        final JTextField cnField = new JTextField(dnUtil.getCn(), 20);
        final JLabel emailLabel = new JLabel(rb.getString("dialog.x509configuration.email"));
        emailLabel.setFont(Constants.FONT_PLAIN);
        final JTextField emailField = new JTextField(dnUtil.getE(), 20);


        final JLabel rootValidFromLabel = new JLabel(rb.getString("dialog.x509configuration.rootValidFrom"));
        rootValidFromLabel.setFont(Constants.FONT_PLAIN);
        final JTextField rootValidFromField = new JTextField(8);
        rootValidFromField.setText(Configuration.getInstance().X509_ROOT_VALID_FROM);

        final JLabel rootValidToLabel = new JLabel(rb.getString("dialog.x509configuration.rootValidTo"));
        rootValidToLabel.setFont(Constants.FONT_PLAIN);
        final JTextField rootValidToField = new JTextField(8);
        rootValidToField.setText(Configuration.getInstance().X509_ROOT_VALID_TO);


        final JLabel serverHeadlineLabel = new JLabel(rb.getString("dialog.x509configuration.serverHeadline"));
        serverHeadlineLabel.setFont(Constants.FONT_BOLD);

        final JLabel serverValidForLabel = new JLabel(rb.getString("dialog.x509configuration.serverValidFor"));
        serverValidForLabel.setFont(Constants.FONT_PLAIN);
        final JTextField serverValidForField = new JTextField(4);
        serverValidForField.setHorizontalAlignment(JTextField.RIGHT);
        serverValidForField.setText(Configuration.getInstance().X509_SERVER_VALID_FOR);

        final JComboBox serverValidityUnitBox = new JComboBox();
        serverValidityUnitBox.addItem(rb.getString("units.days"));
        serverValidityUnitBox.addItem(rb.getString("units.weeks"));
        serverValidityUnitBox.addItem(rb.getString("units.years"));
        serverValidityUnitBox.setSelectedIndex(0);
        serverValidityUnitBox.setFont(Constants.FONT_PLAIN);

        final JLabel clientHeadlineLabel = new JLabel(rb.getString("dialog.x509configuration.clientHeadline"));
        clientHeadlineLabel.setFont(Constants.FONT_BOLD);

        final JLabel clientValidForLabel = new JLabel(rb.getString("dialog.x509configuration.clientValidFor"));
        clientValidForLabel.setFont(Constants.FONT_PLAIN);
        final JTextField clientValidForField = new JTextField(4);
        clientValidForField.setHorizontalAlignment(JTextField.RIGHT);
        clientValidForField.setText(Configuration.getInstance().X509_CLIENT_VALID_FOR);

        final JComboBox clientValidityUnitBox = new JComboBox();
        clientValidityUnitBox.addItem(rb.getString("units.days"));
        clientValidityUnitBox.addItem(rb.getString("units.weeks"));
        clientValidityUnitBox.addItem(rb.getString("units.years"));
        clientValidityUnitBox.setSelectedIndex(0);
        clientValidityUnitBox.setFont(Constants.FONT_PLAIN);

        final JLabel generalHeadlineLabel = new JLabel(rb.getString("dialog.x509configuration.generalHeadline"));
        generalHeadlineLabel.setFont(Constants.FONT_BOLD);

        final JLabel keyStrengthLabel = new JLabel(rb.getString("dialog.x509configuration.keyStrength"));
        keyStrengthLabel.setFont(Constants.FONT_PLAIN);
        final JComboBox keyStrengthBox = new JComboBox(Constants.AVAILABLE_KEYSTRENGTH);
        keyStrengthBox.setSelectedItem(Configuration.getInstance().X509_KEY_STRENGTH);
        final JLabel keyStrengthUnitLabel = new JLabel(rb.getString("units.keystrength"));
        keyStrengthUnitLabel.setFont(Constants.FONT_PLAIN);


        if (rootExisting) {
            cField.setEnabled(false);
            stField.setEnabled(false);
            lField.setEnabled(false);
            oField.setEnabled(false);
            cField.setEnabled(false);
            ouField.setEnabled(false);
            cnField.setEnabled(false);
            emailField.setEnabled(false);
            rootValidFromField.setEnabled(false);
            rootValidToField.setEnabled(false);
            keyStrengthBox.setEnabled(false);
        }


        final JButton saveButton = new JButton(rb.getString("dialog.x509configuration.saveButton"));
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    DNUtil dnUtil = new DNUtil();
                    dnUtil.setC(cField.getText());
                    dnUtil.setSt(stField.getText());
                    dnUtil.setL(lField.getText());
                    dnUtil.setO(oField.getText());
                    dnUtil.setOu(ouField.getText());
                    dnUtil.setCn(cnField.getText());
                    dnUtil.setE(emailField.getText());
                    String dnString = dnUtil.merge();

                    String validateResult = ValidatorAction.validateX509Configuration(
                            dnString,
                            rootValidFromField.getText(),
                            rootValidToField.getText(),
                            Integer.toString(0),
                            serverValidForField.getText(),
                            Integer.toString(0),
                            clientValidForField.getText(),
                            (String)keyStrengthBox.getSelectedItem()
                    );
                    if (validateResult == null || "".equals(validateResult)) {

                        String serverValidFor= serverValidForField.getText();
                        String clientValidFor= clientValidForField.getText();
                        String serverValidityUnit= (String) serverValidityUnitBox.getSelectedItem();
                        String clientValidityUnit= (String) clientValidityUnitBox.getSelectedItem();

                        if(! serverValidityUnit.equals(rb.getString("units.days"))) {
                           if (serverValidityUnit.equals(rb.getString("units.weeks"))) {
                               serverValidFor= Integer.toString(Integer.parseInt(serverValidFor) * 7);
                           } else if (serverValidityUnit.equals(rb.getString("units.years"))) {
                                serverValidFor= Integer.toString(Integer.parseInt(serverValidFor) * 365);
                            }
                        }

                        if(! clientValidityUnit.equals(rb.getString("units.days"))) {
                           if (clientValidityUnit.equals(rb.getString("units.weeks"))) {
                               clientValidFor= Integer.toString(Integer.parseInt(clientValidFor) * 7);
                           } else if (clientValidityUnit.equals(rb.getString("units.years"))) {
                                clientValidFor= Integer.toString(Integer.parseInt(clientValidFor) * 365);
                            }
                        }

                        x509ConfigurationSuccessfullySaved = ConfigurationAction.saveX509Configuration(
                                          dnString,
                                          rootValidFromField.getText(),
                                          rootValidToField.getText(),
                                          Integer.toString(0),
                                          serverValidFor,
                                          Integer.toString(0),
                                          clientValidFor,
                                          (String)keyStrengthBox.getSelectedItem()
                        );
                        dialog.setVisible(false);
                    } else {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(dialog,
                                validateResult,
                                rb.getString("dialog.x509configuration.error.title"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(dialog,
                            ex.getMessage(),
                            rb.getString("dialog.x509configuration.error.title"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        final JButton cancelButton = new JButton();
        final boolean firstCall = !ConfigurationQueries.areConfigurationsExisiting();
        if (firstCall)
            cancelButton.setText(rb.getString("dialog.configuration.skipButton"));
        else
            cancelButton.setText(rb.getString("dialog.configuration.cancelButton"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (firstCall) {
                    // user wants to skip at first application startup
                    // ask him to confirm
                    if (showReallySkipX509Dialog(dialog))
                        dialog.setVisible(false);
                } else
                    dialog.setVisible(false);
            }
        });


        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        inputPanel.add(rootHeadlineLabel, "gapleft 2");
        inputPanel.add(new JLabel(), "");
        inputPanel.add(serverHeadlineLabel, "gapleft 20");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(cLabel, "gapleft 12");
        inputPanel.add(cField, "growx");
        inputPanel.add(serverValidForLabel, "gapleft 30");
        inputPanel.add(serverValidForField, "align right");
        inputPanel.add(serverValidityUnitBox, "align left, wrap");

        inputPanel.add(stLabel, "gapleft 12");
        inputPanel.add(stField, "growx, wrap");

        inputPanel.add(lLabel, "gapleft 12");
        inputPanel.add(lField, "growx");
        inputPanel.add(new JLabel(), "gapleft 30");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(oLabel, "gapleft 12");
        inputPanel.add(oField, "growx");
        inputPanel.add(new JLabel(), "gapleft 30");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(ouLabel, "gapleft 12");
        inputPanel.add(ouField, "growx");
        inputPanel.add(clientHeadlineLabel, "gapleft 20");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(cnLabel, "gapleft 12");
        inputPanel.add(cnField, "growx");
        inputPanel.add(clientValidForLabel, "gapleft 30");
        inputPanel.add(clientValidForField, "align right");
        inputPanel.add(clientValidityUnitBox, "align left, wrap");

        inputPanel.add(emailLabel, "gapleft 12");
        inputPanel.add(emailField, "growx, wrap");

        inputPanel.add(new JLabel(), "height 15!, gapleft 12");
        inputPanel.add(new JLabel(), "");
        inputPanel.add(new JLabel(), "gapleft 30");
        // only to have some space to the right margin
        inputPanel.add(new JLabel(), "width 20!");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(rootValidFromLabel, "gapleft 12");
        inputPanel.add(rootValidFromField, "");
        inputPanel.add(generalHeadlineLabel, "gapleft 20");
        inputPanel.add(new JLabel(), "wrap");

        inputPanel.add(rootValidToLabel, "gapleft 12");
        inputPanel.add(rootValidToField, "");
        inputPanel.add(keyStrengthLabel, "gapleft 30");
        inputPanel.add(keyStrengthBox, "align right");
        inputPanel.add(keyStrengthUnitLabel, "align left, wrap");


        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        if (parentFrame != null) {
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        } else {
            dialog.setLocationRelativeTo(null);
        }

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        dialog.setVisible(true);

        return x509ConfigurationSuccessfullySaved;
    }


    /**
     * dialog for configurations
     *
     * @param parentFrame The parent frame
     * @return true, if all following dialogs shall not be displayed
     */
    public static boolean showConfigurationDialog(final JFrame parentFrame) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        Hashtable<String, String> configs = ConfigurationQueries.getConfigurations();

        final JDialog dialog = new JDialog(parentFrame, true);
        dialog.setTitle(rb.getString("app.title") + " - " + rb.getString("dialog.configuration.title"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);

        final CustomJPanel inputPanel = new CustomJPanel(new MigLayout("insets 5, fillx"));

        final JLabel languageHeadlineLabel = new JLabel(rb.getString("dialog.configuration.languageHeadline"));
        languageHeadlineLabel.setFont(Constants.FONT_BOLD);
        final JLabel moduleHeadlineLabel = new JLabel(rb.getString("dialog.configuration.moduleHeadline"));
        moduleHeadlineLabel.setFont(Constants.FONT_BOLD);
        
        final JCheckBox moduleCA = new JCheckBox(rb.getString("dialog.configuration.moduleCA"));
        moduleCA.setFont(Constants.FONT_PLAIN);

        final JCheckBox moduleCC = new JCheckBox(rb.getString("dialog.configuration.moduleCC"));
        moduleCC.setFont(Constants.FONT_PLAIN);

        try {
            boolean enableCA = Boolean.parseBoolean(configs.get(ConfigurationQueries.CA_ENABLED));
            if (enableCA)
                moduleCA.setSelected(true);
            boolean enableCC = Boolean.parseBoolean(configs.get(ConfigurationQueries.CC_ENABLED));
            if (enableCC)
                moduleCC.setSelected(true);

            if (!enableCA && !enableCC)
                moduleCA.setSelected(true);
        } catch (Exception e) {
            logger.warning("error getting the configured modules");
            moduleCA.setSelected(true);
        }


        final JLabel x509typeChooseLabel = new JLabel(rb.getString("dialog.configuration.x509typechoose"));
        x509typeChooseLabel.setFont(Constants.FONT_BOLD);

        final ButtonGroup radioGroupX509Type = new ButtonGroup();
        final JRadioButton x509typeChooseBase64 = new JRadioButton(rb.getString("dialog.configuration.x509typeChooseBase64"));
        x509typeChooseBase64.setFont(Constants.FONT_PLAIN);
        final JRadioButton x509typeChoosePKCS12 = new JRadioButton(rb.getString("dialog.configuration.x509typeChoosePKCS12"));
        x509typeChoosePKCS12.setFont(Constants.FONT_PLAIN);

        int certificateType = Constants.CERTIFICATE_TYPE_BASE64;
        try {
            certificateType = Integer.parseInt(configs.get(ConfigurationQueries.CERTIFICATE_TYPE));
            if (certificateType == Constants.CERTIFICATE_TYPE_BASE64)
                x509typeChooseBase64.setSelected(true);
            else
                x509typeChoosePKCS12.setSelected(true);
        } catch (Exception e) {
            logger.warning("error getting the configured certificate type");
            x509typeChooseBase64.setSelected(true);
        }
        radioGroupX509Type.add(x509typeChooseBase64);
        radioGroupX509Type.add(x509typeChoosePKCS12);

        final JPanel x509chooserPanel = new JPanel(new MigLayout());
        x509chooserPanel.add(x509typeChooseLabel, "span 2, wrap");
        x509chooserPanel.add(x509typeChooseBase64, "align left, wrap");
        x509chooserPanel.add(x509typeChoosePKCS12, "align left, wrap");

        final JLabel pkcs12PropertiesLabel = new JLabel(rb.getString("dialog.configuration.pkcs12Properties"));
        pkcs12PropertiesLabel.setFont(Constants.FONT_BOLD);
        final JLabel pkcs12NoPasswdLabel = new JLabel(rb.getString("dialog.configuration.pkcs12NoPasswd"));
        pkcs12NoPasswdLabel.setFont(Constants.FONT_PLAIN);
        final JLabel pkcs12SinglePasswdLabel = new JLabel(rb.getString("dialog.configuration.pkcs12SinglePasswd"));
        pkcs12SinglePasswdLabel.setFont(Constants.FONT_PLAIN);
        final JLabel pkcs12GlobalPasswdLabel = new JLabel(rb.getString("dialog.configuration.pkcs12GlobalPasswd"));
        pkcs12GlobalPasswdLabel.setFont(Constants.FONT_PLAIN);
        
        final ButtonGroup radioGroupPkcs12 = new ButtonGroup();
        final JRadioButton pkcs12NoPasswd = new JRadioButton();
        final JRadioButton pkcs12SinglePasswd = new JRadioButton();
        try {
            int type = Integer.parseInt(configs.get(ConfigurationQueries.PKCS12_PASSWORD_TYPE));
            if (type == Constants.PKCS12_NO_PASSWORD)
                pkcs12NoPasswd.setSelected(true);
            else
                pkcs12SinglePasswd.setSelected(true);
        } catch (Exception e) {
            logger.warning("error getting the configured pkcs12 password type");
            pkcs12NoPasswd.setSelected(true);
        }
        radioGroupPkcs12.add(pkcs12NoPasswd);
        radioGroupPkcs12.add(pkcs12SinglePasswd);

        final JPanel pkcs12PasswordChooserPanel = new JPanel(new MigLayout());
        pkcs12PasswordChooserPanel.add(pkcs12PropertiesLabel, "span 2, wrap");
        pkcs12PasswordChooserPanel.add(pkcs12NoPasswd, "width 25!");
        pkcs12PasswordChooserPanel.add(pkcs12NoPasswdLabel, "align left, wrap");
        pkcs12PasswordChooserPanel.add(pkcs12SinglePasswd, "width 25!");
        pkcs12PasswordChooserPanel.add(pkcs12SinglePasswdLabel);
        if (!x509typeChoosePKCS12.isSelected()) {
            Component[] components = pkcs12PasswordChooserPanel.getComponents();
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                component.setEnabled(false);
            }
        }

        x509typeChooseBase64.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Component[] components = pkcs12PasswordChooserPanel.getComponents();
                for (int i = 0; i < components.length; i++) {
                    Component component = components[i];
                    component.setEnabled(false);
                }
            }
        });
        x509typeChoosePKCS12.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Component[] components = pkcs12PasswordChooserPanel.getComponents();
                for (int i = 0; i < components.length; i++) {
                    Component component = components[i];
                    component.setEnabled(true);
                }
            }
        });


        final JCheckBox defaultDB = new JCheckBox(rb.getString("dialog.configuration.defaultdb"));
        boolean useDefaultDB = Configuration.getInstance().USE_DEFAULT_DB;
        defaultDB.setSelected(useDefaultDB);
        defaultDB.setFont(Constants.FONT_PLAIN);
        
        final JLabel dbChooseLabel = new JLabel(rb.getString("dialog.configuration.dbchoose"));
        dbChooseLabel.setFont(Constants.FONT_BOLD);

        final String dbPath = Configuration.getInstance().JDBC_PATH;
        final JTextField dbPathField = new JTextField(35);
        dbPathField.setText(dbPath);

        final JLabel dbPathLabel = new JLabel(rb.getString("dialog.configuration.dbpath"));
        dbPathLabel.setFont(Constants.FONT_PLAIN);
        final JButton dbChooserButton = new JButton();
        dbChooserButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        dbChooserButton.setSize(30, 20);
        dbChooserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.FILES_ONLY);
                    if (filename != null)
                        dbPathField.setText(filename);
                }
            }
        });
        
        defaultDB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (defaultDB.isSelected()) {
                    dbChooserButton.setEnabled(false);
                    
                    // read property file 
                    ResourceBundle configBundle = null;
                    try {
                        configBundle = ResourceBundle.getBundle(Configuration.getInstance().MANAGER_PROPERTIES_NAME);

                        String url = configBundle.getString("jdbc_url");
                        String path = DBUtils.getDBPathFromURL(url);
                        dbPathField.setText(path);
                    } catch (Exception ex) {
                        dbPathField.setText(Configuration.getInstance().JDBC_PATH);
                    }
                    dbPathField.setEnabled(false);
                } else {
                    dbChooserButton.setEnabled(true);
                    dbPathField.setEnabled(true);
                }
            }
        });
        if (useDefaultDB) {
            dbChooserButton.setEnabled(false);
            dbPathField.setEnabled(false);
        }


        final JLabel exportLabel = new JLabel(rb.getString("dialog.configuration.export"));
        exportLabel.setFont(Constants.FONT_BOLD);
        final JTextField certExportPathField = new JTextField(35);
        certExportPathField.setName("export_path");
        certExportPathField.setText(configs.get(ConfigurationQueries.CERT_EXPORT_PATH_KEY));
        if (StringUtils.isEmptyOrWhitespaces(certExportPathField.getText()))
            certExportPathField.setText(Constants.DEFAULT_EXPORT_PATH);
        final JLabel clientCertExportPathLabel = new JLabel(rb.getString("dialog.configuration.exportPath"));
        clientCertExportPathLabel.setFont(Constants.FONT_PLAIN);


        final JButton filechooserButton3 = new JButton();
        filechooserButton3.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton3.setSize(30, 20);
        filechooserButton3.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.DIRECTORIES_ONLY);
                    if (filename != null)
                        certExportPathField.setText(filename);
                }
            }
        });
        
        
        // general settings
        final JLabel generalLabel = new JLabel(rb.getString("dialog.configuration.general"));
        generalLabel.setFont(Constants.FONT_BOLD);
        final JCheckBox pam = new JCheckBox(rb.getString("dialog.configuration.pam"));
        pam.setSelected(Configuration.getInstance().USE_PAM);
        pam.setFont(Constants.FONT_PLAIN);
        
        final JCheckBox openVPNConfigFiles = new JCheckBox(rb.getString("dialog.configuration.create_openvpn_config_files"));
        openVPNConfigFiles.setSelected(Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES);
        openVPNConfigFiles.setFont(Constants.FONT_PLAIN);


        final JButton saveButton = new JButton(rb.getString("dialog.configuration.saveButton"));
        saveButton.setName("config_save");
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    String validateResult = ValidatorAction.validateConfiguration(
                            certExportPathField.getText(),
                            moduleCA.isSelected(),
                            moduleCC.isSelected(),
                            dbPathField.getText()
                    );

                    if (validateResult == null || "".equals(validateResult)) {
                        skipFollowingDialogs = ConfigurationAction.saveConfiguration(
                                certExportPathField.getText(),
                                moduleCA.isSelected(),
                                moduleCC.isSelected(),
                                x509typeChoosePKCS12.isSelected(),
                                pkcs12NoPasswd.isSelected(),
                                pkcs12SinglePasswd.isSelected(),
                                dbPathField.getText(),
                                defaultDB.isSelected(),
                                pam.isSelected(),
                                openVPNConfigFiles.isSelected()
                        );
                        dialog.setVisible(false);

                    } else {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(dialog,
                                validateResult,
                                rb.getString("dialog.configuration.error.title"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(dialog,
                            ex.getMessage(),
                            rb.getString("dialog.configuration.error.title"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        final JButton cancelButton = new JButton();
        final boolean firstCall = !ConfigurationQueries.areConfigurationsExisiting();
        if (firstCall)
            cancelButton.setText(rb.getString("dialog.configuration.skipButton"));
        else
            cancelButton.setText(rb.getString("dialog.configuration.cancelButton"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (firstCall) {
                    // user wants to skip at first application startup
                    // ask him to confirm
                    if (showReallySkipDialog(dialog))
                        dialog.setVisible(false);
                } else
                    dialog.setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        
        final ButtonGroup radioGroup1 = new ButtonGroup();

        final JRadioButton languageDE = new JRadioButton(rb.getString("dialog.configuration.de"));
        languageDE.setFont(Constants.FONT_PLAIN);
        final JRadioButton languageEN = new JRadioButton(rb.getString("dialog.configuration.en"));
        languageEN.setFont(Constants.FONT_PLAIN);

        languageDE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                germanSelected();
                updateConfigurationLabel(
                        dialog,
                        clientCertExportPathLabel,
                        cancelButton,
                        saveButton,
                        exportLabel,
                        languageHeadlineLabel,
                        languageDE,
                        languageEN,
                        moduleHeadlineLabel,
                        moduleCA,
                        moduleCC,
                        x509typeChooseLabel,
                        x509typeChooseBase64,
                        x509typeChoosePKCS12,
                        pkcs12PropertiesLabel,
                        pkcs12NoPasswdLabel,
                        pkcs12SinglePasswdLabel,
                        pkcs12GlobalPasswdLabel,
                        dbChooseLabel,
                        dbPathLabel,
                        defaultDB,
                        generalLabel,
                        pam,
                        openVPNConfigFiles
                );
            }
        });

        languageEN.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                englishSelected();
                updateConfigurationLabel(
                        dialog,
                        clientCertExportPathLabel,
                        cancelButton,
                        saveButton,
                        exportLabel,
                        languageHeadlineLabel,
                        languageDE,
                        languageEN,
                        moduleHeadlineLabel,
                        moduleCA,
                        moduleCC,
                        x509typeChooseLabel,
                        x509typeChooseBase64,
                        x509typeChoosePKCS12,
                        pkcs12PropertiesLabel,
                        pkcs12NoPasswdLabel,
                        pkcs12SinglePasswdLabel,
                        pkcs12GlobalPasswdLabel,
                        dbChooseLabel,
                        dbPathLabel,
                        defaultDB,
                        generalLabel,
                        pam,
                        openVPNConfigFiles
                );
            }
        });

        try {
            String language = configs.get(ConfigurationQueries.LANGUAGE_KEY);
            // on first startup no language is stored in the database
            if (language == null)
                // read the language from the user resourcebundle
                language = ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage();
            
            if (language != null && Constants.LANGUAGE_CODE_GERMAN.equals(language)) {
                languageDE.doClick();
                languageDE.setSelected(true);
            } else {
                languageEN.doClick();
                languageEN.setSelected(true);
            }
        } catch (Exception e) {
            logger.warning("error getting the configured language");
            languageEN.setSelected(true);
        }
        radioGroup1.add(languageDE);
        radioGroup1.add(languageEN);


        JPanel languagePanel = new JPanel(new MigLayout());
        languagePanel.add(languageHeadlineLabel, "span 2, wrap");
        languagePanel.add(languageDE, "wrap");
        languagePanel.add(languageEN, "wrap");


        JPanel modePanel = new JPanel(new MigLayout());
        modePanel.add(moduleHeadlineLabel, "span 2, wrap");
        modePanel.add(moduleCA, "wrap");
        modePanel.add(moduleCC, "wrap");

        /*
         * Components that need to be disabled in case the db path is changing
         */
        final Vector<Component> components = new Vector<Component>();
        components.add(languageDE);
        components.add(languageEN);
        components.add(moduleCA);
        components.add(moduleCC);
        components.add(x509typeChooseBase64);
        components.add(x509typeChoosePKCS12);
        components.add(pkcs12NoPasswd);
        components.add(pkcs12SinglePasswd);
        components.add(certExportPathField);
        
        /*
         * Listener for the db Path field
         * disables all controls after changing the value
         */
        dbPathField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) {
                checkValueChange();
            }
            
            public void insertUpdate(DocumentEvent e) {
                checkValueChange();
            }

            public void changedUpdate(DocumentEvent e) {
                checkValueChange();
            }
            
            /*
             * Disable all components if the value has changed
             */
            private void checkValueChange() {
                if(!dbPath.equals(dbPathField.getText())) {
                    for (Iterator<Component> iter = components.iterator(); iter.hasNext();) {
                        Component comp = (Component) iter.next();
                        comp.setEnabled(false);
                    }
                } else {
                    for (Iterator<Component> iter = components.iterator(); iter.hasNext();) {
                        Component comp = (Component) iter.next();
                        comp.setEnabled(true);
                    }
                }
                
            }
        });
        
        /*
         * add everything to the input panel
         */
        inputPanel.add(languagePanel);
        inputPanel.add(modePanel);
        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");

        // X509 type and password-stuff
        inputPanel.add(x509chooserPanel, "gapbottom push");
        inputPanel.add(pkcs12PasswordChooserPanel);
        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");

        JPanel dbPanel = new JPanel(new MigLayout("insets 0, fillx"));
        dbPanel.add(dbChooseLabel, "gapleft 8, wrap");
        dbPanel.add(defaultDB, "gapleft 8, wrap");
        dbPanel.add(dbPathLabel, "gapleft 12");
        dbPanel.add(dbPathField, "gapleft 8, growx");
        dbPanel.add(dbChooserButton, "width 30!, wrap");
        inputPanel.add(dbPanel, "span 3, growx");

        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");

        JPanel exportPanel = new JPanel(new MigLayout("insets 0, fillx"));
        exportPanel.add(exportLabel, "gapleft 8, wrap");
        exportPanel.add(clientCertExportPathLabel, "gapleft 12");
        exportPanel.add(certExportPathField, "growx");
        exportPanel.add(filechooserButton3, "width 30!, wrap");
        inputPanel.add(exportPanel, "span 3, growx");
        
        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");
        
        JPanel generalPanel = new JPanel(new MigLayout("insets 0, fillx"));
        generalPanel.add(generalLabel, "gapleft 8, wrap");
        generalPanel.add(pam, "gapleft 8, wrap");
        generalPanel.add(openVPNConfigFiles, "gapleft 8, wrap");
        inputPanel.add(generalPanel, "span 3, growx");

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        if (parentFrame != null) {
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        } else {
            dialog.setLocationRelativeTo(null);
        }

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        dialog.setVisible(true);

        return skipFollowingDialogs;
    }
    
    
    
    /**
     * dialog for user import configurations
     *
     * @param parentFrame The parent frame
     * @return true, if all following dialogs shall not be displayed
     */
    public static boolean showImportConfigurationDialog(final JFrame parentFrame) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        Hashtable<String, String> configs = ConfigurationQueries.getConfigurations();

        final JDialog dialog = new JDialog(parentFrame, true);
        dialog.setTitle(rb.getString("app.title") + " - " + rb.getString("dialog.import_configuration.title"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);

        final CustomJPanel inputPanel = new CustomJPanel(new MigLayout("insets 5, fillx"));
        final JPanel filePanel = new JPanel(new MigLayout("insets 0, fillx"));
        final JPanel ldapPanel = new JPanel(new MigLayout("insets 0, fillx"));

        final JLabel fileHeadlineLabel = new JLabel(rb.getString("dialog.configuration.typechooseFileHeadline"));
        fileHeadlineLabel.setFont(Constants.FONT_BOLD);
        final JLabel ldapHeadlineLabel = new JLabel(rb.getString("dialog.configuration.typechooseLdapHeadline"));
        ldapHeadlineLabel.setFont(Constants.FONT_BOLD);

        final JPanel chooserPanel = new JPanel(new MigLayout());
        final JLabel typeChooseLabel = new JLabel(rb.getString("dialog.configuration.typechoose"));
        typeChooseLabel.setFont(Constants.FONT_BOLD);
        
        final ButtonGroup radioGroup = new ButtonGroup();
        final JRadioButton typeChooseFile = new JRadioButton(rb.getString("dialog.configuration.typechooseFile"));
        typeChooseFile.setFont(Constants.FONT_PLAIN);
        final JRadioButton typeChooseLdap = new JRadioButton(rb.getString("dialog.configuration.typechooseLdap"));
        typeChooseLdap.setFont(Constants.FONT_PLAIN);
        
        typeChooseFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Component[] fileComponents = filePanel.getComponents();
                for (int i = 0; i < fileComponents.length; i++) {
                    Component component = fileComponents[i];
                    component.setEnabled(true);
                }
                Component[] ldapComponents = ldapPanel.getComponents();
                for (int i = 0; i < ldapComponents.length; i++) {
                    Component component = ldapComponents[i];
                    component.setEnabled(false);
                }
            }
        });
        typeChooseLdap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Component[] fileComponents = filePanel.getComponents();
                for (int i = 0; i < fileComponents.length; i++) {
                    Component component = fileComponents[i];
                    component.setEnabled(false);
                }
                Component[] ldapComponents = ldapPanel.getComponents();
                for (int i = 0; i < ldapComponents.length; i++) {
                    Component component = ldapComponents[i];
                    component.setEnabled(true);
                }
            }
        });
        int userImportType = Constants.USER_IMPORT_TYPE_FILE;
        try {
            userImportType = Integer.parseInt(configs.get(ConfigurationQueries.USER_IMPORT_TYPE));
            if (userImportType == Constants.USER_IMPORT_TYPE_LDAP) {
                typeChooseLdap.setSelected(true);
            } else
                typeChooseFile.setSelected(true);
        } catch (Exception e) {
            logger.warning("error getting the configured user_import_type");
            typeChooseFile.setSelected(true);
        }
        radioGroup.add(typeChooseFile);
        radioGroup.add(typeChooseLdap);

        chooserPanel.add(typeChooseLabel, "span 2, wrap");
        chooserPanel.add(typeChooseFile, "align left, wrap");
        chooserPanel.add(typeChooseLdap, "align left, wrap");


        final JTextField clientCertImportDirField = new JTextField(35);
        clientCertImportDirField.setText(configs.get(ConfigurationQueries.CLIENT_CERT_IMPORT_DIR_KEY));
        final JLabel clientCertImportDirLabel = new JLabel(rb.getString("dialog.configuration.clientImportDir"));
        clientCertImportDirLabel.setFont(Constants.FONT_PLAIN);
        final JButton filechooserButton = new JButton();
        filechooserButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton.setSize(30, 20);
        filechooserButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.DIRECTORIES_ONLY);
                    if (filename != null)
                        clientCertImportDirField.setText(filename);
                }
            }
        });

        final JTextField userfileField = new JTextField(35);
        userfileField.setText(configs.get(ConfigurationQueries.CLIENT_USERFILE));
        final JLabel userfileLabel = new JLabel(rb.getString("dialog.configuration.userfile"));
        userfileLabel.setFont(Constants.FONT_PLAIN);
        final JButton filechooserButton2 = new JButton();
        filechooserButton2.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton2.setSize(30, 20);
        filechooserButton2.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.FILES_ONLY);
                    if (filename != null)
                        userfileField.setText(filename);
                }
            }
        });


        final JLabel hostLabel = new JLabel(rb.getString("dialog.configuration.host") + " *");
        hostLabel.setFont(Constants.FONT_PLAIN);
        final JTextField hostField = new JTextField(35);
        hostField.setText(configs.get(ConfigurationQueries.LDAP_HOST));
        final JLabel portLabel = new JLabel(rb.getString("dialog.configuration.port") + " *");
        portLabel.setFont(Constants.FONT_PLAIN);
        final JTextField portField = new JTextField(4);
        portField.setText(configs.get(ConfigurationQueries.LDAP_PORT));
        final JLabel dnLabel = new JLabel(rb.getString("dialog.configuration.dn") + " *");
        dnLabel.setFont(Constants.FONT_PLAIN);
        final JTextField dnField = new JTextField(35);
        dnField.setText(configs.get(ConfigurationQueries.LDAP_DN));
        final JLabel objLabel = new JLabel(rb.getString("dialog.configuration.objectclass") + " *");
        objLabel.setFont(Constants.FONT_PLAIN);
        final JTextField objField = new JTextField(15);
        objField.setText(configs.get(ConfigurationQueries.LDAP_OBJECTCLASS));
        final JLabel cnLabel = new JLabel(rb.getString("dialog.configuration.cnattr") + " *");
        cnLabel.setFont(Constants.FONT_PLAIN);
        final JTextField cnField = new JTextField(15);
        cnField.setText(configs.get(ConfigurationQueries.LDAP_CN));

        final JLabel certAttrLabel = new JLabel(rb.getString("dialog.configuration.certattr") + " *");
        certAttrLabel.setFont(Constants.FONT_PLAIN);
        final JTextField certAttrField = new JTextField(15);
        certAttrField.setText(configs.get(ConfigurationQueries.LDAP_CERT_ATTRIBUTE_NAME));
        final JLabel certImportDirLabel = new JLabel(rb.getString("dialog.configuration.certimportdir") + " *");
        certImportDirLabel.setFont(Constants.FONT_PLAIN);
        final JTextField certImportDirField = new JTextField(35);
        certImportDirField.setText(configs.get(ConfigurationQueries.LDAP_CERT_IMPORT_DIR));
        certAttrField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                certImportDirField.setText("");
            }
        });
        certImportDirField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                certAttrField.setText("");
            }
        });
        final JButton filechooserButton4 = new JButton();
        filechooserButton4.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton4.setSize(30, 20);
        filechooserButton4.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.DIRECTORIES_ONLY);
                    if (filename != null)
                        certImportDirField.setText(filename);
                }
            }
        });
        
        
        final JButton saveButton = new JButton(rb.getString("dialog.import_configuration.saveButton"));
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    String validateResult = ValidatorAction.validateImportConfiguration(
                            clientCertImportDirField.getText(),
                            userfileField.getText(),
                            hostField.getText(),
                            portField.getText(),
                            dnField.getText(),
                            objField.getText(),
                            cnField.getText(),
                            certAttrField.getText(),
                            certImportDirField.getText(),
                            typeChooseFile.isSelected()
                    );

                    if (validateResult == null || "".equals(validateResult)) {
                        skipFollowingDialogs = ConfigurationAction.saveImportConfiguration(
                                clientCertImportDirField.getText(),
                                userfileField.getText(),
                                hostField.getText(),
                                portField.getText(),
                                dnField.getText(),
                                objField.getText(),
                                cnField.getText(),
                                certAttrField.getText(),
                                certImportDirField.getText(),
                                typeChooseFile.isSelected()
                        );
                        dialog.setVisible(false);
                        
                        // start the user import
                        boolean create = Dialogs.showCreateCertificatesDialog(ManagerGUI.mainFrame) == UserImport.YES;
                        UserImport im = UserImporter.getUserImportImplementation(create);
                        try {
                            if (im.createUsersFromCN == UserImport.NOT_SET) {
                                // show dialog
                                im.createUsersFromCN = Dialogs.showCreateUserFromCNDialog(ManagerGUI.mainFrame);
                            }
                            im.importUsers();
                        } catch (Exception ex) {
                            CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                    ex.getMessage(),
                                    rb.getString("dialog.configuration.error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                        }

                    } else {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(dialog,
                                validateResult,
                                rb.getString("dialog.configuration.error.title"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(dialog,
                            ex.getMessage(),
                            rb.getString("dialog.configuration.error.title"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        final JButton cancelButton = new JButton();
        cancelButton.setText(rb.getString("dialog.configuration.cancelButton"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                skipFollowingDialogs = false;
            }
        });

        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);


        /*
         * add everything to the input panel
         */
        inputPanel.add(chooserPanel);

        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");
        filePanel.add(fileHeadlineLabel, "gapleft 8, wrap");
        filePanel.add(clientCertImportDirLabel, "gapleft 12");
        filePanel.add(clientCertImportDirField, "growx");
        filePanel.add(filechooserButton, "width 30!, wrap");

        filePanel.add(userfileLabel, "gapleft 12");
        filePanel.add(userfileField, "growx");
        filePanel.add(filechooserButton2, "width 30!, wrap");

        ldapPanel.add(ldapHeadlineLabel, "gapleft 8, wrap");
        ldapPanel.add(hostLabel, "gapleft 12");
        ldapPanel.add(hostField, "growx, wrap");
        ldapPanel.add(portLabel, "gapleft 12");
        ldapPanel.add(portField, "growx, wrap");
        ldapPanel.add(dnLabel, "gapleft 12");
        ldapPanel.add(dnField, "growx, wrap");
        ldapPanel.add(objLabel, "gapleft 12");
        ldapPanel.add(objField, "growx, wrap");
        ldapPanel.add(cnLabel, "gapleft 12");
        ldapPanel.add(cnField, "growx, wrap");
        ldapPanel.add(certAttrLabel, "gapleft 12");
        ldapPanel.add(certAttrField, "growx");
        JLabel orLabel = new JLabel(rb.getString("dialog.configuration.or"));
        orLabel.setFont(Constants.FONT_PLAIN);
        ldapPanel.add(orLabel, "wrap");
        ldapPanel.add(certImportDirLabel, "gapleft 12");
        ldapPanel.add(certImportDirField, "growx");
        ldapPanel.add(filechooserButton4, "width 30!, wrap");
        CssRuleManager.getInstance().format(ldapPanel);

        if (userImportType == Constants.USER_IMPORT_TYPE_LDAP) {
            Component[] fileComponents = filePanel.getComponents();
            for (int i = 0; i < fileComponents.length; i++) {
                Component component = fileComponents[i];
                component.setEnabled(false);
            }
            Component[] ldapComponents = ldapPanel.getComponents();
            for (int i = 0; i < ldapComponents.length; i++) {
                Component component = ldapComponents[i];
                component.setEnabled(true);
            }
        } else {
            Component[] fileComponents = filePanel.getComponents();
            for (int i = 0; i < fileComponents.length; i++) {
                Component component = fileComponents[i];
                component.setEnabled(true);
            }
            Component[] ldapComponents = ldapPanel.getComponents();
            for (int i = 0; i < ldapComponents.length; i++) {
                Component component = ldapComponents[i];
                component.setEnabled(false);
            }
        }
        inputPanel.add(filePanel, "span 3, growx, wrap");
        inputPanel.add(ldapPanel, "span 3, growx");

        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");
        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");
        inputPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, wrap");

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        if (parentFrame != null) {
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        } else {
            dialog.setLocationRelativeTo(null);
        }

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        dialog.setVisible(true);

        return skipFollowingDialogs;
    }


    /**
     * Shows a dialog for configuring the update process
     *
     * @param parentFrame The parent frame
     */
    public static void showUpdateConfigurationDialog(final JFrame parentFrame) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Hashtable<String, String> configs = ConfigurationQueries.getConfigurations();

        UpdateMgmt updateMgmt = UpdateMgmt.getInstance();
        final boolean keystoreExisting = updateMgmt.isUpdateKeystoreExisting();
        String crtPath = LicenceQueries.getCrtPath();
        String keyPath = LicenceQueries.getKeyPath();
        boolean pemSelected = (keyPath == null || "".equals(keyPath)) ? true : false;

        
        final JDialog dialog = new JDialog(parentFrame, true);
        dialog.setTitle(rb.getString("dialog.updateconfiguration.title"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        dialog.setIconImage(icon);

        Font font = dialog.getFont();
        if (font == null)
            font = new Font("Sans-Serif", Font.PLAIN, 12);


        final CustomJPanel inputPanel = new CustomJPanel(new MigLayout("insets 5, fillx"));

        final JCheckBox automaticUpdate = new JCheckBox(rb.getString("dialog.updateconfiguration.automaticUpdateHeadline"));
        try {
            boolean enableUpdate = Boolean.parseBoolean(configs.get(ConfigurationQueries.UPDATE_AUTOMATICALLY));
            if (enableUpdate)
                automaticUpdate.setSelected(true);
        } catch (Exception e) {
            logger.warning("error getting the automatic update state");
            automaticUpdate.setSelected(false);
        }

        String updateServerPath = Configuration.getInstance().UPDATE_SERVER;
        String updateRepository = Configuration.getInstance().UPDATE_REPOSITORY;

        final JLabel serverLabel = new JLabel();
        serverLabel.setText(rb.getString("dialog.updateconfiguration.server"));
        final JTextField serverField = new JTextField(updateServerPath.equals(Constants.UPDATE_HTTPS_SERVER) ? "" : updateServerPath, 35);

        final JLabel repoLabel = new JLabel();
        repoLabel.setText(rb.getString("dialog.updateconfiguration.repo"));
        final JTextField repoField = new JTextField(updateRepository.equals(Constants.UPDATE_REPOSITORY) ? "" : updateRepository, 35);


//      enhanced options
        final JPanel extensionPanel = new JPanel(new MigLayout("insets 0, fillx"));
        extensionPanel.setVisible(false);
        final JButton expandButton = new JButton();
        expandButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_EXPAND, "expand")
        );
        expandButton.setSize(20, 20);
        expandButton.addMouseListener(new java.awt.event.MouseAdapter() {
            boolean expanded = false;

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    extensionPanel.setVisible(!expanded);
                    if (expanded)
                        expandButton.setIcon(
                                ImageUtils.createImageIcon(Constants.ICON_EXPAND, "expand")
                        );
                    else
                        expandButton.setIcon(
                                ImageUtils.createImageIcon(Constants.ICON_COLLAPSE, "collapse")
                        );
                    expanded = !expanded;
                }
            }
        });
        
        JPanel expandTopPanel = new JPanel(new MigLayout("insets 0"));
        JLabel expandLabel = new JLabel(rb.getString("user.details.expandOptions"));
        expandLabel.setFont(Constants.FONT_PLAIN);
        expandTopPanel.add(expandButton, "width 20!");
        expandTopPanel.add(expandLabel);

        String proxy = Configuration.getInstance().UPDATE_PROXY != null ? Configuration.getInstance().UPDATE_PROXY : "";
        String proxyPort = Configuration.getInstance().UPDATE_PROXY_PORT != null ? Configuration.getInstance().UPDATE_PROXY_PORT : "";
        final JTextField proxyField = new JTextField(proxy, 30);
        final JTextField proxyPortField = new JTextField(proxyPort, 6);

        extensionPanel.add(new JLabel(rb.getString("dialog.updateconfiguration.proxy")), "align right");
        extensionPanel.add(proxyField, "span, growx, wrap");
        extensionPanel.add(new JLabel(rb.getString("dialog.updateconfiguration.proxyPort")), "align right");
        extensionPanel.add(proxyPortField, "span, growx, wrap");

        JPanel updatePanel = new JPanel(new MigLayout("insets 0, fillx"));
        updatePanel.add(automaticUpdate, "gapleft 7, wrap");
        updatePanel.add(serverLabel, "span 3, gapleft 7, gaptop 7");
        updatePanel.add(serverField, "growx, wrap");
        updatePanel.add(repoLabel, "span 3, gapleft 7");
        updatePanel.add(repoField, "growx");

        inputPanel.add(updatePanel, "span 3, growx, wrap");
        inputPanel.add(expandTopPanel, "gaptop 10, wrap");
        inputPanel.add(extensionPanel, "span 3, growx, wrap");


        JLabel certChooseLabel = new JLabel();
        certChooseLabel.setText(rb.getString("dialog.updateconfiguration.certChooseLabelNew"));
        if (keystoreExisting)
            certChooseLabel.setText(rb.getString("dialog.updateconfiguration.certChooseLabel"));


        if (font != null)
            certChooseLabel.setFont(font.deriveFont(font.getStyle() ^ Font.BOLD));

        final ButtonGroup radioGroupCertType = new ButtonGroup();
        final JRadioButton certTypeChoosePEM = new JRadioButton(rb.getString("dialog.updateconfiguration.certTypePEMLabel"));
        final JRadioButton certTypeChooseCRT = new JRadioButton(rb.getString("dialog.updateconfiguration.certTypeCRTLabel"));

        radioGroupCertType.add(certTypeChoosePEM);
        radioGroupCertType.add(certTypeChooseCRT);

        final JPanel certChooserPanel = new JPanel(new MigLayout("insets 0"));
        certChooserPanel.add(certChooseLabel, "span 2, wrap");
        certChooserPanel.add(certTypeChoosePEM, "align left, wrap");
        certChooserPanel.add(certTypeChooseCRT, "");

        inputPanel.add(certChooserPanel, "gaptop 10, span 3, wrap");


        final JPanel crtPanel = new JPanel(new MigLayout("insets 0, fillx"));
        final JTextField crtPathField = new JTextField(crtPath, 35);
        final JLabel crtPathLabel = new JLabel(rb.getString("dialog.updateconfiguration.pemPath"));
        final JButton pemChooserButton = new JButton();
        pemChooserButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        pemChooserButton.setSize(30, 20);
        pemChooserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.FILES_ONLY);
                    if (filename != null)
                        crtPathField.setText(filename);
                }
            }
        });

        crtPanel.add(crtPathLabel, "gapleft 12");
        crtPanel.add(crtPathField, "growx");
        crtPanel.add(pemChooserButton, "width 30!, wrap");
        inputPanel.add(crtPanel, "span 3, growx, wrap");


        final JPanel keyPanel = new JPanel(new MigLayout("insets 0, fillx"));
        final JTextField keyPathField = new JTextField(keyPath, 35);
        final JLabel keyPathLabel = new JLabel(rb.getString("dialog.updateconfiguration.keyPath"));
        final JButton keyChooserButton = new JButton();
        keyChooserButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        keyChooserButton.setSize(30, 20);
        keyChooserButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = fileChooser(parentFrame, JFileChooser.FILES_ONLY);
                    if (filename != null)
                        keyPathField.setText(filename);
                }
            }
        });

        keyPanel.add(keyPathLabel, "gapleft 12");
        keyPanel.add(keyPathField, "growx");
        keyPanel.add(keyChooserButton, "width 30!, wrap");
        keyPanel.setVisible(false);
        inputPanel.add(keyPanel, "span 3, growx, wrap");


        certTypeChoosePEM.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                keyPanel.setVisible(false);
                crtPathLabel.setText(rb.getString("dialog.updateconfiguration.pemPath"));
            }
        });
        certTypeChooseCRT.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                keyPanel.setVisible(true);
                crtPathLabel.setText(rb.getString("dialog.updateconfiguration.crtPath"));
            }
        });


        JButton saveButton = new JButton(rb.getString("dialog.updateconfiguration.savebutton"));
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    String validateResult = null;

                    if (!keystoreExisting)
                        // only validate certificate fields if no keystore is existing
                        validateResult = ValidatorAction.validateUpdateConfiguration(
                                certTypeChoosePEM.isSelected(),
                                crtPathField.getText(),
                                keyPathField.getText(),
                                serverField.getText(),
                                repoField.getText(),
                                proxyField.getText(),
                                proxyPortField.getText()
                        );
                    if (validateResult == null || "".equals(validateResult)) {
                        boolean certificateChangesOccured = ConfigurationAction.saveUpdateConfiguration(
                                automaticUpdate.isSelected(),
                                certTypeChoosePEM.isSelected(),
                                crtPathField.getText(),
                                keyPathField.getText(),
                                serverField.getText(),
                                repoField.getText(),
                                proxyField.getText(),
                                proxyPortField.getText()
                        );

                        ManagerGUI.enableUpdateSearch();
                        dialog.dispose();

                        // show success dialogs
                        if (certificateChangesOccured)
                            CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                    rb.getString("dialog.updateconfiguration.success"),
                                    rb.getString("dialog.updateconfiguration.title"),
                                    CustomJOptionPane.INFORMATION_MESSAGE);
                        else
                            CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                    rb.getString("dialog.updateconfiguration.success_save"),
                                    rb.getString("dialog.updateconfiguration.title"),
                                    CustomJOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(dialog,
                                validateResult,
                                rb.getString("dialog.updateconfiguration.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(dialog,
                            ex.getMessage(),
                            rb.getString("dialog.updateconfiguration.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton cancelButton = new JButton(rb.getString("dialog.updateconfiguration.cancelbutton"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);


        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);


        if (parentFrame != null) {
            dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));
        } else {
            dialog.setLocationRelativeTo(null);
        }

        if (pemSelected)
            certTypeChoosePEM.setSelected(true);
        else {
            certTypeChooseCRT.setSelected(true);
            keyPanel.setVisible(true);
            crtPathLabel.setText(rb.getString("dialog.updateconfiguration.crtPath"));
        }


        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        dialog.setVisible(true);
    }


    /**
     * Shows a dialog for choosing to skip configuration or not
     *
     * @param parent The parent dialog
     * @return true, if the configuration shall be skipped
     *         false, if not
     */
    public static boolean showReallySkipDialog(JDialog parent) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {rb.getString("dialog.skipconfiguration.answer_yes"),
                rb.getString("dialog.skipconfiguration.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(parent,
                rb.getString("dialog.skipconfiguration.question"),
                rb.getString("dialog.skipconfiguration.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.YES_OPTION) {
            // skip
            return true;
        } else {
            // go on
            return false;
        }

    }

/**
     * Shows a dialog for choosing to skip password protection or not
     *
     * @param parent The parent jframe
     * @return true, if the configuration shall be skipped
     *         false, if not
     */
    public static boolean showReallySkipPkcs12Password(JFrame parent) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {rb.getString("dialog.skippkcs12pass.answer_yes"),
                rb.getString("dialog.skippkcs12pass.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(parent,
                rb.getString("dialog.skippkcs12pass.question"),
                rb.getString("dialog.skippkcs12pass.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.YES_OPTION) {
            // skip
            return true;
        } else {
            // go on
            return false;
        }
    }

   /**
     * Shows a dialog for choosing to skip x509 configuration or not
     *
     * @param parent The parent dialog
     * @return true, if the configuration shall be skipped
     *         false, if not
     */
    public static boolean showReallySkipX509Dialog(JDialog parent) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {rb.getString("dialog.skipx509configuration.answer_yes"),
                rb.getString("dialog.skipx509configuration.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(parent,
                rb.getString("dialog.skipx509configuration.question"),
                rb.getString("dialog.skipx509configuration.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.YES_OPTION) {
            // skip
            return true;
        } else {
            // go on
            return false;
        }

    }


    /**
     * Shows a dialog for reassigning a x509 certificate
     *
     * @param parent The parent dialog
     * @return true, if the certificate sall be reassigned
     *         false, if not
     */
    public static boolean showX509ReassignDialog(JFrame parent) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Object[] options = {rb.getString("dialog.reassignx509.answer_yes"),
                rb.getString("dialog.reassignx509.answer_no")
        };

        int n = CustomJOptionPane.showOptionDialog(parent,
                rb.getString("dialog.reassignx509.question"),
                rb.getString("dialog.reassignx509.title"),
                CustomJOptionPane.YES_NO_OPTION,
                CustomJOptionPane.QUESTION_MESSAGE,
                null, // icon
                options,
                options[1]); //default button

        if (n == CustomJOptionPane.YES_OPTION) {
            // skip
            return true;
        } else {
            // go on
            return false;
        }

    }


    /**
     * Shows a dialog for deleting a server
     *
     * @param parentFrame The parent Frame
     * @param serverId    The serverId
     */
    public static void showDeleteServerDialog(final JFrame parentFrame, final String serverId) {
        showDeleteServerDialog(parentFrame, serverId, false);
    }


    /**
     * Shows a dialog for deleting a server
     *
     * @param parentFrame The parent Frame
     * @param serverId    The serverId
     * @param dispose     boolean, if true the parentFrame will be disposed
     */
    public static void showDeleteServerDialog(final JFrame parentFrame, final String serverId, final boolean dispose) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        final JDialog dialog = new JDialog(parentFrame, rb.getString("server.dialog.delete.title"), true);

        JPanel inputPanel = new JPanel();
        MigLayout layout = new MigLayout("align center");
        inputPanel.setLayout(layout);

        JLabel deleteLabel = new JLabel(rb.getString("server.dialog.delete.text"));

        final JCheckBox deleteCertificate = new JCheckBox();
        deleteCertificate.setSelected(true);

        inputPanel.add(deleteCertificate);
        inputPanel.add(deleteLabel);


        JButton deleteButton = new JButton(rb.getString("server.details.deletebutton"));
        deleteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    ServerAction.deleteServer(serverId, deleteCertificate.isSelected());
                    dialog.dispose();
                    if (dispose)
                        parentFrame.dispose();
                    else
                        ManagerGUI.clearRightPanel();
                    
                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshServerTable();
                    ManagerGUI.refreshX509Table();
                } catch (Exception ex) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(dialog,
                            rb.getString("server.dialog.delete.errortext"),
                            rb.getString("server.dialog.delete.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton cancelButton = new JButton(rb.getString("dialog.newuser.button_cancel"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocation(GuiUtils.getOffsetLocation(parentFrame));

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        dialog.setVisible(true);
    }


    
    /**
     * A dialog where the user is asked if he really wants
     * to quit
     */
    public static void showExitDialog() {
        if (!Configuration.getInstance().GUI_SHOW_EXIT_DIALOG)
            ManagerGUI.mainFrame.dispose();
        else
            showExitDialog(false);
    }
    
    /**
     * A dialog where the user is asked if he really wants
     * to quit, although there are background tasks running
     */
    public static void showExitDialogWithActiveThreads() {
        if (!Configuration.getInstance().GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS)
            ManagerGUI.mainFrame.dispose();
        else
            showExitDialog(true);
    }
    
    /**
     * A dialog where the user is asked if he really wants
     * to quit
     * @param activeThreads Flag which is true if there are still processes running
     */
    private static void showExitDialog(final boolean activeThreads) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String rbPrefix = "dialog.quit.";
        if (activeThreads)
            rbPrefix = "dialog.activethreads.";
        
        final JDialog dialog = new JDialog(ManagerGUI.mainFrame, rb.getString(rbPrefix + "title"), true);

        JPanel inputPanel = new JPanel(new MigLayout("align left"));

        JLabel textLabel = new JLabel(rb.getString(rbPrefix + "text"));
        inputPanel.add(textLabel, "wrap");

        JLabel askAgainLabel = new JLabel(rb.getString(rbPrefix + "ask_again"));
        inputPanel.add(askAgainLabel);
        final JCheckBox askAgainCheckbox = new JCheckBox();
        askAgainCheckbox.setSelected(false);
        inputPanel.add(askAgainCheckbox);


        JButton exitButton = new JButton(rb.getString(rbPrefix + "answer_yes"));
        exitButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (askAgainCheckbox.isSelected()) {
                    if (activeThreads)
                        Configuration.getInstance().setGuiShowExitDialogActiveThreads(false);
                    else
                        Configuration.getInstance().setGuiShowExitDialog(false);
                }
                
                dialog.dispose();
                ManagerGUI.mainFrame.dispose();
            }
        });
        
        JButton cancelButton = new JButton(rb.getString(rbPrefix + "answer_no"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(exitButton);
        buttonPanel.add(cancelButton);

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(exitButton);

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        
        Point center = GuiUtils.getCenterPositionRelativeTo(dialog, ManagerGUI.mainFrame);
        dialog.setLocation(center);
        
        dialog.setVisible(true);
    }
    
    
    /**
     * Shows a warning box and determines
     * whether the user want to proceed despite changes
     * 
     */
    public static void showWarningUserChangedBox() {
    	final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String rbPrefix = "dialog.quit_alert.";
        
        final JDialog dialog = new JDialog(ManagerGUI.mainFrame, rb.getString(rbPrefix + "title"), true);

        JPanel inputPanel = new JPanel(new MigLayout("align left"));

        JLabel textLabel = new JLabel(rb.getString(rbPrefix + "text"));
        inputPanel.add(textLabel, "wrap");

        JLabel askAgainLabel = new JLabel(rb.getString(rbPrefix + "ask_again"));
        inputPanel.add(askAgainLabel);

        JButton exitButton = new JButton(rb.getString(rbPrefix + "answer_yes"));
        exitButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                ManagerGUI.mainFrame.dispose();
                Configuration.getInstance().NOT_SYNCED_SERVERS.clear();
            }
        });
        
        JButton cancelButton = new JButton(rb.getString(rbPrefix + "answer_no"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(exitButton);
        buttonPanel.add(cancelButton);

        Container contentPane = dialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(exitButton);

        CssRuleManager.getInstance().format(dialog);
        dialog.pack();
        
        Point center = GuiUtils.getCenterPositionRelativeTo(dialog, ManagerGUI.mainFrame);
        dialog.setLocation(center);
        
        dialog.setVisible(true);

    }
    

    /**
     * Shows a popup with application info
     *
     * @param frame The parent frame
     */
    public static void showAboutBox(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JPanel panel = new JPanel();
        MigLayout layout = new MigLayout("wrap 1");
        panel.setLayout(layout);

        JLabel imageLabel = new JLabel(
                ImageUtils.createImageIcon(
                        Configuration.getInstance().BANNER_PATH, "banner"));
        panel.add(imageLabel, "align center");

        JLabel titleLabel = new JLabel(rb.getString("app.title"));
        panel.add(titleLabel, "align center");
        JLabel versionLabel = new JLabel(rb.getString("app.version") + " " + Configuration.getInstance().MANAGER_VERSION);
        panel.add(versionLabel, "align center");
        JLabel revisionLabel = new JLabel(rb.getString("app.revision") + " " + 
                (StringUtils.isEmptyOrWhitespaces(Configuration.getInstance().MANAGER_BUILD) || Configuration.getInstance().MANAGER_BUILD.length()<=8 ? "" : Configuration.getInstance().MANAGER_BUILD.substring(0, 7)));
        panel.add(revisionLabel, "align center");
        JLabel copyrightLabel = new JLabel(rb.getString("app.copyright"));
        panel.add(copyrightLabel, "align center");

        JButton closeButton = new JButton(rb.getString("detailsFrame.closebutton"));
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                popup.hide();
            }
        });
        panel.add(closeButton, "align center");

        // style
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        CssRuleManager.getInstance().format(panel);

        Point location = GuiUtils.getCenterPositionRelativeTo(panel, frame);
        PopupFactory factory = PopupFactory.getSharedInstance();
        popup = factory.getPopup(frame, panel, location.x, location.y);
        popup.show();

    }

    /**
     * Shows a popup with NFR / beta info
     *
     * @param frame The parent frame
     */
    public static void showNfrBox(JFrame frame) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JPanel panel = new JPanel();
        MigLayout layout = new MigLayout("wrap 1");
        panel.setLayout(layout);

        JLabel imageLabel = new JLabel(
                ImageUtils.createImageIcon(
                        Configuration.getInstance().BANNER_PATH, "banner"));
        panel.add(imageLabel, "align center");

        JLabel titleLabel = new JLabel(rb.getString("app.title"));
        panel.add(titleLabel, "align center");
        JLabel versionLabel = new JLabel(rb.getString("app.version") + " " + Configuration.getInstance().MANAGER_VERSION);
        panel.add(versionLabel, "align center");
        JLabel copyrightLabel = new JLabel(rb.getString("app.copyright"));
        panel.add(copyrightLabel, "align center");

        JLabel nfrLabel = new JLabel(rb.getString("app.nfr"));
        panel.add(nfrLabel, "align center");

        JButton closeButton = new JButton(rb.getString("detailsFrame.closebutton"));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.hide();
            }
        });
        panel.add(closeButton, "align center");

        // style
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        CssRuleManager.getInstance().format(panel);

        Point location = GuiUtils.getCenterPositionRelativeTo(panel, frame);
        PopupFactory factory = PopupFactory.getSharedInstance();
        popup = factory.getPopup(frame, panel, location.x, location.y);
        popup.show();

    }

    /**
     * Shows a JFilechooser for choosing the import directory
     *
     * @param frame The parent frame
     * @return String with the selected directory
     */
    private static String fileChooser(Frame frame, int fileType) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        // create a file chooser
        JFileChooser fc = LocalizedFileChooser.getLocalizedFileChooser();

        fc.setFileSelectionMode(fileType);
        fc.setFileHidingEnabled(false);
        fc.setApproveButtonText(rb.getString("dialog.configuration.filechooser.approve"));
        fc.setDialogTitle(rb.getString("dialog.configuration.filechooser.title"));

        // responds to a button click:
        int returnVal = fc.showOpenDialog(frame);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return file.getPath();
        } else {
            return null;
        }

    }


    /**
     * english language selected
     */
    private static void englishSelected() {
        ResourceBundleMgmt.getInstance().setSelectedLanguage(Constants.LANGUAGE_CODE_ENGLISH);
        Configuration.getInstance().setLanguage(Constants.LANGUAGE_CODE_ENGLISH);
    }


    /**
     * german language selected
     */
    private static void germanSelected() {
        ResourceBundleMgmt.getInstance().setSelectedLanguage(Constants.LANGUAGE_CODE_GERMAN);
        Configuration.getInstance().setLanguage(Constants.LANGUAGE_CODE_GERMAN);
    }


    /**
     * updates every label on the configuration dialog
     *
     * @param dialog The dialog
     */
    private static void updateConfigurationLabel(
            JDialog dialog,
            JLabel clientCertExportPathLabel,
            JButton cancelButton,
            JButton saveButton,
            JLabel exportLabel,
            JLabel languageHeadlineLabel,
            JRadioButton languageDE,
            JRadioButton languageEN,
            JLabel moduleHeadlineLabel,
            JCheckBox moduleCA,
            JCheckBox moduleCC,
            JLabel x509typeChooseLabel,
            JRadioButton x509typeChooseBase64,
            JRadioButton x509typeChoosePKCS12,
            JLabel pkcs12PropertiesLabel,
            JLabel pkcs12NoPasswdLabel,
            JLabel pkcs12SinglePasswdLabel,
            JLabel pkcs12GlobalPasswdLabel,
            JLabel dbChooseLabel,
            JLabel dbPathLabel,
            JCheckBox defaultDB,
            JLabel generalLabel,
            JCheckBox pam,
            JCheckBox createOpenVPNConfigFiles
    ) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        dialog.setTitle(rb.getString("dialog.configuration.title"));
        clientCertExportPathLabel.setText(rb.getString("dialog.configuration.exportPath"));
        cancelButton.setText(rb.getString("dialog.configuration.cancelButton"));
        saveButton.setText(rb.getString("dialog.configuration.saveButton"));
        exportLabel.setText(rb.getString("dialog.configuration.export"));
        languageHeadlineLabel.setText(rb.getString("dialog.configuration.languageHeadline"));
        languageEN.setText(rb.getString("dialog.configuration.en"));
        languageDE.setText(rb.getString("dialog.configuration.de"));
        moduleHeadlineLabel.setText(rb.getString("dialog.configuration.moduleHeadline"));
        moduleCA.setText(rb.getString("dialog.configuration.moduleCA"));
        moduleCC.setText(rb.getString("dialog.configuration.moduleCC"));
        x509typeChooseLabel.setText(rb.getString("dialog.configuration.x509typechoose"));
        x509typeChooseBase64.setText(rb.getString("dialog.configuration.x509typeChooseBase64"));
        x509typeChoosePKCS12.setText(rb.getString("dialog.configuration.x509typeChoosePKCS12"));
        pkcs12PropertiesLabel.setText(rb.getString("dialog.configuration.pkcs12Properties"));
        pkcs12NoPasswdLabel.setText(rb.getString("dialog.configuration.pkcs12NoPasswd"));
        pkcs12SinglePasswdLabel.setText(rb.getString("dialog.configuration.pkcs12SinglePasswd"));
        pkcs12GlobalPasswdLabel.setText(rb.getString("dialog.configuration.pkcs12GlobalPasswd"));
        dbChooseLabel.setText(rb.getString("dialog.configuration.dbchoose"));
        dbPathLabel.setText(rb.getString("dialog.configuration.dbpath"));
        defaultDB.setText(rb.getString("dialog.configuration.defaultdb"));
        generalLabel.setText(rb.getString("dialog.configuration.general"));
        pam.setText(rb.getString("dialog.configuration.pam"));
        createOpenVPNConfigFiles.setText(rb.getString("dialog.configuration.create_openvpn_config_files"));
    }
    
    
    public static void main (String[] args) {
        JFrame frame = new JFrame();
        //JDialog dialog = new JDialog();
        //showRootCertDialog(frame);
        //showImportDialog(frame);
        //showCreateCertificatesDialog(frame);
        //showUnsavedDataDialog(frame);
        //showCreateUserFromCNDialog(frame);
        //showKnownHostNewDialog(frame, "", "");
        //showKnownHostChangedDialog(frame, "", "");
        //showKnownHostMistrustDialog(frame, "", "");
        //showSSHReconnectDialog(frame);
        //showLanguageDialog(frame);
        //showNewUserDialog(frame, "1");
        //showPKCS12PasswordDialog(frame);
        //showUserCommandDialog(frame, "");
        showConfigurationDialog(frame);
        //showImportConfigurationDialog(frame);
        //showX509ConfigurationDialog(frame);
        //showX509ReassignDialog(frame);
        //showUpdateConfigurationDialog(frame);
        //showReallySkipDialog(dialog);
        //showReallySkipX509Dialog(dialog);
        //showDeleteServerDialog(frame, "1");
        //showExitDialog();
        //showExitDialogWithActiveThreads();
    }



}
