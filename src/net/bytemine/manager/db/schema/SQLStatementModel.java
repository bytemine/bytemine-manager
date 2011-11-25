/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Holds data needed for generating SQL statements
 * Also generates the statements
 *
 * @author Daniel Rauer
 */
public class SQLStatementModel {

    // flag if create or alter statement shall be created
    private boolean create = false;
    private String tableName;
    private HashMap<String, String> columnNameAndType = new HashMap<String, String>();
    // a list with all statements for one table
    private List<String> statements = new ArrayList<String>();


    /**
     * triggers the sql creation
     *
     * @return A List with all sql statements for one table
     */
    public List<String> createSQL() {
        if (create)
            createCreateStatment();
        else
            createAlterStatments();

        return statements;
    }


    /**
     * generates a create statement
     */
    private void createCreateStatment() {
        StringBuffer createStatement = new StringBuffer();
        createStatement.append("CREATE TABLE " + tableName + "(");
        for (Iterator<String> iterator = columnNameAndType.keySet().iterator(); iterator.hasNext();) {
            String columnName = (String) iterator.next();
            String columnType = columnNameAndType.get(columnName);
            createStatement.append(columnName + " " + columnType);
            if (iterator.hasNext())
                createStatement.append(", ");
            else
                createStatement.append(")");
        }
        statements.add(createStatement.toString());
    }


    /**
     * generates an alter statement
     */
    private void createAlterStatments() {
        for (Iterator<String> iterator = columnNameAndType.keySet().iterator(); iterator.hasNext();) {
            StringBuffer alterStatement = new StringBuffer();
            alterStatement.append("ALTER TABLE " + tableName + " ADD ");
            String columnName = (String) iterator.next();
            String columnType = columnNameAndType.get(columnName);
            alterStatement.append(columnName + " " + columnType);

            statements.add(alterStatement.toString());
        }
    }


    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public HashMap<String, String> getColumnNameAndType() {
        return columnNameAndType;
    }

    public void setColumnNameAndType(HashMap<String, String> columnNameAndType) {
        this.columnNameAndType = columnNameAndType;
    }

    public void addColumnNameAndType(String columnName, String columnType) {
        this.columnNameAndType.put(columnName, columnType);
	}
}
