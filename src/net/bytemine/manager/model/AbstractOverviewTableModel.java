/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Florian Reichel                E-Mail:    reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.awt.event.MouseEvent;
import java.util.HashMap;


interface AbstractOverviewTableModel
{
    void reinit();
    void reloadData();
    void refreshMapping();
    
    String getColumnName(int col);
    int getRowCount();
    int getColumnCount();
    Object getValueAt(int row, int col);
    boolean isCellEditable(int row, int col);
    void setValueAt(String value, int row, int col);
    HashMap<String, String> getIdRowMapping();
    void setIdRowMapping(HashMap<String, String> idRowMap);
    void addIdRowMapping(String key, String value);
    
    abstract class ColumnListener {
        public abstract void mouseClicked(MouseEvent e);
    }
}