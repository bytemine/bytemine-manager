/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Point;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.GuiUtils;
import net.miginfocom.swing.MigLayout;


/**
 * Displays the changelog
 *
 * @author Daniel Rauer
 */
public class ChangeLog {

    private static JDialog parentDialog;
    private String changeLog;

    public ChangeLog(JDialog parent, String changeLog) {
        parentDialog = parent;
        this.changeLog = changeLog;
    }


    public void showDetails() {
        SwingWorker<String, Void> detailsWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t);

                createDetails();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        detailsWorker.execute();
    }


    private void createDetails() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        final JDialog detailsDialog = new JDialog(parentDialog,
                rb.getString("app.title") + " - " + rb.getString("changelog.title")
        );

        MigLayout layout = new MigLayout();
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(layout);

        StringBuffer logFormatted = new StringBuffer("<html>");
        logFormatted.append(changeLog.replaceAll("\n", "<br />"));
        logFormatted.append("</html>");

        JLabel log = new JLabel(logFormatted.toString());
        mainPanel.add(log, "wrap");

        JButton closeButton = new JButton(rb.getString("changelog.closebutton"));
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                detailsDialog.dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, "gaptop 0, span 2, align center");

        detailsDialog.getContentPane().add(mainPanel);

        Point location = GuiUtils.getOffsetLocation(parentDialog);
        detailsDialog.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(detailsDialog);

        detailsDialog.pack();
        detailsDialog.setVisible(true);

    }

}
