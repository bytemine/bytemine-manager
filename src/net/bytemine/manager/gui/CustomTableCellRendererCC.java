/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *

 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.bytemine.manager.Constants;
import net.bytemine.utility.StringUtils;

/**
 * Cell renderer for the tables
 * @author Daniel Rauer
 *
 */
public class CustomTableCellRendererCC extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 8144660529561691989L;

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);
        if (cell instanceof JLabel) {
            JLabel label = (JLabel) cell;
            if (StringUtils.isDigit(label.getText()))
                label.setHorizontalAlignment(JLabel.RIGHT);
            else
                label.setHorizontalAlignment(JLabel.LEFT);
            
            label.setText(" " + label.getText() + " ");
            cell = label;
        }
        if (row % 2 == 0) {
            cell.setBackground(Constants.COLOR_ROW1);
        } else
            cell.setBackground(Constants.COLOR_ROW2);
        return cell;
    }
}
