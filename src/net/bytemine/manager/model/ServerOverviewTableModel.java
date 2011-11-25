/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.AbstractTableModel;

import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.StringUtils;


/**
 * A table model for the server table
 *
 * @author Daniel Rauer
 */
public class ServerOverviewTableModel extends AbstractTableModel implements AbstractOverviewTableModel {

    private static final long serialVersionUID = 7254507708527347679L;

    ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    protected int sortCol = 0;
    protected boolean isSortAsc = true;

    // the column names to display    
    String[] columnNames = {
            rb.getString("server.overview.column2"),
            rb.getString("server.overview.column3"),
            rb.getString("server.overview.column4")};

    // contains the data of a table row
    Vector<String[]> rowData = ServerQueries.getServerOverviewForTable(this);

    // map in which row numbers and ids are stored as key-value-pairs
    private static HashMap<String, String> idRowMapping;


    /**
     * Reinitializes the model by translating the column names
     * using the users resource bundle
     */
    public void reinit() {
        rb = ResourceBundleMgmt.getInstance().getUserBundle();
        columnNames = new String[] {
                rb.getString("server.overview.column2"),
                rb.getString("server.overview.column3"),
                rb.getString("server.overview.column4")};
    }


    /**
     * Reloads the data from the database
     */
    public void reloadData() {
        rowData = ServerQueries.getServerOverviewForTable(this);
        Collections.sort(rowData, new ServerComparator(isSortAsc, sortCol));
        refreshMapping();
        fireTableDataChanged();
    }

    
    /**
     * Refresh the mapping
     */
    public void refreshMapping() {
        int row = 0;
        for (Iterator<String[]> iterator = rowData.iterator(); iterator.hasNext();) {
            String[] rowData = (String[]) iterator.next();
            addIdRowMapping(row + "", rowData[3]);
            row++;
        }
    }
    

    public String getColumnName(int col) {
        String columnName = columnNames[col];
        if (col == sortCol)
            columnName += isSortAsc ? " <" : " >";
        return columnName;
    }


    public int getRowCount() {
        return rowData.size();
    }


    public int getColumnCount() {
        return columnNames.length;
    }


    public Object getValueAt(int row, int col) {
        Object[] rowStr = (Object[]) rowData.get(row);
        return rowStr[col];
    }


    public boolean isCellEditable(int row, int col) {
        return false;
    }


    public void setValueAt(String value, int row, int col) {
        Object[] rowStr = (Object[]) rowData.get(row);
        rowStr[col] = value;
        fireTableCellUpdated(row, col);
    }


    public HashMap<String, String> getIdRowMapping() {
        return idRowMapping;
    }

    public void setIdRowMapping(HashMap<String, String> idRowMap) {
        idRowMapping = idRowMap;
    }

    public void addIdRowMapping(String key, String value) {
        if (idRowMapping == null)
            idRowMapping = new HashMap<String, String>();
        idRowMapping.put(key, value);
    }


    public class ColumnListener extends MouseAdapter {
        protected JTable table;

        public ColumnListener(JTable t) {
            table = t;
        }

        public void mouseClicked(MouseEvent e) {
            TableColumnModel colModel = table.getColumnModel();
            int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(columnModelIndex)
                    .getModelIndex();

            if (modelIndex < 0)
                return;
            if (sortCol == modelIndex)
                isSortAsc = !isSortAsc;
            else
                sortCol = modelIndex;

            for (int i = 0; i < getColumnCount(); i++) {
                TableColumn column = colModel.getColumn(i);
                column.setHeaderValue(getColumnName(column.getModelIndex()));
            }
            table.getTableHeader().repaint();

            Collections.sort(rowData, new X509Comparator(isSortAsc, sortCol));
            // refresh the mapping
            refreshMapping();
            
            table.tableChanged(new TableModelEvent(
                    ServerOverviewTableModel.this));
            table.repaint();
        }
    }

}


class ServerComparator implements Comparator<String[]> {
    protected boolean isSortAsc;
    protected int sortCol;

    public ServerComparator(boolean sortAsc, int sortCol) {
        this.isSortAsc = sortAsc;
        this.sortCol = sortCol;
    }

    public int compare(String[] s1, String[] s2) {
        int result = 0;
        String i1_str = s1[sortCol];
        String i2_str = s2[sortCol];
        if (i1_str == null)
            i1_str = "";
        if (i2_str == null)
            i2_str = "";

        if (StringUtils.isDigit(i1_str) && StringUtils.isDigit(i2_str)) {
            Integer i1 = Integer.parseInt(i1_str);
            Integer i2 = Integer.parseInt(i2_str);
            result = i1.compareTo(i2);
        } else {
            result = i1_str.compareTo(i2_str);
        }


        if (!isSortAsc)
            result = -result;
        return result;
    }


    public boolean equals(Object obj) {
        if (obj instanceof ServerComparator) {
            ServerComparator compObj = (ServerComparator) obj;
            return compObj.isSortAsc == isSortAsc;
        }
        return false;
    }

}