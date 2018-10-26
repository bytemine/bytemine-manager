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
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ServerDAO;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;


/**
 * a frame managing certificate assignments
 *
 * @author Daniel Rauer
 */
public class X509Manager {

    private JFrame parentFrame = null;
    private JComponent parent = null;

    private Vector<Integer> types = new Vector<>();
    private Server server = null;
    private User user = null;
    private int connectedX509 = -1;
    private Vector<Integer> assignedX509Ids = new Vector<>();
    private UserDetails userDetails = null;
    private ServerDetails serverDetails = null;
    private JFrame mgmtFrame = null;


    X509Manager(JFrame parent, Server s) {
        parentFrame = parent;
        server = s;
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = server.getX509id();
    }

    X509Manager(JFrame parent, User u) {
        parentFrame = parent;
        user = u;
        types.add(X509.X509_TYPE_CLIENT);
        types.add(X509.X509_TYPE_PKCS12);
        connectedX509 = user.getX509id();
    }

    public X509Manager(UserDetails details, User u) {
        userDetails = details;
        parentFrame = details.getParentFrame();
        user = u;
        types.add(X509.X509_TYPE_CLIENT);
        types.add(X509.X509_TYPE_PKCS12);
        connectedX509 = user.getX509id();
    }


    public X509Manager(ServerDetails details, Server s) {
        serverDetails = details;
        parentFrame = details.getParentFrame();
        server = s;
        types = new Vector<Integer>();
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = server.getX509id();
    }
    
    
    X509Manager(JComponent parent, Server s) {
        serverDetails = null;
        this.parent = parent;
        server = s;
        types = new Vector<Integer>();
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = server.getX509id();
    }
    
    X509Manager(JComponent parent, User u) {
        userDetails = null;
        this.parent = parent;
        user = u;
        types = new Vector<Integer>();
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = user.getX509id();
    }
    
    public X509Manager(Server s) {
        server = s;
        types = new Vector<>();
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = server.getX509id();
    }
    
    public X509Manager(User u) {
        user = u;
        types = new Vector<>();
        types.add(X509.X509_TYPE_SERVER);
        connectedX509 = user.getX509id();
    }


    void showX509ManagerFrame() {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t);

                createX509ManagerFrame();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();
    }


    /**
     * creates a new frame
     */
    private void createX509ManagerFrame() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        mgmtFrame = new JFrame(
                rb.getString("app.title") + " - " + rb.getString("x509.mgmt.title")
        );
        mgmtFrame.setResizable(true);
        mgmtFrame.setLayout(new MigLayout("fill"));
        mgmtFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        mgmtFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = createX509ManagerPanel();

        mgmtFrame.getContentPane().add(mainPanel, "wrap, span 2");

        Point location = null;
        if (parentFrame !=null)
            location = GuiUtils.getOffsetLocation(parentFrame);
        else if (parent != null)
            location = GuiUtils.getOffsetLocation(parent);
        assert location != null;
        mgmtFrame.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(mgmtFrame);

        mgmtFrame.pack();
        mgmtFrame.setVisible(true);
    }
    
    
    
    JPanel createX509ManagerPanel() {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        JPanel mainPanel = new JPanel(new MigLayout("fill"));
        final JButton saveButton = new JButton();

        String title = rb.getString("x509.mgmt.text");
        if (user != null)
            title += " " + rb.getString("x509.mgmt.user") +": " + user.getUsername();
        else
            title += " " + rb.getString("x509.mgmt.server") + ": " + server.getName();
        JLabel titleLabel = new JLabel(title);

        Vector<String[]> allCertificates = new Vector<String[]>();
        assignedX509Ids = new Vector<Integer>();

        // first entry is "empty" assignment
        String[] noCertificate = new String[3];
        noCertificate[0] = "-1";
        noCertificate[1] = rb.getString("x509.mgmt.nocertificate");
        noCertificate[2] = "";
        allCertificates.add(noCertificate);

        types.forEach(type -> {
            allCertificates.addAll(X509Queries.getX509TypeOverview(type));
            assignedX509Ids.addAll(X509Queries.getAssignedX509Ids(type));
        });

        JPanel textPanel = new JPanel(new MigLayout("fillx"));
        ButtonGroup group = new ButtonGroup();
        allCertificates.forEach(strings -> {
            final int x509id = Integer.parseInt(strings[0]);
            JLabel availability = new JLabel(rb.getString("x509.mgmt.available"));
            if (assignedX509Ids.contains(x509id)) {
                availability.setText(rb.getString("x509.mgmt.assigned"));
                availability.setForeground(Color.RED);
            }
            JLabel text = new JLabel(
                    strings[1] + ", " + strings[2]);
            text.setFont(Constants.FONT_PLAIN);
            JRadioButton radio = new JRadioButton();
            if (connectedX509 == x509id)
                radio.setSelected(true);
            else
                radio.setSelected(false);
            group.add(radio);
            radio.addActionListener(e -> {
                AbstractButton radio1 = (AbstractButton) e.getSource();
                boolean selected = radio1.getModel().isSelected();
                if (selected)
                    connectedX509 = x509id;

                ManagerGUI.serverUserTreeModel.setUnsavedData(true);
                saveButton.setText(rb.getString("x509.mgmt.savebutton") + " *");
            });
            textPanel.add(radio);
            textPanel.add(text);
            textPanel.add(availability, "gapleft 12, wrap");
        });
        JScrollPane scrollPane = new JScrollPane(textPanel);

        JButton closeButton = new JButton();
        closeButton.addActionListener(e -> mgmtFrame.dispose());
        closeButton.setText(rb.getString("x509.mgmt.closebutton"));

        saveButton.addActionListener(e -> {
            saveButton.setText(rb.getString("x509.mgmt.savebutton"));
            ManagerGUI.serverUserTreeModel.setUnsavedData(false);
            apply(mgmtFrame);
        });
        saveButton.setText(rb.getString("x509.mgmt.savebutton"));

        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(saveButton);
        if (mgmtFrame != null)
            buttonPanel.add(closeButton);
        
        mainPanel.add(titleLabel, "wrap");
        mainPanel.add(scrollPane, "growx, growy, wrap");
        mainPanel.add(buttonPanel, "align right");
        CssRuleManager.getInstance().format(mainPanel);
        return mainPanel;
    }


    /**
     * Applies the changes
     */
    private void apply(JFrame mgmtFrame) {
        // the chosen certificate is already assigned to a user or server
        if (assignedX509Ids.contains(connectedX509))
            if (!Dialogs.showX509ReassignDialog(parentFrame))
                // return without changes
                return;
            else
                try {
                    for (Integer type : types) {
                        // delete previous assignment
                        X509Queries.deleteX509Assignment(connectedX509, type);
                    }
                } catch (Exception e) {
                    new VisualException(e);
                }

        // save new assignment
        if (types.contains(X509.X509_TYPE_SERVER) && server != null) {
            server.setX509id(connectedX509);
            ServerDAO.getInstance().update(server);

            ManagerGUI.refreshServerTable();
        } else if (
                (types.contains(X509.X509_TYPE_CLIENT) || types.contains(X509.X509_TYPE_PKCS12))
                        && user != null) {
            user.setX509id(connectedX509);
            UserDAO.getInstance().update(user);
            
            UserAction.reassignToX509(user);

            ManagerGUI.refreshUserTable();
        }

        if (mgmtFrame != null)
            mgmtFrame.dispose();
        
        if (userDetails != null) {
            userDetails.getUserDetailsFrame().dispose();
            userDetails.showUserDetailsFrame();
        } else if (serverDetails != null) {
            serverDetails.getServerDetailsFrame().dispose();
            serverDetails.showServerDetailsFrame();
        }
    }
}
