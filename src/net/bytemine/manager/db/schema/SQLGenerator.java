/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db.schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Generates SQL statements for database schema updates
 *
 * @author Daniel Rauer
 */
public class SQLGenerator {

    private Logger logger = Logger.getLogger(SQLGenerator.class.getName());

    private String currentTableName = null;


    /**
     * Generates SQL
     *
     * @param entries String[] like [0]:tableName, [1]:columnName, [2]:columnType
     * @return A List with SQL commands
     * @throws Exception
     */
    public List<String> generateSQL(Vector<String[]> entries) throws Exception {
        List<String> statements = new ArrayList<String>();
        try {
            SQLStatementModel model = null;

            // iterate over all entries
            for (Iterator<String[]> iterator = entries.iterator(); iterator.hasNext();) {
                String[] entry = (String[]) iterator.next();

                if (!entry[0].equals(currentTableName)) {
                    // the table name has changed, so a new model is needed
                    currentTableName = entry[0];

                    if (model != null) {
                        // create SQL statement and add it to return List
                        statements.addAll(model.createSQL());
                    }

                    model = new SQLStatementModel();
                    model.setTableName(currentTableName);
                }

                if (entry[1] == null)
                    // tableName, null, null
                    // table is not existing
                    model.setCreate(true);
                else {
                    // tableName, columnName, columnType
                    model.addColumnNameAndType(entry[1], entry[2]);
                }
            }

            if (model != null)
                // create the sql of the last model
                statements.addAll(model.createSQL());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error generating update SQL statements", e);
            throw e;
        }
        return statements;
	}
	
}
