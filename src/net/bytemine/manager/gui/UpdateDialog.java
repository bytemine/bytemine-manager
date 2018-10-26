/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.BorderLayout;

import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.update.UpdateMgmt;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;


/**
 * Displays an update dialog
 * @author Daniel Rauer
 */
public class UpdateDialog {

    private static UpdateDialog instance;
    private JLabel textLabel;
    private JPanel buttonPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton changeLogButton;
    private JDialog updateDialog;


    private UpdateDialog() {
    }

    public static UpdateDialog getInstance() {
        if (instance == null)
            instance = new UpdateDialog();

        return instance;
    }


    /**
     * Shows a dialog for the update process
     * Starts the update process
     *
     * @param parentFrame The parent frame
     * @param updateMgmt  An instance of the update manager
     * @param changeLog   A changelog or null
     */
    public void showUpdateDialog(final JFrame parentFrame, UpdateMgmt updateMgmt, final String changeLog) {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        updateDialog = new JDialog(parentFrame, true);
        updateDialog.setTitle(rb.getString("dialog.update.title"));
        Image icon = ImageUtils.readImage(Configuration.getInstance().ICON_PATH);
        updateDialog.setIconImage(icon);

        final CustomJPanel inputPanel = new CustomJPanel(new MigLayout("insets 15, align center"));

        String text = rb.getString("dialog.update.noUpdate");
        boolean updateAvailable = updateMgmt.isUpdateAvailable();
        if (updateAvailable) {
            text = rb.getString("dialog.update.available1") + " " +
                    updateMgmt.getNewVersion() + " " +
                    rb.getString("dialog.update.available2");
        }

        textLabel = new JLabel(text);
        inputPanel.add(textLabel, "wrap");


        final JButton downloadButton = new JButton(rb.getString("dialog.update.downloadbutton"));
        downloadButton.addActionListener(e -> {
            try {
                downloadButton.setEnabled(false);

                textLabel.setText(rb.getString("dialog.update.downloading"));

                // download updates
                UpdateMgmt updateMgmt1 = UpdateMgmt.getInstance();
                updateMgmt1.downloadUpdates();
            } catch (Exception ex) {
                // show error dialog
                CustomJOptionPane.showMessageDialog(updateDialog,
                        ex.getMessage(),
                        rb.getString("dialog.update.errortitle"),
                        CustomJOptionPane.ERROR_MESSAGE);
            }
        });


        okButton = new JButton();
        okButton.setText(rb.getString("dialog.update.okbutton"));
        okButton.addActionListener(e -> updateDialog.dispose());


        cancelButton = new JButton();
        cancelButton.setText(rb.getString("dialog.update.cancelbutton"));
        cancelButton.addActionListener(e -> {
            UpdateMgmt updateMgmt12 = UpdateMgmt.getInstance();
            updateMgmt12.cancelUpdate();
            updateDialog.dispose();
        });

        changeLogButton = new JButton();
        changeLogButton.setText(rb.getString("dialog.update.changelogbutton"));
        changeLogButton.addActionListener(e -> {
            ChangeLog log = new ChangeLog(updateDialog, changeLog);
            log.showDetails();
        });


        buttonPanel = new JPanel();
        if (updateAvailable) {
            buttonPanel.add(cancelButton);
            buttonPanel.add(downloadButton);
            if (changeLog != null)
                buttonPanel.add(changeLogButton);
        } else
            buttonPanel.add(okButton);


        Container contentPane = updateDialog.getContentPane();
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);


        if (parentFrame != null) {
            Point center = GuiUtils.getCenterPositionRelativeTo(updateDialog, parentFrame);
            updateDialog.setLocation(center);
        } else {
            updateDialog.setLocationRelativeTo(null);
        }

        CssRuleManager.getInstance().format(updateDialog);
        updateDialog.pack();
        updateDialog.setVisible(true);
    }


    /**
     * displays a success message to the user, informing him of the new filename to run
     */
    public void downloadFinished() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String text = "<html>" + rb.getString("dialog.update.downloaded1") +"</p></html>";
        textLabel.setText(text);

        Container contentPane = updateDialog.getContentPane();
        contentPane.remove(buttonPanel);
        buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        CssRuleManager.getInstance().format(updateDialog);
        updateDialog.pack();
    }


    /**
     * disposes the dialog
     */
    public void dispose() {
        updateDialog.dispose();
    }


}
