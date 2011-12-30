/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db.schema;

import java.util.HashMap;
import java.util.Set;


/**
 * Model which holds schema data of a database.
 * That means table names, column names and column types
 *
 * @author Daniel Rauer
 */
public class SchemaModel {

    // holds the schema data
    // tableName, <columnName, columnType>
    private HashMap<String, HashMap<String, String>> tables = new HashMap<String, HashMap<String, String>>();


    /**
     * Adds the complete table schema
     *
     * @param tableName           The table name
     * @param columnNamesAndTypes The names and types of the table columns
     */
    public void addTable(String tableName, HashMap<String, String> columnNamesAndTypes) {
        tables.put(tableName, columnNamesAndTypes);
    }


    /**
     * Returns all table names
     *
     * @return Set with table names
     */
    public Set<String> getTableNames() {
        return tables.keySet();
    }


    /**
     * Returns the schema data
     *
     * @return The schema data as HashMaps
     */
    public HashMap<String, HashMap<String, String>> getTables() {
        return tables;
    }

}
