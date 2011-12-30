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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import net.bytemine.manager.Constants;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.StringUtils;


/**
 * A table model for the x509 table
 *
 * @author Daniel Rauer
 */
public class X509OverviewTableModel extends AbstractTableModel implements AbstractOverviewTableModel {

    private static final long serialVersionUID = 1L;

    ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    protected int sortCol = 2;
    protected boolean isSortAsc = true;

    // the column names to display    
    String[] columnNames = {
            rb.getString("x509.overview.column2"),
            rb.getString("x509.overview.column3"),
            rb.getString("x509.overview.column4"),
            rb.getString("x509.overview.column5")};

    // contains the data of a table row
    Vector<String[]> rowData = X509Queries.getX509Overview(this);

    // map in which row numbers and ids are stored as key-value-pairs
    private static HashMap<String, String> idRowMapping;


    /**
     * Reinitializes the model by translating the column names
     * using the users resource bundle
     */
    public void reinit() {
        rb = ResourceBundleMgmt.getInstance().getUserBundle();
        columnNames = new String[] {
                rb.getString("x509.overview.column2"),
                rb.getString("x509.overview.column3"),
                rb.getString("x509.overview.column4"),
                rb.getString("x509.overview.column5")};
    }


    /**
     * Reloads the data from the database
     */
    public void reloadData() {
        rowData = X509Queries.getX509Overview(this);
        Collections.sort(rowData, new X509Comparator(isSortAsc, sortCol));
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
            addIdRowMapping(row + "", rowData[4]);
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
        String[] rowStr = (String[]) rowData.get(row);
        return rowStr[col];
    }


    public boolean isCellEditable(int row, int col) {
        return false;
    }


    public void setValueAt(String value, int row, int col) {
        String[] rowStr = (String[]) rowData.get(row);
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
                    X509OverviewTableModel.this));
            table.repaint();
        }
    }
}


class X509Comparator implements Comparator<String[]> {
    protected boolean isSortAsc;
    protected int sortCol;

    public X509Comparator(boolean sortAsc, int sortCol) {
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

        Date d1 = null;
        Date d2 = null;
        try {
            d1 = Constants.getShowFormatForCurrentLocale().parse(i1_str);
            d2 = Constants.getShowFormatForCurrentLocale().parse(i2_str);
        } catch(Exception e) {
            try {
                d1 = Constants.getDetailedFormatForCurrentLocale().parse(i1_str);
                d2 = Constants.getDetailedFormatForCurrentLocale().parse(i2_str);
            } catch(Exception e2) {}
        }
        
        if (StringUtils.isDigit(i1_str) && StringUtils.isDigit(i2_str)) {
            Long l1 = new Long(i1_str);
            Long l2 = new Long(i2_str);
            result = l1.compareTo(l2);
        } else if(d1 != null && d2 != null) {
            // compare the dates
            result = d1.compareTo(d2);
        } else {
            result = i1_str.compareTo(i2_str);
        }


        if (!isSortAsc)
            result = -result;
        return result;
    }

    public boolean equals(Object obj) {
        if (obj instanceof X509Comparator) {
            X509Comparator compObj = (X509Comparator) obj;
            return compObj.isSortAsc == isSortAsc;
        }
        return false;
    }

}