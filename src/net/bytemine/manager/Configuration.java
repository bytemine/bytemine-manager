/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.awt.Toolkit;
import java.util.Calendar;

import java.util.Vector;

import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.db.ConfigurationQueries;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.utility.StringUtils;


/**
 * Main application configurations are stored here;
 * Implemented as singleton
 *
 * @author Daniel Rauer
 */
public class Configuration {
    private static Logger logger = Logger.getLogger(Configuration.class.getName());

    private static Configuration instance = null;
    
    private boolean rootCertExisting = false;

    public String JDBC_DRIVER_CLASSNAME = "org.sqlite.JDBC";
    public String JDBC_URL_PREFIX = "jdbc:sqlite:";
    public String JDBC_PATH =  (System.getProperty("os.name").contains("Windows"))
    	? System.getProperty("user.home")+"\\bytemine-manager\\manager.db" : 
    	  System.getProperty("user.home")+"/.bytemine-manager/manager.db";
    public String MANAGER_USER_DIRECTORY = "bytemine-manager";
    public String DB_FILENAME = "manager.db";
    public boolean USE_DEFAULT_DB = true;
    
    // use PAM authentication
    public boolean USE_PAM = false;
    
    // switch on to import users coming from passwd file on synchronisation
    public boolean IMPORT_USERS_ON_SYNCHRONISATION = false;

    // path to the icon
    public String ICON_PATH = null;
    // path to the banner
    public String BANNER_PATH = null;

    public String MANAGER_PROPERTIES_NAME = "manager";

    // path to logging properties
    private static boolean DEBUG_LOGGING;
    String LOGGING_PROPERTIES = "/logging.properties";
    String LOGGING_DEBUG_PROPERTIES = "/logging_debug.properties";

    // path to css file
    String CSS_FILE = "./css.xml";

    // minimum length of users password
    public int USER_PASSWORD_LENGTH = 6;

    // number of snapshots to keep
    public int COUNTED_SNAPSHOTS = 10;

    // server defaults for bytemine openbsd appliance
    public String SERVER_WRAPPER_COMMAND;
    public String SERVER_USERNAME;
    public String SERVER_KEYS_PATH;
    public String SERVER_PASSWD_PATH;
    public String SERVER_CC_PATH;
    public String SERVER_NETWORK_ADDRESS;
    public int SERVER_DEVICE;

    // how the users are imported
    public int USER_IMPORT_TYPE;

    // gets inserted by ant task
    public String MANAGER_VERSION = "2.3.3";
    public String MANAGER_BUILD = "20681562f7e6f5facb324263996240df4f8b3fae";


    // update settings
    public boolean UPDATE_AUTOMATICALLY = false;
    public String UPDATE_SERVER = Constants.UPDATE_HTTPS_SERVER;
    public String UPDATE_REPOSITORY = Constants.UPDATE_REPOSITORY;
    public String UPDATE_PROXY;
    public String UPDATE_PROXY_PORT;
    private String UPDATE_PROXY_USERNAME;
    private String UDPATE_PROXY_PASSWORD;

    // Base64 or PKCS12
    public int CERTIFICATE_TYPE;
    public int PKCS12_PASSWORD_TYPE;
    public boolean TEST_PKCS12_PASSWORD_DISABLED = false;

    public boolean CA_ENABLED;
    public boolean CC_ENABLED;
    public boolean DEBUG_SSH;

    public String LANGUAGE = null;
    public String CLIENT_CERT_IMPORT_DIR = null;
    public String CLIENT_USERFILE = null;
    public String CERT_EXPORT_PATH = null;

    public String LDAP_HOST = null;
    public String LDAP_PORT = null;
    public String LDAP_DN = null;
    public String LDAP_OBJECTCLASS = null;
    private String LDAP_CN = null;
    public String LDAP_CERT_ATTRNAME = null;
    public String LDAP_CERT_IMPORTDIR = null;

    public String X509_ROOT_SUBJECT = null;
    public String X509_ROOT_VALID_FROM = null;
    public String X509_ROOT_VALID_TO = null;
    public String X509_SERVER_SUBJECT = null;
    private String X509_SERVER_VALID_FROM = null;
    public String X509_SERVER_VALID_FOR = null;
    public String X509_CLIENT_SUBJECT = null;
    private String X509_CLIENT_VALID_FROM = null;
    public String X509_CLIENT_VALID_FOR = null;
    public String X509_KEY_STRENGTH = null;

    // shall a user password be suggested 
    private boolean SUGGEST_PASSWORD = true;
    
    // initial GUI size and position
    public int GUI_WIDTH = 720;
    public int GUI_HEIGHT = 620;
    public int GUI_LOCATION_X = 30;
    public int GUI_LOCATION_Y = 30;
    public int SERVER_USER_TREE_DIVIDER_LOCATION = 150;
    public int CONTROL_CENTER_LOG_DIVIDER_LOCATION = 60;
    
    // initial GUI size and position (X509Details)
    public int X509_GUI_WIDTH = -1;			// window will be positioned by offset
    public int X509_GUI_HEIGHT = -1;		// 
    public int X509_GUI_LOCATION_X = -1;	// 
    public int X509_GUI_LOCATION_Y = -1;	// 
    /**
    // initial GUI size and position (ServerDetails)
    public int SERVER_GUI_WIDTH = -1;		// window will be positioned by offset
    public int SERVER_GUI_HEIGHT = -1;		// 										
    public int SERVER_GUI_LOCATION_X = -1;	// 
    public int SERVER_GUI_LOCATION_Y = -1;	// 
    
    // initial GUI size and position (UserDetails)
    public int USER_GUI_WIDTH = -1;			// window will be positioned by offset
    public int USER_GUI_HEIGHT = -1;		// 
    public int USER_GUI_LOCATION_X = -1;	// 
    public int USER_GUI_LOCATION_Y = -1;	// 
    **/
    public boolean GUI_SHOW_CRL_TAB = false;
    public boolean GUI_SHOW_CR_X509 = true;
    public boolean GUI_SHOW_EXIT_DIALOG = true;
    public boolean GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS = true;
    public boolean GUI_SHOW_OPENVPN_IP_WARNING = true;

    public Vector<String> NOT_SYNCED_SERVERS = new Vector<>();
    
    public boolean CREATE_OPENVPN_CONFIG_FILES = false;
    

    /**
     * Private constructor
     */
    private Configuration() {
        initialize();
    }


    /**
     * Returns the instance
     *
     * @return the Configuration instance
     */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }

        return instance;
    }


    /**
     * Reads the properties-file and sets the variables
     */
    private void initialize() {

        // read property file 
        ResourceBundle configBundle = null;
        try {
            configBundle = ResourceBundle.getBundle(MANAGER_PROPERTIES_NAME);

            setJdbcUrlPrefix(configBundle.getString("jdbc_url_prefix"));
            setJdbcDriver(configBundle.getString("jdbc_driver"));
            setManagerUserDirectory(configBundle.getString("manager_user_directory"));
            setDbFilename(configBundle.getString("db_filename"));
            setIconPath(configBundle.getString("icon"));
            setBannerPath(configBundle.getString("banner"));
            setLoggingProperties(configBundle.getString("logging_properties"));
            setCssFile(configBundle.getString("css_file"));
            setUserPasswordLength(Integer.parseInt(configBundle.getString("user_password_length")));
            setServerWrapperCommand(configBundle.getString("wrapper_command"));
            setServerUsername(configBundle.getString("username"));
            setServerKeysPath(configBundle.getString("keys_path"));
            setServerPasswdPath(configBundle.getString("passwd_path"));
            setServerCCPath(configBundle.getString("cc_path"));
            setServerNetworkAddress(configBundle.getString("network_address"));
            setServerDevice(Integer.parseInt((configBundle.getString("vpn_device"))));
            setUpdateKeystorePath(configBundle.getString("keystore_path"));
            setDebugSSH(false);

        } catch (NumberFormatException e) {
            logger.warning("manager.properties could not be processed");
        }
    }


    /**
     * initializes config entries stored in database
     */
    void initializeDB() {
        // read database
        this.LANGUAGE = ConfigurationQueries.getValue(ConfigurationQueries.LANGUAGE_KEY);
        this.CLIENT_CERT_IMPORT_DIR = ConfigurationQueries.getValue(ConfigurationQueries.CLIENT_CERT_IMPORT_DIR_KEY);
        this.CLIENT_USERFILE = ConfigurationQueries.getValue(ConfigurationQueries.CLIENT_USERFILE);
        this.CERT_EXPORT_PATH = ConfigurationQueries.getValue(ConfigurationQueries.CERT_EXPORT_PATH_KEY);
        this.LDAP_HOST = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_HOST);
        this.LDAP_PORT = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_PORT);
        this.LDAP_DN = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_DN);
        this.LDAP_OBJECTCLASS = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_OBJECTCLASS);
        this.LDAP_CN = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_CN);
        this.LDAP_CERT_ATTRNAME = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_CERT_ATTRIBUTE_NAME);
        this.LDAP_CERT_IMPORTDIR = ConfigurationQueries.getValue(ConfigurationQueries.LDAP_CERT_IMPORT_DIR);

        this.USER_IMPORT_TYPE = Constants.USER_IMPORT_TYPE_FILE;
        try {
            this.USER_IMPORT_TYPE = Integer.parseInt(ConfigurationQueries.getValue(ConfigurationQueries.USER_IMPORT_TYPE));
        } catch (NumberFormatException e) {
            setUserImportType(Constants.USER_IMPORT_TYPE_FILE);
        }

        this.CERTIFICATE_TYPE = Constants.CERTIFICATE_TYPE_BASE64;
        try {
            this.CERTIFICATE_TYPE = Integer.parseInt(ConfigurationQueries.getValue(ConfigurationQueries.CERTIFICATE_TYPE));
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "error detecting certificate type, key: " + ConfigurationQueries.CERTIFICATE_TYPE, e);
            setCertificateType(Constants.CERTIFICATE_TYPE_BASE64);
        }

        this.PKCS12_PASSWORD_TYPE = Constants.PKCS12_NO_PASSWORD;
        try {
            this.PKCS12_PASSWORD_TYPE = Integer.parseInt(ConfigurationQueries.getValue(ConfigurationQueries.PKCS12_PASSWORD_TYPE));
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "error detecting pkcs12 password type, key: " +
                    ConfigurationQueries.PKCS12_PASSWORD_TYPE +
                    ". Is okay if configuration was saved for the first time.");
            setPkcs12PasswordType(Constants.PKCS12_NO_PASSWORD);
        }

        this.CA_ENABLED = true;
        this.CC_ENABLED = false;
        try {
            this.CA_ENABLED = Boolean.parseBoolean(ConfigurationQueries.getValue(ConfigurationQueries.CA_ENABLED));
            this.CC_ENABLED = Boolean.parseBoolean(ConfigurationQueries.getValue(ConfigurationQueries.CC_ENABLED));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting mode, key: " + ConfigurationQueries.CA_ENABLED, e);
        }

        this.UPDATE_AUTOMATICALLY = false;
        try {
            this.UPDATE_AUTOMATICALLY = Boolean.parseBoolean(ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_AUTOMATICALLY));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting automatic update, key: " + ConfigurationQueries.UPDATE_AUTOMATICALLY, e);
        }

        String serverPath = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_SERVER_PATH);
        if (serverPath != null)
            this.UPDATE_SERVER = serverPath;
        String repoPath = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_REPOSITORY);
        if (repoPath != null)
            this.UPDATE_REPOSITORY = repoPath;
        String proxy = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_PROXY);
        if (proxy != null)
            this.UPDATE_PROXY = proxy;
        String proxyPort = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_PROXY_PORT);
        if (proxyPort != null)
            this.UPDATE_PROXY_PORT = proxyPort;
        String proxyUsername = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_PROXY_USERNAME);
        if (proxyUsername != null)
            this.UPDATE_PROXY_USERNAME = proxyUsername;
        String proxyPassword = ConfigurationQueries.getValue(ConfigurationQueries.UPDATE_PROXY_PASSWORD);
        if (proxyPassword != null)
            this.UDPATE_PROXY_PASSWORD = proxyPassword;

        // main window positioning
        
        this.GUI_WIDTH = setWindowPosition(ConfigurationQueries.GUI_WIDTH, 570, 
        		Toolkit.getDefaultToolkit().getScreenSize().width);
        
        this.GUI_HEIGHT = setWindowPosition(ConfigurationQueries.GUI_HEIGHT, 535, 
        		Toolkit.getDefaultToolkit().getScreenSize().height);
        
        this.GUI_LOCATION_X = setWindowPosition(ConfigurationQueries.GUI_LOCATION_X, 535, 
        		Toolkit.getDefaultToolkit().getScreenSize().width);
        
        this.GUI_LOCATION_Y = setWindowPosition(ConfigurationQueries.GUI_LOCATION_Y, 535, 
        		Toolkit.getDefaultToolkit().getScreenSize().height);
        
        String serverUserTreeDividerLocation = ConfigurationQueries.getValue(ConfigurationQueries.SERVER_USER_TREE_DIVIDER_LOCATION);
        if (!StringUtils.isEmptyOrWhitespaces(serverUserTreeDividerLocation))
            this.SERVER_USER_TREE_DIVIDER_LOCATION = Integer.parseInt(serverUserTreeDividerLocation);
        
        String showCRL = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_CRL_TAB);
        if (!StringUtils.isEmptyOrWhitespaces(showCRL) && "true".equals(showCRL))
            this.GUI_SHOW_CRL_TAB = true;
        
        String showCR = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_CR_X509);
        if (!StringUtils.isEmptyOrWhitespaces(showCR))
            this.GUI_SHOW_CR_X509 = "true".equals(showCR);
        
        String showExitDialog = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_EXIT_DIALOG);
        if (!StringUtils.isEmptyOrWhitespaces(showExitDialog) && "false".equals(showExitDialog))
            this.GUI_SHOW_EXIT_DIALOG = false;
        String showExitDialogActiveThreads = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS);
        if (!StringUtils.isEmptyOrWhitespaces(showExitDialogActiveThreads) && "false".equals(showExitDialogActiveThreads))
            this.GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS = false;
        
        String showOpenVpnIpWarning = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_OPENVPN_IP_WARNING_DIALOG);
        if (!StringUtils.isEmptyOrWhitespaces(showOpenVpnIpWarning) && "false".equals(showOpenVpnIpWarning))
            this.GUI_SHOW_OPENVPN_IP_WARNING = false;
        
        String defaultDB = ConfigurationQueries.getValue(ConfigurationQueries.USE_DEFAULT_DB, DBConnector.getInstance().getBaseConnection());
        if (defaultDB != null) {
            this.USE_DEFAULT_DB = defaultDB.equals("true");
        }
        
        String usePamStr = ConfigurationQueries.getValue(ConfigurationQueries.USE_PAM);
        if (usePamStr != null) {
            this.USE_PAM = usePamStr.equals("true");
        }

        // x509 window positioning

        this.X509_GUI_LOCATION_X = setWindowPosition(ConfigurationQueries.X509_GUI_LOCATION_X, 0, 
                                                     Toolkit.getDefaultToolkit().getScreenSize().width);
        
        this.X509_GUI_LOCATION_Y = setWindowPosition(ConfigurationQueries.X509_GUI_LOCATION_Y, 0, 
        		                                     Toolkit.getDefaultToolkit().getScreenSize().height);
        
        this.X509_GUI_WIDTH = setWindowPosition(ConfigurationQueries.X509_GUI_WIDTH, 570, 
        		                                Toolkit.getDefaultToolkit().getScreenSize().width);
        
        this.X509_GUI_HEIGHT = setWindowPosition(ConfigurationQueries.X509_GUI_HEIGHT, 535, 
        		                                 Toolkit.getDefaultToolkit().getScreenSize().height);
        
    }
    
    /**
     * Helper Function to
     * initialize Window Positioning
     *
     * @param query  the query for the db
     * @param fallback_pos fallback value for position
     * @param screen size of the screen
     * 
     * @return the position for the window
     */
    private int setWindowPosition(String query, int fallback_pos,
    							   int screen ) {
    	int pos;
    	String get_pos =  ConfigurationQueries.getValue(query);
        pos = !StringUtils.isEmptyOrWhitespaces(get_pos) ? Integer.parseInt(get_pos) : fallback_pos;
        
        if ( pos > screen ) {
        	pos = fallback_pos;
        }
        
        return pos;
    }
    
    
    /**
     * Initialize X509 certificate settings.
     * Read settings from database, fallback reading from property file
     * if settings could not be read from database.
     */
    public void initializeX509() {

        this.X509_ROOT_SUBJECT = ConfigurationQueries.getValue(ConfigurationQueries.X509_ROOT_SUBJECT);
        this.X509_ROOT_VALID_FROM = ConfigurationQueries.getValue(ConfigurationQueries.X509_ROOT_VALID_FROM);
        this.X509_ROOT_VALID_TO = ConfigurationQueries.getValue(ConfigurationQueries.X509_ROOT_VALID_TO);
        this.X509_SERVER_SUBJECT = ConfigurationQueries.getValue(ConfigurationQueries.X509_SERVER_SUBJECT);
        this.X509_SERVER_VALID_FOR = ConfigurationQueries.getValue(ConfigurationQueries.X509_SERVER_VALID_FOR);
        this.X509_SERVER_VALID_FROM = ConfigurationQueries.getValue(ConfigurationQueries.X509_SERVER_VALID_FROM);
        this.X509_CLIENT_SUBJECT = ConfigurationQueries.getValue(ConfigurationQueries.X509_CLIENT_SUBJECT);
        this.X509_CLIENT_VALID_FOR = ConfigurationQueries.getValue(ConfigurationQueries.X509_CLIENT_VALID_FOR);
        this.X509_CLIENT_VALID_FROM = ConfigurationQueries.getValue(ConfigurationQueries.X509_CLIENT_VALID_FROM);
        this.X509_KEY_STRENGTH = ConfigurationQueries.getValue(ConfigurationQueries.X509_KEY_STRENGTH);


        ResourceBundle rootCertBundle;
        ResourceBundle serverCertBundle;
        ResourceBundle clientCertBundle;
        try {
            rootCertBundle = ResourceBundle.getBundle(Constants.ROOT_BUNDLE_NAME);
            serverCertBundle = ResourceBundle.getBundle(Constants.SERVER_BUNDLE_NAME);
            clientCertBundle = ResourceBundle.getBundle(Constants.CLIENT_BUNDLE_NAME);

            if (this.X509_ROOT_SUBJECT == null || "".equals(this.X509_ROOT_SUBJECT))
                this.X509_ROOT_SUBJECT = rootCertBundle.getString("subject");
            if (this.X509_ROOT_VALID_FROM == null || "".equals(this.X509_ROOT_VALID_FROM))
                this.X509_ROOT_VALID_FROM = formatStartDate(rootCertBundle.getString("startDate"));
            if (this.X509_ROOT_VALID_TO == null || "".equals(this.X509_ROOT_VALID_TO))
                this.X509_ROOT_VALID_TO = formatEndDate(rootCertBundle.getString("endDate"));

            if (this.X509_SERVER_SUBJECT == null || "".equals(this.X509_SERVER_SUBJECT))
                this.X509_SERVER_SUBJECT = serverCertBundle.getString("subject");
            if (this.X509_SERVER_VALID_FROM == null || "".equals(this.X509_SERVER_VALID_FROM))
                this.X509_SERVER_VALID_FROM = formatStartDate(serverCertBundle.getString("startDate"));
            if (this.X509_SERVER_VALID_FOR == null || "".equals(this.X509_SERVER_VALID_FOR))
                this.X509_SERVER_VALID_FOR = Integer.toString(Constants.DEFAULT_SERVER_CERT_VALIDITY);

            if (this.X509_CLIENT_SUBJECT == null || "".equals(this.X509_CLIENT_SUBJECT))
                this.X509_CLIENT_SUBJECT = clientCertBundle.getString("subject");
            if (this.X509_CLIENT_VALID_FROM == null || "".equals(this.X509_CLIENT_VALID_FROM))
                this.X509_CLIENT_VALID_FROM = formatStartDate(clientCertBundle.getString("startDate"));
            if (this.X509_CLIENT_VALID_FOR == null || "".equals(this.X509_CLIENT_VALID_FOR))
                this.X509_CLIENT_VALID_FOR = Integer.toString(Constants.DEFAULT_CLIENT_CERT_VALIDITY);

            if (this.X509_KEY_STRENGTH == null || "".equals(this.X509_KEY_STRENGTH))
                this.X509_KEY_STRENGTH = rootCertBundle.getString("key_strength");

        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: *_cert.properties could not be read", mre);
        }
    }


    /**
     * Formats the given date
     *
     * @param date The date to format
     * @return A String representing the date
     */
    private String formatStartDate(String date) {
        if ("now".equals(date))
            return Constants.PROPERTIES_DATE_FORMAT.format(new Date());

        return date;
    }


    /**
     * Formats the given date
     *
     * @param date The date to format
     * @return A String representing the date
     */
    private String formatEndDate(String date) {
        if (date != null && date.startsWith("now+")) {
            String years = date.substring(4);
            Calendar now = Calendar.getInstance();
            now.add(Calendar.YEAR, Integer.parseInt(years));
            return Constants.PROPERTIES_DATE_FORMAT.format(now.getTime());
        }

        return date;
    }


    /**
     * Sets the jdbc driver classname
     *
     * @param driver the new jdbc driver classname
     */
    private void setJdbcDriver(String driver) {
        this.JDBC_DRIVER_CLASSNAME = driver;
    }


    /**
     * Sets the jdbc url prefix
     *
     * @param prefix The URL prefix
     */
    private void setJdbcUrlPrefix(String prefix) {
        this.JDBC_URL_PREFIX = prefix;
    }


    /**
     * Sets the jdbc path
     *
     * @param path the new path
     */
    void setJdbcPath(String path) {
        this.JDBC_PATH = path;
        ConfigurationQueries.setValue(ConfigurationQueries.DB_PATH, path, DBConnector.getInstance().getBaseConnection());
    }
    
    
    /**
     * Sets the directory in the users home directory
     *
     * @param dir The directory
     */
    private void setManagerUserDirectory(String dir) {
        this.MANAGER_USER_DIRECTORY = dir;
    }
    
    
    /**
     * Sets the database filename
     * @param filename The filename
     */
    private void setDbFilename(String filename) {
        DB_FILENAME = filename;
    }


    /**
     * Sets the status, if the default database or an external database will be used
     * @param defaultDB true, if the default database will be used
     */
    public void setUseDefaultDB(boolean defaultDB) {
        this.USE_DEFAULT_DB = defaultDB;
        ConfigurationQueries.setValue(ConfigurationQueries.USE_DEFAULT_DB, defaultDB + "", DBConnector.getInstance().getBaseConnection());
    }
    
    /**
     * Use PAM or not
     * @param usePAM true, if PAM will be used for authentication
     */
    public void setUsePAM(boolean usePAM) {
        this.USE_PAM = usePAM;
        ConfigurationQueries.setValue(ConfigurationQueries.USE_PAM, usePAM + "");
    }

    /**
     * Sets the icon path
     */
    private void setIconPath(String iconPath) {
        this.ICON_PATH = iconPath;
    }


    /**
     * Sets the banner path
     */
    private void setBannerPath(String bannerPath) {
        this.BANNER_PATH = bannerPath;
    }


    /**
     * Sets the path to the logging properties
     *
     * @param filename the new filename
     */
    private void setLoggingProperties(String filename) {
        this.LOGGING_PROPERTIES = filename;
    }


    /**
     * Sets the path to the css file
     *
     * @param filename the new filename
     */
    private void setCssFile(String filename) {
        this.CSS_FILE = filename;
    }

    public void setLanguage(String languageCode) {
        ConfigurationQueries.setValue(ConfigurationQueries.LANGUAGE_KEY, languageCode);
        this.LANGUAGE = languageCode;
    }

    public void setClientCertImportDir(String dir) {
        ConfigurationQueries.setValue(ConfigurationQueries.CLIENT_CERT_IMPORT_DIR_KEY, dir);
        this.CLIENT_CERT_IMPORT_DIR = dir;
    }

    public void setClientUserfile(String userfile) {
        ConfigurationQueries.setValue(ConfigurationQueries.CLIENT_USERFILE, userfile);
        this.CLIENT_USERFILE = userfile;
    }

    public void setCertExportPath(String path) {
        ConfigurationQueries.setValue(ConfigurationQueries.CERT_EXPORT_PATH_KEY, path);
        this.CERT_EXPORT_PATH = path;
    }

    public void setLdapHost(String host) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_HOST, host);
        this.LDAP_HOST = host;
    }

    public void setLdapPort(String port) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_PORT, port);
        this.LDAP_PORT = port;
    }

    public void setLdapDN(String dn) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_DN, dn);
        this.LDAP_DN = dn;
    }

    public void setLdapObjectclass(String objClass) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_OBJECTCLASS, objClass);
        this.LDAP_OBJECTCLASS = objClass;
    }

    public void setLdapCN(String cn) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_CN, cn);
        this.LDAP_CN = cn;
    }

    public void setLdapCertAttrName(String name) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_CERT_ATTRIBUTE_NAME, name);
        this.LDAP_CERT_ATTRNAME = name;
    }

    public void setLdapCertImportDir(String dir) {
        ConfigurationQueries.setValue(ConfigurationQueries.LDAP_CERT_IMPORT_DIR, dir);
        this.LDAP_CERT_IMPORTDIR = dir;
    }

    public void setUserImportType(int type) {
        ConfigurationQueries.setValue(ConfigurationQueries.USER_IMPORT_TYPE, type + "");
        this.USER_IMPORT_TYPE = type;
    }

    public void setCertificateType(int type) {
        ConfigurationQueries.setValue(ConfigurationQueries.CERTIFICATE_TYPE, type + "");
        this.CERTIFICATE_TYPE = type;
    }

    public void setPkcs12PasswordType(int type) {
        ConfigurationQueries.setValue(ConfigurationQueries.PKCS12_PASSWORD_TYPE, type + "");
        this.PKCS12_PASSWORD_TYPE = type;
    }
    
    public void setTestPkcsPasswordDisabled(boolean flag) {
        this.TEST_PKCS12_PASSWORD_DISABLED = flag;
    }

    public void setCAEnabled(boolean enabled) {
        ConfigurationQueries.setValue(ConfigurationQueries.CA_ENABLED, enabled + "");
        this.CA_ENABLED = enabled;
    }

    public void setCCEnabled(boolean enabled) {
        ConfigurationQueries.setValue(ConfigurationQueries.CC_ENABLED, enabled + "");
        this.CC_ENABLED = enabled;
    }


    public void setX509RootSubject(String x509_root_subject) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_ROOT_SUBJECT, x509_root_subject);
        this.X509_ROOT_SUBJECT = x509_root_subject;
    }

    public void setX509RootValidFrom(String x509_root_valid_from) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_ROOT_VALID_FROM, x509_root_valid_from);
        this.X509_ROOT_VALID_FROM = x509_root_valid_from;
    }

    public void setX509RootValidTo(String x509_root_valid_to) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_ROOT_VALID_TO, x509_root_valid_to);
        this.X509_ROOT_VALID_TO = x509_root_valid_to;
    }

    public void setX509ServerSubject(String x509_server_subject) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_SERVER_SUBJECT, x509_server_subject);
        this.X509_SERVER_SUBJECT = x509_server_subject;
    }

    public void setX509ServerValidFrom(String x509_server_valid_from) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_SERVER_VALID_FROM, x509_server_valid_from);
        this.X509_SERVER_VALID_FROM = x509_server_valid_from;
    }

    public void setX509ServerValidFor(String x509_server_valid_for) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_SERVER_VALID_FOR, x509_server_valid_for);
        this.X509_SERVER_VALID_FOR = x509_server_valid_for;
    }

    public void setX509ClientSubject(String x509_client_subject) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_CLIENT_SUBJECT, x509_client_subject);
        this.X509_CLIENT_SUBJECT = x509_client_subject;
    }

    public void setX509ClientValidFrom(String x509_client_valid_from) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_CLIENT_VALID_FROM, x509_client_valid_from);
        this.X509_CLIENT_VALID_FROM = x509_client_valid_from;
    }

    public void setX509ClientValidFor(String x509_client_valid_for) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_CLIENT_VALID_FOR, x509_client_valid_for);
        this.X509_CLIENT_VALID_FOR = x509_client_valid_for;
    }


    public void setX509KeyStrength(String x509_key_strength) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_KEY_STRENGTH, x509_key_strength);
        this.X509_KEY_STRENGTH = x509_key_strength;
    }

    private void setUserPasswordLength(int userPasswordLength) {
        this.USER_PASSWORD_LENGTH = userPasswordLength;
    }


    private void setServerWrapperCommand(String wrapperCommand) {
        this.SERVER_WRAPPER_COMMAND = wrapperCommand;
    }

    private void setServerUsername(String username) {
        this.SERVER_USERNAME = username;
    }

    private void setServerPasswdPath(String path) {
        this.SERVER_PASSWD_PATH = path;
    }
    
    private void setServerCCPath(String path) {
    	this.SERVER_CC_PATH = path;
    }
    
    private void setServerNetworkAddress(String address) {
        this.SERVER_NETWORK_ADDRESS = address;
    }
    
    private void setServerDevice(int device) {
        this.SERVER_DEVICE = device;
    }

    private void setServerKeysPath(String path) {
        this.SERVER_KEYS_PATH = path;
    }

    void setDebugSSH(boolean debug_ssh) {
        DEBUG_SSH = debug_ssh;
    }

    void setDebugLogging(boolean debugLogging) { DEBUG_LOGGING = debugLogging; }

    private void setUpdateKeystorePath(String path) {
        String UPDATE_KEYSTORE_PATH = path;
    }


    public void setUpdateAutomatically(boolean autoUpdate) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_AUTOMATICALLY, autoUpdate + "");
        UPDATE_AUTOMATICALLY = autoUpdate;
    }

    public void setUpdateServerPath(String path) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_SERVER_PATH, path);
        UPDATE_SERVER = path;
    }

    public void setUpdateRepository(String repo) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_REPOSITORY, repo);
        UPDATE_REPOSITORY = repo;
    }

    public void setUpdateProxy(String proxy) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_PROXY, proxy);
        UPDATE_PROXY = proxy;
    }

    public void setUpdateProxyPort(String proxyPort) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_PROXY_PORT, proxyPort);
        UPDATE_PROXY_PORT = proxyPort;
    }

    public void setUpdateProxyUsername(String proxyUsername) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_PROXY_USERNAME, proxyUsername);
        UPDATE_PROXY_USERNAME = proxyUsername;
    }

    public void setUpdateProxyPassword(String proxyPassword) {
        ConfigurationQueries.setValue(ConfigurationQueries.UPDATE_PROXY_PASSWORD, proxyPassword);
        UDPATE_PROXY_PASSWORD = proxyPassword;
    }

    public boolean isClientCertImportDirSet() {
        return CLIENT_CERT_IMPORT_DIR != null && !"".equals(CLIENT_CERT_IMPORT_DIR);
    }
    
    public void setRootCertExisting(boolean existing) {
        rootCertExisting = existing;
    }
    
    public boolean isRootCertExisting() {
        return (rootCertExisting || X509DAO.isRootCertExisting());
    }

    public void setGuiWidth(int width) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_WIDTH, width + "");
        this.GUI_WIDTH = width;
    }
    
    public void setGuiHeight(int height) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_HEIGHT, height + "");
        this.GUI_HEIGHT = height;
    }

    public void setGuiLocationX(int locationX) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_LOCATION_X, locationX + "");
        this.GUI_LOCATION_X = locationX;
    }
    
    public void setGuiLocationY(int locationY) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_LOCATION_Y, locationY + "");
        this.GUI_LOCATION_Y = locationY;
    }
    
    public void setX509GuiHeight(int height) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_GUI_HEIGHT, height + "");
        this.X509_GUI_HEIGHT = height;
    }
    
    public void setX509GuiWidth(int width) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_GUI_WIDTH, width + "");
        this.X509_GUI_WIDTH = width;
    }
    
    public void setX509GuiLocationX(int locationX) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_GUI_LOCATION_X, locationX + "");
        this.X509_GUI_LOCATION_X = locationX;
    }
    
    public void setX509GuiLocationY(int locationY) {
        ConfigurationQueries.setValue(ConfigurationQueries.X509_GUI_LOCATION_Y, locationY + "");
        this.X509_GUI_LOCATION_Y = locationY;
    }
    
    
    public void setServerUserTreeDividerLocation(int serverUserTreeDividerLocation) {
        ConfigurationQueries.setValue(ConfigurationQueries.SERVER_USER_TREE_DIVIDER_LOCATION, serverUserTreeDividerLocation + "");
        this.SERVER_USER_TREE_DIVIDER_LOCATION = serverUserTreeDividerLocation;
    }
    
    public void setGuiShowCRL(boolean showCRL) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_SHOW_CRL_TAB, showCRL + "");
        this.GUI_SHOW_CRL_TAB = showCRL;
    }
    
    public void setGuiShowCRX509(boolean crX509) {
    	ConfigurationQueries.setValue(ConfigurationQueries.GUI_SHOW_CR_X509, crX509 + "");
        this.GUI_SHOW_CR_X509 = crX509;
    }
    
    public void setGuiShowExitDialog(boolean showExitDialog) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_SHOW_EXIT_DIALOG, showExitDialog + "");
        this.GUI_SHOW_EXIT_DIALOG = showExitDialog;
    }
    
    public void setGuiShowExitDialogActiveThreads(boolean showExitDialogActiveThreads) {
        ConfigurationQueries.setValue(ConfigurationQueries.GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS, showExitDialogActiveThreads + "");
        this.GUI_SHOW_EXIT_DIALOG_ACTIVE_THREADS = showExitDialogActiveThreads;
    }

    public boolean suggestPasswords() {
        return SUGGEST_PASSWORD;
    }

    public void setSuggestPasswords(boolean suggestPasswords) {
        this.SUGGEST_PASSWORD = suggestPasswords;
    }
    
    public void setGuiShowOpenVPNIpWarningDialog(boolean show) {
    	ConfigurationQueries.setValue(ConfigurationQueries.GUI_SHOW_OPENVPN_IP_WARNING_DIALOG, show + "");
    	this.GUI_SHOW_OPENVPN_IP_WARNING = show;
    }
    
    public void setCreateOpenVPNConfigFiles(boolean createOpenVPNConfigFiles) {
        ConfigurationQueries.setValue(ConfigurationQueries.CREATE_OPENVPN_CONFIG_FILES, createOpenVPNConfigFiles + "");
        this.CREATE_OPENVPN_CONFIG_FILES = createOpenVPNConfigFiles;
    }

    public String getLDAP_CN() {
        return LDAP_CN;
    }

    public void setLDAP_CN(String LDAP_CN) {
        this.LDAP_CN = LDAP_CN;
    }
}
