/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.cli;

import java.sql.Connection;

import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.DBTasks;


/**
 * Some database scripts
 *
 * @author Daniel Rauer
 */
public class DBScripts {

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                printUsageManual();
            } else {
                if ("prepareDB".equals(args[0])) {
                    boolean keepConfiguration = false;
                    if (args.length > 1 && "true".equals(args[1]))
                        keepConfiguration = true;

                    String driverClassname = null;
                    if (args.length > 2)
                        driverClassname = args[2];
                    String jdbcUrl = null;
                    if (args.length > 3)
                        jdbcUrl = args[3];

                    if (driverClassname != null && jdbcUrl != null) {
                        Connection dbConnection = DBConnector.getInstance(driverClassname, jdbcUrl).getConnection();
                        DBTasks.createTables(dbConnection, keepConfiguration);
                    } else
                        DBTasks.createTables(keepConfiguration);
                } else if ("resetDB".equals(args[0])) {
                    boolean keepConfiguration = false;
                    if (args.length > 1 && "true".equals(args[1]))
                        keepConfiguration = true;

                    DBTasks.resetDB(keepConfiguration);
                } else if ("export-schema".equals(args[0])) {
                    String path = args[1];

                    String driverClassname = null;
                    if (args.length > 2)
                        driverClassname = args[2];
                    String jdbcUrl = null;
                    if (args.length > 3)
                        jdbcUrl = args[3];

                    if (driverClassname != null && jdbcUrl != null) {
                        Connection dbConnection = DBConnector.getInstance(driverClassname, jdbcUrl).getConnection();
                        DBTasks.exportSchema(path, dbConnection);
                    }
                } else {
                    printUsageManual();
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * prints a manual to System.out
     */
    private static void printUsageManual() {
        System.out.println("Usage: java DBScripts <options>");
        System.out.println("options are:");
        System.out.println("    help: shows this dialog");
        System.out.println("    prepareDB <keepConfiguration>: prepare the database");
        System.out.println("    prepareDB <keepConfiguration> <driver classname> <jdbc url>: prepare the database");
        System.out.println("    resetDB <keepConfiguration>: reset the database");
        System.out.println("    export-schema <path> <driver classname> <jdbc url>: exports the database schema as xml");
    }

}
