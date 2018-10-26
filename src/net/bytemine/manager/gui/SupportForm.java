/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.action.ValidatorAction;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.SupportModel;
import net.bytemine.manager.utility.EmailUtils;
import net.bytemine.utility.GuiUtils;
import net.bytemine.utility.ImageUtils;
import net.miginfocom.swing.MigLayout;


/**
 * Provides a support form
 * @author Daniel Rauer
 *
 */
public class SupportForm {
    
    private JFrame supportFrame = null;
    private JButton sendButton = null;
    private JButton cancelButton = null;

    public static void main(String[] args) {
        SupportForm sf = new SupportForm();
        sf.show();
    }
    
    SupportForm() {
        
    }
    
    public void show() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        String rbPrefix = "supportformular.";
        
        supportFrame = new JFrame(
                rb.getString("app.title") + " - " + rb.getString(rbPrefix + "title")
        );
        
        supportFrame.setLayout(new MigLayout("wrap 2, fill"));
        supportFrame.setIconImage(ImageUtils.readImage(Configuration.getInstance().ICON_PATH));
        supportFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        supportFrame.setPreferredSize(new Dimension(600, 400));
        
        JPanel mainPanel = new JPanel(new MigLayout("insets 5, fillx", "[left]", "[top]"));
        
        JLabel headlineLabel = new JLabel(rb.getString(rbPrefix + "headline"));
        headlineLabel.setFont(Constants.FONT_BOLD);
        
        JLabel nameLabel = new JLabel(rb.getString(rbPrefix + "name"));
        nameLabel.setFont(Constants.FONT_BOLD);
        final JTextField nameField = new JTextField(25);
        nameField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                if (!nameField.getText().isEmpty())
                    nameField.setBackground(Color.WHITE);
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });
        
        JLabel customerLabel = new JLabel(rb.getString(rbPrefix + "customer"));
        customerLabel.setFont(Constants.FONT_PLAIN);
        final JTextField customerField = new JTextField(10);
        
        JLabel mailLabel = new JLabel(rb.getString(rbPrefix + "mail"));
        mailLabel.setFont(Constants.FONT_BOLD);
        final JTextField mailField = new JTextField(25);
        mailField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                if (!mailField.getText().isEmpty())
                    mailField.setBackground(Color.WHITE);
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });
        
        JLabel phoneLabel = new JLabel(rb.getString(rbPrefix + "phone"));
        phoneLabel.setFont(Constants.FONT_PLAIN);
        final JTextField phoneField = new JTextField(10);
        
        JLabel messageLabel = new JLabel(rb.getString(rbPrefix + "message"));
        messageLabel.setFont(Constants.FONT_BOLD);
        final JTextArea message = new JTextArea();
        message.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            
            public void keyReleased(KeyEvent e) {
                if (!message.getText().isEmpty())
                    message.setBackground(Color.WHITE);
            }
            
            public void keyPressed(KeyEvent e) {
            }
        });
        JScrollPane scroll = new JScrollPane(message);
        
        mainPanel.add(headlineLabel, "span, wrap");
        mainPanel.add(nameLabel);
        mainPanel.add(nameField, "wrap");
        mainPanel.add(customerLabel);
        mainPanel.add(customerField, "wrap");
        mainPanel.add(mailLabel);
        mainPanel.add(mailField, "wrap");
        mainPanel.add(phoneLabel);
        mainPanel.add(phoneField, "wrap");
        mainPanel.add(messageLabel);
        mainPanel.add(scroll, "height 100:550:700, growx, growy");
        
        sendButton = new JButton(rb.getString(rbPrefix + "sendbutton"));
        sendButton.addActionListener(e -> {
            try {
                boolean valid = ValidatorAction.validateSupportRequest(
                        nameField.getText(), mailField.getText(), message.getText());
                if (valid) {
                    SupportModel model = new SupportModel(
                            nameField.getText(),
                            customerField.getText(),
                            mailField.getText(),
                            phoneField.getText(),
                            message.getText(),
                            SupportModel.TYPE_REQUEST);

                    // disable the buttons
                    disableButtons();
                    // send the request
                    EmailUtils.sendSupportRequest(model);
                }
            } catch (ValidationException ve) {
                // set focus on error field
                if (ve.getCode() > 0) {
                    int code = ve.getCode();
                    switch (code) {
                        case 1: nameField.requestFocus(); nameField.setBackground(Constants.COLOR_ERROR); break;
                        case 2: mailField.requestFocus(); mailField.setBackground(Constants.COLOR_ERROR); break;
                        case 3: message.requestFocus(); message.setBackground(Constants.COLOR_ERROR); break;
                        default: break;
                    }
                }

                // show validation error dialog
                CustomJOptionPane.showMessageDialog(supportFrame,
                        ve.getMessage(),
                        ve.getTitle(),
                        CustomJOptionPane.ERROR_MESSAGE);

            } catch (Exception ex) {
                new VisualException(supportFrame, ex);
            }
        });
        cancelButton = new JButton(rb.getString(rbPrefix + "cancelbutton"));
        cancelButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new MigLayout("align right"));
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        
        supportFrame.add(mainPanel, "growx, growy, wrap");
        supportFrame.add(buttonPanel, "gaptop 20, growx, span 3");
        
        Point location = GuiUtils.getOffsetLocation(ManagerGUI.mainFrame);
        supportFrame.setLocation(location.x, location.y);
        CssRuleManager.getInstance().format(supportFrame);

        supportFrame.pack();
        supportFrame.setVisible(true);
    }
    
    /**
     * disposes the frame
     */
    public void dispose() {
        supportFrame.dispose();
        ManagerGUI.supportForm = null;
    }
    
    /**
     * activates the buttons
     */
    public void enableButtons() {
        sendButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }
    
    /**
     * deactivates the buttons
     */
    private void disableButtons() {
        sendButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }
    
    /**
     * Move the frame to front
     */
    public void toFront() {
        supportFrame.toFront();
    }
    
}
