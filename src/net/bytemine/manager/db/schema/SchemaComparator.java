/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 * Compares two SchemaModels and stores the differences as Vector of String[3]
 *
 * @author Daniel Rauer
 */
public class SchemaComparator {

    // all differences of the tables
    private Vector<String[]> entries = new Vector<String[]>();

    /**
     * Compares the two models
     *
     * @param oldModel The oldModel, containing the full featured database schema
     * @param newModel The new and incomplete model
     * @return All differences of the models als Vector<String[3]>
     */
    public Vector<String[]> compare(SchemaModel oldModel, SchemaModel newModel) {
        HashMap<String, HashMap<String, String>> completeTables = oldModel.getTables();
        HashMap<String, HashMap<String, String>> incompleteTables = newModel.getTables();

        for (Iterator<String> iterator = completeTables.keySet().iterator(); iterator.hasNext();) {
            String tableName = (String) iterator.next();
            if (!incompleteTables.containsKey(tableName)) {
                // the new schema does not contain this table
                // will result in 'CREATE table <tableName>' statement
                createEntry(tableName);
            }
            HashMap<String, String> tableEntries = completeTables.get(tableName);
            for (Iterator<String> iterator2 = tableEntries.keySet().iterator(); iterator2
                    .hasNext();) {
                String columnName = (String) iterator2.next();
                String columnType = tableEntries.get(columnName);

                if (!incompleteTables.containsKey(tableName)) {
                    // the new schema does not contain this table
                    createEntry(tableName, columnName, columnType);
                } else {
                    HashMap<String, String> incompleteTableEntries = incompleteTables.get(tableName);
                    if (!incompleteTableEntries.containsKey(columnName)) {
                        // the table exists, but not the column.
                        createEntry(tableName, columnName, columnType);
                    }
                }
            }
        }

        return entries;
    }


    /**
     * creates a String[3] with only the table name in String[0]
     *
     * @param tableName The table name
     */
    private void createEntry(String tableName) {
        createEntry(tableName, null, null);
    }


    /**
     * creates a String[3]
     *
     * @param tableName  The table name
     * @param columnName The column name
     * @param type       The column type
     */
    private void createEntry(String tableName, String columnName, String type) {
        String[] entry = new String[3];
        entry[0] = tableName;
        entry[1] = columnName;
        entry[2] = type;
		entries.add(entry);
	}
	

}
