/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.css;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * Manages css rules
 * Mainly taken from http://today.java.net/pub/a/today/2003/10/14/swingcss.html,
 * published under BSD license
 *
 * @author Daniel Rauer
 */
public class CssRule {

    private static Logger logger = Logger.getLogger(CssRule.class.getName());

    private Class<?> clss;
    private String propertyName;
    private String value;


    public CssRule(Class<?> clss, String propertyName, String value) {
        this.clss = clss;
        this.propertyName = propertyName;
        this.value = value;
    }


    public boolean matches(Object obj) {
        if (clss.isInstance(obj)) {
            return true;
        }
        return false;
    }


    /**
     * Applies a rule to the corresponding component
     *
     * @param obj The component to style
     */
    public void apply(Object obj) {
        JComponent component = null;
        try {
            component = (JComponent) obj;
        } catch (Exception e) {
            logger.severe("Cannot style object: " + obj.getClass().getName());
        }
        
        if (propertyName.equals("background")) {
            component.setBackground(Color.decode(value));
        }
        if (propertyName.equals("foreground")) {
            component.setForeground(Color.decode(value));
        }
        if (propertyName.equals("margin")) {
            int margin = Integer.parseInt(value);
            Border m_border =
                    BorderFactory.createEmptyBorder(margin, margin, margin, margin);
            Border c_border =
                    BorderFactory.createCompoundBorder(component.getBorder(), m_border);
            component.setBorder(c_border);
        }
        if (propertyName.equals("alignment")) {
            if (component instanceof JLabel) {
                int align = -1;
                if (value.equals("left")) {
                    align = SwingConstants.LEFT;
                }
                if (value.equals("center")) {
                    align = SwingConstants.CENTER;
                }
                if (value.equals("right")) {
                    align = SwingConstants.RIGHT;
                }
                ((JLabel) component).setHorizontalAlignment(align);
            }
        }
        if (propertyName.equals("font-style")) {
            if (value.equals("bold"))
                component.setFont(component.getFont().deriveFont(Font.BOLD));
            else if (value.equals("italics"))
                component.setFont(component.getFont().deriveFont(Font.ITALIC));
            else if (value.equals("plain"))
                component.setFont(component.getFont().deriveFont(Font.PLAIN));
        }
        if (propertyName.equals("layout")) {
            if (value.equals("column")) {
                component.setLayout(new BoxLayout(component, BoxLayout.Y_AXIS));
            }
        }
    }


    /**
     * Applies a rule to the given dialog
     *
     * @param dialog The dialog to style
     */
    public void applyToDialog(JDialog dialog) {
        if (propertyName.equals("background")) {
            dialog.setBackground(Color.decode(value));
        }
        if (propertyName.equals("foreground")) {
            dialog.setForeground(Color.decode(value));
        }
        if (propertyName.equals("font-style")) {
            if (value.equals("bold")) {
                dialog.setFont(dialog.getFont().deriveFont(Font.BOLD));
            }
            if (value.equals("italics")) {
                dialog.setFont(dialog.getFont().deriveFont(Font.ITALIC));
            }
        }
        if (propertyName.equals("layout")) {
            if (value.equals("column")) {
                dialog.setLayout(new BoxLayout(dialog, BoxLayout.Y_AXIS));
            }
        }
    }


    public String getClss() {
        return clss.getName();
    }

}
