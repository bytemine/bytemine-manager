/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.action.ValidatorAction;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.Password;
import net.miginfocom.swing.MigLayout;

/**
 * a frame showing user details
 *
 * @author Daniel Rauer
 */
public class UserDetails {

    private static JScrollPane scroller = new JScrollPane();

    private static final Vector<String> connectedServers = new Vector<String>();

    private static JFrame parentFrame;
    private static JFrame userDetailsFrame = null;
    private static String userid;
    
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    private String title = rb.getString("app.title") + " - " + rb.getString("user.details.title");
    
    // only used when re-enable 'deleted' user
    private String username = null;
    private String x509id = null;
    

    public UserDetails(JFrame parent, String id) {
        parentFrame = parent;
        userid = id;
    }

    public UserDetails(JFrame parent, String id, String username, String x509id) {
        parentFrame = parent;
        userid = id;
        this.username = username;
        this.x509id = x509id;
    }
    

    public void showUserDetailsFrame() {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t);

                createUserDetailsFrame();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();

    }


    /**
     * creates a new frame showing all data of the selected user
     */
    private void createUserDetailsFrame() {
        // retrieve connected servers from database
        connectedServers.clear();
        connectedServers.addAll(ServerQueries.getServersForUser(userid));


        userDetailsFrame = new JFrame(title);
        userDetailsFrame.setLayout(new MigLayout("fillx", "", "align top"));
        userDetailsFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        userDetailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        final JPanel mainPanel = createUserDetailsPanel(userDetailsFrame);

        scroller = createServerManagement();
        userDetailsFrame.add(mainPanel, "growx");
        userDetailsFrame.add(
                scroller, "growx, growy");

        Point location = GuiUtils.getOffsetLocation(parentFrame);
        userDetailsFrame.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(userDetailsFrame);

        userDetailsFrame.pack();
        userDetailsFrame.setVisible(true);

    }


    /**
     * creates a new frame showing all data of the selected user
     * @return Jpanel with users details
     */
    public JPanel createUserDetailsPanel() {
        return createUserDetailsPanel(null);
    }
    
    /**
     * creates a new frame showing all data of the selected user
     * @param parentFrame if this dialog is opened as part of a JFrame, the parent
     * JFrame can be passed, in order to display a cancel button to leave the dialog.
     * If there is no parent frame just pass null instead.
     * @return Jpanel with users details
     */
    public JPanel createUserDetailsPanel(final JFrame parentFrame) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (userid == null)
            userid = "-1";
        int uId = Integer.parseInt(userid);
        final boolean newUser = (uId <= 0) ? true : false;
        
        // retrieve data from database
        final String[] details = UserQueries.getUserDetails(userid);

        final JPanel mainPanel = new JPanel(new MigLayout("insets 5, fillx"));
        final JButton saveButton = new JButton(rb.getString("user.details.savebutton") + "  ");

        final JTextField newpasswordField = new JTextField("", 10);
        final JTextField idField = new JTextField(details[0], 5);
        idField.setEnabled(false);
        if (!newUser) {
            JLabel idLabel = new JLabel(rb.getString("user.details.id"));
            idLabel.setFont(Constants.FONT_PLAIN);
            
            mainPanel.add(idLabel, "align left");
            mainPanel.add(idField, "wrap");
        } else {
            if (Configuration.getInstance().suggestPasswords()) {
                newpasswordField.setText(Password.generatePassword(10));
                newpasswordField.setToolTipText(rb.getString("dialog.newuser.password.tooltip"));
            }
            
            newpasswordField.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {
                }
                
                public void keyReleased(KeyEvent e) {
                    if (!"".equals(newpasswordField.getText()))
                        newpasswordField.setBackground(Color.WHITE);
                    ManagerGUI.serverUserTreeModel.setUnsavedData(true);
                    saveButton.setText(rb.getString("user.details.savebutton") + " *");
                }
                
                public void keyPressed(KeyEvent e) {
                }
            });
        }
        newpasswordField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                ManagerGUI.serverUserTreeModel.setUnsavedData(true);
                saveButton.setText(rb.getString("user.details.savebutton") + " *");
                if (userDetailsFrame != null)
                    userDetailsFrame.setTitle(title + " *");
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });

        JLabel usernameLabel = new JLabel(rb.getString("user.details.username"));
        usernameLabel.setFont(Constants.FONT_BOLD);
        mainPanel.add(usernameLabel, "align left");
        
        // used only for the re-enable feature
        if(username!=null) {
        	details[1]=username;
        	details[5]=username;
        }
        
        final JTextField usernameField = new JTextField(details[1], 10);
        	
        mainPanel.add(usernameField, "span, growx, wrap");

        JLabel passwordLabel = null;
        if (!newUser) {
            passwordLabel = new JLabel(rb.getString("user.details.newpassword"));
            passwordLabel.setFont(Constants.FONT_BOLD);
        } else {
            passwordLabel = new JLabel(rb.getString("user.details.password"));
            passwordLabel.setFont(Constants.FONT_PLAIN);
        }
        
        mainPanel.add(passwordLabel, "align left");
        mainPanel.add(newpasswordField, "span, growx, wrap");


        // enhanced options
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
                    mainPanel.revalidate();
                    expanded = !expanded;
                }
            }
        });
        JPanel expandTopPanel = new JPanel(new MigLayout("insets 0"));
        JLabel expandLabel = new JLabel(rb.getString("user.details.expandOptions"));
        expandLabel.setFont(Constants.FONT_PLAIN);
        expandTopPanel.add(expandButton, "width 20!");
        expandTopPanel.add(expandLabel);
        mainPanel.add(expandTopPanel, "gaptop 12, wrap");

        final JTextField cnField = new JTextField(details[5], 10);
        final JTextField ouField = new JTextField(details[6], 10);
        final JTextField yubiField = new JTextField(details[7], 10);

        usernameField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                if (!"".equals(usernameField.getText()))
                    usernameField.setBackground(Color.WHITE);
                if (newUser && x509id==null)
                    cnField.setText(usernameField.getText());
                ManagerGUI.serverUserTreeModel.setUnsavedData(true);
                saveButton.setText(rb.getString("user.details.savebutton") + " *");
                if (userDetailsFrame != null)
                    userDetailsFrame.setTitle(title + " *");
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });
        
        if(!newUser || x509id!=null) {
            cnField.setEnabled(true);
            ouField.setEnabled(true);
        }
        
        JLabel cnLabel = new JLabel(rb.getString("user.details.cn"));
        cnLabel.setFont(Constants.FONT_PLAIN);
        JLabel ouLabel = new JLabel(rb.getString("user.details.ou"));
        ouLabel.setFont(Constants.FONT_PLAIN);
        JLabel yubiLabel = new JLabel(rb.getString("user.details.yubikeyid"));
        yubiLabel.setFont(Constants.FONT_PLAIN);
        extensionPanel.add(cnLabel, "align right");
        extensionPanel.add(cnField, "span, growx, wrap");
        extensionPanel.add(ouLabel, "align right");
        extensionPanel.add(ouField, "span, growx, wrap");
        extensionPanel.add(yubiLabel, "align right");
        extensionPanel.add(yubiField, "span, growx, wrap");
        
        final JLabel clientValidForLabel = new JLabel(rb.getString("user.details.clientValidFor"));
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
        if (newUser && x509id==null) {
            extensionPanel.add(clientValidForLabel, "align right");
            extensionPanel.add(clientValidForField, "align right");
            extensionPanel.add(clientValidityUnitBox, "align left, wrap");
        }
        
        mainPanel.add(extensionPanel, "gapleft 20, growx, span 3, wrap");
        
        final JPanel certPanel = new JPanel(new MigLayout("insets 0"));
        JButton reassignButton = new JButton(rb.getString("user.details.reassignbutton"));
        reassignButton.setToolTipText(rb.getString("user.details.assignbutton_tt"));
        reassignButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
                User user = new User(userid);
                user = UserDAO.getInstance().read(user);

                X509Manager x509Manager = null;
                if (parentFrame != null) {
                    x509Manager = new X509Manager(parentFrame, user);
                    x509Manager.showX509ManagerFrame();
                } else {
                    x509Manager = new X509Manager(mainPanel, user);
                    JPanel x509Panel = x509Manager.createX509ManagerPanel();
                    ManagerGUI.updateServerUserDetails(x509Panel);
                }
            }
        });
        
        JButton exportButton = new JButton(rb.getString("detailsFrame.exportbutton"));
        exportButton.setToolTipText(rb.getString("user.details.exportbutton_tt"));
        exportButton.addActionListener(new ActionListener() {

        	public void actionPerformed(ActionEvent evt)
            {
                try {
                    String path = X509Action.exportToFilesystem(details[3]);
                    CustomJOptionPane.showMessageDialog(mainPanel,
                            rb.getString("detailsFrame.exportmessage") 
                                +"\n" + path,
                            rb.getString("detailsFrame.exporttitle"),
                            CustomJOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    new VisualException(rb.getString("detailsFrame.exporterror"));
                }
            }
        });

        if (!newUser || x509id!=null) {
            JLabel x509Label = new JLabel(rb.getString("user.details.x509"));
            x509Label.setFont(Constants.FONT_PLAIN);
            certPanel.add(x509Label, "align left");
            if(x509id!=null) {
            	details[3] = x509id;
            	details[4] = X509Queries.getX509Details(x509id)[2];
            }
            if (details[3] != null && !"0".equals(details[3]) && !"-1".equals(details[3])) {
                JButton showButton = new JButton(rb.getString("user.details.showbutton"));
                showButton.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        X509Details x509DetailsFrame = new X509Details(mainPanel, details[3]);
                        x509DetailsFrame.showDetails();
                    }
                });


                certPanel.add(showButton, "align left");
                certPanel.add(exportButton, "align left");
                certPanel.add(reassignButton, "align left, wrap");

                JLabel filenameLabel = new JLabel(rb.getString("server.details.x509filename"));
                filenameLabel.setFont(Constants.FONT_PLAIN);
                certPanel.add(filenameLabel, "align left");
                certPanel.add(new JDataLabel(details[4]), "align left, span, wrap");

            } else {
                certPanel.add(new JDataLabel(rb.getString("user.details.nocert")));
                reassignButton.setText(rb.getString("user.details.assignbutton"));
                certPanel.add(reassignButton, "wrap");
            }
        }
        mainPanel.add(certPanel, "span 3, wrap");

        JButton deleteButton = new JButton(rb.getString("user.details.deletebutton"));
        deleteButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("user.dialog.delete.answer_yes"),
                        rb.getString("user.dialog.delete.answer_no")
                };
                int answer = CustomJOptionPane.showOptionDialog(
                        ManagerGUI.mainFrame,
                        rb.getString("user.dialog.delete.text"),
                        rb.getString("user.dialog.delete.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                // delete
                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        UserAction.deleteUser(idField.getText());
                    } catch (Exception e) {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("user.dialog.delete.errortext"),
                                rb.getString("user.dialog.delete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                    ManagerGUI.serverUserTreeModel.setUnsavedData(false);
                    saveButton.setText(rb.getString("user.details.savebutton"));

                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();

                    if (parentFrame == null)
                        ManagerGUI.clearRightPanel();
                    else
                        parentFrame.dispose();
                }

            }
        });
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

                try {

                    if (newUser && x509id==null) {
                        // save
                        if (ValidatorAction.validateUserCreation(usernameField.getText(), newpasswordField.getText(), yubiField.getText())) {
                            String password = null;
                            if (Configuration.getInstance().PKCS12_PASSWORD_TYPE
                                            == Constants.PKCS12_SINGLE_PASSWORD) {
                                // show password input dialog
                                while (password == null) {
                                    String dialogHeadline = rb.getString("dialog.pkcs12password.headline1");
                                    dialogHeadline += " " + usernameField.getText();
                                    dialogHeadline += " " + rb.getString("dialog.pkcs12password.headline2");
                                    
                                    password = Dialogs.showPKCS12PasswordDialog(ManagerGUI.mainFrame, dialogHeadline);
                                    if (password == null)
                                       if (Dialogs.showReallySkipPkcs12Password(ManagerGUI.mainFrame))
                                            break;
                                }
                            }
                            
                            String clientValidFor= clientValidForField.getText();
                            String clientValidityUnit= (String) clientValidityUnitBox.getSelectedItem();
                            clientValidFor = Dialogs.getString(rb, clientValidFor, clientValidityUnit);

                            int userid = UserAction.createUserAndCertificate(
                                    usernameField.getText(),
                                    newpasswordField.getText(),
                                    cnField.getText(),
                                    ouField.getText(),
                                    password,
                                    clientValidFor,
                                    yubiField.getText()
                            );
                            // connect server and users
                            UserQueries.reconnectServersAndUser(userid, connectedServers);
                            
                            // create vpn-config file
                            if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES)
                                UserAction.createVPNConfigFile(User.getUserByID(userid));
                            
                            // update synced servers
                            UserAction.updateSyncedServers(Integer.toString(userid));
                        }
                    } else if(newUser && x509id!=null) {
                    	// save
                        if (ValidatorAction.validateUserCreation(usernameField.getText(), newpasswordField.getText(), yubiField.getText())) {
                        	
                            
                            User user = new User(username, newpasswordField.getText(), 
                            								Integer.parseInt(x509id), true,
                            					cnField.getText(),
                            					ouField.getText(),
                            					yubiField.getText());
                            int userid = user.getUserid();
                            
                            // connect server and users
                            UserQueries.reconnectServersAndUser(userid, connectedServers);
                            
                            // create vpn-config file
                            if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES)
                                UserAction.createVPNConfigFile(User.getUserByID(userid));
                            
                            // update synced servers
                            UserAction.updateSyncedServers(Integer.toString(userid));
                        }
                    } else {
                        //update
                        if (ValidatorAction.validateUserUpdate(usernameField.getText(), details[1], newpasswordField.getText(), yubiField.getText())) {
                            UserAction.updateUser(
                                    idField.getText(),
                                    usernameField.getText(),
                                    newpasswordField.getText(),
                                    cnField.getText(),
                                    ouField.getText(),
                                    yubiField.getText()
                            );
                            // connect server and users
                            UserQueries.reconnectServersAndUser(idField.getText(), connectedServers);
                            
                            // create vpn-config file
                            if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES)
                                UserAction.createVPNConfigFile(User.getUserByID(Integer.parseInt(idField.getText())));
                            
                            // update synced servers
                            UserAction.updateSyncedServers(idField.getText());
                        }
                    }
                    
                    ManagerGUI.serverUserTreeModel.setUnsavedData(false);
                    saveButton.setText(rb.getString("user.details.savebutton"));

                    if (newUser) {
                        if (parentFrame == null) {
                            UserDetails uDetails = new UserDetails(null, null);
                            JPanel detailsPanel = uDetails.createUserDetailsPanel();
                            ManagerGUI.updateServerUserDetails(detailsPanel);
                        } else {
                            userDetailsFrame.dispose();
                        }

                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                         rb.getString("dialog.newuser.success.save"));
                    } else {
                        if (parentFrame!= null)
                            parentFrame.dispose();
                        if (userDetailsFrame != null)
                            userDetailsFrame.setTitle(title);

                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                        rb.getString("user.dialog.success.save"));
                    }

                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();
                } catch (ValidationException ve) {
                    // show validation error dialog
                    CustomJOptionPane.showMessageDialog(mainPanel,
                            ve.getMessage(),
                            ve.getTitle(),
                            CustomJOptionPane.ERROR_MESSAGE);
                    
                    // set focus on error field
                    if (ve.getCode() > 0) {
                        int code = ve.getCode();
                        switch (code) {
                        case 1: usernameField.setBackground(Constants.COLOR_ERROR);
                        		usernameField.requestFocus(); break;
                        case 2: newpasswordField.setBackground(Constants.COLOR_ERROR);
                        		newpasswordField.requestFocus(); break;
                        case 3: yubiField.setBackground(Constants.COLOR_ERROR);
                                yubiField.requestFocus(); break;
                        default: break;
                        }
                    }
                } catch (Exception e) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(mainPanel,
                            rb.getString("user.dialog.update.errortext"),
                            rb.getString("user.dialog.update.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel buttonPanel2 = new JPanel(new MigLayout("align right"));
        buttonPanel2.add(saveButton);

        if (!newUser)
            buttonPanel2.add(deleteButton);

        JButton cancelButton = new JButton(rb.getString("dialog.updateconfiguration.cancelbutton"));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (ManagerGUI.serverUserTreeModel != null)
                    ManagerGUI.serverUserTreeModel.setUnsavedData(false);

                if (parentFrame != null)
                    userDetailsFrame.dispose();
                else
                    ManagerGUI.clearRightPanel();
            }
        });

        buttonPanel2.add(cancelButton);

        
        // used only for the re-enable feature
        if(username!=null) {
        	usernameField.setEditable(false);
        	cnField.setEditable(false);
        	ouField.setEditable(false);
        	exportButton.setEnabled(false);
        	reassignButton.setEnabled(false);
        }

        
        mainPanel.add(buttonPanel2, "gaptop 30, growx, span 3");
        CssRuleManager.getInstance().format(mainPanel);
        
        return mainPanel;
    }



    /**
     * Create the scrollpane with checkboxes and servernames
     *
     * @return The scrollpane
     */
    private static JScrollPane createServerManagement() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JPanel serverMgrPanel = new JPanel(new MigLayout("fillx"));

        JLabel title = new JLabel(rb.getString("user.details.srvmgmt.title"));
        serverMgrPanel.add(title, "wrap");

        Vector<String[]> allUsers = ServerQueries.getServerOverview(ServerQueries.order_name);

        for (Iterator<String[]> it = allUsers.iterator(); it.hasNext();) {
            String[] strings = it.next();
            final String serverid = strings[0];
            JCheckBox box = new JCheckBox(strings[1] + ", " + strings[2]);
            if (connectedServers != null && connectedServers.contains(strings[0]))
                box.setSelected(true);
            else
                box.setSelected(false);

            box.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    AbstractButton checkbox = (AbstractButton) e.getSource();
                    boolean selected = checkbox.getModel().isSelected();
                    if (selected && !connectedServers.contains(serverid))
                        connectedServers.add(serverid);
                    else if (!selected && connectedServers.contains(serverid))
                        connectedServers.remove(serverid);
                }
            });

            serverMgrPanel.add(box, "wrap");
        }

        return new JScrollPane(serverMgrPanel);
    }


    public JFrame getParentFrame() {
        return parentFrame;
    }

    public JFrame getUserDetailsFrame() {
        return userDetailsFrame;
    }
    
    public static void main(String[] args) {
        UserDetails d = new UserDetails(null, "1");
        d.createUserDetailsFrame();
    }

}
