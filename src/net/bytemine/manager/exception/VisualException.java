/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.exception;

import java.awt.Frame;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import net.bytemine.manager.gui.CustomJOptionPane;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;

/**
 * handles exceptions that will be displayed to the user
 * Extends Exception
 *
 * @author Daniel Rauer
 */
public class VisualException extends Exception {

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(VisualException.class.getName());

    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    private String errorTitle = rb.getString("error.general.title");
    private String errorText = rb.getString("error.general.text");


    public VisualException(Throwable t) {
        super(t);
        logger.log(Level.SEVERE, errorText, t);
        displayException(t);
    }

    public VisualException(Throwable t, String title) {
        super(t);
        logger.log(Level.SEVERE, errorText, t);
        displayException(t, title);
    }

    public VisualException(String message, String title, Throwable t) {
        super(message, t);
        logger.log(Level.SEVERE, message, t);
        displayException(message, title);
    }


    public VisualException(Frame parentFrame, Throwable t) {
        super(t);
        logger.log(Level.SEVERE, t.getMessage(), t);
        displayException(parentFrame, t);
    }


    public VisualException(Frame parentFrame, String message, String title, Throwable t) {
        super(message, t);
        logger.log(Level.SEVERE, message, t);
        displayException(parentFrame, message, title);
    }


    public VisualException(String message, Throwable t) {
        super(message, t);
        logger.log(Level.SEVERE, message, t);
        displayException(message);
    }

    public VisualException(String message) {
        super(message);
        logger.log(Level.SEVERE, message);
        displayException(message);
    }

    public VisualException(String message, String title) {
        super(message);
        logger.log(Level.SEVERE, message);
        displayException(message, title);
    }

    public VisualException() {
        logger.log(Level.SEVERE, "an error occurred");
        displayException();
    }


    private void displayException(String message) {
        Frame parentFrame = ManagerGUI.mainFrame;
        displayException(parentFrame, message);
    }


    private void displayException(String message, String title) {
        Frame parentFrame = ManagerGUI.mainFrame;
        displayException(parentFrame, message, title);
    }


    private void displayException(Throwable t) {
        Frame parentFrame = ManagerGUI.mainFrame;
        displayException(parentFrame, t.getMessage());
    }


    private void displayException(Throwable t, String title) {
        Frame parentFrame = ManagerGUI.mainFrame;
        displayException(parentFrame, t.getMessage(), title);
    }


    private void displayException() {
        Frame parentFrame = ManagerGUI.mainFrame;

        displayException(parentFrame, errorText, errorTitle);
    }


    private void displayException(Frame parentFrame, String message) {
        displayException(parentFrame, message, errorTitle);
    }

    private void displayException(Frame parentFrame, Throwable t) {
        displayException(parentFrame, t, errorTitle);
    }


    private void displayException(Frame parentFrame, Throwable t, String title) {
        displayException(parentFrame, t.getMessage(), title);
    }


    private void displayException(Frame parentFrame, String message, String title) {
        CustomJOptionPane.showMessageDialog(parentFrame, message, title,
                JOptionPane.ERROR_MESSAGE);
    }

}
