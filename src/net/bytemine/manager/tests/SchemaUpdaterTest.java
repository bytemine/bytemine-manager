/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.File;
import java.util.HashMap;

import net.bytemine.manager.Constants;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.DBTasks;
import net.bytemine.manager.db.schema.SchemaModel;
import net.bytemine.manager.db.schema.SchemaUpdater;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchemaUpdaterTest {
    
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up SchemaUpdaterTest");
        setUp();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down SchemaUpdaterTest");
    }
    
    // create database with incomplete schema
    private static void setUp() {
        System.err.println("\n> Setting up test environment");
        new File(ManagerTestSuite.testdirpath).mkdir();
        
        DBConnector.getInstance("org.sqlite.JDBC", "jdbc:sqlite:" + ManagerTestSuite.testdirpath + "/db");
        // The base connection is the reference to the internal manager database
        DBConnector.getInstance().setBaseConnection(DBConnector.getInstance().getConnection());
        try {
            DBTasks.createIncompleteTables(DBConnector.getInstance().getConnection());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        System.err.println("> Test environment set up\n");
    }
    
    @Test
    public void testSchemaUpdater() throws Exception {
        SchemaModel schema = SchemaUpdater.detect(DBConnector.getInstance().getConnection());
        // only 9 tables were created
        assertEquals(9, schema.getTableNames().size());
        
        Object[] incompleteTables = schema.getTables().keySet().toArray();
        assertArrayEquals(incompleteTables, new String[] {"GROUPS_USER", "X509", "KNOWNHOSTS", "SERVER_USER", "PKCS12", "GROUPS", "CRL", "SERVER", "CRLENTRY"});
        
        // some columns are missing in incomplete schema
        HashMap<String, String> crlentryTable = schema.getTables().get("CRLENTRY");
        assertEquals(3, crlentryTable.keySet().size());
        assertArrayEquals(crlentryTable.keySet().toArray(), new String[] {"crlentryid", "username", "x509id"});
                
        
        // update the database schema
        SchemaUpdater updater = new SchemaUpdater(Constants.UPDATE_SCHEMA_FILE);
        updater.updateFromXml();
        
        
        schema = SchemaUpdater.detect(DBConnector.getInstance().getConnection());
        // now the schema should be complete
        assertEquals(13, schema.getTableNames().size());
        
        Object[] completeTables = schema.getTables().keySet().toArray();
        assertArrayEquals(completeTables, new String[] {"X509", "KNOWNHOSTS", "SERVER_USER", "USER", "GROUPS_USER", "CONFIGURATION", "GROUPS", "PKCS12", "CRL", "LICENCE", "TREESTATES", "SERVER", "CRLENTRY"});
        
        // all columns exist in complete schema
        crlentryTable = schema.getTables().get("CRLENTRY");
        assertEquals(6, crlentryTable.keySet().size());
        assertArrayEquals(crlentryTable.keySet().toArray(), new String[] {"crlentryid", "username", "revocationdate", "serial", "x509id", "crlid"});
    }

}
