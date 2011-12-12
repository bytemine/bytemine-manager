/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Component;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.TreeConfiguration;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.*;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.CRLOverviewTableModel;
import net.bytemine.manager.model.ServerOverviewTableModel;
import net.bytemine.manager.model.ServerUserTreeModel;
import net.bytemine.manager.model.UserOverviewTableModel;
import net.bytemine.manager.model.X509OverviewTableModel;
import net.bytemine.manager.update.UpdateMgmt;
import net.bytemine.manager.utility.CRLExporter;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.BatchUserSync;
import net.bytemine.openvpn.UserSync;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.PrintUtils;
import net.miginfocom.swing.MigLayout;


/**
 * The main manager gui
 *
 * @author Daniel Rauer
 */
public class ManagerGUI {

    private static Logger logger = Logger.getLogger(ManagerGUI.class.getName());

    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    public static JFrame mainFrame = null;
    public static JPanel detailsPanel = new JPanel();
    public static SupportForm supportForm = null;

    public static final int mainWidth = Configuration.getInstance().GUI_WIDTH;
    public static final int mainHeight = Configuration.getInstance().GUI_HEIGHT;
    public static final int locationX = Configuration.getInstance().GUI_LOCATION_X;
    public static final int locationY = Configuration.getInstance().GUI_LOCATION_Y;
    public static final int serverUserTreeDividerLocation = Configuration.getInstance().SERVER_USER_TREE_DIVIDER_LOCATION;

    private static ManagerGUI app = null;
    private JPanel mainPanel = null;
    private JPanel toolBar = null;
    private static JTable x509Table = null;
    private static JTable serverTable = null;
    private static JTable userTable = null;
    private static JTable crlTable = null;
    private static JTabbedPane tabs = null;
    private static Hashtable<String, ControlCenterTab> openCCTabs = new Hashtable<String, ControlCenterTab>();
    
    private static int crlTabIndex = -1;

    private static JMenuItem updateMenuItem = null;

    private static JLabel statusTextLabel = new JLabel();
    private static JLabel waitingImageLabel = new JLabel();
    private static JPanel bottomPanel = new JPanel();
    private static JPanel statusPanel = new JPanel();
    private static JPanel threadPanel = new JPanel();
    private static JPanel printPanel = new JPanel();
    private static Vector<StatusMessage> statusMessages = new Vector<StatusMessage>();

    private final UpdateMgmt updateMgmt;

    private static JScrollPane serverUserDetailsScrollPane;
    public static JSplitPane serverUserSplitPane;
    private static JTree serverUserTree;
    protected static ServerUserTreeModel serverUserTreeModel;
    
    // containing all servers which are opened in a control-tab
    public static Vector<String> OPEN_CONTROL_SERVERS = new Vector<String>();
    
    public ManagerGUI() {
        app = this;
        updateMgmt = UpdateMgmt.getInstance();

        // the main frame
        app.showMainFrame();

        // show some dialogs if necessary
        app.showDialogs();
    }


    /**
     * opens some dialogs if necessary
     */
    private void showDialogs() {
        // is set, if a different database gets selected
        boolean skipFollowingDialogs = false;
        boolean generateRootCertificate = false;

        if (!ConfigurationQueries.areConfigurationsExisiting()) {
            // shows first configuration dialog
            skipFollowingDialogs = Dialogs.showConfigurationDialog(mainFrame);
        }


        // only ask for import if CA module is enabled
        if (Configuration.getInstance().CA_ENABLED && !skipFollowingDialogs) {
            if (!X509DAO.isCertificatesExisting()) {
                // show first import dialog
                boolean importSkipped = true;

                //if (Configuration.getInstance().isClientCertImportDirSet())
                importSkipped = Dialogs.showImportDialog(mainFrame);

                if (importSkipped) {
                    // shows x509 configuration dialog
                    generateRootCertificate = Dialogs.showX509ConfigurationDialog(mainFrame);
                } else {
                    // Import configuration dialog, starts the import after
                    // configuration is completed
                    if(!Dialogs.showImportConfigurationDialog(mainFrame)) {
                        generateRootCertificate = Dialogs.showX509ConfigurationDialog(mainFrame);
                    }
                }
            } else if (!Configuration.getInstance().isRootCertExisting()) {
                // shows x509 configuration dialog
                generateRootCertificate = Dialogs.showX509ConfigurationDialog(mainFrame);
            }

            if (generateRootCertificate)
                X509Utils.createRootCertificate();

            X509Utils.createDHParameters();
        }

        if (Configuration.getInstance().UPDATE_AUTOMATICALLY) {
            // displays a dialog only if an update is available
            UpdateMgmt updateMgmt = UpdateMgmt.getInstance(false);
            updateMgmt.searchForUpdates();
        }

    }


    /**
     * the main application
     */
    private void showMainFrame() {
        rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Image icon = Toolkit.getDefaultToolkit().getImage(Configuration.getInstance().ICON_PATH);
        try {
            icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        } catch (Exception e) {
            logger.warning("icon could not be read: " + Configuration.getInstance().ICON_PATH);
        }

        JMenuBar menuBar = app.createMenu();

        tabs = new JTabbedPane();
        tabs.setName("main_tabs");
        tabs.add(rb.getString("tab.x509.name"), createX509Tab());
        tabs.add(rb.getString("tab.server.name"), createServerTab());
        tabs.add(rb.getString("tab.user.name"), createUserTab());
        tabs.add(rb.getString("tab.server_user.name"), createServerAndUserTab());
        if (Configuration.getInstance().GUI_SHOW_CRL_TAB)
            openCRLTab(); 
        
        // reload the tree to confirm that the tree is displayed correctly
        reloadServerUserTree();

        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                int controlCenterIndex = 3;
                if (Configuration.getInstance().GUI_SHOW_CRL_TAB)
                    controlCenterIndex = 4;
                
                if (tabs.getSelectedIndex() > controlCenterIndex)
                    invisiblePrintPanel();
                else
                    showPrintPanel();
                
                switch (tabs.getSelectedIndex()) {
                case 0: createX509ToolBar(null); break;
                case 1: createServerToolBar(null); break;
                case 2: createUserToolBar(null); break;
                case 3: createServerUserToolBar(); break;
                case 4: createCRLToolBar(); break;
                default: createCCToolBar(); break;
                }
            }
        });

        mainPanel = new JPanel(new MigLayout("insets 0, fill, align left"));
        createX509ToolBar(null);
        mainPanel.add(toolBar, "wrap");
        mainPanel.add(tabs, "grow");

        createBottomPanel();

        mainFrame = new JFrame(rb.getString("app.title"));
        mainFrame.setLayout(new MigLayout("insets 0, fill"));
        mainFrame.setIconImage(icon);
        mainFrame.setJMenuBar(menuBar);
        // catch this event in MainWindowListener, to show a confirmation dialog
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        mainFrame.add(mainPanel, "wrap, grow");
        mainFrame.add(bottomPanel, "growx");

        mainFrame.setPreferredSize(new Dimension(mainWidth, mainHeight));
        mainFrame.setLocation(new Point(locationX, locationY));

        mainFrame.addWindowListener(new MainWindowListener());

        CssRuleManager.getInstance().format(mainFrame);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    /**
     * Creates a dynamic toolbar for the x509 tab
     * @param x509Id The x509id or null
     */
    private void createX509ToolBar(final String x509Id) {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, fill"));
        JButton detailsButton = new JButton(rb.getString("toolbar.x509.details"));
        detailsButton.setToolTipText(rb.getString("toolbar.x509.details_tt"));
        detailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showX509Details(x509Id);
            }
        });
        if (x509Id == null)
            detailsButton.setEnabled(false);
        toolBar.add(detailsButton);
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }
    
    /**
     * Creates a dynamic toolbar for the server tab
     * @param serverId The serverId or null
     */
    private void createServerToolBar(final String serverId) {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, fill"));
        
        JButton newButton = new JButton(rb.getString("toolbar.server.new"));
        newButton.setToolTipText(rb.getString("toolbar.server.new_tt"));
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ServerDetails serverDetailsFrame = new ServerDetails(mainFrame, "-1");
                serverDetailsFrame.showServerDetailsFrame();
            }
        });
        
        JButton detailsButton = new JButton(rb.getString("toolbar.server.details"));
        detailsButton.setToolTipText(rb.getString("toolbar.server.details_tt"));
        detailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showServerDetails(serverId);
            }
        });
        
        JButton syncButton = new JButton(rb.getString("toolbar.server.sync"));
        syncButton.setToolTipText(rb.getString("toolbar.server.sync_tt"));
        syncButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncUsersClicked(serverId);
            }
        });
        
        JButton startVpnButton = new JButton(rb.getString("toolbar.server.startVpn"));
        startVpnButton.setToolTipText(rb.getString("toolbar.server.startVpn_tt"));
        startVpnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Server server = Server.getServerById(Integer.parseInt(serverId));
                try {
                    Dialogs.showOpenVpnStartDaemonDialog(ManagerGUI.mainFrame,server);
                } catch (Exception ex) {
                    logger.warning("showOpenVpnStartDaemonDialog throwed: "+ex.toString());
                }
            }
        });

        JButton deleteButton = new JButton(rb.getString("toolbar.server.delete"));
        deleteButton.setToolTipText(rb.getString("toolbar.server.delete_tt"));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Dialogs.showDeleteServerDialog(ManagerGUI.mainFrame, serverId);
            }
        });

        JButton ccButton = new JButton(rb.getString("toolbar.server.cc"));
        ccButton.setToolTipText(rb.getString("toolbar.server.cc_tt"));
        if (serverId == null) {
            // disable some buttons
            detailsButton.setEnabled(false);
            deleteButton.setEnabled(false);
            ccButton.setEnabled(false);
            syncButton.setEnabled(false);
            startVpnButton.setEnabled(false);
        } else {
            Server s = new Server(serverId);
            final Server server = ServerDAO.getInstance().read(s);
            ccButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	// only permit one open control tab for every server    
                	if (!OPEN_CONTROL_SERVERS.contains(serverId)) {
                		
                		// is there any cctab having the same hostname
                		if (openCCTabs.containsKey(server.getHostname())) {
                			// is vpnport equal
                			if (openCCTabs.get(server.getHostname()).getVpnPort()==server.getVpnPort())
                				// error
                				CustomJOptionPane.showMessageDialog(mainPanel,
                                        rb.getString("dialog.cctab.hostip.msg"),
                                        rb.getString("dialog.cctab.hostip.title"),
                                        CustomJOptionPane.ERROR_MESSAGE);
                			
                				return;
                		}
                		
                		//openCCTabs.get()
                		openCCTab(server);
                		OPEN_CONTROL_SERVERS.addElement(serverId);
                	}
                }
            });
        }

        toolBar.add(newButton);
        toolBar.add(detailsButton);
        toolBar.add(syncButton);
        toolBar.add(startVpnButton);
        if (Configuration.getInstance().CC_ENABLED)
            toolBar.add(ccButton);
        toolBar.add(deleteButton);
        
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }
    
    /**
     * Creates a dynamic toolbar for the user tab
     * @param userId The userId
     */
    private void createUserToolBar(final String userId) {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, fill"));
        
        JButton newButton = new JButton(rb.getString("toolbar.user.new"));
        newButton.setToolTipText(rb.getString("toolbar.user.new_tt"));
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showUserDetails("-1");
            }
        });
        
        JButton detailsButton = new JButton(rb.getString("toolbar.user.details"));
        detailsButton.setToolTipText(rb.getString("toolbar.user.details_tt"));
        detailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showUserDetails(userId);
            }
        });
        
        JButton deleteButton = new JButton(rb.getString("toolbar.user.delete"));
        deleteButton.setToolTipText(rb.getString("toolbar.user.delete_tt"));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
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
                        UserAction.deleteUser(userId);
                    } catch (Exception e) {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("user.dialog.delete.errortext"),
                                rb.getString("user.dialog.delete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }

                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();
                }
            }
        });

        if (userId == null) {
            // disable some buttons
            detailsButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
        }

        toolBar.add(newButton);
        toolBar.add(detailsButton);
        toolBar.add(deleteButton);
        
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }
    
    /**
     * Creates a dynamic toolbar for the server/user tab
     */
    private void createServerUserToolBar() {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, fill"));
        JButton newUserButton = new JButton(rb.getString("toolbar.server_user.new_user"));
        newUserButton.setToolTipText(rb.getString("toolbar.server_user.new_user_tt"));
        newUserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                UserDetails uDetails = new UserDetails(null, null);
                JPanel detailsPanel = uDetails.createUserDetailsPanel();
                updateServerUserDetails(detailsPanel);
            }
        });
        JButton newServerButton = new JButton(rb.getString("toolbar.server_user.new_server"));
        newServerButton.setToolTipText(rb.getString("toolbar.server_user.new_server_tt"));
        newServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ServerDetails sDetails = new ServerDetails(null, null);
                JTabbedPane detailsPanel = sDetails.createServerDetailsPanel();
                sDetails.setTabbedPane(detailsPanel);
                updateServerUserDetails(detailsPanel);
            }
        });
        
        toolBar.add(newUserButton);
        toolBar.add(newServerButton);
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }
    
    /**
     * Creates a dynamic toolbar for the crl tab
     */
    private void createCRLToolBar() {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, fill"));
        
        JButton exportButton = new JButton(rb.getString("toolbar.crl.export"));
        exportButton.setToolTipText(rb.getString("toolbar.crl.export_tt"));
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    CRLExporter.exportCRLToFile();
                    addStatusMessage(new StatusMessage(rb.getString("statusBar.crl.export")));
                } catch (Exception e) {
                    CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                            rb.getString("toolbar.crl.errortext"),
                            rb.getString("toolbar.crl.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        toolBar.add(exportButton);
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }
    
    /**
     * Creates a dynamic toolbar for the control center tab
     */
    private void createCCToolBar() {
        if (toolBar != null)
            mainPanel.remove(0);
        
        toolBar = new JPanel(new MigLayout("insets 0, height 24!, fill"));
        JLabel label = new JLabel(" ");
        toolBar.add(label);
        CssRuleManager.getInstance().format(toolBar);
        
        mainPanel.add(toolBar, "wrap", 0);
        mainPanel.validate();
    }

    /**
     * creates all graphical components
     *
     * @return Component with all graphical elements
     */
    private Component createX509Tab() {
        JPanel mainPanel = new JPanel(new MigLayout("insets 0, fill"));
        // create the x509 table
        x509Table = new JTable(new X509OverviewTableModel());
        JTableHeader header = x509Table.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener((
                (X509OverviewTableModel) x509Table.getModel())
                .new ColumnListener(x509Table));
        x509Table.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent e) {
                Point position = new Point(e.getX(), e.getY());
                int row = x509Table.rowAtPoint(position);
                x509Table.setRowSelectionInterval(row, row);
                X509OverviewTableModel model = (X509OverviewTableModel) x509Table.getModel();
                // get the x509id from the mapping in the tableModel
                String x509Id = (String) model.getIdRowMapping().get(row + "");

                if (e.getButton() != MouseEvent.BUTTON1) {
                    // context menue
                    showX509Context(position, x509Id);
                } else if (e.getClickCount() == 2) {
                    // details
                    showX509Details(x509Id);
                } else if (e.getClickCount() == 1) {
                    createX509ToolBar(x509Id);
                }
            }
        });
        TableCellRenderer renderer = new CustomTableCellRenderer();
        try {
            x509Table.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch(ClassNotFoundException ex) {}

        x509Table.setFillsViewportHeight(false);
        x509Table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        
        JScrollPane scrollPane = new JScrollPane(x509Table);
        mainPanel.add(scrollPane, "grow");
        return mainPanel;
    }


    /**
     * Creates the server tab
     *
     * @return a panel representing the whole server tab
     */
    private Component createServerTab() {
        JPanel serverPanel = new JPanel(new MigLayout("insets 0, fill"));

        // create the server table
        serverTable = new JTable(new ServerOverviewTableModel());
        JTableHeader header = serverTable.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(((ServerOverviewTableModel) serverTable.getModel()).new ColumnListener(serverTable));
        serverTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent e) {
                Point position = new Point(e.getX(), e.getY());
                int row = serverTable.rowAtPoint(position);
                serverTable.setRowSelectionInterval(row, row);
                ServerOverviewTableModel model = (ServerOverviewTableModel) serverTable.getModel();
                // get the serverId from the mapping in the tableModel
                String serverId = (String) model.getIdRowMapping().get(row + "");

                if (e.getButton() != MouseEvent.BUTTON1) {
                    // context menue
                    showServerContext(position, serverId);
                } else if (e.getClickCount() == 2) {
                    // details
                    showServerDetails(serverId);
                } else if (e.getClickCount() == 1) {
                    createServerToolBar(serverId);
                }
            }
        });
        TableCellRenderer renderer = new CustomTableCellRenderer();
        try {
            serverTable.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch(ClassNotFoundException ex) {}

        serverTable.setFillsViewportHeight(false);
        serverTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(serverTable);
        serverPanel.add(scrollPane, "grow");

        return serverPanel;
    }

    /**
     * Creates the user tab
     *
     * @return a panel representing the whole user tab
     */
    private Component createUserTab() {
        JPanel userPanel = new JPanel(new MigLayout("insets 2, fill"));

        // create the user table
        userTable = new JTable(new UserOverviewTableModel());
        JTableHeader header = userTable.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(((UserOverviewTableModel) userTable.getModel()).new ColumnListener(userTable));

        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Point position = new Point(e.getX(), e.getY());
                int row = userTable.rowAtPoint(position);
                userTable.setRowSelectionInterval(row, row);
                UserOverviewTableModel model = (UserOverviewTableModel) userTable.getModel();
                // get the userid from the mapping in the tableModel
                String userId = (String) model.getIdRowMapping().get(row + "");

                if (e.getButton() != MouseEvent.BUTTON1) {
                    // context menue
                    showUserContext(position, userId);
                } else if (e.getClickCount() == 2) {
                    // details
                    showUserDetails(userId);
                } else if (e.getClickCount() == 1) {
                    createUserToolBar(userId);
                }
            }
        });
        TableCellRenderer renderer = new CustomTableCellRenderer();
        try {
            userTable.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch(ClassNotFoundException ex) {}

        userTable.setFillsViewportHeight(false);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(userTable);
        userPanel.add(scrollPane, "grow");

        return userPanel;
    }

    /**
     * Creates the CRL tab
     *
     * @return a panel representing the whole CRL tab
     */
    private Component createCRLTab() {
        JPanel crlPanel = new JPanel(new MigLayout("insets 0, fill"));

        // retrieve data from database
        String[] details = CRLQueries.getCrlDetails();
        // CRL entries
        Vector<String[]> entries = CRLQueries.getCRLEntries(details[0]);
        
        CRLOverviewTableModel model = new CRLOverviewTableModel();
        model.setRowData(entries);
        
        // create the crl table
        crlTable = new JTable(model);
        JTableHeader header = crlTable.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(((CRLOverviewTableModel) crlTable.getModel()).new ColumnListener(crlTable));
        
        TableCellRenderer renderer = new CustomTableCellRenderer();
        try {
            crlTable.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch(ClassNotFoundException ex) {}

        crlTable.setFillsViewportHeight(false);
        crlTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(crlTable);
        crlPanel.add(scrollPane, "grow");
        
        CssRuleManager.getInstance().format(crlPanel);

        return crlPanel;
    }

    /**
     * Creates the user and server tab
     *
     * @return a panel representing the whole user and server tab
     */
    private Component createServerAndUserTab() {
        JPanel mainServerAndUserPanel = new JPanel(new MigLayout("insets 0, fill"));
        
        serverUserTreeModel = ServerUserTreeModel.getInstance();
        serverUserTree = serverUserTreeModel.getServerUserTree();
        if (TreeConfiguration.DRAG_USERS_TO_GROUPS)
            serverUserTree.setDragEnabled(true);
        serverUserTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        serverUserTree.setDropMode(DropMode.USE_SELECTION);
        
        // add an empty panel
        serverUserDetailsScrollPane = new JScrollPane(new JPanel());
        CssRuleManager.getInstance().format(serverUserDetailsScrollPane);
        
        // add the tree
        serverUserSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(serverUserTree), serverUserDetailsScrollPane);
        
        serverUserSplitPane.setOneTouchExpandable(true);
        serverUserSplitPane.setDividerLocation(serverUserTreeDividerLocation);
        
        JPanel searchPanel = new JPanel(new MigLayout("insets 0, fillx"));
        final JTextField searchField = new JTextField(15);
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                serverUserTreeModel.reload(searchField.getText());
                serverUserSplitPane.repaint();
                serverUserSplitPane.getParent().repaint();
            }
        });
        
        // add the filter panel
        searchPanel.add(new JLabel(rb.getString("serverUserTree.filterText")), "");
        searchPanel.add(searchField, "span, growx");

        mainServerAndUserPanel.add(serverUserSplitPane, "span, grow");
        mainServerAndUserPanel.add(searchPanel, "cell 0 1");
        return mainServerAndUserPanel;
    }

    /**
     * Update the server<->user details panel
     * @param panel
     */
    public static void updateServerUserDetails(JPanel panel) {
        serverUserSplitPane.remove(serverUserDetailsScrollPane);
        if (panel != null) {
            serverUserDetailsScrollPane = new JScrollPane(panel);
            serverUserSplitPane.add(serverUserDetailsScrollPane);
        }
        serverUserSplitPane.setDividerLocation(serverUserSplitPane.getDividerLocation());
        serverUserSplitPane.repaint();
        serverUserSplitPane.getParent().repaint();
    }
    
    /**
     * Update the server<->user details panel
     * @param panel The panel to update
     */
    public static void updateServerUserDetails(JTabbedPane panel) {
        serverUserSplitPane.remove(serverUserDetailsScrollPane);
        if (panel != null) {
        	CssRuleManager.getInstance().format(panel);
            serverUserDetailsScrollPane = new JScrollPane(panel);
            serverUserSplitPane.add(serverUserDetailsScrollPane);
        }
        serverUserSplitPane.setDividerLocation(serverUserSplitPane.getDividerLocation());
        serverUserSplitPane.repaint();
        serverUserSplitPane.getParent().repaint();
    }
    

    public void createBottomPanel() {
        bottomPanel = new JPanel(new MigLayout("fillx"));
        bottomPanel.add(createPrintButton(), "");
        bottomPanel.add(createStatusBar(), "width " + (mainWidth - 30));
        bottomPanel.add(createThreadBar(), "align right");
        bottomPanel.setVisible(true);
    }

    /**
     * Shows a context menu in the user tree for a selected user
     *
     * @param position The mouse position
     * @param userId  The userId
     */
    public static void showUserContextFromTree(Point position, String userId) {
        Vector<String> userIds = new Vector<String>();
        userIds.add(userId);
        
        showMultipleUsersContextFromTree(position, userIds);
    }

    /**
     * Shows a context menu in the user tree for multiple selected users
     *
     * @param position The mouse position
     * @param userIds  The userIds
     */
    public static void showMultipleUsersContextFromTree(Point position, final Vector<String> userIds) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem deleteMenu = new JMenuItem(rb.getString("user.details.deletebutton"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("user.dialog.delete.answer_yes"),
                        rb.getString("user.dialog.delete.answer_no")
                };
                String text;
                if (userIds.size() > 1)
                    text = rb.getString("user.dialog.delete_multiple.text");
                else
                    text = rb.getString("user.dialog.delete.text");
                int answer = CustomJOptionPane.showOptionDialog(
                        ManagerGUI.mainFrame,
                        text,
                        rb.getString("user.dialog.delete.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                // delete all selected users
                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        for (Iterator<String> iter = userIds.iterator(); iter.hasNext();) {
                            String userId = (String) iter.next();
                            UserAction.deleteUser(userId);
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "error deleting users", e);
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("user.dialog.delete_multiple.errortext"),
                                rb.getString("user.dialog.delete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                    
                    serverUserTreeModel.reload();
                    serverUserSplitPane.repaint();
                    serverUserSplitPane.getParent().repaint();

                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();
                }

            }
        });
        
        JMenuItem x509MgmtMenu = new JMenuItem();
        x509MgmtMenu.setText(rb.getString("userContextMenu.certmgmt"));
        if (userIds.size() > 1)
            x509MgmtMenu.setEnabled(false);
        else {
            x509MgmtMenu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showX509UserManager(userIds.get(0));
                }
            });
        }

        contextMenu.add(deleteMenu);
        contextMenu.add(x509MgmtMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }

    /**
     * Shows a context menu in the user tree for the top user node
     *
     * @param position The mouse position
     */
    public static void showTopUserContextFromTree(Point position) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem newUserMenuItem = new JMenuItem(rb.getString("actionMenu.newUser.text"));
        newUserMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UserDetails uDetails = new UserDetails(null, null);
                JPanel detailsPanel = uDetails.createUserDetailsPanel();
                updateServerUserDetails(detailsPanel);
            }
        });
        contextMenu.add(newUserMenuItem);

        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }
    
    /**
     * Shows a context menu in the server tree for the top server node
     *
     * @param position The mouse position
     */
    public static void showTopServerContextFromTree(Point position) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem newServerMenuItem = new JMenuItem(rb.getString("actionMenu.newServer.text"));
        newServerMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServerDetails sDetails = new ServerDetails(null, null);
                JTabbedPane detailsPanel = sDetails.createServerDetailsPanel();
                updateServerUserDetails(detailsPanel);
            }
        });
        contextMenu.add(newServerMenuItem);

        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }
    
    /**
     * Shows a context menu in the server tree for a selected server
     *
     * @param position The mouse position
     * @param serverId  The serverId
     */
    public static void showServerContextFromTree(Point position, final String serverId) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem x509MgmtMenu = new JMenuItem();
        x509MgmtMenu.setText(rb.getString("serverContextMenu.certmgmt"));
        x509MgmtMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showX509ServerManager(serverId);
            }
        });

        JMenuItem syncMenu = new JMenuItem();
        syncMenu.setText(rb.getString("serverContextMenu.sync"));
        syncMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncUsersClicked(serverId);
            }
        });

        JMenuItem deleteMenu = new JMenuItem(rb.getString("serverContextMenu.delete"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Dialogs.showDeleteServerDialog(ManagerGUI.mainFrame, serverId);
            }
        });


        Server s = new Server(serverId);
        final Server server = ServerDAO.getInstance().read(s);
        JMenuItem ccMenu = new JMenuItem();
        ccMenu.setText(rb.getString("serverContextMenu.cc"));
        ccMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openCCTab(server);
            }
        });
        
        JMenuItem startVpnMenu = new JMenuItem();
        startVpnMenu.setText(rb.getString("serverContextMenu.startVpn"));
        startVpnMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Dialogs.showOpenVpnStartDaemonDialog(ManagerGUI.mainFrame,server);
                } catch (Exception ex) {
                    logger.warning("showOpenVpnStartDaemonDialog throwed: " + ex.toString());
                }
            }
        });

        contextMenu.add(deleteMenu);
        contextMenu.add(x509MgmtMenu);
        contextMenu.add(syncMenu);
        if (Configuration.getInstance().CC_ENABLED) {
            if (!openCCTabs.containsKey(server.getHostname())) {
                contextMenu.add(ccMenu);
            } else {
                ccMenu.setEnabled(false);
                contextMenu.add(ccMenu);
            }
        }
        contextMenu.add(startVpnMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }

    /**
     * Shows a context menu in the server tree for multiple selected servers
     *
     * @param position The mouse position
     * @param serverIds  The serverIds
     */
    public static void showMultipleServersContextFromTree(Point position, final Vector<String> serverIds) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem deleteMenu = new JMenuItem(rb.getString("server.details.deletebutton"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("server.dialog.delete.answer_yes"),
                        rb.getString("server.dialog.delete.answer_no")
                };
                String text;
                if (serverIds.size() > 1)
                    text = rb.getString("server.dialog.delete_multiple.text");
                else
                    text = rb.getString("server.dialog.delete.text");
                int answer = CustomJOptionPane.showOptionDialog(
                        ManagerGUI.mainFrame,
                        text,
                        rb.getString("server.dialog.delete.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                // delete all selected servers
                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        for (Iterator<String> iter = serverIds.iterator(); iter.hasNext();) {
                            String serverId = (String) iter.next();
                            ServerAction.deleteServer(serverId);
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "error deleting servers", e);
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("server.dialog.delete_multiple.errortext"),
                                rb.getString("server.dialog.delete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                    
                    serverUserTreeModel.reload();
                    serverUserSplitPane.repaint();
                    serverUserSplitPane.getParent().repaint();

                    ManagerGUI.refreshServerTable();
                    ManagerGUI.refreshX509Table();
                }

            }
        });

        contextMenu.add(deleteMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }

    /**
     * Shows a context menu in the tree for a selected server
     *
     * @param position The mouse position
     * @param serverId  The serverId
     * @param userId  The userId
     */
    public static void showConnectionContextFromTree(Point position, String serverId, String userId) {
        Vector<String> serverIds = new Vector<String>();
        serverIds.add(serverId);
        
        Vector<String> userIds = new Vector<String>();
        userIds.add(userId);
        
        showMultipleConnectionsContextFromTree(position, serverIds, userIds);
    }

    /**
     * Shows a context menu in the tree for multiple selected servers
     *
     * @param position The mouse position
     * @param serverIds  The serverIds
     * @param userId  The userId
     */
    public static void showMultipleConnectionsContextFromTree(Point position, final Vector<String> serverIds, 
            final String userId) {
        Vector<String> userIds = new Vector<String>();
        userIds.add(userId);
        
        showMultipleConnectionsContextFromTree(position, serverIds, userIds);
    }
    
    /**
     * Shows a context menu in the tree for multiple selected users
     *
     * @param position The mouse position
     * @param serverId  The serverId
     * @param userIds  The userIds
     */
    public static void showMultipleConnectionsContextFromTree(Point position, final String serverId, 
            final Vector<String> userIds) {
        Vector<String> serverIds = new Vector<String>();
        serverIds.add(serverId);
        
        showMultipleConnectionsContextFromTree(position, serverIds, userIds);
    }

    /**
     * Shows a context menu in the tree for deleting the assignment of multiple selected servers or users
     * One of the Vectors with IDs has to be either only one userID or one serverID
     *
     * @param position The mouse position
     * @param serverIds  The serverIds
     * @param userIds  The userIds
     */
    private static void showMultipleConnectionsContextFromTree(Point position, final Vector<String> serverIds, 
            final Vector<String> userIds) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem deleteMenu = new JMenuItem(rb.getString("serverUserTree.userTree.serverConnectionDelete.text"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (userIds.size() == 1) {
                        for (Iterator<String> iter = serverIds.iterator(); iter.hasNext();) {
                            String serverId = (String) iter.next();
                            UserQueries.removeUserFromServer(userIds.get(0), serverId);
                        }    
                    } else if (serverIds.size() == 1) {
                        for (Iterator<String> iter = userIds.iterator(); iter.hasNext();) {
                            String userId = (String) iter.next();
                            UserQueries.removeUserFromServer(userId, serverIds.get(0));
                        }    
                    } else {
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("serverUserTree.userTree.serverConnectionDelete.illegalChoice"),
                                rb.getString("serverUserTree.userTree.serverConnectionDelete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error deleting connection between servers and user", e);
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                            rb.getString("serverUserTree.userTree.serverConnectionDelete.errortext"),
                            rb.getString("serverUserTree.userTree.serverConnectionDelete.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
                
                serverUserTreeModel.reload();
                serverUserSplitPane.repaint();
                serverUserSplitPane.getParent().repaint();
            }
        });

        contextMenu.add(deleteMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverUserTree, position.x + 10, position.y);
    }

    /**
     * Reloads the server<->user tree
     */
    public static void reloadServerUserTree() {
        serverUserTreeModel.reload();
        serverUserSplitPane.repaint();
        serverUserSplitPane.getParent().repaint();
    }

    private JPanel createPrintButton() {
        printPanel = new JPanel(new MigLayout());
        JButton printButton = new JButton(ImageUtils.createImageIcon(Constants.ICON_PRINT, rb.getString("print.button")));
        printButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    switch (tabs.getSelectedIndex()) {
                        case 0: x509Table.print(); break;
                        case 1: serverTable.print(); break;
                        case 2: userTable.print(); break;
                        case 3: PrintUtils.printTreeAsTable(serverUserTree); break;
                        case 4: crlTable.print(); break;
                        default: break;
                    }
                    
                    BufferedImage image = new BufferedImage(serverUserTree.getWidth(), serverUserTree.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    serverUserTree.paint(g);
                    g.dispose();

                } catch (Exception e1) {
                    new VisualException(rb.getString("print.error.text"), rb.getString("print.error.title"));
                }
            }
        });
        printButton.setToolTipText(rb.getString("print.button_tt"));
        printButton.setSize(20, 20);
        printPanel.add(printButton, "width 30!");
        return printPanel;
    }

    /**
     * Creates a status bar at the bottom of the main frame
     */
    private static JPanel createStatusBar() {
        statusPanel = new JPanel(new MigLayout());
        statusPanel.setSize(mainWidth, 10);

        String iconPath = null;
        String toolTip = rb.getString("statusBar.button.tooltip");
        int type = StatusMessage.TYPE_INFO;
        if (!statusMessages.isEmpty())
            type = statusMessages.lastElement().getType();
        switch (type) {
            case StatusMessage.TYPE_CONFIRM: iconPath = Constants.ICON_CLOSE_PATH; break;
            case StatusMessage.TYPE_INFO: iconPath = Constants.ICON_INFO_PATH; break;
            case StatusMessage.TYPE_ERROR: iconPath = Constants.ICON_ERROR_PATH; break;
            default: iconPath = Constants.ICON_INFO_PATH; break;
        }
        
        final JButton closeButton = new JButton();
        closeButton.setIcon(
                ImageUtils.createImageIcon(iconPath, "close")
        );
        closeButton.setToolTipText(toolTip);
        closeButton.setSize(20, 20);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StatusMessage nextMessage = removeCurrentStatusMessage();
                
                if (nextMessage != null) {
                    String iconPath = null;
                    String toolTip = rb.getString("statusBar.button.tooltip");

                    int type = nextMessage.getType();
                    switch (type) {
                        case StatusMessage.TYPE_CONFIRM: iconPath = Constants.ICON_CLOSE_PATH; break;
                        case StatusMessage.TYPE_INFO: iconPath = Constants.ICON_INFO_PATH; break;
                        case StatusMessage.TYPE_ERROR: iconPath = Constants.ICON_ERROR_PATH; break;
                        default: iconPath = Constants.ICON_INFO_PATH; break;
                    }
                    closeButton.setIcon(ImageUtils.createImageIcon(iconPath, "close"));
                    closeButton.setToolTipText(toolTip);
                }
            }
            
        });
        statusTextLabel = new JLabel("");
        statusTextLabel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                StatusMessage nextMessage = removeCurrentStatusMessage();
                
                if (nextMessage != null) {
                    String iconPath = null;
                    String toolTip = rb.getString("statusBar.button.tooltip");

                    int type = nextMessage.getType();
                    switch (type) {
                        case StatusMessage.TYPE_CONFIRM: iconPath = Constants.ICON_CLOSE_PATH; break;
                        case StatusMessage.TYPE_INFO: iconPath = Constants.ICON_INFO_PATH; break;
                        case StatusMessage.TYPE_ERROR: iconPath = Constants.ICON_ERROR_PATH; break;
                        default: iconPath = Constants.ICON_INFO_PATH; break;
                    }
                    closeButton.setIcon(ImageUtils.createImageIcon(iconPath, "close"));
                    closeButton.setToolTipText(toolTip);
                }
            }
        });    
        statusPanel.add(statusTextLabel);
        statusPanel.add(closeButton, "width 24!");
        statusPanel.setVisible(false);

        return statusPanel;
    }

    /**
     * Removes the current status message from gui
     * @return The next status message from stack 
     */
    private static StatusMessage removeCurrentStatusMessage() {
        StatusMessage nextMessage = null;
        
        if (statusMessages.isEmpty())
            statusPanel.setVisible(false);
        else {
            statusMessages.remove(statusMessages.size()-1);
        }

        if (statusMessages.isEmpty())
            statusPanel.setVisible(false);
        else {
            nextMessage = statusMessages.lastElement();
            String display = "(" + statusMessages.size() + ") " + nextMessage;
            statusTextLabel.setText(display);
        }
        
        return nextMessage;
    }

    /**
     * Adds a message to the status bar
     *
     * @param message The message to display
     */
    public static void addStatusMessage(StatusMessage message) {
        statusMessages.add(message);

        if (statusPanel != null && statusTextLabel != null) {
            String display = "(" + statusMessages.size() + ") " + message;
            statusTextLabel.setText(display);
            statusPanel.setVisible(true);
            bottomPanel.setVisible(true);
            bottomPanel.validate();
        }
    }

    /**
     * Creates a small animated gif, displaying that processes are
     * running in background
     */
    private static JPanel createThreadBar() {
        threadPanel = new JPanel(new MigLayout("insets 0"));
        threadPanel.setSize(mainWidth, 10);

        ImageIcon icon = ImageUtils.createImageIcon(Constants.ICON_WAITING_PATH, "waiting ...");
        waitingImageLabel = new JLabel();
        waitingImageLabel.setIcon(icon);
        waitingImageLabel.setToolTipText(rb.getString("statusBar.activethreads.tooltip"));

        threadPanel.add(waitingImageLabel, "width 15!");
        threadPanel.setVisible(false);
        bottomPanel.validate();

        return threadPanel;
    }

    /**
     * Updates the tooltip for the animated gif
     *
     * @param message The message to display
     */
    public static void setWaitingToolTip(String message) {
        waitingImageLabel.setToolTipText(message);
        waitingImageLabel.validate();
    }

    /**
     * Display the animated gif for displaying running processes
     */
    public static void setThreadRunning() {
        threadPanel.setVisible(true);
        bottomPanel.setVisible(true);
        bottomPanel.validate();
    }

    /**
     * Hide the animated gif for displaying running processes
     */
    public static void unsetThreadRunning() {
        threadPanel.setVisible(false);
        bottomPanel.validate();
    }

    /**
     * Hides the print panel, if printing is not available
     */
    private static void invisiblePrintPanel() {
        printPanel.setVisible(false);
    }
    
    /**
     * Shows the print panel
     */
    private static void showPrintPanel() {
        printPanel.setVisible(true);
    }

    /**
     * creates the menu bar
     *
     * @return JMenuBar the menu bar
     */
    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu();
        fileMenu.setText(rb.getString("fileMenu.text"));

        JMenu helpMenu = new JMenu();
        helpMenu.setText(rb.getString("helpMenu.text"));

        JMenu actionMenu = new JMenu();
        actionMenu.setText(rb.getString("actionMenu.text"));

        JMenuItem exitMenuItem = new JMenuItem();
        JMenuItem printMenuItem = new JMenuItem("print");
        JMenuItem aboutMenuItem = new JMenuItem();
        JMenuItem supportMenuItem = new JMenuItem();
        JMenuItem newUserMenuItem = new JMenuItem();
        JMenuItem newServerMenuItem = new JMenuItem();
        JMenuItem newGroupMenuItem = new JMenuItem();
        JMenuItem importMenuItem = new JMenuItem();
        final JCheckBoxMenuItem crlMenuItem = new JCheckBoxMenuItem(
                rb.getString("actionMenu.crl.text"), 
                Configuration.getInstance().GUI_SHOW_CRL_TAB);
        final JCheckBoxMenuItem crX509MenuItem = new JCheckBoxMenuItem(
        		rb.getString("actionMenu.crX509.text"),
                Configuration.getInstance().GUI_SHOW_CR_X509);
        JMenuItem dbResetMenuItem = new JMenuItem();
        JMenuItem dbSnapshotMenuItem = new JMenuItem();
        JMenuItem crlClearMenuItem = new JMenuItem();
        JMenuItem exportMenuItem = new JMenuItem();
        JMenuItem configMenuItem = new JMenuItem();
        JMenuItem x509ConfigMenuItem = new JMenuItem();
        JMenuItem syncBatchMenuItem = new JMenuItem();
        JMenuItem updateSettingsMenuItem = new JMenuItem();
        updateMenuItem = new JMenuItem();

        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ThreadMgmt.getInstance().areThreadsRunning()) {
                    Dialogs.showExitDialogWithActiveThreads();
                } else {
                    Dialogs.showExitDialog();
                }
            }
        });
        exitMenuItem.setText(rb.getString("fileMenu.exit.text"));

        configMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dialogs.showConfigurationDialog(mainFrame);
            }
        });
        configMenuItem.setText(rb.getString("actionMenu.configuration.text"));

        x509ConfigMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dialogs.showX509ConfigurationDialog(mainFrame);
            }
        });
        x509ConfigMenuItem.setText(rb.getString("actionMenu.x509configuration.text"));

        fileMenu.add(configMenuItem);
        fileMenu.add(x509ConfigMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(importMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitMenuItem);
        
        printMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    x509Table.print();
                } catch (PrinterException e1) {
                    e1.printStackTrace();
                }
            }
        });
        printMenuItem.setText("print");

        aboutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dialogs.showAboutBox(mainFrame);
            }
        });
        aboutMenuItem.setText(rb.getString("helpMenu.about.text"));


        updateSettingsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dialogs.showUpdateConfigurationDialog(mainFrame);
            }
        });
        updateSettingsMenuItem.setText(rb.getString("helpMenu.updatesettings.text"));

        updateMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // look for updates
                updateMgmt.searchForUpdates();
            }
        });
        updateMenuItem.setText(rb.getString("helpMenu.update.text"));
        if (!updateMgmt.isUpdateKeystoreExisting())
            updateMenuItem.setEnabled(false);
        
        supportMenuItem.setText(rb.getString("helpMenu.support.text"));
        supportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                supportForm = new SupportForm();
                supportForm.show();
            }
        });

        helpMenu.add(updateSettingsMenuItem);
        helpMenu.add(updateMenuItem);
        helpMenu.add(aboutMenuItem);


        newUserMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showUserDetails("-1");
            }
        });
        newUserMenuItem.setText(rb.getString("actionMenu.newUser.text"));


        newServerMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServerDetails serverDetailsFrame = new ServerDetails(mainFrame, "-1");
                serverDetailsFrame.showServerDetailsFrame();
            }
        });
        newServerMenuItem.setText(rb.getString("actionMenu.newServer.text"));
        
        newGroupMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GroupDetails groupDetailsFrame = new GroupDetails(mainFrame);
                groupDetailsFrame.showDetails();
            }
        });
        newGroupMenuItem.setText(rb.getString("actionMenu.newGroup.text"));


        importMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dialogs.showImportConfigurationDialog(mainFrame);
                ManagerGUI.refreshX509Table();
            }
        });
        importMenuItem.setText(rb.getString("actionMenu.import.text"));


        crlMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Configuration.getInstance().setGuiShowCRL(crlMenuItem.getState());
                if (crlMenuItem.getState()) {
                    openCRLTab();
                } else {
                    closeCRLTab();
                }
            }
        });

        crX509MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Configuration.getInstance().setGuiShowCRX509(crX509MenuItem.getState());
                ManagerGUI.refreshX509Table();
            }
        });
        

        dbResetMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("dialog.dbreset.answer_yes"),
                        rb.getString("dialog.dbreset.answer_no")
                };
                int answer = CustomJOptionPane.showOptionDialog(
                        mainFrame,
                        rb.getString("dialog.dbreset.text"),
                        rb.getString("dialog.dbreset.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                // delete
                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        boolean keepConfiguration = true;
                        DBTasks.resetDB(keepConfiguration);

                        ManagerGUI.refreshAllTables();
                    } catch (Exception ex) {
                        new VisualException(ex.getMessage());
                    }
                }

            }
        });
        dbResetMenuItem.setText(rb.getString("actionMenu.dbreset.text"));

        dbSnapshotMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("dialog.dbsnapshot.answer_yes"),
                        rb.getString("dialog.dbsnapshot.answer_no")
                };
                int answer = CustomJOptionPane.showOptionDialog(
                        mainFrame,
                        rb.getString("dialog.dbsnapshot.text"),
                        rb.getString("dialog.dbsnapshot.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        DBConnector.backupDatabase(Configuration.getInstance().JDBC_PATH);
                    } catch (Exception ex) {
                        new VisualException(ex.getMessage());
                    }
                }

            }
        });
        dbSnapshotMenuItem.setText(rb.getString("actionMenu.dbsnapshot.text"));


        crlClearMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                Object[] options = {
                        rb.getString("dialog.crlclear.answer_yes"),
                        rb.getString("dialog.crlclear.answer_no")
                };
                int answer = CustomJOptionPane.showOptionDialog(
                        mainFrame,
                        rb.getString("dialog.crlclear.text"),
                        rb.getString("dialog.crlclear.title"),
                        CustomJOptionPane.YES_NO_OPTION,
                        CustomJOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options,
                        options[0]);

                // delete
                if (answer == CustomJOptionPane.YES_OPTION) {
                    try {
                        DBTasks.clearCrl();

                        ManagerGUI.refreshAllTables();
                    } catch (Exception ex) {
                        new VisualException(ex.getMessage());
                    }
                }

            }
        });

        crlClearMenuItem.setText(rb.getString("actionMenu.crlclear.text"));

        exportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                X509Action.exportAllCertificatesToFilesystem();
            }
        });
        exportMenuItem.setText(rb.getString("actionMenu.export.text"));
        
        syncBatchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BatchUserSync userSync = BatchUserSync.getInstance();
                userSync.startBatchSync();
            }
        });
        syncBatchMenuItem.setText(rb.getString("actionMenu.batch_sync.text"));

        actionMenu.add(newUserMenuItem);
        actionMenu.add(newServerMenuItem);
        actionMenu.add(new JSeparator());
        actionMenu.add(crlMenuItem);
        actionMenu.add(crX509MenuItem);
        actionMenu.add(new JSeparator());
        actionMenu.add(dbResetMenuItem);
        actionMenu.add(dbSnapshotMenuItem);
        actionMenu.add(crlClearMenuItem);
        actionMenu.add(new JSeparator());
        actionMenu.add(exportMenuItem);
        actionMenu.add(syncBatchMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(actionMenu);
        menuBar.add(helpMenu);

        CssRuleManager.getInstance().format(menuBar);

        return menuBar;
    }

    public static void enableUpdateSearch() {
        updateMenuItem.setEnabled(true);
    }

    /**
     * Shows a context menue in the x509 table
     *
     * @param position The mouse position
     * @param x509Id   The x509Id
     */
    private void showX509Context(Point position, final String x509Id) {
        JPopupMenu contextMenu = new JPopupMenu();

        int x509id = Integer.parseInt(x509Id);
        X509 x509a = new X509(x509id);
        final X509 x509 = X509DAO.getInstance().read(x509a);
        boolean isCertificateRevoked = false;
        try {
            isCertificateRevoked = CRLQueries.isCertificateRevoked(x509.getSerial());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if certificate is revoked", e);
        }

        JMenuItem detailsMenu = new JMenuItem();
        detailsMenu.setText(rb.getString("x509ContextMenu.details"));
        detailsMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showX509Details(x509Id);
            }
        });

        final JMenuItem withdrawMenu = new JMenuItem();
        withdrawMenu.setText(rb.getString("x509ContextMenu.withdraw"));
        withdrawMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Utils.revokeCertificate(x509);
                } catch (Exception e1) {
                    new VisualException(rb.getString("error.revocation"));
                }
                refreshX509Table();
                refreshCrlTable();
            }
        });
        final JMenuItem enableMenu = new JMenuItem();
        enableMenu.setText(rb.getString("x509ContextMenu.enable"));
        enableMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Utils.reEnableCertificate(x509);
                } catch (Exception e1) {
                    new VisualException(rb.getString("error.revocation"));
                }
                refreshX509Table();
                refreshCrlTable();
            }
        });
        final JMenuItem renewMenu = new JMenuItem();
        renewMenu.setText(rb.getString("x509ContextMenu.renew"));
        renewMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Utils.renewCertificate(x509);
                } catch (Exception e1) {
                    new VisualException(rb.getString("error.renew"));
                }
            }
        });
        final JMenuItem enableUserMenu = new JMenuItem();
        enableUserMenu.setText(rb.getString("x509ContextMenu.enableUser"));
        enableUserMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Utils.reEnableCertificate(x509);
                } catch (Exception e1) {
                    new VisualException(rb.getString("error.revocation"));
                }
                // call create new user function
                showUserDetails( "-1", X509Queries.getX509Username(Integer.toString(x509.getX509id())),
                				Integer.toString(x509.getX509id()));
                
                refreshX509Table();
                refreshCrlTable();
            }
        });
        
        
        contextMenu.add(detailsMenu);
        if (x509.getType() == X509.X509_TYPE_CLIENT || x509.getType() == X509.X509_TYPE_PKCS12)
            if (isCertificateRevoked) {
                contextMenu.add(enableMenu);
                contextMenu.add(enableUserMenu);
            } else {
                contextMenu.add(renewMenu);
                contextMenu.add(withdrawMenu);
            }
        // for now we special case the Server, since I only want to deal with !revoked certificates
        if (x509.getType() == X509.X509_TYPE_SERVER)
            if (!isCertificateRevoked)
                contextMenu.add(renewMenu);
                contextMenu.add(withdrawMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(x509Table, position.x + 10, position.y);
    }

    /**
     * Shows a context menue in the server table
     *
     * @param position The mouse position
     * @param serverId The serverId
     */
    private void showServerContext(Point position, final String serverId) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem x509MgmtMenu = new JMenuItem();
        x509MgmtMenu.setText(rb.getString("serverContextMenu.certmgmt"));
        x509MgmtMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showX509ServerManager(serverId);
            }
        });

        JMenuItem syncMenu = new JMenuItem();
        syncMenu.setText(rb.getString("serverContextMenu.sync"));
        syncMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncUsersClicked(serverId);
            }
        });

        JMenuItem detailsMenu = new JMenuItem();
        detailsMenu.setText(rb.getString("serverContextMenu.details"));
        detailsMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showServerDetails(serverId);
            }
        });

        JMenuItem deleteMenu = new JMenuItem(rb.getString("serverContextMenu.delete"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Dialogs.showDeleteServerDialog(ManagerGUI.mainFrame, serverId);
            }
        });


        Server s = new Server(serverId);
        final Server server = ServerDAO.getInstance().read(s);
        JMenuItem ccMenu = new JMenuItem();
        ccMenu.setText(rb.getString("serverContextMenu.cc"));
        ccMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openCCTab(server);
            }
        });
        
        JMenuItem startVpnMenu = new JMenuItem();
        startVpnMenu.setText(rb.getString("serverContextMenu.startVpn"));
        startVpnMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Dialogs.showOpenVpnStartDaemonDialog(ManagerGUI.mainFrame,server);
                } catch (Exception ex) {
                    logger.warning("showOpenVpnStartDaemonDialog throwed: " + ex.toString());
                }
            }
        });

        contextMenu.add(detailsMenu);
        contextMenu.add(deleteMenu);
        contextMenu.add(x509MgmtMenu);
        contextMenu.add(syncMenu);
        if (Configuration.getInstance().CC_ENABLED) {
            if (!openCCTabs.containsKey(server.getHostname())) {
                contextMenu.add(ccMenu);
            } else {
                ccMenu.setEnabled(false);
                contextMenu.add(ccMenu);
            }
        }
        contextMenu.add(startVpnMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(serverTable, position.x + 10, position.y);
    }

    /**
     * Opens a ControlCenterTab for the given server
     *
     * @param server The server
     */
    private static void openCCTab(Server server) {
        ControlCenterTab ccTab = new ControlCenterTab(server);
        tabs.add(server.getName(), ccTab.createCCTab());
        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        openCCTabs.put(server.getHostname(), ccTab);
    }
    
    
    /**
     * Opens the CRL tab
     */
    private void openCRLTab() {
        tabs.add(rb.getString("tab.crl.name"), createCRLTab());
        crlTabIndex = tabs.getTabCount() - 1;
    }
    
    /**
     * Closed the CRL tab
     */
    private static void closeCRLTab() {
        if (crlTabIndex > -1)
            tabs.remove(crlTabIndex);
        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        crlTabIndex = -1;
    }

    /**
     * Shows a context menue in the user table
     *
     * @param position The mouse position
     * @param userId   The userId
     */
    private void showUserContext(Point position, final String userId) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem detailsMenu = new JMenuItem();
        detailsMenu.setText(rb.getString("userContextMenu.details"));
        detailsMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showUserDetails(userId);
            }
        });

        JMenuItem deleteMenu = new JMenuItem(rb.getString("user.details.deletebutton"));
        deleteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
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
                        UserAction.deleteUser(userId);
                    } catch (Exception e) {
                        // show error dialog
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("user.dialog.delete.errortext"),
                                rb.getString("user.dialog.delete.errortitle"),
                                CustomJOptionPane.ERROR_MESSAGE);
                    }

                    ManagerGUI.reloadServerUserTree();
                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();
                }

            }
        });

        JMenuItem x509MgmtMenu = new JMenuItem();
        x509MgmtMenu.setText(rb.getString("userContextMenu.certmgmt"));
        x509MgmtMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showX509UserManagerFrame(userId);
            }
        });

        contextMenu.add(detailsMenu);
        contextMenu.add(deleteMenu);
        contextMenu.add(x509MgmtMenu);
        CssRuleManager.getInstance().format(contextMenu);
        contextMenu.show(userTable, position.x + 10, position.y);
    }

    /**
     * synchronizes the users with the selected server
     */
    private static void syncUsersClicked(String serverId) {

        if (serverId != null && !"".equals(serverId)) {
            try {
                new UserSync(serverId);
            } catch (ConnectException ce) {
                CustomJOptionPane.showMessageDialog(mainFrame,
                        ce.getMessage(),
                        rb.getString("error.syncusers.title"),
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, ce.getMessage(), ce);
            } catch (Exception ex) {
                CustomJOptionPane.showMessageDialog(mainFrame,
                        ex.getMessage(),
                        rb.getString("error.syncusers.title"),
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else {
            CustomJOptionPane.showMessageDialog(mainFrame,
                    rb.getString("server.dialog.select.text"),
                    rb.getString("server.dialog.select.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Displays details of a x509 in a new frame
     *
     * @param id The ID of the x509 that is to show
     */
    private void showX509Details(String id) {
        X509Details x509DetailsFrame = new X509Details(mainFrame, id);
        x509DetailsFrame.showDetails();
    }

    /**
     * Displays details of a server in a new frame
     *
     * @param id The ID of the server that is to show
     */
    private void showServerDetails(String id) {
        ServerDetails serverDetailsFrame = new ServerDetails(mainFrame, id);
        serverDetailsFrame.showServerDetailsFrame();
    }

    /**
     * Displays details of a user in a new frame
     *
     * @param id The ID of the user that is to show
     */
    private void showUserDetails(String id) {
        UserDetails userDetailsFrame = new UserDetails(mainFrame, id);
        userDetailsFrame.showUserDetailsFrame();
    }
    
    
    /**
     * Displays details of a (new) user in a new frame
     *
     * @param id The ID of the user that is to show
     * @param username The name of the (new) user
     * @param x509id The ID of the certificate object
     */
    private void showUserDetails(String id, String username, String x509id) {
        UserDetails userDetailsFrame = new UserDetails(mainFrame, id, username, x509id);
        userDetailsFrame.showUserDetailsFrame();
    }

    
    /**
     * Displays a certificate manager
     *
     * @param serverId The serverId
     */
    private static void showX509ServerManager(String serverId) {
        if (serverId != null && !"".equals(serverId)) {
            Server server = new Server(serverId);
            server = ServerDAO.getInstance().read(server);

            X509Manager x509Manager = new X509Manager(mainFrame, server);
            JPanel x509Panel = x509Manager.createX509ManagerPanel();
            updateServerUserDetails(x509Panel);
        } else {
            CustomJOptionPane.showMessageDialog(mainFrame,
                    rb.getString("user.dialog.select.text"),
                    rb.getString("user.dialog.select.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }

    }

    /**
     * Displays a certificate manager in a new frame
     *
     * @param userId The userId
     */
    private static void showX509UserManagerFrame(String userId) {
        if (userId != null && !"".equals(userId)) {
            User user = new User(userId);
            user = UserDAO.getInstance().read(user);

            X509Manager x509Manager = new X509Manager(mainFrame, user);
            x509Manager.showX509ManagerFrame();
        } else {
            CustomJOptionPane.showMessageDialog(mainFrame,
                    rb.getString("user.dialog.select.text"),
                    rb.getString("user.dialog.select.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }

    }
    
    /**
     * Displays a certificate manager
     *
     * @param userId The userId
     */
    private static void showX509UserManager(String userId) {
        if (userId != null && !"".equals(userId)) {
            User user = new User(userId);
            user = UserDAO.getInstance().read(user);

            X509Manager x509Manager = new X509Manager(mainFrame, user);
            JPanel x509Panel = x509Manager.createX509ManagerPanel();
            updateServerUserDetails(x509Panel);
        } else {
            CustomJOptionPane.showMessageDialog(mainFrame,
                    rb.getString("user.dialog.select.text"),
                    rb.getString("user.dialog.select.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }

    }

    public static void refreshUserTable() {
        if (userTable != null && userTable.getModel() != null)
            ((UserOverviewTableModel) userTable.getModel()).reloadData();
    }

    public static void refreshServerTable() {
        if (serverTable != null && serverTable.getModel() != null)
            ((ServerOverviewTableModel) serverTable.getModel()).reloadData();
    }

    public static void refreshX509Table() {
        if (x509Table != null && x509Table.getModel() != null)
            ((X509OverviewTableModel) x509Table.getModel()).reloadData();
    }
    
    public static void refreshCrlTable() {
        if (crlTable != null && crlTable.getModel() != null)
            ((CRLOverviewTableModel) crlTable.getModel()).reloadData();
    }

    public static void refreshAllTables() {
        refreshX509Table();
        refreshServerTable();
        refreshUserTable();
        refreshCrlTable();
    }

    public static JTabbedPane getTabs() {
        return tabs;
    }

    public static Hashtable<String, ControlCenterTab> getOpenCCTabs() {
        return openCCTabs;
    }

    public static ControlCenterTab getOpenCCTab(String hostname) {
        if (hostname == null)
            return null;
        ControlCenterTab tab = openCCTabs.get(hostname);
        return tab;
    }

    /**
     * clears the right panel of the main app window, if in tree mode.
     */
    public static void clearRightPanel() {
        JPanel panel = new JPanel();
        CssRuleManager.getInstance().format(panel);
        ManagerGUI.updateServerUserDetails(panel);
    }

}
