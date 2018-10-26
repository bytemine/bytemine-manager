/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
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
import java.util.stream.IntStream;

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

    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private int sortCol = 0;
    private boolean isSortAsc = true;

    // the column names to display    
    private String[] columnNames = {
            rb.getString("server.overview.column2"),
            rb.getString("server.overview.column3"),
            rb.getString("server.overview.column4")};

    // contains the data of a table row
    private Vector<String[]> rowData = ServerQueries.getServerOverviewForTable(this);

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
        rowData.sort(new ServerComparator(isSortAsc, sortCol));
        refreshMapping();
        fireTableDataChanged();
    }

    
    /**
     * Refresh the mapping
     */
    public void refreshMapping() {
        int row = 0;
        for (String[] rowData : rowData) {
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
        Object[] rowStr = rowData.get(row);
        return rowStr[col];
    }


    public boolean isCellEditable(int row, int col) {
        return false;
    }


    public void setValueAt(String value, int row, int col) {
        Object[] rowStr = rowData.get(row);
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
            idRowMapping = new HashMap<>();
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

            IntStream.range(0, getColumnCount()).mapToObj(colModel::getColumn).forEach(column -> column.setHeaderValue(getColumnName(column.getModelIndex())));
            table.getTableHeader().repaint();

            rowData.sort(new X509Comparator(isSortAsc, sortCol));
            // refresh the mapping
            refreshMapping();
            
            table.tableChanged(new TableModelEvent(
                    ServerOverviewTableModel.this));
            table.repaint();
        }
    }

}


class ServerComparator implements Comparator<String[]> {
    private boolean isSortAsc;
    private int sortCol;

    ServerComparator(boolean sortAsc, int sortCol) {
        this.isSortAsc = sortAsc;
        this.sortCol = sortCol;
    }

    public int compare(String[] s1, String[] s2) {
        return compare(s1, s2);
    }


    public boolean equals(Object obj) {
        if (obj instanceof ServerComparator) {
            ServerComparator compObj = (ServerComparator) obj;
            return compObj.isSortAsc == isSortAsc;
        }
        return false;
    }

}