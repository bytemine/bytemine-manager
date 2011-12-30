/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.openvpn.ScpTool;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;

/**
 * Displays some informations about import and user synchronisation
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class StatusFrame {

    private static Logger logger = Logger.getLogger(StatusFrame.class.getName());
    private final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    public static final int TYPE_SYNC = 0;
    public static final int TYPE_IMPORT = 1;
    public static final int TYPE_CONNTEST = 2;
    public static final int TYPE_LDAP = 3;
    public static final int TYPE_INFO = 4;

    public JFrame statusFrame;
    private JFrame parentFrame;
    private int type;
    private Server server;
    private JLabel statusLabel = new JLabel();
    private JPanel detailsPanel = new JPanel(new MigLayout());
    private JButton okButton;
    private static final int frameWidth = 520;
    private static final int frameHeight = 150;
    private static final int frameHeightFull = 300;
    private ScpTool scpTool;


    public StatusFrame() {
    }

    /**
    *
    * @param server   the serer for which the status frame is
    */
    public StatusFrame(Server server) {
        this.server = server;
    }

    /*
    *
    * @param type        The type: sync or import
    * @param parentFrame The parent frame
    */
    public StatusFrame(int type, JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.type = type;
    }

   /**
    *
    * @param type        The type: sync or import
    * @param parentFrame The parent frame
    * @param server The server to sync, connect or import from
-     */
    public StatusFrame(int type, JFrame parentFrame, Server server) {
        this.parentFrame = parentFrame;
        this.type = type;
        this.server = server;
    }

    /**
     * builds the frame
     */
    private void createFrame() {
        String title = rb.getString("status.title.general");
        if (type == TYPE_SYNC)
            title = rb.getString("status.title.sync");
        else if (type == TYPE_IMPORT)
            title = rb.getString("status.title.import");
        else if (type == TYPE_CONNTEST)
            title = rb.getString("status.title.connection");

        if (server != null) {
            title += " " + server.getName();
        }
        
        statusFrame = new JFrame(title);

        statusFrame.setResizable(true);
        statusFrame.setLayout(new MigLayout("align center, fill"));
        statusFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        statusFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statusFrame.setPreferredSize(new Dimension(frameWidth, frameHeight));

        JPanel mainPanel = new JPanel(new MigLayout("align center, fill"));

        mainPanel.add(statusLabel, "grow, wrap");

        detailsPanel.setVisible(false);
        detailsPanel.setLayout(new MigLayout());
        mainPanel.add(detailsPanel, "wrap");

        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        okButton.setEnabled(false);
        mainPanel.add(okButton, "align center");
        statusFrame.add(mainPanel, "align center");
        
        if (parentFrame != null) {
            Point location = GuiUtils.getOffsetLocation(parentFrame, 60);
            statusFrame.setLocation(location.x, location.y);
        }
        
        statusFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                if (scpTool != null)
                    scpTool.disconnectSession();
            }
        });
        
        CssRuleManager.getInstance().format(statusFrame);

        statusFrame.pack();
        statusFrame.setVisible(true);
    }


    public void addDetailsText(String text) {
        detailsPanel.add(new JLabel(text), "wrap");
    }

    public void showDetails() {
        statusFrame.setSize(frameWidth, frameHeightFull);
        detailsPanel.setVisible(true);
        statusFrame.toFront();
    }

    /**
     * Updates the status message
     *
     * @param message The new message to show
     */
    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.validate();
        } 
        if (statusFrame != null)
            statusFrame.validate();
    }

    public void done() {
        okButton.setEnabled(true);
    }


    public void show() {
        createFrame();

    }

    public void close() {
        this.statusFrame.dispose();
        logger.fine("closing status frame");
    }


    public void toFront() {
        this.statusFrame.toFront();
    }

    
    public void setScpTool(ScpTool scpTool) {
        this.scpTool = scpTool;
    }
}
