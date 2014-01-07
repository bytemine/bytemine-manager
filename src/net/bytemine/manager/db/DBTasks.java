/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.db.schema.SchemaModel;
import net.bytemine.manager.db.schema.SchemaUpdater;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Serializer;
import net.bytemine.utility.FileUtils;


/**
 * Some database tasks
 *
 * @author Daniel Rauer
 */
public class DBTasks {

    private static Logger logger = Logger.getLogger(DBTasks.class.getName());
    private static ResourceBundle rb;


    /**
     * (Re-)creates the database tables
     */
    public static void resetDB(boolean keepConfiguration) throws Exception {
        Connection dbConnection = DBConnector.getInstance().getConnection();
        resetDB(dbConnection, keepConfiguration);
    }


    public static void resetDB(Connection dbConnection, boolean keepConfiguration) throws Exception {
        dropTables(dbConnection, keepConfiguration);
        createTables(dbConnection, keepConfiguration);
    }


    /**
     * (Re-)creates the database tables
     */
    public static String dropTables(Connection dbConnection, boolean keepConfiguration) throws Exception {
        logger.info("Dropping all tables");

        boolean errorOccured = false;
        Statement st = dbConnection.createStatement();
        try {
            st.execute("drop table x509");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table x509 could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table user");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table user could not be deleted", se);
            errorOccured = true;
        }
        try {
            if (!keepConfiguration)
                st.execute("drop table configuration");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table configuration could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table server");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table server could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table server_user");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table server_user could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table crl");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table crl could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table crlentry");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table crlentry could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table pkcs12");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table pkcs12 could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table knownhosts");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table knownhosts could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table licence");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table licence could not be deleted", se);
            errorOccured = true;
        }
        
        try {
            st.execute("drop table groups");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table groups could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table groups_user");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table groups_user could not be deleted", se);
            errorOccured = true;
        }
        try {
            st.execute("drop table treestates");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table treestates could not be deleted", se);
            errorOccured = true;
        }

        if (errorOccured) {
            String errorText = new String();
            if (rb != null)
                errorText += rb.getString("error.db.reset.lock");
            else
                errorText = "Reset not successful. The database seems to be locked.\n Unlock and retry.";
            return errorText;
        }
        return null;
    }


    /**
     * (Re-)creates the database tables
     */
    public static void createTables(boolean keepConfiguration) throws Exception {
        Connection dbConnection = DBConnector.getInstance().getConnection();
        rb = ResourceBundleMgmt.getInstance().getUserBundle();
        createTables(dbConnection, keepConfiguration);
    }


    /**
     * (Re-)creates the database tables
     */
    public static void createTables(Connection dbConnection, boolean keepConfiguration) throws Exception {
        logger.info("Creating all tables");

        Statement st = dbConnection.createStatement();

        try {
            st.execute("create table x509(" +
                    "x509id int not null primary key, " +
                    "version text, " +
                    "filename text, " +
                    "path text, " +
                    "serial text, " +
                    "issuer text, " +
                    "subject text, " +
                    "content text, " +
                    "contentdisplay text, " +
                    "certserialized text, " +
                    "key text, " +
                    "keycontent text, " +
                    "type int, " +
                    "createdate text, " +
                    "validfrom text, " +
                    "validto text, " +
                    "generated boolean default 0, " +
                    "userid int" +
                    ")");

            if (!keepConfiguration)
                st.execute("create table configuration(configurationid int not null primary key, key text, value text)");

            st.execute("create table user(userid int not null primary key, username text, password text, cn text, ou text, x509id int, yubikeyid text)");
            st.execute("create table groups(groupid int not null primary key, name text, description text)");
            st.execute("create table groups_user(groupid int not null, userid int not null, primary key(groupid, userid))");
            
            st.execute("create table server(" +
                    "serverid int not null primary key, " +
                    "name text, " +
                    "hostname text, " +
                    "authtype int, " +
                    "username text, " +
                    "keyfilepath text, " +
                    "userfilepath text, " +
                    "exportpath text, " +
                    "statusport int, " +
                    "statustype int, " +
                    "statusinterval int, " +
                    "sshport int, " +
                    "servertype int, " +
                    "wrappercommand int, " +
                    "x509id int, " +
                    "vpnport int, " +
                    "vpnprotocol int, " +
                    "vpncc int, " +
                    "vpnccpath text, " +
                    "vpnNetworkAddress text, " +
                    "vpnSubnetMask int," +
                    "vpnDevice int, " + 
                    "vpnRedirectGateway boolean default 0, " +
                    "vpnDuplicateCN boolean default 0, " +
                    "vpnUser text, " +
                    "vpnGroup text, "+
                    "vpnKeepAlive text, " +
                    "cn text, " +
                    "ou text" + 
                    ")");
            st.execute("create table server_user(serverid int not null, userid int not null, ip text, primary key(serverid, userid))");

            st.execute("CREATE TABLE crl(" +
                    "crlid int not null primary key, " +
                    "crlnumber text, " +
                    "version text, " +
                    "filename text, " +
                    "path text, " +
                    "issuer text, " +
                    "content text, " +
                    "contentdisplay text, " +
                    "crlserialized text, " +
                    "createdate text, " +
                    "validfrom text, " +
                    "nextupdate text" +
                    ")");

            st.execute("CREATE TABLE crlentry(" +
                    "crlentryid int not null primary key, " +
                    "serial text, " +
                    "revocationdate text, " +
                    "x509id int, " +
                    "crlid int," +
                    "username text" +
                    ")");

            st.execute("CREATE TABLE pkcs12(" +
                    "pkcs12id INT NOT NULL PRIMARY KEY, friendlyname TEXT, password TEXT, content TEXT, x509id INT)");

            st.execute("CREATE TABLE knownhosts(" +
                    "hostname TEXT NOT NULL PRIMARY KEY, fingerprint TEXT, trusted int)");

            st.execute("CREATE TABLE licence(" +
                    "licid INT NOT NULL PRIMARY KEY, keystore TEXT, crtpath TEXT, keypath TEXT)");
            
            st.execute("CREATE TABLE treestates(" +
                    "stateid INT NOT NULL PRIMARY KEY, treename TEXT, expandednodes TEXT)");

            st.close();

            logger.info("Finished creating tables");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while creating database tables", e);

            if (rb != null)
                throw new Exception(rb.getString("error.db.reset.create"));
            else
                throw new Exception("Some database tables could not be created. Unlock the database and retry.");
        }
    }
    
    /**
     * Creates incomplete database tables, used for JUnit tests
     */
    public static void createIncompleteTables(Connection dbConnection) throws Exception {
        logger.info("Creating some incomplete tables");

        Statement st = dbConnection.createStatement();

        try {
            st.execute("create table x509(" +
                    "x509id int not null primary key, " +
                    "version text, " +
                    "filename text, " +
                    "path text, " +
                    "serial text, " +
                    "contentdisplay text, " +
                    "certserialized text, " +
                    "key text, " +
                    "keycontent text, " +
                    "createdate text, " +
                    "validto text, " +
                    "generated boolean default 0, " +
                    "userid int" +
                    ")");

            st.execute("create table groups(groupid int not null primary key, name text, description text)");
            st.execute("create table groups_user(groupid int not null, userid int not null, primary key(groupid, userid))");
            
            st.execute("create table server(" +
                    "serverid int not null primary key, " +
                    "name text, " +
                    "hostname text, " +
                    "authtype int, " +
                    "username text, " +
                    "exportpath text, " +
                    "statusport int, " +
                    "statusinterval int, " +
                    "sshport int, " +
                    "wrappercommand int, " +
                    "vpnport int, " +
                    "vpnprotocol int, " +
                    "vpncc int, " +
                    "vpnNetworkAddress text, " +
                    "vpnSubnetMask int," +
                    "vpnRedirectGateway boolean default 0, " +
                    "vpnUser text, " +
                    "vpnGroup text, "+
                    "vpnKeepAlive text, " +
                    "cn text, " +
                    "ou text" + 
                    ")");
            st.execute("create table server_user(serverid int not null, userid int not null, ip text, primary key(serverid, userid))");

            st.execute("CREATE TABLE crl(" +
                    "crlid int not null primary key, " +
                    "crlnumber text, " +
                    "version text, " +
                    "filename text, " +
                    "contentdisplay text, " +
                    "crlserialized text, " +
                    "validfrom text, " +
                    "nextupdate text" +
                    ")");

            st.execute("CREATE TABLE crlentry(" +
                    "crlentryid int not null primary key, " +
                    "x509id int, " +
                    "username text" +
                    ")");

            st.execute("CREATE TABLE pkcs12(" +
                    "pkcs12id INT NOT NULL PRIMARY KEY, friendlyname TEXT, password TEXT, content TEXT, x509id INT)");

            st.execute("CREATE TABLE knownhosts(" +
                    "hostname TEXT NOT NULL PRIMARY KEY, fingerprint TEXT, trusted int)");

            st.close();

            logger.info("Finished creating some incomplete tables");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while creating database tables", e);
            throw new Exception("Some database tables could not be created. Unlock the database and retry.");
        }
    }

    /**
     * Clears the CRL table
     */
    public static String clearCrl() throws Exception {
        Connection dbConnection = DBConnector.getInstance().getConnection();
        rb = ResourceBundleMgmt.getInstance().getUserBundle();

        boolean errorOccured = false;
        Statement st = dbConnection.createStatement();
        try {
            st.execute("DELETE FROM crlentry");
        } catch (SQLException se) {
            logger.log(Level.SEVERE, "table crlentry could not be cleared", se);
            errorOccured = true;
        }

        if (errorOccured) {
            String errorText = new String();
            if (rb != null)
                errorText += rb.getString("error.db.reset.lock");
            else
                errorText = "Clear was not successful. The database seems to be locked.\n Unlock and retry.";
            return errorText;
        }
        return null;
    }

    
    /**
     * Exports the database schema as xml
     * @param path The path where the xml file will be written
     * @param conn The database connection
     * @throws Exception
     */
    public static void exportSchema(String path, Connection conn) throws Exception {
        SchemaModel model = SchemaUpdater.detect(conn);

        X509Serializer xs = X509Serializer.getInstance();
        String xml = xs.toXML(model);
        FileUtils.writeStringToFile(xml, path);
    }


}
