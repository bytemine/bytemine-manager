/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SQL-Queries for the configuration entries
 *
 * @author Daniel Rauer
 */
public class ConfigurationQueries {

    private static Logger logger = Logger.getLogger(ConfigurationQueries.class.getName());

    public static final String LANGUAGE_KEY = "language";
    public static final String CA_ENABLED = "enableCA";
    public static final String CC_ENABLED = "enableCC";

    public static final String DB_PATH = "externalDbPath";
    public static final String DB_UP_TO_DATE = "externalDbUpToDate";
    public static final String USE_DEFAULT_DB = "useDefaultDB";
    public static final String USE_PAM = "usePAM";

    public static final String UPDATE_AUTOMATICALLY = "updateAutomatically";
    public static final String UPDATE_KEYSTORE_PATH = "updateKeystorePath";
    public static final String UPDATE_SERVER_PATH = "updateServerPath";
    public static final String UPDATE_REPOSITORY = "updateRepository";
    public static final String UPDATE_PROXY = "updateProxy";
    public static final String UPDATE_PROXY_PORT = "updateProxyPort";
    public static final String UPDATE_PROXY_USERNAME = "updateProxyUsername";
    public static final String UPDATE_PROXY_PASSWORD = "updateProxyPassword";

    public static final String LICENCE_CODE = "licenceCode";

    public static final String CLIENT_CERT_IMPORT_DIR_KEY = "clientCertImportDir";
    public static final String CLIENT_USERFILE = "clientUserfile";
    public static final String CERT_EXPORT_PATH_KEY = "clientCertExportPath";

    public static final String LDAP_HOST = "ldapHost";
    public static final String LDAP_PORT = "ldapPort";
    public static final String LDAP_DN = "ldapDN";
    public static final String LDAP_OBJECTCLASS = "ldapObjClass";
    public static final String LDAP_CN = "ldapCN";
    public static final String LDAP_CERT_ATTRIBUTE_NAME = "ldapCertAttr";
    public static final String LDAP_CERT_IMPORT_DIR = "ldapCertImportDir";

    public static final String USER_IMPORT_TYPE = "userImportType";
    public static final String CERTIFICATE_TYPE = "certificateType";
    public static final String PKCS12_PASSWORD_TYPE = "pkcs12PasswordType";

    public static final String X509_ROOT_SUBJECT = "x509RootSubject";
    public static final String X509_ROOT_VALID_FROM = "x509RootValidFrom";
    public static final String X509_ROOT_VALID_TO = "x509RootValidTo";
    public static final String X509_SERVER_SUBJECT = "x509ServerSubject";
    public static final String X509_SERVER_VALID_FROM = "x509ServerValidFrom";
    public static final String X509_SERVER_VALID_FOR = "x509ServerValidFor";
    public static final String X509_CLIENT_SUBJECT = "x509ClientSubject";
    public static final String X509_CLIENT_VALID_FROM = "x509ClientValidFrom";
    public static final String X509_CLIENT_VALID_FOR = "x509ClientValidFor";
    public static final String X509_KEY_STRENGTH = "x509KeyStrength";
    
    public static final String GUI_WIDTH = "guiWidth";
    public static final String GUI_HEIGHT = "guiHeight";
    public static final String GUI_LOCATION_X = "guiLocationX";
    public static final String GUI_LOCATION_Y = "guiLocationY";
    public static final String SERVER_USER_TREE_DIVIDER_LOCATION = "serverUserTreeDividerLocation";
    public static final String GUI_SHOW_CRL_TAB = "guiShowCRLTab";
    public static final String GUI_SHOW_CR_X509 = "guiShowCRX509";
    
    public static final String X509_GUI_WIDTH = "X509guiWidth";
    public static final String X509_GUI_HEIGHT = "X509guiHeight";
    public static final String X509_GUI_LOCATION_X= "X509guiLocationX";
    public static final String X509_GUI_LOCATION_Y = "X509guiLocationY";
    
    public static final String GUI_SHOW_EXIT_DIALOG = "guiShowExitDialog";
    public static final String GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS = "guiShowExitDialogActiveThreads";
    
    public static final String GUI_SHOW_OPENVPN_IP_WARNING_DIALOG = "guiShowOpenVpnWarningDialog";
    
    public static final String CREATE_OPENVPN_CONFIG_FILES = "createOpenVPNConfigFiles";

    
    /**
     * Returns the value of the key, or null
     *
     * @param key The name of the key
     * @return The value or null
     */
    public static String getValue(String key) {
        return getValue(key, DBConnector.getInstance().getConnection());
    }

    /**
     * Returns the value of the key, or null
     *
     * @param key The name of the key
     * @param conn The database connection to use
     * @return The value or null
     */
    public static String getValue(String key, Connection conn) {
        String value = null;
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT value FROM configuration WHERE key=?");
            pst.setString(1, key);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                value = rs.getString("value");
            }

            rs.close();
            pst.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting value from key: " + key, e);
        }
        return value;
    }


    /**
     * Sets the value for the key
     * creates the key if not exists
     *
     * @param key   The key
     * @param value The value
     */
    public static void setValue(String key, String value) {
        Connection conn = DBConnector.getInstance().getConnection();
        setValue(key, value, conn);
    }

    /**
     * Sets the value for the key
     * creates the key if not exists
     *
     * @param key   The key
     * @param value The value
     * @param conn  The database connection to use.
     */
    public static void setValue(String key, String value, Connection conn) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE configuration SET value=? WHERE key=?");
            pst.setString(1, value);
            pst.setString(2, key);
            int result = pst.executeUpdate();

            if (result == 0) {
                // key does not exist
                createKey(key, value);
            }

            pst.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error setting value for key: " + key, e);
        }
    }


    /**
     * Creates a configuration entry
     *
     * @param key   The key
     * @param value The value
     */
    private static void createKey(String key, String value) {
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO configuration (configurationid, key, value) VALUES (?,?,?)");
            pst.setInt(1, getNextConfigurationId());
            pst.setString(2, key);
            pst.setString(3, value);
            pst.executeUpdate();

            pst.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error creating key: " + key, e);
        }
    }


    /**
     * Returns all configuration values stored in the database
     *
     * @return Vector containing all values
     */
    public static Vector<String> getAllValues() {
        Vector<String> values = new Vector<>();
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT value FROM configuration ORDER BY configurationid");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                values.add(rs.getString("value"));
            }

            rs.close();
            pst.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting all values", e);
        }
        return values;
    }


    /**
     * Returns all configurations stored in the database
     *
     * @return Hashtable containing all keys and values
     */
    public static Hashtable<String, String> getConfigurations() {
        Hashtable<String, String> configs = new Hashtable<>();
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT key, value FROM configuration");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                configs.put(rs.getString("key"), rs.getString("value"));
            }

            rs.close();
            pst.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting all configuration entries", e);
        }
        return configs;
    }


    /**
     * Returns the next configurationid
     *
     * @return The next configurationId or 0
     */
    private static int getNextConfigurationId() {
        int configurationId = 0;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT max(configurationid) AS maxId FROM configuration");
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                configurationId = rs.getInt("maxId") + 1;

            rs.close();
            pst.close();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting next configurationid", e);
        }

        return configurationId;
    }


    /**
     * Detects if configurations are already stored
     *
     * @return true, if configurations exist
     */
    public static boolean areConfigurationsExisiting() {
        boolean existing = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT count(configurationid) as number FROM configuration");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int number = rs.getInt("number");
                // ignore first language entry
                if (number > 1)
                    existing = true;
            }

            rs.close();
            pst.close();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error detecting if configuration entries exist", e);
        }

        return existing;
    }
}
