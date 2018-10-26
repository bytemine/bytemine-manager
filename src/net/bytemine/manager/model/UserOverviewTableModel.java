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

import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.StringUtils;


/**
 * A table model for the user table
 *
 * @author Daniel Rauer
 */
public class UserOverviewTableModel extends AbstractTableModel implements AbstractOverviewTableModel {

    private static final long serialVersionUID = -5940231640180913764L;

    ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private int sortCol = 0;
    private boolean isSortAsc = true;

    // the column names to display    
    private String[] columnNames = {
            rb.getString("user.overview.column2"),
            rb.getString("user.overview.column3"),
            rb.getString("user.overview.column4")
    };

    // contains the data of a table row
    private Vector<String[]> rowData = UserQueries.getAllUsersAsVectorForTable(this);

    // map in which row numbers and ids are stored as key-value-pairs
    private static HashMap<String, String> idRowMapping;


    /**
     * Reinitializes the model by translating the column names
     * using the users resource bundle
     */
    public void reinit() {
        rb = ResourceBundleMgmt.getInstance().getUserBundle();
        columnNames = new String[] {
                rb.getString("user.overview.column2"),
                rb.getString("user.overview.column3"),
                rb.getString("user.overview.column4")
        };
    }


    /**
     * Reloads the data from the database
     */
    public void reloadData() {
        rowData = UserQueries.getAllUsersAsVectorForTable(this);
        rowData.sort(new UserComparator(isSortAsc, sortCol));
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


    public int getRowCount() {
        return rowData.size();
    }


    public String getColumnName(int col) {
        String columnName = columnNames[col];
        if (col == sortCol)
            columnName += isSortAsc ? " <" : " >";
        return columnName;
    }


    public int getColumnCount() {
        return columnNames.length;
    }


    public Object getValueAt(int row, int col) {
        String[] rowStr = rowData.get(row);
        return rowStr[col];
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

            rowData.sort(new UserComparator(isSortAsc, sortCol));
            // refresh the mapping
            refreshMapping();
            
            table.tableChanged(new TableModelEvent(
                    UserOverviewTableModel.this));
            table.repaint();
        }
    }

}


class UserComparator implements Comparator<String[]> {
    private boolean isSortAsc;
    private int sortCol;

    UserComparator(boolean sortAsc, int sortCol) {
        this.isSortAsc = sortAsc;
        this.sortCol = sortCol;
    }

    public int compare(String[] s1, String[] s2) {
        return compare(s1, s2, sortCol, isSortAsc);
    }

    private static int compare(String[] s1, String[] s2, int sortCol, boolean isSortAsc) {
        int result;
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
        if (obj instanceof UserComparator) {
            UserComparator compObj = (UserComparator) obj;
            return compObj.isSortAsc == isSortAsc;
        }
        return false;
    }

}