/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Florian Reichel                E-Mail:    reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.awt.event.MouseEvent;
import java.util.HashMap;


interface AbstractOverviewTableModel
{
    public void reinit();
    public void reloadData();
    public void refreshMapping();
    
    public String getColumnName(int col);
    public int getRowCount();
    public int getColumnCount();
    public Object getValueAt(int row, int col);
    public boolean isCellEditable(int row, int col);
    public void setValueAt(String value, int row, int col);
    public HashMap<String, String> getIdRowMapping();
    public void setIdRowMapping(HashMap<String, String> idRowMap);
    public void addIdRowMapping(String key, String value);
    
    public abstract class ColumnListener {
        public abstract void mouseClicked(MouseEvent e);
    }
}