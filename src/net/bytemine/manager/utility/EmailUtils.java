/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.utility;

import java.net.UnknownHostException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.SwingWorker;

import com.sun.mail.smtp.SMTPAddressFailedException;

import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.CustomJOptionPane;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.SupportModel;


/**
 * Provides functions to send out emails, e.g. for support requests
 * @author Daniel Rauer
 *
 */
public class EmailUtils {

    private static Logger logger = Logger.getLogger(EmailUtils.class.getName());
    
    public static void main(String[] args) {
        sendTestMail();
    }
    
    /**
     * Sends a support request based on the given SupportModel 
     * @param model The SupportModel
     */
    public static void sendSupportRequest(SupportModel model) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        Properties props = new Properties();
        props.put("mail.smtp.host", Constants.SUPPORT_SERVER);
        Session s = Session.getInstance(props,null);
        MimeMessage mimeMessage = new MimeMessage(s);
        InternetAddress from = null;
        InternetAddress to = null;
        try {
            from = new InternetAddress(model.getMail(), true);
            to = new InternetAddress(Constants.SUPPORT_EMAIL, true);
        } catch (AddressException e) {
            logger.log(Level.SEVERE, "Email address invalid", e);
            new VisualException(rb.getString("supportformular.mail_error.address"), rb.getString("supportformular.mail_error.title"));
            return;
        }
        try {
            mimeMessage.setFrom(from);
            mimeMessage.addRecipient(Message.RecipientType.TO, to);
            mimeMessage.setSubject("Support request for the bytemine manager");
            mimeMessage.setText(model.printRequest());

            sendMail(mimeMessage);
        } catch (MessagingException e) {
            if (e.getCause().getClass().isInstance(new UnknownHostException())) {
                logger.log(Level.SEVERE, "Unknown host error", e);
                new VisualException(rb.getString("supportformular.mail_error.unknown_host"), rb.getString("supportformular.mail_error.title"));
            } else {
                logger.log(Level.SEVERE, "Messaging error", e);
                new VisualException(rb.getString("supportformular.mail_error.message"), rb.getString("supportformular.mail_error.title"));
            }
        }           
    }
    
    /**
     * Sends out a mail based on the given mimeMessage
     * @param mimeMessage The mimeMessage to send
     */
    private static void sendMail(final MimeMessage mimeMessage) {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            boolean exceptionOccured = false;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.support.tooltip"));

                try {
                    Transport.send(mimeMessage);
                } catch (SMTPAddressFailedException e) {
                    exceptionOccured = true;
                    logger.log(Level.SEVERE, "SMTP error", e);
                    new VisualException(rb.getString("supportformular.mail_error.smtp"), rb.getString("supportformular.mail_error.title"));
                }  catch (SendFailedException e) {
                    exceptionOccured = true;
                    logger.log(Level.SEVERE, "Send failed error", e);
                    new VisualException(rb.getString("supportformular.mail_error.send"), rb.getString("supportformular.mail_error.title"));
                } catch (MessagingException e) {
                    exceptionOccured = true;
                    if (e.getCause().getClass().isInstance(new UnknownHostException())) {
                        logger.log(Level.SEVERE, "Unknown host error", e);
                        new VisualException(rb.getString("supportformular.mail_error.unknown_host"), rb.getString("supportformular.mail_error.title"));
                    } else {
                        logger.log(Level.SEVERE, "Messaging error", e);
                        new VisualException(rb.getString("supportformular.mail_error.message"), rb.getString("supportformular.mail_error.title"));
                    }
                }           

                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
                
                // this email is a support request
                if (ManagerGUI.supportForm != null) {
                    if (!exceptionOccured) {
                        // close the form and show a success message
                        ManagerGUI.supportForm.dispose();
                        CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame,
                                rb.getString("supportformular.mail_success.text"),
                                rb.getString("supportformular.title"),
                                CustomJOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // move the form to front and enable the buttons
                        ManagerGUI.supportForm.toFront();
                        ManagerGUI.supportForm.enableButtons();
                    }
                }
                
            }

        };
        generateWorker.execute();
    }

    /**
     * Sends a test email
     */
    public static void sendTestMail() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail1.bytemine.net");
        Session s = Session.getInstance(props,null);
        MimeMessage message = new MimeMessage(s);
        InternetAddress from = null;
        InternetAddress to = null;
        try {
            from = new InternetAddress("manager_support@bytemine.net", true);
            to = new InternetAddress("rauer@bytemine.net", true);
        } catch (AddressException e) {
            e.printStackTrace();
        }
        try {
            message.setFrom(from);
            message.addRecipient(Message.RecipientType.TO, to);
            message.setSubject("Test from JavaMail.");
            message.setText("Hello from JavaMail!");

            sendMail(message);
        } catch (SendFailedException e) {
            System.out.println("send failed");
            e.printStackTrace();
        } catch (MessagingException e) {
            System.out.println("messsage exception");
            if (e.getCause().getClass().isInstance(new UnknownHostException())) {
                System.out.println("unknown host");
            }
            e.printStackTrace();
        } 
    }
}
