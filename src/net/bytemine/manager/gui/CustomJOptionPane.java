/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.Component;
import java.awt.HeadlessException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.bytemine.manager.css.CssRuleManager;

/**
 * Enhanced JOptionPane for the use with CssRuleManager
 *
 * @author Daniel Rauer
 */
public class CustomJOptionPane extends JOptionPane {

    private static final long serialVersionUID = -6386412521720670452L;
    private static Logger logger = Logger.getLogger(CustomJOptionPane.class.getName());
    
    public static void showMessageDialog(Component parentComponent,
                                         Object message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog;
        if (parentComponent != null)
            dialog = pane.createDialog(parentComponent, null);
        else
            dialog = pane.createDialog(null);
        CssRuleManager.getInstance().format(pane);

        dialog.pack();
        dialog.setVisible(true);
    }


    public static void showMessageDialog(Component parentComponent,
                                         Object message, String title) {
        JOptionPane pane = new JOptionPane(message);
        JDialog dialog = parentComponent != null ? pane.createDialog(parentComponent, title) : pane.createDialog(title);
        CssRuleManager.getInstance().format(pane);

        dialog.pack();
        dialog.setVisible(true);
    }


    public static void showMessageDialog(Component parentComponent,
                                         Object message, String title,
                                         int messageType) {
    	 JOptionPane pane = new JOptionPane(message, messageType);

		 JDialog dialog = parentComponent != null ? pane.createDialog(parentComponent, title) : pane.createDialog(title);
		 CssRuleManager.getInstance().format(pane);
		
		 dialog.pack();
		 dialog.setVisible(true);
    }
    

    public static int showOptionDialog(Component parentComponent,
            Object message, String title,
            int optionType, int messageType,
            Icon icon, Object[] options,
            Object initialValue) {
        return showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue, -1);
    }
    

    public static int showOptionDialog(Component parentComponent,
                                       Object message, String title,
                                       int optionType, int messageType,
                                       Icon icon, Object[] options,
                                       Object initialValue, int focusPosition) {
    	
        JOptionPane pane;
        
        if (focusPosition == -1) {
        	// no focus
        	pane= new JOptionPane(message, messageType,
        			optionType, icon,
        			options, initialValue);
        } else {
        	// focus on element
        	Component get_field = null;
        	try {
        		get_field = (Component)((Object[])message)[focusPosition];
        	} catch (ClassCastException e) {
        		get_field = ((JPanel)message).getComponent(focusPosition);
        	} catch (Exception e) {
        		logger.log(Level.WARNING, "Couldn't focus on element ", e);
        	}
        		
        	final Component field = get_field;
        	
    		pane = new JOptionPane(message, messageType,
    							   optionType, icon,
    							   options, initialValue) {
                    private static final long serialVersionUID = -1070752361451762069L;

                    @Override
    				public void selectInitialValue() {
                        if (field != null)
                            field.requestFocusInWindow();
    				}
    		};
        }
         
        pane.setInitialValue(initialValue);
        CssRuleManager.getInstance().format(pane);

        JDialog dialog;
        if (parentComponent != null)
            dialog = pane.createDialog(parentComponent, title);
        else
            dialog = pane.createDialog(title);

        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();

        Object selectedValue = pane.getValue();

        if (selectedValue == null) {
            return JOptionPane.CLOSED_OPTION;
        }
        if (options == null) {
            if (selectedValue instanceof Integer) {
                return (Integer) selectedValue;
            }
            return JOptionPane.CLOSED_OPTION;
        }
        for (int counter = 0, maxCounter = options.length;
             counter < maxCounter; counter++) {
            if (options[counter].equals(selectedValue)) {
                return counter;
            }
        }
        return JOptionPane.CLOSED_OPTION;
    }
    

    public static int showConfirmDialog(Component parentComponent,
                                        Object message) throws HeadlessException {

        return showConfirmDialog(parentComponent, message,
                UIManager.getString("OptionPane.titleText"),
                YES_NO_CANCEL_OPTION);
    }


    public static int showConfirmDialog(Component parentComponent,
                                        Object message, String title, int optionType)
            throws HeadlessException {

        return showConfirmDialog(parentComponent, message,
                title, optionType, QUESTION_MESSAGE);
    }

    
    public static int showConfirmDialog(Component parentComponent,
                                        Object message, String title, int optionType, int messageType)
            throws HeadlessException {

        return showConfirmDialog(parentComponent, message, title, optionType,
                messageType, null);
    }


    public static int showConfirmDialog(Component parentComponent,
                                        Object message, String title, int optionType,
                                        int messageType, Icon icon) throws HeadlessException {

        return showOptionDialog(parentComponent, message, title, optionType,
                messageType, icon, null, null, -1);
    }

    
}
