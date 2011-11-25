/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;
import java.io.File;

import java.io.IOException;

import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.DBTasks;
import net.bytemine.utility.FileUtils;
import junit.framework.Test;
import junit.extensions.TestSetup;

public class ManagerTestSetup extends TestSetup {
    
    private String dbDir = "db";
    private String dbFilename = dbDir + "/manager-test.db";
    private String exportDir = "tmp/export";
    private String exportDir2 = "tmp/export_tmp";

    public ManagerTestSetup(Test test) {
        super(test);
    }

    public void setUp() {
        File db = new File(dbFilename);
        try {
            File dbDirectory = new File(dbDir);
            if (!dbDirectory.exists())
                dbDirectory.mkdirs();
            FileUtils.deleteDirectory(new File(exportDir));
            FileUtils.deleteDirectory(new File(exportDir2));
            db.delete();
            db.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DBConnector.getInstance("org.sqlite.JDBC", "jdbc:sqlite:"+dbFilename);
        // The base connection is the reference to the internal manager database
        DBConnector.getInstance().setBaseConnection(DBConnector.getInstance().getConnection());
        try {
            DBTasks.createTables(false);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void tearDown() {
        System.out.println("tearing down");
    }
}
