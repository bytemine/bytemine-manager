/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Point;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.GroupAction;
import net.bytemine.manager.action.ValidatorAction;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.GroupQueries;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;


/**
 * a frame showing group details
 *
 * @author Daniel Rauer
 */
public class GroupDetails {

    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private JFrame parentFrame;
    private JFrame groupDetailsFrame = null;
    private String groupid;

    private JTextField nameField = null;
    private JTextArea descriptionField = null;

    
    public GroupDetails(JFrame parent) {
        parentFrame = parent;
        this.groupid = "-1";
    }

    public GroupDetails(JFrame parent, String groupid) {
        parentFrame = parent;
        this.groupid = groupid;
    }


    public void showDetails() {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t);

                createGroupDetails();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();

    }


    /**
     * creates a new frame for group management
     */
    private void createGroupDetails() {
        groupDetailsFrame = new JFrame(
                rb.getString("app.title") + " - " + rb.getString("group.details.title")
        );
        groupDetailsFrame.setLayout(new MigLayout("fillx", "", "align top"));
        groupDetailsFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        groupDetailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        final JPanel mainPanel = new JPanel(new MigLayout("insets 0"));
        
        // new group to create or existing group to update
        int gId = Integer.parseInt(groupid);
        final boolean newGroup = (gId <= 0) ? true : false;
        
        final String[] details = GroupQueries.getGroupDetails(groupid);

        mainPanel.add(new JLabel(rb.getString("group.details.new") + ":"), "wrap");

        mainPanel.add(new JLabel(rb.getString("group.details.name") + ":"), "align right");
        nameField = new JTextField(details[1]);
        mainPanel.add(nameField, "growx, wrap");

        mainPanel.add(new JLabel(rb.getString("group.details.description") + ":"), "align right");
        descriptionField = new JTextArea(details[2], 3, 30);
        mainPanel.add(descriptionField, "span, wrap");


        JButton closeButton = new JButton(rb.getString("user.details.closebutton"));
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                groupDetailsFrame.dispose();
            }
        });

        JButton saveButton = new JButton(rb.getString("user.details.savebutton"));
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

                try {
                    if (newGroup) {
                        // save
                        if (ValidatorAction.validateGroupCreation(nameField.getText(), descriptionField.getText())) {
                            GroupAction.createGroup(
                                    nameField.getText(),
                                    descriptionField.getText()
                            );
                        }
                    } else {
                        // update
                        if (ValidatorAction.validateGroupUpdate(nameField.getText(), details[1], descriptionField.getText())) {
                            GroupAction.updateGroup(
                                    groupid,
                                    nameField.getText(),
                                    descriptionField.getText()
                            );
                        }
                    }

                    groupDetailsFrame.dispose();

                    ManagerGUI.refreshUserTable();
                    ManagerGUI.refreshX509Table();
                } catch (ValidationException ve) {
                    // show validation error dialog
                    CustomJOptionPane.showMessageDialog(groupDetailsFrame,
                            ve.getMessage(),
                            ve.getTitle(),
                            CustomJOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    // show error dialog
                    CustomJOptionPane.showMessageDialog(groupDetailsFrame,
                            rb.getString("group.dialog.update.errortext"),
                            rb.getString("group.dialog.update.errortitle"),
                            CustomJOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel buttonPanel2 = new JPanel(new MigLayout("align right"));
        buttonPanel2.add(saveButton);
        buttonPanel2.add(closeButton);

        mainPanel.add(buttonPanel2, "gaptop 10, span 3, align right");

        groupDetailsFrame.getContentPane().add(mainPanel);

        Point location = GuiUtils.getOffsetLocation(parentFrame);
        groupDetailsFrame.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(groupDetailsFrame);

        groupDetailsFrame.pack();
        groupDetailsFrame.setVisible(true);
    }


    public JFrame getParentFrame() {
        return parentFrame;
    }

    public JFrame getUserDetailsFrame() {
        return groupDetailsFrame;
    }

}
