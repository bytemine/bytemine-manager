/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.BorderLayout;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.SSHClientTableModel;
import net.bytemine.openvpn.ssh.SSHCommunicator;
import net.bytemine.openvpn.ssh.SSHConnector;
import net.bytemine.openvpn.ssh.SSHConstants;
import net.bytemine.openvpn.ssh.SSHParser;
import net.bytemine.openvpn.ssh.SSHSessionPool;
import net.bytemine.openvpn.ssh.SSHUtils;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.StringUtils;
import net.miginfocom.swing.MigLayout;

import com.jcraft.jsch.Session;


/**
 * GUI class for displaying a control center tab
 *
 * @author Daniel Rauer
 */
public class ControlCenterTab {

    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private Session sshSession;
    private Server server;
    private SSHCommunicator communicator;
    public SSHConnector sshConnection;
    private Timer statusTimer;

    private JPanel ccPanel;
    private JPanel mainPanel;
    private JPanel downPanel;
    private JFrame debugFrame;
    private JTabbedPane channelTabs;
    
    private Hashtable<String, JButton> connectButtons = new Hashtable<String, JButton>();
    private Hashtable<String, JButton> disconnectButtons = new Hashtable<String, JButton>();
    private Hashtable<String, JPanel> channelPanels = new Hashtable<String, JPanel>();
    private Hashtable<JFrame, JTextArea> debugLogs = new Hashtable<JFrame, JTextArea>();
    private Hashtable<String, JTable> clientTables = new Hashtable<String, JTable>();
    private Hashtable<String, JTextArea> logs = new Hashtable<String, JTextArea>();
    private Hashtable<String, JScrollPane> clientTablePanes = new Hashtable<String, JScrollPane>();

    private JTextArea outputField;
    private Dimension commandOutputDialogSize = new Dimension(300,400);

    ControlCenterTab(Server server) {
        this.server = server;
    }


    /**
     * Creates a control center tab for the given server
     *
     * @return a panel representing the whole cc tab
     */
    JPanel createCCTab() {
        ccPanel = new JPanel(new BorderLayout());

        channelTabs = new JTabbedPane();
        channelTabs.setVisible(false);
        mainPanel = new JPanel(new MigLayout("fillx"));
        downPanel = new JPanel(new MigLayout("fillx"));

        final JButton closeButton = new JButton(rb.getString("ccTab.closebutton"));
        closeButton.setToolTipText(rb.getString("ccTab.closebutton_tt"));
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (closeButton.isEnabled()) {
                    Session session = SSHSessionPool.getInstance().getSession(server.getHostname());
                    if (session != null && session.isConnected()) {
                        // close all open channels and the session and wait for it
                        communicator.closeAllChannels();
                    } else
                        // no session, simply close the tab
                        closeTabAndSession();
                }
            }
        });

        mainPanel.add(channelTabs, "align left, gaptop 0, gapleft 0, growx, growy, wrap");
        mainPanel.add(closeButton);

        ccPanel.add(mainPanel, BorderLayout.CENTER);
        ccPanel.add(downPanel, BorderLayout.SOUTH);

        CssRuleManager.getInstance().format(ccPanel);

        // automatically connect to server
        try {
            Session session = SSHSessionPool.getInstance().getSession(server.getHostname());
            if (session == null || !session.isConnected()) {
                sshConnection = new SSHConnector(server);
                sshConnection.createSession();
            }
        } catch (Exception e) {
            new VisualException(e);
        }

        if (Configuration.getInstance().DEBUG_SSH)
            debugFrame = createDebugFrame();

        return ccPanel;
    }


    /**
     * tries to read the available channels
     * is called after the ssh session is established
     */
    public void detectAvailableChannels() {
        communicator = new SSHCommunicator(
                sshSession, server.getWrapperCommand(), this);
        Thread communicatorThread = new Thread(communicator);
        communicatorThread.start();
    }

    
    /**
     * Opens a new dialog for the user to enter a custom command
     *
     * @param channelNumber The channelNumber
     */
    private void openUserCommandDialog(String channelNumber) {
    	// outputfield
    	outputField = new JTextArea("");
    	outputField.setEditable(false);
    	
    	String command = Dialogs.showUserCommandOutputDialog(ManagerGUI.mainFrame,
    			rb.getString("ccTab.command_label"), outputField, this.commandOutputDialogSize);
    	if (!command.equals("")) {
    		boolean forbidden = false;
    		if (command.startsWith("verb ")) {
    			String[] tokens = command.split(" "); 
    			if (StringUtils.isDigit(tokens[1])) {
    				int level = Integer.parseInt(tokens[1]);
    				if (level > SSHConstants.MAX_VERB_LEVEL) {
    					forbidden = true;
    					new VisualException(rb.getString("ccTab.verb_level"));
    				}
    			}
    		}
    		
    		if (!forbidden)
    			communicator.sendUserCommand(channelNumber, command);
    		openUserCommandDialog(channelNumber);
    	}
    }
    
    
    public void updateUserCommandDialog(String line, String channel) {
    	try {
    		outputField.setText(outputField.getText()+line+"\n");
    		outputField.repaint();
    	} catch(NullPointerException e) {
    		System.out.println("wanting to show vpn-output without form: "+e);
    	}
    }
    

    /**
     * Displays the available channels on the channel panel
     *
     * @param availableChannels HashTable with <channelNumber, channelData[]>
     */
    public void displayAvailableChannels(Hashtable<String, String[]> availableChannels) {
        JPanel channelOverviewPanel = new JPanel();
        channelOverviewPanel.setLayout(new MigLayout("fillx"));

        JLabel headline = new JLabel(rb.getString("ccTab.channel_headline"));
        channelOverviewPanel.add(headline, "wrap");

        if (availableChannels != null) {
            List<String> channels = Collections.list(availableChannels.keys());
            Collections.sort(channels);
            channels.stream().map(availableChannels::get).forEach(channelStr -> {
                final String channelNumber = channelStr[0];
                final String channelType = channelStr[1];
                boolean knownServiceType = SSHUtils.isServiceCodeKnown(channelType);
                final String channelName = channelStr[2];
                String channelEntry = channelNumber + " " + channelType + ", " + channelName;
                channelOverviewPanel.add(new JLabel(channelEntry), "gapleft 5, growx");
                final JButton connectButton = new JButton(rb.getString("ccTab.connect"));
                final JButton disconnectButton = new JButton(rb.getString("ccTab.disconnect"));
                disconnectButton.setToolTipText(rb.getString("ccTab.disconnect_tt"));
                if (!knownServiceType)
                    connectButton.setEnabled(false);
                connectButton.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent evt) {
                        if (connectButton.isEnabled()) {
                            communicator.openChannel(channelNumber);
                            // connect succesful
                            disconnectButton.setEnabled(true);
                            connectButton.setEnabled(false);
                        }
                    }
                });
                disconnectButton.setEnabled(false);
                disconnectButton.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent evt) {
                        if (disconnectButton.isEnabled()) {
                            communicator.closeChannel(channelNumber);
                            // disconnect successful
                            connectButton.setEnabled(true);
                            disconnectButton.setEnabled(false);
                        }
                    }
                });
                channelOverviewPanel.add(connectButton, "gapleft 5");
                channelOverviewPanel.add(disconnectButton, "gapleft 5, wrap");
                connectButtons.put(channelNumber, connectButton);
                disconnectButtons.put(channelNumber, disconnectButton);
            });
        }
        JScrollPane channelOverviewPane = new JScrollPane(channelOverviewPanel);
        CssRuleManager.getInstance().format(channelOverviewPanel);


        // remove the channel overview if already exists
        if (downPanel.getComponentCount() > 0)
            downPanel.remove(0);

        downPanel.add(channelOverviewPane, "growx, growy");
        downPanel.getParent().getParent().repaint();
    }


    /**
     * Updates the client table data for the given channelNumber
     *
     * @param clientList    A Vector with client data
     * @param channelNumber The channelNumber
     */
    public void updateClientTable(Vector<String[]> clientList, final String channelNumber) {
        // try to get the ssh client table
        JTable clientTable = clientTables.get(channelNumber);
        if (clientTable != null) {
            SSHClientTableModel model = (SSHClientTableModel) clientTable.getModel();
            model.reloadData(clientList);

            clientTables.put(channelNumber, clientTable);
        }
    }


    /**
     * Displays a new log message in the log area of the channel
     *
     * @param message       The new message
     * @param channelNumber The channel the message is for
     */
    public void displayNewLogMessage(String message, final String channelNumber) {
        JTextArea log = logs.get(channelNumber);
        if (log != null) {
            log.append(message + "\n");

            log.setCaretPosition(log.getDocument().getLength());
            log.repaint();
        }
    }


    /**
     * Displays version information
     *
     * @param version       The version information
     */
    public void displayVersion(String version) {
        Dialogs.showOpenVPNVersionDialog(ManagerGUI.mainFrame, version);
    }

    
    /**
     * disables disconnect button
     *
     * @param channel The channel
     */
    public void disableDisconnectButton(String channel) {
        JButton dButton = disconnectButtons.get(channel);
        if (dButton != null)
            dButton.setEnabled(false);
    }
    
    /**
     * enables connect button
     *
     * @param channel The channel
     */
    public void enableConnectButton(String channel) {
        JButton cButton = connectButtons.get(channel);
        if (cButton != null)
            cButton.setEnabled(true);
    }

    /**
     * toggles connect and disconnect buttons
     *
     * @param channel The channel the buttons shall be toggled for
     */
    public void toggleConnectButton(String channel) {
        JButton cButton = connectButtons.get(channel);
        JButton dButton = disconnectButtons.get(channel);
        if (cButton == null || dButton == null)
            return;
        if (cButton.isEnabled()) {
            cButton.setEnabled(false);
            dButton.setEnabled(true);
        } else {
            cButton.setEnabled(true);
            dButton.setEnabled(false);
        }
    }


    /**
     * Adds a debug message to the debug frame, if debugging is activated
     *
     * @param message The debug message
     */
    public void displayDebugMessage(String message) {
        if (debugFrame != null) {
            JTextArea logArea = (JTextArea) debugLogs.get(debugFrame);
            logArea.append(message + SSHConstants.NEWLINE);

            // jump to last position
            logArea.setCaretPosition(logArea.getDocument().getLength());
            logArea.repaint();
            logArea.getParent().repaint();
        }
    }

    /**
     * Show and hide version
     * 
     * @param channelPanel
     * @param channelNr
     */
    private void mouseClickedVersion(JPanel channelPanel, String channelNr) {
        communicator.callVersion(channelNr);
    }
    
    
    /**
     * Opens a new panel for this channel
     *
     * @param channelNr The channelNr
     */
    public void openChannelPanel(final String channelNr) {
        final JPanel channelPanel = new JPanel(new MigLayout("fillx"));

        // The client table
        final JTable clientTable = new JTable(
                new SSHClientTableModel(
                        new Vector<>()));
        
        // Log area
        JTextArea logArea = new JTextArea();
        final JScrollPane logPane =
                new JScrollPane(logArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);


        // Table
        JTableHeader header = clientTable.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(((SSHClientTableModel)
                clientTable.getModel()).new ColumnListener(clientTable));

        clientTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Point position = new Point(e.getX(), e.getY());
                int row = clientTable.rowAtPoint(position);
                clientTable.setRowSelectionInterval(row, row);

                SSHClientTableModel model = (SSHClientTableModel) clientTable.getModel();
                String username = model.getNameRowMapping().get(row + "");

                if (e.getButton() != MouseEvent.BUTTON1) {
                    // context menue
                    showClientContext(position, username, channelNr);
                } else if (e.getClickCount() == 2) {
                    // details
                }
            }
        });
        TableCellRenderer renderer = new CustomTableCellRendererCC();
        try {
            clientTable.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch(ClassNotFoundException ignored) {}

        clientTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        final JScrollPane clientTablePane = new JScrollPane(clientTable);

     
        // Buttons
        JButton statusButton = new JButton(rb.getString("ccTab.status"));
        statusButton.setToolTipText(rb.getString("ccTab.status_tt"));
        statusButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                communicator.callStatus(channelNr);
            }
        });

        final JButton versionButton = new JButton(rb.getString("ccTab.version"));
        versionButton.setToolTipText(rb.getString("ccTab.version_tt"));
        versionButton.addMouseListener(new java.awt.event.MouseAdapter() {
        	
            public void mouseClicked(java.awt.event.MouseEvent evt) {
        		mouseClickedVersion(channelPanel, channelNr);
            }
        });

        final JButton logButton = new JButton(rb.getString("ccTab.log_on"));
        logButton.setToolTipText(rb.getString("ccTab.log_on_tt"));
        logButton.addMouseListener(new java.awt.event.MouseAdapter() {
            boolean logging = false;

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JTextArea log = logs.get(channelNr);
                
                // create split panel
                final JSplitPane serverUserSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                		clientTablePane, logPane);
                serverUserSplitPane.setOneTouchExpandable(true);
                serverUserSplitPane.setDividerLocation(Configuration.getInstance().CONTROL_CENTER_LOG_DIVIDER_LOCATION);
                
                // toggle logging
                if (logging) {
                    // switch off
                	channelPanel.remove(1);	// 1=serverUserSplitPane        	
                	channelPanel.add(clientTablePane,"height 100:600:800, growx, growy, wrap");
                	
                    communicator.endLog(channelNr);
                    logButton.setText(rb.getString("ccTab.log_on"));
                    logButton.setToolTipText(rb.getString("ccTab.log_on_tt"));
                } else {
                    // switch on
                    channelPanel.remove(clientTablePane);
                    channelPanel.add(serverUserSplitPane,"height 100:600:800, growx, growy, wrap");
                	
                    communicator.callLog(channelNr);
                    logButton.setText(rb.getString("ccTab.log_off"));
                    logButton.setToolTipText(rb.getString("ccTab.log_off_tt"));
                }
                logging = !logging;

                logButton.repaint();
                log.repaint();
                logPane.repaint();

                channelPanel.repaint();
                channelPanel.getParent().repaint();
            }
        });

        JButton commandButton = new JButton(rb.getString("ccTab.command"));
        commandButton.setToolTipText(rb.getString("ccTab.command_tt"));
        commandButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                openUserCommandDialog(channelNr);
            }
        });
        
        JButton printButton = new JButton(ImageUtils.createImageIcon(Constants.ICON_PRINT, rb.getString("print.button")));
        printButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    clientTable.print();
                } catch (PrinterException e1) {
                    new VisualException(rb.getString("print.error.text"), rb.getString("print.error.title"));
                }
            }
        });
        printButton.setToolTipText(rb.getString("print.button_tt"));
        printButton.setSize(20, 20);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(statusButton);
        buttonPanel.add(versionButton);
        buttonPanel.add(logButton);
        buttonPanel.add(commandButton);
        buttonPanel.add(printButton);
        
        channelPanel.add(buttonPanel, "wrap");   
        
        channelPanel.add(clientTablePane, "height 100:600:800, growx, growy, wrap");
        CssRuleManager.getInstance().format(channelPanel);

        channelTabs.add(rb.getString("ccTab.channel") + " " + channelNr, channelPanel);
        channelTabs.setSelectedIndex(channelTabs.getTabCount() - 1);
        channelTabs.setVisible(true);
        channelTabs.getParent().repaint();

        channelPanels.put(channelNr, channelPanel);
        logs.put(channelNr, logArea);
        clientTables.put(channelNr, clientTable);
        clientTablePanes.put(channelNr, clientTablePane);

        communicator.callStatus(channelNr);


        // update status periodically
        TimerTask watcherTask = new StatusWatcher(communicator, channelNr);
        statusTimer = new Timer();
        // repeat every $interval seconds
        int interval = 60;
        if (server.getStatusInterval() > 0)
            interval = server.getStatusInterval();
        statusTimer.schedule(watcherTask, new Date(), interval * 1000);
    }


    /**
     * closes the channel panel for the given channel number
     *
     * @param channelNr The channelNr
     */
    public void closeChannelPanel(String channelNr) {
        // stop the actualizing timer
        statusTimer.purge();
        statusTimer.cancel();

        JPanel channelPanel = channelPanels.get(channelNr);
        if (channelPanel != null)
            channelTabs.remove(channelPanel);
        channelPanels.remove(channelNr);
        channelTabs.setSelectedIndex(channelTabs.getTabCount() - 1);

        if (channelPanels.isEmpty())
            channelTabs.setVisible(false);
        channelTabs.getParent().repaint();
    }


    /**
     * closes all channel panels
     */
    public void closeAllChannelPanel() {
        // stop the actualizing timer
        statusTimer.purge();
        statusTimer.cancel();

        Set<String> keys = channelPanels.keySet();
        List<String> channelNrs = new ArrayList<>();
        keys.forEach(channelNr -> {
            JPanel channelPanel = channelPanels.get(channelNr);
            if (channelPanel != null)
                channelTabs.remove(channelPanel);
            channelNrs.add(channelNr);
        });
        channelNrs.forEach(channelNr -> channelPanels.remove(channelNr));

        channelTabs.setVisible(false);
        channelTabs.getParent().repaint();
    }


    /**
     * close tab, debug frame and ssh session
     */
    public void closeTabAndSession() {
        if (statusTimer != null)
            statusTimer.cancel();
        if (debugFrame != null)
            debugFrame.dispose();
        ManagerGUI.getTabs().remove(ccPanel);
        ManagerGUI.getOpenCCTabs().remove(server.getHostname());
        // jump to server table
        // jump to last CC tab
        ManagerGUI.getTabs().setSelectedIndex(ManagerGUI.getOpenCCTabs().isEmpty() ? 1 : ManagerGUI.getTabs().getComponentCount() - 1);
        Session session = SSHSessionPool.getInstance().getSession(server.getHostname());
        if (session != null && session.isConnected()) {
            session.disconnect();
            SSHSessionPool.getInstance().removeSession(server.getHostname());
        }
        
        try{
        	ManagerGUI.OPEN_CONTROL_SERVERS.removeElement(""+server.getServerid());
        } catch(Exception e){
        	Logger.getLogger(SSHParser.class.getName()).warning("Couldn't remove server from open_cctab-list:"+e.toString());
        }
    }


    /**
     * Shows a context menue in the ssh client table
     *
     * @param position      The mouse position
     * @param username      The username
     * @param channelNumber The channelNumber
     */
    private void showClientContext(Point position,
                                   final String username, final String channelNumber) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem detailsMenu = new JMenuItem();
        detailsMenu.setText(rb.getString("sshClientContextMenu.kill"));
        detailsMenu.addActionListener(e -> communicator.killUser(channelNumber, username));

        contextMenu.add(detailsMenu);
        CssRuleManager.getInstance().format(contextMenu);
        JTable clientTable = clientTables.get(channelNumber);
        contextMenu.show(clientTable, position.x + 10, position.y);
    }


    /**
     * Creates a frame showing debug messages
     *
     * @return debug frame
     */
    private JFrame createDebugFrame() {
        JFrame debugFrame = new JFrame();
        debugFrame.setLayout(new MigLayout());
        debugFrame.setPreferredSize(new Dimension(500, 400));
        debugFrame.setResizable(true);
        debugFrame.setTitle(rb.getString("ssh.debug.title") + " " + server.getHostname());

        JTextArea logArea = new JTextArea("", 30, 50);
        logArea.setEditable(false);

        JScrollPane scrollLog =
                new JScrollPane(logArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        debugFrame.getContentPane().add(scrollLog);
        debugFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        CssRuleManager.getInstance().format(debugFrame);

        debugFrame.pack();
        debugFrame.setVisible(true);

        debugLogs.put(debugFrame, logArea);

        return debugFrame;
    }


    public Session getSshSession() {
        return sshSession;
    }


    public void setSshSession(Session sshSession) {
        this.sshSession = sshSession;
    }

    public JPanel getCcPanel() {
        return ccPanel;
    }

    public String getHostname() {
        return server.getHostname();
    }
    
    public int getVpnPort() {
    	return server.getVpnPort();
    }
}


class StatusWatcher extends TimerTask {

    private SSHCommunicator communicator;
    private String channel;

    public StatusWatcher(SSHCommunicator communicator, String channel) {
        this.communicator = communicator;
        this.channel = channel;
    }

    public final void run() {
		communicator.callStatus(channel);
	}
	
}