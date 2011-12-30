/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.File;

import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.DBTasks;

import org.apache.commons.io.FileUtils;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.utility.X509Generator;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({ SchemaUpdaterTest.class,
                      X509ActionTest.class,
                      UserActionTest.class,
                      ServerActionTest.class,
                      ServerConfigTest.class,
                      ServerUserTest.class,
                      UserImportTest.class,
                      UserConfigTest.class
                      })
public class ManagerTestSuite {
    
    static String testdirpath = System.getProperty("user.dir")+"/tests/";
    
    /**
     * general set-up for every test
     *
     */
    public static void setUpTest() {
        System.err.println("\n> Setting up test environment");
        new File(testdirpath).mkdir();
        
        DBConnector.getInstance("org.sqlite.JDBC", "jdbc:sqlite:"+testdirpath+"/db");
        // The base connection is the reference to the internal manager database
        DBConnector.getInstance().setBaseConnection(DBConnector.getInstance().getConnection());
        try {
            DBTasks.createTables(false);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        
        Configuration.getInstance().setCertExportPath(testdirpath);
        Configuration.getInstance().setCAEnabled(true);
        Configuration.getInstance().initializeX509();
        System.err.println("> Test environment set up\n");
    }
    
    /**
     * general tear-down for every test
     *
     */
    public static void tearDownTest() {
        System.err.println("\n> Tearing down test environment");
    	try {
            FileUtils.deleteDirectory(new File(testdirpath));
        } catch(Exception e) {
            System.out.println("Can't remove testdir: "+testdirpath+e.toString());
        }
        System.err.println("> Test environment teared down\n");
    }
    
    public static void rootCreation() {
        X509Generator gen = new X509Generator();
        try {
            gen.createRootCertImmediately();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}