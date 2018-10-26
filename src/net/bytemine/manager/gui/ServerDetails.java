/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.Component;
import java.awt.event.*;
import java.util.*;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.ValidatorAction;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ServerDAO;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.openvpn.UserSync;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * a frame showing x509 details
 *
 * @author Daniel Rauer
 */
public class ServerDetails {

    private static Logger logger = Logger.getLogger(ServerDetails.class.getName());

    private static JScrollPane scroller = new JScrollPane();
    private static JFrame parentFrame;
    private static JFrame serverDetailsFrame = null;
    private static String serverid;

    private static final Vector<String> connectedUsers = new Vector<String>();

    private static final int mgrWidth = 200;
    private static final int frameHeight = 520;
    private JTextField idField;
    
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    private String title = rb.getString("app.title") + " - " + rb.getString("server.details.title");
    
    private JTabbedPane tabbedPane;
    private boolean staticIpTabActive = false;
    private Hashtable<String,JTextField> userServerIp;
    
    // special components, FocusLost Events have to be ignored
    final JRadioButton authTypePassword = new JRadioButton();
    final JRadioButton authTypeKeyfile = new JRadioButton();
    final JCheckBox staticIpField = new JCheckBox();
    
    final JComboBox<? extends String> serverTypeBox = new JComboBox<>(
            new String[]{
                    rb.getString("server.details.type.openvpn"),
                    rb.getString("server.details.type.appliance")
            });
    final JComboBox<? extends String> serverVpnProtocolBox = new JComboBox<>(
            new String[]{
                    rb.getString("server.vpn.protocol.tcp"),
                    rb.getString("server.vpn.protocol.udp")
            });
    final JComboBox<? extends String> serverVpnDeviceBox = new JComboBox<>(
            new String[]{
                    rb.getString("server.vpn.device.tun"),
                    rb.getString("server.vpn.device.tun0"),
                    rb.getString("server.vpn.device.tun1"),
            });
    private final JLabel vpnNetworkAddress = new JLabel();
    private final int vpnSubnetMask = 24;
    private JLabel netmaskLabel = new JLabel();
    private final static String formatClientIp = "%1$-1s.%2$-1s.%3$-1s.";
    private final static String formatNetmask = "%1$-1s.%2$-1s.%3$-1s.%4$-1s";
    private final List<JLabel> networkIPList = new ArrayList<>();
    private final JTextField ip1 = new JTextField(3);
    private final JTextField ip2 = new JTextField(3);
    private final JTextField ip3 = new JTextField(3);
    

    public ServerDetails(JFrame parent, String id) {
        parentFrame = parent;
        serverid = id;
        serverDetailsFrame = new JFrame(title);
    }


    void showServerDetailsFrame() {

        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t);

                createServerDetailsFrame();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };

        generateWorker.execute();
    }

    
    /**
     * creates a new frame showing all data of the selected server
     */
    private void createServerDetailsFrame() {
    	
        // retrieve connected users from database
        connectedUsers.clear();
        connectedUsers.addAll(UserQueries.getUsersForServer(serverid));

        serverDetailsFrame = new JFrame(title);
        serverDetailsFrame.setLayout(new MigLayout("fillx", "", "align top"));
        serverDetailsFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        serverDetailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        userServerIp = new Hashtable<>();
        initializeUserServerIpHashtable();
        
        tabbedPane = new JTabbedPane();
        tabbedPane = createServerDetailsPanel(serverDetailsFrame);
        tabbedPane.addChangeListener (arg0 -> {
            if(staticIpTabActive && tabbedPane.getSelectedIndex()==2) {
                networkIPList.forEach(c -> c.setText(String.format(formatClientIp, ip1.getText(), ip2.getText(), ip3.getText())));
                netmaskLabel.setText(String.format(formatNetmask, ip1.getText(), ip2.getText(), ip3.getText(), 0));
            }
        });
        
  	  	JPanel mainPanel = new JPanel(new MigLayout("insets 0, fill, align left"));
  	    mainPanel.add(tabbedPane, "grow");

        scroller = createUserManagement();

        serverDetailsFrame.add(mainPanel, "growx, growy");
        serverDetailsFrame.add(scroller, "growx, growy");
        
        Point location = GuiUtils.getOffsetLocation(parentFrame);
        serverDetailsFrame.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(serverDetailsFrame);

        serverDetailsFrame.pack();
        serverDetailsFrame.setVisible(true);
    }
        
    /**
     * creates a new frame showing all data of the selected server
     * @return JTabbedPane with server details
     */
    public JTabbedPane createServerDetailsPanel() {
    	userServerIp = new Hashtable<>();
        initializeUserServerIpHashtable();
        final JTabbedPane tabbedPane = createServerDetailsPanel(null);
        tabbedPane.addChangeListener (arg0 -> {
            if(staticIpTabActive && tabbedPane.getSelectedIndex()==2) {
                networkIPList.forEach(c -> c.setText(String.format(formatClientIp, ip1.getText(), ip2.getText(), ip3.getText())));
            }
        });
        return tabbedPane;
    }
    
    /**
     * a vpnKeepAliveListener, only numbers are permitted
     */
    private class keyStandardListenerNumber implements KeyListener {
        JTextField field;
        JButton saveButton ;
        
        keyStandardListenerNumber(JTextField field, JButton button) {
            this.field = field;
            this.saveButton = button;
        }
        
        public void keyTyped(KeyEvent e) {
        }
        
        public void keyReleased(KeyEvent e) {
            ManagerGUI.serverUserTreeModel.setUnsavedData(true);
            saveButton.setText(rb.getString("server.details.savebutton") + " *");
            if (serverDetailsFrame != null)
                serverDetailsFrame.setTitle(title + " *");
            
            try {
                if (Integer.parseInt(field.getText())>-1)
                    field.setBackground(Constants.COLOR_WHITE);
            } catch(Exception exception) {
                if (field.getText().length()>0)
                    field.setBackground(Constants.COLOR_ERROR);
            }
        }
        
        public void keyPressed(KeyEvent e) {
        }
    }
    
    
    /**
     * a standard keyListener, focus on the field and if no error
     * occurs highligthing white
     */
    private class keyStandardListener implements KeyListener {
    	JTextField field;
    	JButton saveButton ;
    	
    	keyStandardListener(JTextField field, JButton button) {
    		this.field = field;
    		this.saveButton = button;
    	}
    	
    	public void keyTyped(KeyEvent e) {
        }
        
        public void keyReleased(KeyEvent e) {
            if (!this.field.getText().isEmpty())
                field.setBackground(Color.WHITE);
            ManagerGUI.serverUserTreeModel.setUnsavedData(true);
            saveButton.setText(rb.getString("server.details.savebutton") + " *");
            if (serverDetailsFrame != null)
                serverDetailsFrame.setTitle(title + " *");
        }
        
        public void keyPressed(KeyEvent e) {
        }
    }
    
    
    /**
     * creates a new frame showing all data of the selected server
     * @param myWindow the JFrame we're openend in. null if we're in the main window.
     * @return JTabbedPane with server details
     */
    private JTabbedPane createServerDetailsPanel(final JFrame myWindow) {
    	
        if (serverid == null)
            serverid = "-1";
        int sId = Integer.parseInt(serverid);
        final boolean newServer = sId <= 0;
        
        // retrieve data from database
        final String[] details = ServerQueries.getServerDetails(serverid);
        
        final JPanel mainPanel = new JPanel(new MigLayout("insets 5, fillx, gap 5"));
        final JButton saveButton = new JButton(rb.getString("server.details.savebutton") + "  ");

        final JPanel secondaryPanel = new JPanel(new MigLayout("insets 5, fillx, gap 5"));
        
        JTabbedPane tmp = new JTabbedPane();
        tmp.addTab(rb.getString("server.details.tab1"), mainPanel);
        tmp.addTab(rb.getString("server.details.tab2"), secondaryPanel);
        
        final JTabbedPane panels = tmp;
        
        // field: server-id 
        idField = new JTextField(details[0], 5);
        idField.setEnabled(false);
        if (!newServer) {
            JLabel idLabel = new JLabel(rb.getString("server.details.id"));
            idLabel.setFont(Constants.FONT_PLAIN);
            mainPanel.add(idLabel, "align left");
            mainPanel.add(idField, "wrap");
        }

        
        // field: server-name
        JLabel nameLabel = new JLabel(rb.getString("server.details.name"));
        nameLabel.setFont(Constants.FONT_BOLD);
        mainPanel.add(nameLabel, "align left");
        final JTextField nameField = new JTextField(details[1], 10);
       
        if (newServer) {
        	serverDetailsFrame.addWindowFocusListener(new WindowAdapter() {
        		private boolean justOpened = true;
        		
        		public void windowGainedFocus(WindowEvent e) {
        			if (justOpened)
        				nameField.requestFocusInWindow();
     	     	}
        		
        		public void windowLostFocus(WindowEvent e) {
        			justOpened=false;
        		}
         	});
        }
        
        nameField.addKeyListener(new keyStandardListener(nameField, saveButton));
        mainPanel.add(nameField, "span, growx, wrap");
        
        // field: server-hostname
        JLabel hostnameLabel = new JLabel(rb.getString("server.details.hostname"));
        hostnameLabel.setFont(Constants.FONT_BOLD);
        mainPanel.add(hostnameLabel, "align left");
        final JTextField hostnameField = new JTextField(details[2], 10);

        hostnameField.addKeyListener(new keyStandardListener(hostnameField, saveButton));
        mainPanel.add(hostnameField, "span, growx, wrap");
        
        
        // field: server-cn
        JLabel cnLabel = new JLabel(rb.getString("server.details.cn"));
        cnLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(cnLabel, "align left");
        
        final JTextField cnField = new JTextField(details[27], 10);

        cnField.addKeyListener(new keyStandardListener(cnField, saveButton));
        mainPanel.add(cnField, "span, growx, wrap");
        
        
        // field: server-ou
        JLabel ouLabel = new JLabel(rb.getString("server.details.ou"));
        ouLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(ouLabel, "align left");
        
        final JTextField ouField = new JTextField(details[28], 10);

        ouField.addKeyListener(new keyStandardListener(ouField, saveButton));
        mainPanel.add(ouField, "span, growx, wrap");
        

        nameField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                if (!nameField.getText().isEmpty())
                    nameField.setBackground(Color.WHITE);
                if (newServer)
                    cnField.setText(nameField.getText());
                ManagerGUI.serverUserTreeModel.setUnsavedData(true);
                saveButton.setText(rb.getString("server.details.savebutton") + " *");
                if (serverDetailsFrame != null)
                    serverDetailsFrame.setTitle(title + " *");
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });
        
        if (!newServer) {
            cnField.setEnabled(false);
            ouField.setEnabled(false);
        }
        

        // wrapper
        String wrapperCommand = details[13];
        if (wrapperCommand == null || wrapperCommand.isEmpty())
            wrapperCommand = Configuration.getInstance().SERVER_WRAPPER_COMMAND;
        final JTextField wrapperField = new JTextField(wrapperCommand, 10);

        
        // field: user-name
        final JTextField usernameField = new JTextField(details[4], 10);
        usernameField.addKeyListener(new keyStandardListener(usernameField, saveButton));
        
        
        // field: user-file	(passwd)	Panel 2
        final JTextField userfileField = new JTextField(details[6], 10);
        userfileField.addKeyListener(new keyStandardListener(userfileField, saveButton));


        // field: exportpath	Panel 2
        final JTextField exportPathField = new JTextField(details[7], 10);
        exportPathField.addKeyListener(new keyStandardListener(exportPathField, saveButton));

        
        // field: Client Configuration Directory		Panel 2
        final JTextField ccdField = new JTextField(details[18], 10);  
        ccdField.addKeyListener(new keyStandardListener(ccdField, saveButton));
        
        // field: server-type
        //   choose between openvpn/appliance - servertype       
        JLabel typeLabel = new JLabel(rb.getString("server.details.type"));
        typeLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(typeLabel, "align left");
        
        int serverType = Server.SERVER_TYPE_REGULAR_OPENVPN;
        try {
            serverType = Integer.parseInt(details[12]);
        } catch (NumberFormatException e) {
            logger.warning("server type is not an int!");
        }
        
        if (serverType == Server.SERVER_TYPE_REGULAR_OPENVPN) {
            serverTypeBox.setSelectedIndex(0);
        } else if (serverType == Server.SERVER_TYPE_BYTEMINE_APPLIANCE) {
            serverTypeBox.setSelectedIndex(1);
            wrapperField.setEnabled(false);
        }
        
        serverTypeBox.addItemListener(e -> {
            ManagerGUI.serverUserTreeModel.setUnsavedData(true);
            saveButton.setText(rb.getString("server.details.savebutton") + " *");

            int selectedIndex = serverTypeBox.getSelectedIndex();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (selectedIndex == Server.SERVER_TYPE_REGULAR_OPENVPN) {
                    wrapperField.setEnabled(true);
                    usernameField.setText(details[4] == null || details[4].isEmpty() ? "" : details[4]);

                    userfileField.setText(details[6] == null || details[6].isEmpty() ? "" : details[6]);

                    exportPathField.setText(details[7] == null || details[7].isEmpty() ? "" : details[7]);

                    ccdField.setText(details[18] == null || details[18].isEmpty() ? "" : details[18]);

                    vpnNetworkAddress.setText(details[19] == null || details[19].isEmpty() ? "" : details[19]);

                    setIpField(vpnNetworkAddress.getText());
                } else if (selectedIndex == Server.SERVER_TYPE_BYTEMINE_APPLIANCE) {
                    wrapperField.setEnabled(false);
                    usernameField.setText(details[4] == null || details[4].isEmpty() ? Configuration.getInstance().SERVER_USERNAME : details[4]);

                    userfileField.setText(details[6] == null || details[6].isEmpty() ? Configuration.getInstance().SERVER_PASSWD_PATH : details[6]);

                    exportPathField.setText(details[7] == null || details[7].isEmpty() ? Configuration.getInstance().SERVER_KEYS_PATH : details[7]);

                    ccdField.setText(details[18] == null || details[18].isEmpty() ? Configuration.getInstance().SERVER_CC_PATH : details[18]);

                    vpnNetworkAddress.setText(details[19] == null || details[19].isEmpty() ? Configuration.getInstance().SERVER_NETWORK_ADDRESS : details[19]);

                    setIpField(vpnNetworkAddress.getText());

                    if (details[21] == null || details[21].isEmpty())
                        serverVpnDeviceBox.setSelectedIndex(Configuration.getInstance().SERVER_DEVICE);
                    else
                        vpnNetworkAddress.setText(details[21]);
                }

            }
        });
        serverTypeBox.setFont(Constants.FONT_PLAIN);
        mainPanel.add(serverTypeBox, "span, growx, wrap");

        
        JLabel authTypeLabel = new JLabel(rb.getString("server.details.authType"));
        authTypeLabel.setFont(Constants.FONT_PLAIN);
        final JLabel authTypePasswordLabel = new JLabel(rb.getString("server.details.authType.password"));
        authTypePasswordLabel.setFont(Constants.FONT_PLAIN);
        final JLabel authTypeKeyfileLabel = new JLabel(rb.getString("server.details.authType.keyfile"));
        authTypeKeyfileLabel.setFont(Constants.FONT_PLAIN);
        final ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(authTypePassword);
        radioGroup.add(authTypeKeyfile);

        mainPanel.add(authTypeLabel, "align left");
        int row = 6;
        if (newServer) {
            row = 5;
        }
        mainPanel.add(authTypePassword, "cell 1 "+row);
        mainPanel.add(authTypePasswordLabel, "cell 1 "+row);

        mainPanel.add(new JLabel());
        mainPanel.add(authTypeKeyfile, "cell 1 "+row);
        mainPanel.add(authTypeKeyfileLabel, "cell 1 "+row+", wrap");

        JLabel usernameLabel = new JLabel(rb.getString("server.details.username"));
        usernameLabel.setFont(Constants.FONT_BOLD);
        mainPanel.add(usernameLabel, "align left");
        mainPanel.add(usernameField, "span, growx, wrap");


        int authType = Server.AUTH_TYPE_PASSWORD;
        try {
            authType = Integer.parseInt(details[3]);
        } catch (NumberFormatException e) {
            logger.warning("auth type is not an int!");
        }

        // field: keyfilepath 		Panel: 1
        final JLabel keyfileLabel = new JLabel(rb.getString("server.details.keyfilepath"));
        keyfileLabel.setFont(authType == Server.AUTH_TYPE_PASSWORD ? Constants.FONT_PLAIN : Constants.FONT_BOLD);
        mainPanel.add(keyfileLabel, "align left");
        
        // set default keyfile-path to user.home/.ssh/id_rsa.pub
        String x = null == details[5] ? System.getProperty("user.home") + "/.ssh/id_rsa" : details[5];

        final JTextField keyfileField = new JTextField(x, 10);
        
        final JButton filechooserButton = new JButton();
        filechooserButton.setIcon(
            ImageUtils.createImageIcon(Constants.ICON_OPEN_PATH, "open")
        );
        filechooserButton.setSize(30, 20);
        filechooserButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (((JButton) evt.getSource()).isEnabled()) {
                    String filename = keyfileChooser(mainPanel);
                    if (filename != null)
                        keyfileField.setText(filename);
                }
            }
        });
        mainPanel.add(keyfileField, "growx");
        mainPanel.add(filechooserButton, "width 30!, wrap");

        // auth type radio buttons
        if (authType == Server.AUTH_TYPE_PASSWORD) {
            authTypePassword.setSelected(true);
            authTypeKeyfile.setSelected(false);
            keyfileField.setEnabled(false);
            filechooserButton.setEnabled(false);
        } else {
            authTypeKeyfile.setSelected(true);
            authTypePassword.setSelected(false);
            keyfileField.setEnabled(true);
            filechooserButton.setEnabled(true);
        }
        authTypePassword.addActionListener(e -> {
            keyfileField.setEnabled(false);
            keyfileLabel.setFont(Constants.FONT_PLAIN);
            filechooserButton.setEnabled(false);

            ManagerGUI.serverUserTreeModel.setUnsavedData(true);
            saveButton.setText(rb.getString("server.details.savebutton") + " *");
            if (serverDetailsFrame != null)
                serverDetailsFrame.setTitle(title + " *");
        });
        authTypeKeyfile.addActionListener(e -> {
            keyfileField.setEnabled(true);
            keyfileLabel.setFont(Constants.FONT_BOLD);
            filechooserButton.setEnabled(true);

            ManagerGUI.serverUserTreeModel.setUnsavedData(true);
            saveButton.setText(rb.getString("server.details.savebutton") + " *");
            if (serverDetailsFrame != null)
                serverDetailsFrame.setTitle(title + " *");
        });

        // server path to passwd-file		Panel: 2
        JLabel userfilepathLabel = new JLabel(rb.getString("server.details.userfilepath"));
        userfilepathLabel.setFont(Constants.FONT_BOLD);
        if (!Configuration.getInstance().USE_PAM) {
            secondaryPanel.add(userfilepathLabel, "align left");
            secondaryPanel.add(userfileField, "span, growx, wrap");
        }

        // server path to certificates		Panel: 2
        JLabel exportpathLabel = new JLabel(rb.getString("server.details.exportpath"));
        exportpathLabel.setFont(Constants.FONT_BOLD);
        secondaryPanel.add(exportpathLabel, "align left");
        secondaryPanel.add(exportPathField, "span, growx, wrap");


        JLabel wrappercommandLabel = new JLabel(rb.getString("server.details.wrappercommand"));
        wrappercommandLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(wrappercommandLabel, "align left");
        mainPanel.add(wrapperField, "span, growx, wrap");

        final JTextField statustypeField = new JTextField(Server.transformStatusTypeToString(details[9]), 10);
        statustypeField.setEnabled(false);

        final JTextField statusportField = new JTextField(details[8], 5);
        statusportField.setEnabled(false);

        String interval = "60";
        if (details[10] != null && !"".equals(details[10]))
            interval = details[10];
        final JTextField statusintervalField = new JTextField(interval, 5);

        
        // field: ssh-port		Panel: 2
        JLabel sshportLabel = new JLabel(rb.getString("server.details.sshport"));
        sshportLabel.setFont(Constants.FONT_BOLD);
        mainPanel.add(sshportLabel, "align left");
        String sshPort = (details[11] == null || details[11].isEmpty()) ? Constants.DEFAULT_SSH_PORT : details[11];
        final JTextField sshportField = new JTextField(sshPort, 5);
        sshportField.addKeyListener(new keyStandardListener(sshportField, saveButton));
        mainPanel.add(sshportField, "wrap");

        
        // field: openVPN-port		Panel: 2
        JLabel opnvpnLabel = new JLabel(rb.getString("server.details.vpnport"));
        opnvpnLabel.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(opnvpnLabel, "align left");     
        String vpnPort = (details[15] == null || details[15].isEmpty()) ? Constants.DEFAULT_OPENVPN_PORT : details[15];
        final JTextField vpnportField = new JTextField(vpnPort, 5);
        vpnportField.addKeyListener(new keyStandardListener(vpnportField, saveButton));
        secondaryPanel.add(vpnportField,  "wrap");

        
        // field: openVPN-protocol	Panel: 2
        //   choose between tcp or udp
        JLabel vpnprotoLabel = new JLabel(rb.getString("server.details.vpnproto"));
        vpnprotoLabel.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(vpnprotoLabel, "align left");
        
        int vpnProtocol = Server.SERVER_OPENVPN_PROTOCOL_UDP;
        try {
        	vpnProtocol = Integer.parseInt(details[16]);
        } catch (Exception ignored) {
        }

        serverVpnProtocolBox.setSelectedIndex(vpnProtocol == Server.SERVER_OPENVPN_PROTOCOL_TCP ? 0 : 1);
            
        this.serverVpnProtocolBox.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(this.serverVpnProtocolBox, "wrap");
        

        // field: server-valid for			Panel: 1
        final JLabel serverValidForLabel = new JLabel(rb.getString("server.details.serverValidFor"));
        serverValidForLabel.setFont(Constants.FONT_PLAIN);
        final JTextField serverValidForField = new JTextField(4);
        serverValidForField.setHorizontalAlignment(JTextField.RIGHT);
        serverValidForField.setText(Configuration.getInstance().X509_SERVER_VALID_FOR);

        final JComboBox<String> serverValidityUnitBox = new JComboBox<>();
        serverValidityUnitBox.addItem(rb.getString("units.days"));
        serverValidityUnitBox.addItem(rb.getString("units.weeks"));
        serverValidityUnitBox.addItem(rb.getString("units.years"));
        serverValidityUnitBox.setSelectedIndex(0);
        serverValidityUnitBox.setFont(Constants.FONT_PLAIN);
        if (newServer) {
            mainPanel.add(serverValidForLabel, "align left");
            mainPanel.add(serverValidForField, "align right");
            mainPanel.add(serverValidityUnitBox, "align left, wrap");
        } 
               
        ip1.setDocument(new JTextFieldLimit(3));
        ip2.setDocument(new JTextFieldLimit(3));
        ip3.setDocument(new JTextFieldLimit(3));
        
        vpnNetworkAddress.setText(details[19]);
        if(vpnNetworkAddress.getText() == null)
            vpnNetworkAddress.setText("");
        setIpField(vpnNetworkAddress.getText());
        
        JLabel netmaskLabel = new JLabel(rb.getString("server.details.tab3network"));
        netmaskLabel.setFont(Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES ? Constants.FONT_BOLD : Constants.FONT_PLAIN);
                
        secondaryPanel.add(netmaskLabel);
        final JPanel ipPanel = new JPanel(new MigLayout("insets 0"));
        ipPanel.add(ip1);
        ipPanel.add(new JLabel("."));
        ipPanel.add(ip2);
        ipPanel.add(new JLabel("."));
        ipPanel.add(ip3);
        ipPanel.add(new JLabel(".0 /24"));
        secondaryPanel.add(ipPanel, "wrap");

        setBackground(ip1);
        setBackground(ip2);
        setBackground(ip3);

        // label for vpnDevice       panel 2
        JLabel vpnDevLabel = new JLabel(rb.getString("server.details.vpnDevice"));
        vpnDevLabel.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(vpnDevLabel);
        
        

        // field: vpnDevice       
        int device = Server.SERVER_OPENVPN_DEVICE_TUN;
        try {
            device = Integer.parseInt(details[21]);
        } catch (Exception e) {
            logger.warning("Problems parsing vpnDevice");
        }
        serverVpnDeviceBox.setSelectedIndex(device);
        
        serverVpnDeviceBox.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(serverVpnDeviceBox, "wrap");
        
        
        // option: redirectGateway      (Panel 2)
        JLabel vpnRedirectGatewayLabel = new JLabel(rb.getString("server.details.redirectGateway"));
        vpnRedirectGatewayLabel.setFont(Constants.FONT_PLAIN);
        
        final JCheckBox redirectGatewayBox = new JCheckBox();
        redirectGatewayBox.setSelected(Server.transformBooleanOption((details[22])));
        redirectGatewayBox.setBorder(BorderFactory.createEmptyBorder());

        
        secondaryPanel.add(vpnRedirectGatewayLabel);
        secondaryPanel.add(redirectGatewayBox, "wrap");

        
        // label for ccd		panel 2
        JLabel ccdLabel = new JLabel(rb.getString("server.details.vpnccd"));
        ccdLabel.setFont(Constants.FONT_PLAIN);
        
        // field: staticIps			Panel 2
        final JLabel staticIpLabel = new JLabel(rb.getString("server.details.vpncc"));
        staticIpLabel.setFont(Constants.FONT_PLAIN);
        secondaryPanel.add(staticIpLabel, "align left");
        
        // get staticIp - Field, determine whether ccd-field is enabled or disabled
        staticIpField.setBorder(BorderFactory.createEmptyBorder());
        if(!Server.transformBooleanOption(details[17])) {
        	staticIpField.setSelected(false);
        	staticIpTabActive = false;
        } else {
        	staticIpField.setSelected(true);
        	staticIpTabActive = true;
        	panels.add(createIpForUserPanelFromHashtable(), rb.getString("server.details.tab3"));
        }
        
        
        staticIpField.addItemListener(e -> {
            if(staticIpField.isSelected())
                showIpForUserTab();
            else
                hideIpForUserTab();
        });
        secondaryPanel.add(staticIpField, "wrap");
        secondaryPanel.add(ccdLabel, "align left, growx 2");
        secondaryPanel.add(ccdField, "span, growx, wrap");
        
        
        // enhanced options
        final JPanel extensionPanel = new JPanel(new MigLayout("insets 0, fillx"));
        extensionPanel.setVisible(false);
        final JButton expandButton = new JButton();
        expandButton.setIcon(
                ImageUtils.createImageIcon(Constants.ICON_EXPAND, "expand")
        );
        expandButton.setSize(20, 20);
        onMouseClicked(mainPanel, extensionPanel, expandButton);
        JPanel expandTopPanel = new JPanel(new MigLayout("insets 0"));
        JLabel expandLabel = new JLabel(rb.getString("server.details.expandOptions"));
        expandLabel.setFont(Constants.FONT_PLAIN);
        expandTopPanel.add(expandButton, "width 20!");
        expandTopPanel.add(expandLabel);
        secondaryPanel.add(expandTopPanel, "gaptop 12, wrap");
        
        // option: duplicate-cn      (Panel 2)
        JLabel vpnDuplicateCNLabel = new JLabel(rb.getString("server.details.vpnDuplicateCN"));
        vpnDuplicateCNLabel.setFont(Constants.FONT_PLAIN);
        
        final JCheckBox vpnDuplicateCNBox = new JCheckBox();
        vpnDuplicateCNBox.setSelected(Server.transformBooleanOption(details[23]));
        vpnDuplicateCNBox.setBorder(BorderFactory.createEmptyBorder());
        
        extensionPanel.add(vpnDuplicateCNLabel);
        extensionPanel.add(vpnDuplicateCNBox, "wrap");
        secondaryPanel.add(extensionPanel, "gapleft 20, growx, span 3, wrap");
        
        // textfield: vpnUser   (Panel 2)
        JLabel vpnUser = new JLabel(rb.getString("server.details.vpnUser"));
        vpnUser.setFont(Constants.FONT_PLAIN);
        final JTextField vpnUserField = new JTextField((details[24]==null ? Constants.DEFAULT_OPENVPN_USER : details[24]), 8);
        vpnUserField.addKeyListener(new keyStandardListener(vpnUserField, saveButton));
        extensionPanel.add(vpnUser);
        extensionPanel.add(vpnUserField, "wrap");

        
        // textfield: vpnGroup  (Panel 2)
        JLabel vpnGroup = new JLabel(rb.getString("server.details.vpnGroup"));
        vpnGroup.setFont(Constants.FONT_PLAIN);
        final JTextField vpnGroupField = new JTextField((details[25]==null ? Constants.DEFAULT_OPENVPN_GROUP : details[25]), 8);
        vpnGroupField.addKeyListener(new keyStandardListener(vpnGroupField, saveButton));
        extensionPanel.add(vpnGroup);
        extensionPanel.add(vpnGroupField, "wrap");
        
        
        // textfield: keepalive  (Panel 2)
        JLabel vpnKeepAliveLabel = new JLabel(rb.getString("server.details.vpnKeepAlive"));
        vpnKeepAliveLabel.setFont(Constants.FONT_PLAIN);
        final JTextField vpnKeepAliveFieldPing = new JTextField(3);
        try { 
            vpnKeepAliveFieldPing.setText(details[26].split(" ")[0]);
        } catch (Exception e) {
            vpnKeepAliveFieldPing.setText(Constants.DEFAULT_OPENVPN_KEEPALIVE_PING);
        }
        vpnKeepAliveFieldPing.addKeyListener(new keyStandardListenerNumber(vpnKeepAliveFieldPing, saveButton));
        final JTextField vpnKeepAliveFieldAssume = new JTextField(5);
        try { 
            vpnKeepAliveFieldAssume.setText(details[26].split(" ")[1]);
        } catch (Exception e) {
            vpnKeepAliveFieldAssume.setText(Constants.DEFAULT_OPENVPN_KEEPALIVE_ASSUME);
        }
        vpnKeepAliveFieldAssume.addKeyListener(new keyStandardListenerNumber(vpnKeepAliveFieldAssume, saveButton));
        extensionPanel.add(vpnKeepAliveLabel);
        JPanel vpnKeepAliveField = new JPanel(new MigLayout("insets 0"));
        vpnKeepAliveField.add(vpnKeepAliveFieldPing);
        vpnKeepAliveField.add(vpnKeepAliveFieldAssume);
        extensionPanel.add(vpnKeepAliveField);
        
        
        // buttons        
        JButton reassignButton = new JButton(rb.getString("server.details.reassignbutton"));
        reassignButton.setToolTipText(rb.getString("server.details.assignbutton_tt"));
        reassignButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Server server = new Server(serverid);
                server = ServerDAO.getInstance().read(server);

                X509Manager x509Manager;
                if (parentFrame != null) {
                    x509Manager = new X509Manager(parentFrame, server);
                    x509Manager.showX509ManagerFrame();
                } else {
                    x509Manager = new X509Manager(mainPanel, server);
                    JPanel x509Panel = x509Manager.createX509ManagerPanel();
                    ManagerGUI.updateServerUserDetails(x509Panel);
                }
            }
        });
        
        JButton exportButton = new JButton(rb.getString("server.details.exportbutton"));
        exportButton.setToolTipText(rb.getString("server.details.exportbutton_tt"));
        exportButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    String path = X509Action.exportToFilesystem(details[14]);
                    CustomJOptionPane.showMessageDialog(mainPanel,
                            rb.getString("server.details.exportmessage") 
                                +"\n" + path,
                            rb.getString("server.details.exporttitle"),
                            CustomJOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    new VisualException(rb.getString("server.details.exporterror"));
                }
            }
        });

        if (!newServer) {
            JLabel x509Label = new JLabel(rb.getString("server.details.x509"));
            x509Label.setFont(Constants.FONT_PLAIN);
            mainPanel.add(x509Label, "align left");
            if (details[12] != null && !"0".equals(details[14]) && !"-1".equals(details[14])) {
                JButton showButton = new JButton(rb.getString("server.details.showbutton"));
                showButton.addMouseListener(new java.awt.event.MouseAdapter() {

                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        X509Details x509DetailsFrame = new X509Details(mainPanel, details[14]);
                        x509DetailsFrame.showDetails();
                    }
                });
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(showButton);
                buttonPanel.add(exportButton);
                buttonPanel.add(reassignButton);
                mainPanel.add(buttonPanel, "wrap");
            } else {
                JTextField x509Field = new JTextField(rb.getString("server.details.nocert"), 15);
                x509Field.setEditable(false);
                reassignButton.setText(rb.getString("server.details.assignbutton"));
                reassignButton.setToolTipText(rb.getString("server.details.assignbutton_tt"));

                JPanel bPanel = new JPanel();
                bPanel.add(x509Field);
                bPanel.add(reassignButton);
                mainPanel.add(bPanel, "wrap");
            }
        }


        final JButton vpnStartButton = new JButton(rb.getString("server.details.vpnStartButton"));
        vpnStartButton.setToolTipText(rb.getString("server.details.vpnStartButton_tt"));
        vpnStartButton.addActionListener(e -> {
            Server server = Server.getServerById(Integer.parseInt(serverid));
            try {
                Dialogs.showOpenVpnStartDaemonDialog(ManagerGUI.mainFrame, server);
            } catch (Exception ex) {
                logger.warning("showOpenVpnStartDaemonDialog throwed: " + ex.toString());
            }
        });

        final JButton syncUsersButton = new JButton(rb.getString("serverContextMenu.sync"));
        syncUsersButton.setToolTipText(rb.getString("server.details.syncbutton_tt"));
        syncUsersButton.addActionListener(e -> {
            try {
                new UserSync(idField.getText());
            } catch (Exception ex) {
                CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                        ex.getMessage(),
                        rb.getString("error.syncusers.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            ManagerGUI.refreshUserTable();
        });


        final JButton deleteButton = new JButton(rb.getString("server.details.deletebutton"));
        deleteButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                boolean dispose = false;

                if (myWindow != null)
                    dispose = true;

                Dialogs.showDeleteServerDialog(myWindow, idField.getText(), dispose);

                ManagerGUI.serverUserTreeModel.setUnsavedData(false);
                saveButton.setText(rb.getString("server.details.savebutton"));
            }
        });
        
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // save or update
            	JTextField ipField = null;
                try {
                    boolean valid = ValidatorAction.validateServerCreation(
                            nameField.getText(),
                            details[1],
                            hostnameField.getText(),
                            usernameField.getText(),
                            authTypeKeyfile.isSelected(),
                            keyfileField.getText(),
                            userfileField.getText(),
                            exportPathField.getText(),
                            statusportField.getText(),
                            statusintervalField.getText(),
                            sshportField.getText(),
                            newServer,
                            vpnportField.getText(),
                            staticIpField.isSelected(),
                            ccdField.getText(),
                            vpnNetworkAddress.getText()
                    );
                    
                    if (valid && staticIpTabActive) {
                    	
                    	ipField = ValidatorAction.validateServerCreationUserServerIp(userServerIp);
                    	if (ipField != null) {
                            throw new ValidationException(
                                    rb.getString("server.details.errorUserIp"),
                                    rb.getString("server.details.errortitle"),
                                    11
                    		);
                    	}
                    	String winConformIP = ValidatorAction.validateStaticIpForWindowsClients(userServerIp);
                    	if (winConformIP != null && Configuration.getInstance().GUI_SHOW_OPENVPN_IP_WARNING)
                    		Dialogs.showOpenVPNIpWarningDialog(serverDetailsFrame, winConformIP);
                    }
                    	
                    if (valid) {
                        if (newServer) {
                            String serverValidFor= serverValidForField.getText();
                            String serverValidityUnit= (String) serverValidityUnitBox.getSelectedItem();
                            assert serverValidityUnit != null;
                            if (!Objects.equals(serverValidityUnit, rb.getString("units.days"))) {
                                if (serverValidityUnit.equals(rb.getString("units.weeks")))
                                    serverValidFor= Integer.toString(Integer.parseInt(serverValidFor) * 7);
                                else if (serverValidityUnit.equals(rb.getString("units.years")))
                                    serverValidFor= Integer.toString(Integer.parseInt(serverValidFor) * 365);
                            }
                            
                            int serverId = ServerAction.createServerAndCertificate(
                                    usernameField.getText(),
                                    keyfileField.getText(),
                                    authTypeKeyfile.isSelected(),
                                    nameField.getText(),
                                    cnField.getText(),
                                    ouField.getText(),
                                    hostnameField.getText(),
                                    userfileField.getText(),
                                    exportPathField.getText(),
                                    statusportField.getText(),
                                    statustypeField.getText(),
                                    statusintervalField.getText(),
                                    sshportField.getText(),
                                    serverTypeBox.getSelectedIndex(),
                                    wrapperField.getText(),
                                    serverValidFor,
                                    vpnportField.getText(),
                                    serverVpnProtocolBox.getSelectedIndex(),
                                    staticIpField.isSelected(),
                                    ccdField.getText(),
                                    vpnNetworkAddress.getText(),
                                    vpnSubnetMask,
                                    serverVpnDeviceBox.getSelectedIndex(),
                                    redirectGatewayBox.isSelected(),
                                    vpnDuplicateCNBox.isSelected(),
                                    vpnUserField.getText(),
                                    vpnGroupField.getText(),
                                    vpnKeepAliveFieldPing.getText()+" "+vpnKeepAliveFieldAssume.getText()
                                    
                            );
        
                            idField.setText(serverId + "");
                            // connect server and users
                            ServerQueries.reconnectUsersAndServer(idField.getText(), connectedUsers);
                            if (staticIpTabActive)
                            	ServerQueries.reassignIpToUserServer(idField.getText(), userServerIp);
                            
                            // create vpn-config files
                            if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES) {
                                ServerAction.createVPNUserConfigFile(Server.getServerById(Integer.parseInt((idField.getText()))));
                                ServerAction.createVPNServerConfigFile(Server.getServerById(Integer.parseInt((idField.getText()))));
                            }
                        } else {	
                            ServerAction.updateServer(
                                    idField.getText(),
                                    nameField.getText(),
                                    cnField.getText(),
                                    ouField.getText(),
                                    hostnameField.getText(),
                                    usernameField.getText(),
                                    authTypeKeyfile.isSelected(),
                                    keyfileField.getText(),
                                    userfileField.getText(),
                                    exportPathField.getText(),
                                    statusportField.getText(),
                                    statustypeField.getText(),
                                    statusintervalField.getText(),
                                    sshportField.getText(),
                                    serverTypeBox.getSelectedIndex(),
                                    wrapperField.getText(),
                                    vpnportField.getText(),
                                    serverVpnProtocolBox.getSelectedIndex(),
                                    staticIpField.isSelected(),
                                    ccdField.getText(),
                                    vpnNetworkAddress.getText(),
                                    vpnSubnetMask,
                                    serverVpnDeviceBox.getSelectedIndex(),
                                    redirectGatewayBox.isSelected(),
                                    vpnDuplicateCNBox.isSelected(),
                                    vpnUserField.getText(),
                                    vpnGroupField.getText(),
                                    vpnKeepAliveFieldPing.getText()+" "+vpnKeepAliveFieldAssume.getText()
                            );
                            // connect server and users
                            ServerQueries.reconnectUsersAndServer(idField.getText(), connectedUsers);
                            if (staticIpTabActive)
                            	ServerQueries.reassignIpToUserServer(idField.getText(), userServerIp);
                            
                            // create vpn-config files
                            if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES) {
                                ServerAction.createVPNUserConfigFile(Server.getServerById(Integer.parseInt((idField.getText()))));
                                ServerAction.createVPNServerConfigFile(Server.getServerById(Integer.parseInt((idField.getText()))));
                            }
                        }
                        
                        ManagerGUI.serverUserTreeModel.setUnsavedData(false);
                        saveButton.setText(rb.getString("server.details.savebutton"));
                        if (serverDetailsFrame != null)
                            serverDetailsFrame.setTitle(title);
                        
                        ManagerGUI.reloadServerUserTree();
                        ManagerGUI.refreshServerTable();
                        ManagerGUI.refreshX509Table();

                        if (newServer) {
                            if (myWindow == null) {
                                ServerDetails sDetails = new ServerDetails(null, null);
                                JTabbedPane detailsPanel = sDetails.createServerDetailsPanel();
                                ManagerGUI.updateServerUserDetails(detailsPanel);
                            } else
                                myWindow.dispose();

                            CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                         rb.getString("dialog.newserver.success.save")); 
                        } else {
                            if (myWindow != null)
                                myWindow.dispose();
                            if (serverDetailsFrame != null)
                                serverDetailsFrame.setTitle(title);
                            
                            CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                        rb.getString("server.dialog.success.save"));
                        }
                    }
                } catch (ValidationException ve) {
                    // set focus on error field, switch to tab containing the error field
                    if (ve.getCode() > 0) {
                        int code = ve.getCode();
                        switch (code) {
                            case 1: nameField.requestFocus(); panels.setSelectedIndex(0);
                            		nameField.setBackground(Constants.COLOR_ERROR); break;
                            case 2: hostnameField.requestFocus();panels.setSelectedIndex(0);
                            		hostnameField.setBackground(Constants.COLOR_ERROR); break;
                            case 3: usernameField.requestFocus(); panels.setSelectedIndex(0);
                            		usernameField.setBackground(Constants.COLOR_ERROR); break;
                            case 4: keyfileField.requestFocus(); panels.setSelectedIndex(0);
                            		keyfileField.setBackground(Constants.COLOR_ERROR); break;
                            case 5: userfileField.requestFocus(); panels.setSelectedIndex(1);
                            		userfileField.setBackground(Constants.COLOR_ERROR); break;
                            case 6: exportPathField.requestFocus();panels.setSelectedIndex(1);
                            		exportPathField.setBackground(Constants.COLOR_ERROR); break;
                            case 7: statusintervalField.requestFocus();panels.setSelectedIndex(0);
                            		statusintervalField.setBackground(Constants.COLOR_ERROR); break;
                            case 8: sshportField.requestFocus();panels.setSelectedIndex(0);
                            		sshportField.setBackground(Constants.COLOR_ERROR); break;
                            case 9: vpnportField.requestFocus();panels.setSelectedIndex(1);
                            		vpnportField.setBackground(Constants.COLOR_ERROR); break; 
                            case 10:ccdField.requestFocus();panels.setSelectedIndex(1);
                            		ccdField.setBackground(Constants.COLOR_ERROR); break;
                            case 11:
                                assert ipField != null;
                                ipField.requestFocus();panels.setSelectedIndex(2);
                    				ipField.setBackground(Constants.COLOR_ERROR); break;
                            case 12:panels.setSelectedIndex(1);
                                Arrays.stream(ipPanel.getComponents()).filter(c -> c instanceof JTextField).filter(c -> "".equals(((JTextField) c).getText())).forEach(c -> c.setBackground((Constants.COLOR_ERROR)));
                            default: break;
                        }
                    }
                    
                    // show validation error dialog
                    CustomJOptionPane.showMessageDialog(serverDetailsFrame,
                            ve.getMessage(),
                            ve.getTitle(),
                            CustomJOptionPane.ERROR_MESSAGE);
                    
                } catch (Exception e) {
                    if (serverDetailsFrame != null)
                        new VisualException(serverDetailsFrame, e);
                    else
                        new VisualException(ManagerGUI.mainFrame, e);
                }
            }
        });


        if (newServer) {
            syncUsersButton.setEnabled(false);
            vpnStartButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }

        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        JButton cancelButton = new JButton(rb.getString("server.details.closebutton"));
        cancelButton.addActionListener(e -> {
            ManagerGUI.serverUserTreeModel.setUnsavedData(false);
            if (serverDetailsFrame != null)
                serverDetailsFrame.dispose();
            else
                ManagerGUI.clearRightPanel();
        });

        buttonPanel.add(cancelButton);
        
        mainPanel.add(syncUsersButton, "gaptop 20, wrap");
        //mainPanel.add(vpnStartButton, "wrap");
        mainPanel.add(buttonPanel, "gaptop 10, align right, span 3, wrap");

        CssRuleManager.getInstance().format(mainPanel);

        return panels;
    }

    static void onMouseClicked(JPanel mainPanel, JPanel extensionPanel, JButton expandButton) {
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
    }

    private void setBackground(JTextField ip1) {
        ip1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                setTextFieldBackground(ip1);
                vpnNetworkAddress.setText(String.format("%s.%s.%s.", ip1.getText(), ip2.getText(), ip3.getText()));
            }
        });
    }

    private void setTextFieldBackground(JTextField ip1) {
        try {
            if (Integer.parseInt(ip1.getText()) > 255)
                throw new Exception();
            ip1.setBackground(Constants.COLOR_WHITE);
        } catch (Exception e) {
            if (ip1.getText().length() > 0)
                ip1.setBackground(Constants.COLOR_ERROR);
        }
    }


    /**
     * Fills the UserServerIp-Hashtable with the values of the database
     *
     */
    private void initializeUserServerIpHashtable() {

    	userServerIp.clear();

        
        Vector<String[]> allUsers = UserQueries.getAllUsersAsVector(UserQueries.order_username);

        allUsers.forEach(strings -> {
            JTextField ip = new JTextField(20);
            ip.setText(ServerQueries.getIpFromUserServer(serverid, strings[0]));
            if (connectedUsers.contains(strings[0])) {
                userServerIp.put(strings[0], ip);
            }
        });
    }
    

    /**
     * Create a IpForUserPanel from the UserServerIp-Hashtable
     *
     * @return The panel
     */
    private JPanel createIpForUserPanelFromHashtable() {
    	JPanel panel = new JPanel(new MigLayout());
    	panel.setSize(new Dimension(0, frameHeight - 10));
    	
    	JLabel titleLabel = new JLabel(rb.getString("server.details.tab3title"));
		panel.add(titleLabel, "wrap, gapbottom 10");

        JLabel netLabel = new JLabel(rb.getString("server.details.tab3network") + ":");
        panel.add(netLabel);
        
        netmaskLabel = new JLabel(String.format(formatNetmask, ip1.getText(), ip2.getText(), ip3.getText(), "0"));
        netmaskLabel.setFont(Constants.FONT_PLAIN);
        JLabel subnetLabel = new JLabel("/" + vpnSubnetMask);
        subnetLabel.setFont(Constants.FONT_PLAIN);
        panel.add(netmaskLabel);
        panel.add(subnetLabel, "wrap, gapbottom 20");
        
        panel.add(new JLabel(rb.getString("server.details.tab3user")));
        panel.add(new JLabel(rb.getString("server.details.tab3ip")), "wrap, gapbottom 5");
        
        Vector<String[]> allUsers = UserQueries.getAllUsersAsVector(UserQueries.order_username);

        allUsers.forEach(strings -> {
            JLabel username = new JLabel(strings[1]);
            JLabel clientIPNetwork = new JLabel(String.format(formatClientIp, ip1.getText(), ip2.getText(), ip3.getText()));
            clientIPNetwork.setFont(Constants.FONT_PLAIN);
            final JTextField clientIP = new JTextField(3);
            clientIP.setDocument(new JTextFieldLimit(3));
            if (userServerIp.containsKey(strings[0]))
                clientIP.setText(userServerIp.get(strings[0]).getText());
            else
                clientIP.setText(ServerQueries.getIpFromUserServer(serverid, strings[0]));
            clientIP.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent evt) {
                    checkIp(clientIP);
                }
            });
            clientIP.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent fe) {
                    checkForDuplicateIp();
                }
            });
            userServerIp.put(strings[0], clientIP);
            panel.add(username, "gapleft 3");
            panel.add(clientIPNetwork);
            networkIPList.add(clientIPNetwork);
            panel.add(clientIP, "wrap");
            username.setFont(Constants.FONT_PLAIN);
            if (connectedUsers.contains(strings[0]))
                clientIP.setEditable(true);
            else {
                clientIP.setEditable(false);
                clientIP.setText("");
            }
            checkForDuplicateIp();
        });
        
        CssRuleManager.getInstance().format(panel);
		return panel;
    }

    
    /**
     * checks ip-list for duplicate entries
     * highlights the duplicate ip's red
     */     
    private void checkForDuplicateIp() {
        
        // check if every ip is an integer
        // (former duplicate ips are marked valid again)
        for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();) {
            String userID = e.nextElement();
            checkIp(userServerIp.get(userID));
        }      
       
       // check for duplicate ips
       for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();) {
           String userId = e.nextElement();
            
           for (Enumeration<String> en = userServerIp.keys(); en.hasMoreElements();) {
               String userID = en.nextElement();

               if (!userId.equals(userID) && userServerIp.get(userID).getText().length() > 0 && userServerIp.get(userId).getText().length() > 0 && userServerIp.get(userID).getText().equals(userServerIp.get(userId).getText())) {
                   userServerIp.get(userID).setBackground(Constants.COLOR_ERROR);
               }
           }
        }
    }
    
    /**
     * check if ip is an integer under 256
     * @param ip
     */
    private void checkIp(JTextField ip) {
        setTextFieldBackground(ip);
    }
    
    
    /**
     * Show the IpForUserTab
     */
	private void showIpForUserTab() {
		if (!staticIpTabActive) {
            JPanel tab3 = createIpForUserPanelFromHashtable();
			try{
				tabbedPane.add(new JScrollPane(tab3), rb.getString("server.details.tab3"));
				tabbedPane.setSelectedIndex(2);	// focus on this tab
			} catch (Exception e) {
				logger.severe("IpForUserTab couldn't be opened: " + e.toString());
			}
			staticIpTabActive = true;
		}
    }
	
	
	/**
     * Hide the IpForUserTab
     */
	private void hideIpForUserTab() {
		if (staticIpTabActive) {
			try {
			    tabbedPane.remove(2);
			} catch(Exception e) {
				logger.warning("Couldn't remove tab3:" + e.toString());
			}			
			staticIpTabActive = false;
		}
    }
    
    
    /**
     * Create the scrollpane with checkboxes and usernames
     *
     * @return The scrollpane
     */
    private JScrollPane createUserManagement() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        JPanel usrMgrPanel = new JPanel();
        usrMgrPanel.setLayout(new MigLayout());
        usrMgrPanel.setSize(new Dimension(mgrWidth, frameHeight - 10));

        JLabel title = new JLabel(rb.getString("server.details.usrmgmt.title"));
        usrMgrPanel.add(title, "wrap");

        Vector<String[]> allUsers = UserQueries.getAllUsersAsVector(UserQueries.order_username);

        allUsers.forEach(strings -> {
            final String userid = strings[0];
            JCheckBox box = new JCheckBox(strings[1]);
            if (connectedUsers.contains(strings[0]))
                box.setSelected(true);
            else
                box.setSelected(false);
            box.addActionListener(e -> {
                AbstractButton checkbox = (AbstractButton) e.getSource();
                boolean selected = checkbox.getModel().isSelected();
                if (selected && !connectedUsers.contains(userid)) {
                    connectedUsers.add(userid);
                    if (staticIpTabActive)
                        refreshIpForUserTab();
                } else if (!selected && connectedUsers.contains(userid)) {
                    connectedUsers.remove(userid);
                    if (staticIpTabActive) {
                        userServerIp.remove(userid);
                        refreshIpForUserTab();
                    }
                }
            });
            usrMgrPanel.add(box, "wrap");
        });

        return new JScrollPane(usrMgrPanel);
    }
    
    /**
     * Refresh the IpForUserTab
     *
     * method will just remove and (re)add the UserIp-Tab
     */
    private void refreshIpForUserTab() {
    	JPanel tab3 = createIpForUserPanelFromHashtable();
    	tabbedPane.remove(2);
    	tabbedPane.add(new JScrollPane(tab3), rb.getString("server.details.tab3"));
		tabbedPane.setSelectedIndex(2);	// focus on this tab	
    }
    
    /**
     * Shows a JFilechooser for choosing the key file
     *
     * @param parent The parent component
     * @return String with the selected filename
     */
    static String keyfileChooser(Component parent) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        // create a file chooser
        final JFileChooser fc = LocalizedFileChooser.getLocalizedFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileHidingEnabled(false);
        fc.setApproveButtonText(rb.getString("fileChooser.rootCert.button"));
        fc.setDialogTitle(rb.getString("fileChooser.rootCert.title"));

        // responds to a button click:
        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return file.getPath();
        } else {
            return null;
        }
    }
    
    private void setIpField(String networkAddress) {      
        try {
            if (vpnNetworkAddress.getText().length() > 3) {
                ip1.setText(vpnNetworkAddress.getText().split("\\.")[0].trim());
                ip2.setText(vpnNetworkAddress.getText().split("\\.")[1].trim());
                ip3.setText(vpnNetworkAddress.getText().split("\\.")[2].trim());
            }
        } catch (Exception ex) {
            logger.warning("couldn't process " + vpnNetworkAddress.getText() + ex.toString());
        }
    }
    
    void setTabbedPane(JTabbedPane pane) {
    	tabbedPane = pane;
    }


    public JFrame getParentFrame() {
        return parentFrame;
    }


    JFrame getServerDetailsFrame() {
        return serverDetailsFrame;
    }


    public static void main(String[] args) {
        ServerDetails d = new ServerDetails(null, "-1");
        d.createServerDetailsFrame();
    }
}

class JTextFieldLimit extends PlainDocument {
    private static final long serialVersionUID = 4369737827843330618L;

    private int limit;
	JTextFieldLimit(int limit) {
		super();
		this.limit = limit;
	}

	JTextFieldLimit(int limit, boolean upper) {
		super();
		this.limit = limit;
	}

	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if (str == null)
			return;

		if ((getLength() + str.length()) <= limit) {
			super.insertString(offset, str, attr);
		}
	}
}