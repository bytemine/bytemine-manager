/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * JLabel subclass in order to style data label with css
 *
 * @author Daniel Rauer
 */
public class JDataLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    public JDataLabel() {
        super();
    }

    public JDataLabel(String text) {
        super(text);
    }

    public JDataLabel(Icon icon) {
        super(icon);
    }

    public JDataLabel(Icon icon, int horizontalAlignment) {
        super(icon, horizontalAlignment);
    }

    public JDataLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    public JDataLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }
}
