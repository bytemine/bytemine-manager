/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Date;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;


/**
 * a frame showing x509 details
 *
 * @author Daniel Rauer
 */
public class X509Details {

    private static String x509id;
    private static JFrame parentFrame = null;
    static JFrame detailsFrame = null;
    
    private JComponent parent;

    private final int mainWidth = Configuration.getInstance().X509_GUI_WIDTH;
    private final int mainHeight = Configuration.getInstance().X509_GUI_HEIGHT;
    private final int locationX = Configuration.getInstance().X509_GUI_LOCATION_X;
    private final int locationY = Configuration.getInstance().X509_GUI_LOCATION_Y;
    
    X509Details(JFrame parent, String id) {
        x509id = id;
        parentFrame = parent;
    }
    X509Details(JComponent parent, String id) {
        x509id = id;
        this.parent = parent;
    }

    public X509Details(String id) {
        x509id = id;
    }


    public void showDetails() {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
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
        generateWorker.execute();
    }


    private void createDetails() {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        // true: show nonsense content of pkcs12, false for the original x509 content
        boolean displayPKCS12Content = false;
        // retrieve data from database
        String[] details = X509Queries.getX509Details(x509id, displayPKCS12Content);

        JPanel mainPanel = new JPanel(new MigLayout("wrap 2, fill", "[20][10]", "25"));
         
        JLabel idLabel = new JLabel(rb.getString("detailsFrame.id"));
        idLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(idLabel, "align left");
        mainPanel.add(new JDataLabel(details[0]));

        JLabel versionLabel = new JLabel(rb.getString("detailsFrame.version"));
        versionLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(versionLabel, "align left");
        mainPanel.add(new JDataLabel(details[1]));

        JLabel typeLabel = new JLabel(rb.getString("detailsFrame.type"));
        typeLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(typeLabel, "align left");
        JDataLabel typeDataLabel = new JDataLabel(X509.transformTypeToString(Integer.parseInt(details[11])));
        mainPanel.add(typeDataLabel);

        JLabel issuerLabel = new JLabel(rb.getString("detailsFrame.issuer"));
        issuerLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(issuerLabel, "align left");
        mainPanel.add(new JDataLabel(details[5]));

        JLabel subjectLabel = new JLabel(rb.getString("detailsFrame.subject"));
        subjectLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(subjectLabel, "align left");
        mainPanel.add(new JDataLabel(details[14]));

        JLabel serialLabel = new JLabel(rb.getString("detailsFrame.serial"));
        serialLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(serialLabel, "align left");
        mainPanel.add(new JDataLabel(details[4]));

        JLabel createDateLabel = new JLabel(rb.getString("detailsFrame.createDate"));
        createDateLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(createDateLabel, "align left");
        JLabel createDateDataLabel = new JDataLabel();
        try {
            Date createDate = Constants.parseDetailedFormat(details[12]);
            createDateDataLabel.setText(Constants.getShowFormatForCurrentLocale().format(createDate));
        } catch (Exception e) {
            createDateDataLabel.setText(details[12]);
        }
        mainPanel.add(createDateDataLabel);

        JLabel validFromLabel = new JLabel(rb.getString("detailsFrame.validFrom"));
        validFromLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(validFromLabel, "align left");
        JLabel validFromDataLabel = new JDataLabel();
        try {
            Date validFromDate = Constants.parseDetailedFormat(details[15]);
            validFromDataLabel.setText(Constants.getShowFormatForCurrentLocale().format(validFromDate));
        } catch (Exception e) {
            validFromDataLabel.setText(details[15]);
        }
        mainPanel.add(validFromDataLabel);

        JLabel validToLabel = new JLabel(rb.getString("detailsFrame.validTo"));
        validToLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(validToLabel, "align left");
        JLabel validToDataLabel = new JDataLabel();
        try {
            Date validToDate = Constants.parseDetailedFormat(details[16]);
            validToDataLabel.setText(Constants.getShowFormatForCurrentLocale().format(validToDate));
        } catch (Exception e) {
            validToDataLabel.setText(details[16]);
        }
        mainPanel.add(validToDataLabel);

        JLabel textLabel = new JLabel(rb.getString("detailsFrame.generatedText"));
        textLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(textLabel, "align left");
        JDataLabel genTypeLabel = new JDataLabel(X509.transformGenerationTypeToString(details[17]));
        mainPanel.add(genTypeLabel);

        JLabel filenameLabel = new JLabel(rb.getString("detailsFrame.fileName"));
        filenameLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(filenameLabel, "align left");
        JDataLabel filenameDataLabel = new JDataLabel(details[3] + details[2]);
        mainPanel.add(filenameDataLabel);


        JLabel contentLabel = new JLabel(rb.getString("detailsFrame.content"));
        contentLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(contentLabel, "align left");
        int type = Integer.parseInt(details[11]);
        JTextArea contentArea = new JTextArea(details[6]);
        contentArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(contentArea);
        mainPanel.add(scroll, "height 100:300:500, growx, growy");

        JLabel keyContentLabel = new JLabel(rb.getString("detailsFrame.keyContent"));
        keyContentLabel.setFont(Constants.FONT_PLAIN);
        mainPanel.add(keyContentLabel, "align left");
        JTextArea keyContentArea = new JTextArea(details[10]);
        keyContentArea.setEditable(false);
        JScrollPane scroll5 = new JScrollPane(keyContentArea);
        mainPanel.add(scroll5, "height 100:300:500, growx, growy");

        //  final JFrame
        detailsFrame = new JFrame(
                rb.getString("app.title") + " - " + rb.getString("detailsFrame.title")
        );
        detailsFrame.addWindowListener(new X509WindowListener());
        
        detailsFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton closeButton = new JButton(rb.getString("detailsFrame.closebutton"));
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                detailsFrame.dispose();
            }
        });
        
        JButton exportButton = new JButton(rb.getString("detailsFrame.exportbutton"));
        exportButton.setToolTipText(rb.getString("detailsFrame.exportbutton_tt"));
        exportButton.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    String path = X509Action.exportToFilesystem(x509id);
                    CustomJOptionPane.showMessageDialog(detailsFrame,
                            rb.getString("detailsFrame.exportmessage") 
                                +"\n" + path,
                            rb.getString("detailsFrame.exporttitle"),
                            CustomJOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    new VisualException(rb.getString("detailsFrame.exporterror"));
                }
            }
        });

        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, "gaptop 0, span 2, align right");

        detailsFrame.getContentPane().add(mainPanel);
        
        Point location = null;
        if(locationX == -1) {
        	if (parentFrame != null)
        		location = GuiUtils.getOffsetLocation(parentFrame);
        	else if (parent != null)
        		location = GuiUtils.getOffsetLocation(parent);
            assert location != null;
            detailsFrame.setLocation(location.x, location.y);
        } else {
        	detailsFrame.setLocation(locationX, locationY);
        	detailsFrame.setPreferredSize(new Dimension(mainWidth,mainHeight));
        }
        CssRuleManager.getInstance().format(detailsFrame);

        detailsFrame.pack();
        detailsFrame.setVisible(true);
    }

    public static void main(String[] args) {
        X509Details d = new X509Details(new JFrame(), "1");
        d.createDetails();
    }

}
