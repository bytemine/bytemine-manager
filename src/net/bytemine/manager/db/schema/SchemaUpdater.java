/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db.schema;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.utility.X509Serializer;


/**
 * Does all the schema update process
 *
 * @author Daniel Rauer
 */
public class SchemaUpdater {

    private static Logger logger = Logger.getLogger(SchemaUpdater.class.getName());

    private String classname;
    private String url;
    private String xmlFile;


    private SchemaUpdater(String newClassname, String newUrl) {
        this.classname = newClassname;
        this.url = newUrl;
    }


    public SchemaUpdater(String xmlFile) {
        this.xmlFile = xmlFile;
    }


    /**
     * Triggers the update process from an xml schema
     */
    public void updateFromXml() throws Exception {
        logger.info("Updating DB schema from xml");

        // get database schema from the new, fully equiped database
        SchemaModel model1 = detectFromXml(this.xmlFile);

        Connection conn = DBConnector.getInstance().getConnection();
        // get database schema from the old, to be updated database
        SchemaModel model2 = detect(conn);

        // compares the two database models
        SchemaComparator comp = new SchemaComparator();
        // the part of the schema that need to be updated
        Vector<String[]> entries = comp.compare(model1, model2);

        // generates SQL from the missing schema part
        SQLGenerator sqlGen = new SQLGenerator();
        // sql statements to be applied to the outdated database
        List<String> statements = sqlGen.generateSQL(entries);
        try {
            // run the sql commands
            executeSQL(statements);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "errors occured during database update", e);
            throw e;
        }
    }


    /**
     * Triggers the update process
     */
    public void update() throws Exception {
        logger.info("Updating DB schema");

        Connection conn1 = DBConnector.getInstance().getConnection();
        // get database schema from the new, fully equiped database
        SchemaModel model1 = detect(conn1);

        // connect to the outdated database
        DBConnector.resetInstance();
        DBConnector.getInstance(
                this.classname,
                this.url);

        Connection conn2 = DBConnector.getInstance().getConnection();
        // get database schema from the old, to be updated database
        SchemaModel model2 = detect(conn2);

        // compares the two database models
        SchemaComparator comp = new SchemaComparator();
        // the part of the schema that need to be updated
        Vector<String[]> entries = comp.compare(model1, model2);

        // generates SQL from the missing schema part
        SQLGenerator sqlGen = new SQLGenerator();
        // sql statements to be applied to the outdated database
        List<String> statements = sqlGen.generateSQL(entries);
        try {
            // run the sql commands
            executeSQL(statements);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "errors occured during database update", e);
            throw e;
        }
    }


    /**
     * Executes the generated SQL commands to the outdated database
     *
     * @param statements A List of String with SQL statements
     * @throws Exception
     */
    private void executeSQL(List<String> statements) throws Exception {
        Statement st = DBConnector.getInstance().getConnection().createStatement();

        for (String sqlStatement : statements) {
            logger.info("executing SQL: " + sqlStatement);

            try {
                st.executeUpdate(sqlStatement);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "error executing schema update SQL: " + sqlStatement, e);
                throw e;
            }
        }
        st.close();
    }


    /**
     * Detects the database schema behind the given connection
     * Returns the result as SchemaModel
     *
     * @param conn The connection to look after the database schema
     * @return A SchemaModel containing the schema data
     * @throws Exception
     */
    public static SchemaModel detect(Connection conn) throws Exception {
        SchemaModel model = new SchemaModel();
        try {
            DatabaseMetaData meta = conn.getMetaData();

            // the table names
            Vector<String> tableNames = new Vector<String>();
            ResultSet schemas = meta.getTables(null, null, null, null);
            while (schemas.next()) {
                String tableName = schemas.getString(3);
                // ignore system tables
                if (tableName != null && !tableName.startsWith("SQLITE_")) {
                    tableNames.add(tableName);
                }
            }

            // detect column names and types for each table
            for (String tableName : tableNames) {
                HashMap<String, String> columnNameAndType = new HashMap<>();
                ResultSet rs2 = meta.getColumns(null, null, tableName, null);
                while (rs2.next()) {
                    String columnName = rs2.getString(4);
                    String columnType = rs2.getString(6);
                    columnNameAndType.put(columnName, columnType);
                }
                // store the table data to the model
                model.addTable(tableName, columnNameAndType);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error retrieving the database schema: ", e);
            throw e;
        }
        return model;
    }


    /**
     * retrieves a SchemaModel from a xml schema file
     *
     * @param xmlFile The schema file
     * @return A SchemaModel
     * @throws Exception
     */
    private SchemaModel detectFromXml(String xmlFile) throws Exception {
        X509Serializer xs = X509Serializer.getInstance();

        URL xmlURL = ClassLoader.getSystemResource(xmlFile);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        xmlURL.openStream()));

        String inputLine;
        StringBuilder sb = new StringBuilder();

        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);

        return (SchemaModel) xs.fromXML(sb.toString());
    }


    public static void main(String[] args) {
        String className = Configuration.getInstance().JDBC_DRIVER_CLASSNAME;
        String url = "jdbc:sqlite:w:/Manager/manager_alt.db";
        SchemaUpdater schemaUpdater = new SchemaUpdater(className, url);
        try {
            schemaUpdater.update();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error updating the database schema", e);
		}
		
	}
}

