/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * Connects the database and holds the connection
 * Implemented as singleton
 *
 * @author Daniel Rauer
 */
public class DBConnector {

    private static Logger logger = Logger.getLogger(DBConnector.class.getName());

    private static DBConnector instance = null;
    // current database connection
    private static Connection connection = null;
    // connection to the internal database
    private static Connection baseConnection = null;
    public static String dbPath = null;
    public static boolean dbPathChanged = false; 

    private DBConnector() {
        try {
            Class.forName(Configuration.getInstance().JDBC_DRIVER_CLASSNAME);
            initializeDB();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error connecting database", e);
        }
    }


    private DBConnector(String driverClassname, String url) {
        try {
            Class.forName(driverClassname);
            connection = DriverManager.getConnection(url);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error connecting database: " + url, e);
        }
    }


    /**
     * Returns the instance
     *
     * @return the DBConnector instance
     */
    public static DBConnector getInstance() {
        DBConnector.dbPathChanged = false;
        if (instance == null) {
            instance = new DBConnector();
        }

        return instance;
    }


    /**
     * Returns the instance
     *
     * @param driverClassname The classname of the db driver
     * @param url             The jdbc URL
     * @return the DBConnector instance
     */
    public static DBConnector getInstance(String driverClassname, String url) {
        DBConnector.dbPathChanged = false;
        if (instance == null)
            instance = new DBConnector(driverClassname, url);

        return instance;
    }
    
    
    /**
     * Sets the instance and connection variable to null.
     */
    public static void resetInstance() {
        instance = null;
        connection = null;
        baseConnection = null;
        dbPathChanged = false;
    }


    /**
     * Returns the database connection
     *
     * @return Connection
     */
    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection conn) {
        connection = conn;
    }
    
    /**
     * Returns the database connection to the internal database
     *
     * @return Connection
     */
    public Connection getBaseConnection() {
        return baseConnection;
    }
    
    public void setBaseConnection(Connection baseConn) {
        baseConnection = baseConn;
    }


    /**
     * Tests if the db connection has been established
     *
     * @return true or false
     * @throws java.lang.Exception
     */
    public boolean testConnection() {
        boolean ok = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT count(configurationid) FROM configuration");
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                ok = true;
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error database connection test", e);
        }
        return ok;
    }


    /**
     * Tests if there is an external database to be used
     *
     * @return true, if the external db will be used
     */
    public boolean useExternalDB() {
        String externalDBPath = ConfigurationQueries.getValue(ConfigurationQueries.DB_PATH);
        if (externalDBPath != null && !"".equals(externalDBPath) && !Configuration.getInstance().JDBC_PATH.equals(externalDBPath)) {
            // test if database exists
            File dbFile = new File(externalDBPath);
            if (dbFile.exists())
                return true;
            else {
                ConfigurationQueries.setValue(ConfigurationQueries.DB_PATH, Configuration.getInstance().JDBC_PATH);
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                new VisualException(rb.getString("dialog.switch_database.text"), rb.getString("dialog.switch_database.title"));
            }
        }
        return false;
    }


    /**
     * Switches from default to an external database
     */
    public void switchToExternalDatabase() {
        try {
            String externalDBPath = ConfigurationQueries.getValue(ConfigurationQueries.DB_PATH);
            //String defaultUrl = Configuration.getInstance().JDBC_URL;
            if (externalDBPath != null) {
                String className = Configuration.getInstance().JDBC_DRIVER_CLASSNAME;
                Configuration.getInstance().JDBC_PATH = externalDBPath;

                backupDatabase(externalDBPath);

                // switch to external DB
                instance.getConnection().close();
                instance = new DBConnector(className, 
                        Configuration.getInstance().JDBC_URL_PREFIX +
                        Configuration.getInstance().JDBC_PATH);

                logger.info("Using external database: " + externalDBPath);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error switching to external database", e);
        }
    }

    
    /**
     * Initializes a database connection
     * - detects the directory where the db should be located
     * - if no db exists copy a DB file from the old location (db/manager.db) to the
     *      new location (if an old DB exists)
     * - if no database file can be found create an empty database
     * - if no database can be found or created the application will exit, displaying a message to the user
     */
    private void initializeDB() {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        String osName = System.getProperty("os.name");
        String userHome = System.getProperty("user.home");
        String usrDir = Configuration.getInstance().MANAGER_USER_DIRECTORY;
        usrDir = ((osName.indexOf("Windows") > -1) ? "" : ".") + usrDir;
        File dbDir = new File(userHome + "/" + usrDir);
        File newDB = new File(dbDir + "/" + Configuration.getInstance().DB_FILENAME);
        String url = Configuration.getInstance().JDBC_URL_PREFIX + newDB.toString();

        try {
            if (!dbDir.exists())
                dbDir.mkdirs();
            
            if (!newDB.exists()) {
                DBConnector.dbPathChanged = true;
                File oldDB = new File("db/" + Configuration.getInstance().DB_FILENAME);
                if (oldDB.exists()) {
                    FileUtils.copyFileToDirectory(oldDB, dbDir);
                    FileUtils.deleteQuietly(oldDB);
                }
                else {
                    newDB.createNewFile();
                    connection = DriverManager.getConnection(url);
                    DBTasks.createTables(connection, false);
                }
            }

            // set this DB as active
            DBConnector.dbPath = newDB.toString();

            // backup database
            DBConnector.backupDatabase(DBConnector.dbPath);

            // connect to the database
            DBConnector.getInstance(Configuration.getInstance().JDBC_DRIVER_CLASSNAME, Configuration.getInstance().JDBC_URL_PREFIX + newDB.toString());
            
            // always keep a connection to the internal database
            baseConnection = DriverManager.getConnection(Configuration.getInstance().JDBC_URL_PREFIX + newDB.toString());
            logger.log(Level.INFO, "Using database at " + newDB.toString());
        } catch (Exception e) {
            // database is not available
            logger.log(Level.SEVERE, "the database file could not be found/created at " + newDB.toString(), e);
            new VisualException(rb.getString("error.db.text"), rb.getString("error.db.title"));
            System.exit(0);
        }
    }

    /**
     * creates a snapshot of the database
     *
     * @param dbPath       Path that includes the db file
     */

   public static void backupDatabase(String dbPath) {
        boolean connectionExisted = false;

        logger.log(Level.INFO, "Snapshoting database", dbPath);

        if (connection != null) {
            connectionExisted = true;
            try {
                logger.log(Level.INFO, "Shutting down database connection to", dbPath);
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failure to close the database connection",e);
            }
        }

        backupDatabaseLoop(dbPath, 0, Configuration.getInstance().COUNTED_SNAPSHOTS);

        if (connectionExisted) {
            logger.log(Level.INFO, "Reconnecting to database", dbPath);
            try {
                connection = DriverManager.getConnection(Configuration.getInstance().JDBC_URL_PREFIX +
                                                   Configuration.getInstance().JDBC_PATH);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failure to reinitiate the database connection",e);
            }
        }
    }

    private static void backupDatabaseLoop(String dbPath, int cnt, int limit) {
        if (cnt > limit)
            return;

        File tmpDb= new File(dbPath +"."+ cnt);
        if(tmpDb.exists()) {
            int cntNew= cnt + 1;
            backupDatabaseLoop(dbPath, cntNew, limit);
        }

        if(cnt == 0) {
            net.bytemine.utility.DBUtils.copyDatabase(dbPath, dbPath +".0");
        } else {
            File lowDb= new File(dbPath +"."+ Integer.toString(cnt-1));
            lowDb.renameTo(tmpDb);
        }
    }

}
