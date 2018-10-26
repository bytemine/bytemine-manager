/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.*;

import net.bytemine.manager.action.ConfigurationAction;
import net.bytemine.manager.css.CssLoader;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ConfigurationQueries;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.schema.SchemaUpdater;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * The main manager app
 *
 * @author Daniel Rauer
 */
public class ManagerApp {

    private static Logger logger = Logger.getLogger(ManagerApp.class.getName());
    private static boolean exportConfig = false;
    public static boolean intermediate = false;
    private static boolean debugLogging = false;
    private static boolean debugSSH = false;

    public static void main(String[] args) {

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (("--debugSSH".equalsIgnoreCase(arg)) ||
                    ("debugSSH".equalsIgnoreCase(arg)))
                    debugSSH = true;
                else if (("--debugLogging".equalsIgnoreCase(arg)) ||
                         ("debugLogging".equalsIgnoreCase(arg)))
                    debugLogging = true;
                else if (("--export-config".equalsIgnoreCase(arg)) ||
                         ("export-config".equalsIgnoreCase(arg)))
                    exportConfig = true;
                else if (("--intermediate".equalsIgnoreCase(arg)) ||
                         ("intermediate".equalsIgnoreCase(arg)))
                    intermediate = true;
                else if (("--dbPath".equalsIgnoreCase(arg)) ||
                         ("dbPath".equalsIgnoreCase(arg))) {
                    if (args[i+1] != null) {
                        Configuration.getInstance().setJdbcPath(args[i+1]);
                        i++;
                    } else {
                        printUsageManual();
                    }
                }
                else {
                    printUsageManual();
                    return;
                }
            }
        }
        
        startup();
    }
    
    
    private static void startup() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    String loggingProperties = Configuration.getInstance().LOGGING_PROPERTIES;
                    if (debugLogging) {
                        loggingProperties = Configuration.getInstance().LOGGING_DEBUG_PROPERTIES;
                    }
                    InputStream is = getClass().getResourceAsStream(loggingProperties);
                    LogManager.getLogManager().readConfiguration(is);
                } catch (IOException | SecurityException e) {
                    System.err.println("!!! logging.properties could not be read !!!");
                    e.printStackTrace();
                }
                logger.info("Starting application at " + new Date());

                Properties systemProperties = System.getProperties();
                Enumeration<?> systemPropertiesKeys = systemProperties.keys();

                while(systemPropertiesKeys.hasMoreElements()) {
                    String prop = (String) systemPropertiesKeys.nextElement();
                    logger.info(prop +": "+ systemProperties.getProperty(prop));
                }
                
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

                String osName = System.getProperty("os.name");
                String userHome = System.getProperty("user.home");
                String dbDirName = (osName.contains("Windows")) ? "bytemine-manager" : ".bytemine-manager";
                File dbDir = new File(userHome + "/" + dbDirName);
                File newDB = new File(dbDir + "/manager.db");
                try {
                    DBConnector.getInstance();
                    boolean dbPathChanged = DBConnector.dbPathChanged;
                    
                    if (dbPathChanged)
                        Configuration.getInstance().setJdbcPath(DBConnector.dbPath);
                    
                    // look for schema update and update if needed
                    updateSchema(rb);

                } catch (Exception e) {
                    // database is not available
                    logger.log(Level.SEVERE, "the database file could not be found/created at "+newDB.toString(), e);
                    new VisualException(rb.getString("error.db.text"), rb.getString("error.db.title"));
                    System.exit(0);
                }

                try {
                    if (osName.contains("Windows"))
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    else
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

                } catch (ClassNotFoundException | IllegalAccessException | UnsupportedLookAndFeelException | InstantiationException e) {
                    logger.log(Level.SEVERE, "could not set look and feel", e);
                }

                // load css
                try {
                    InputStream xmlStream = getClass().getResourceAsStream(Configuration.getInstance().CSS_FILE);
                    CssLoader.load(xmlStream, CssRuleManager.getInstance());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "could not load css rules", e);
                }

                // initialize Configuration
                Configuration config = Configuration.getInstance();
                if (ConfigurationQueries.areConfigurationsExisiting())
                    config.initializeDB();
                // switch to an external database?
                if (!config.USE_DEFAULT_DB)
                    DBConnector.getInstance().switchToExternalDatabase();
                
                // look for schema update and update if needed
                updateSchema(rb);

                // register BouncyCastleProvider
                // Usage: KeyFactory.getInstance("RSA", "BC");
                // Usage: CertificateFactory.getInstance("X509", "BC");
                Security.addProvider(new BouncyCastleProvider());

                // initialize Configuration from current database
                config = Configuration.getInstance();
                if (ConfigurationQueries.areConfigurationsExisiting())
                    config.initializeDB();
                config.initializeX509();
                
                if (config.LANGUAGE != null)
                    // set language with database setting
                    ResourceBundleMgmt.getInstance().setSelectedLanguage(config.LANGUAGE);

                if (exportConfig)
                    ConfigurationAction.dumpConfigurationToFile();
                if (debugSSH)
                    Configuration.getInstance().setDebugSSH(true);
                if (debugLogging)
                    Configuration.getInstance().setDebugLogging(true);

                // build up the application
                new ManagerGUI();
                
            }
        });
    }

    private static void updateSchema(ResourceBundle rb) {
        try {
            // update database schema
            SchemaUpdater schemaUpdater = new SchemaUpdater(Constants.UPDATE_SCHEMA_FILE);
            schemaUpdater.updateFromXml();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while updating the database schema", e);
            new VisualException(
                    rb.getString("dialog.switch_database.db_update_text"),
                    rb.getString("dialog.switch_database.db_update_title"));
        }
    }


    /**
     * prints a manual to System.out
     */
    private static void printUsageManual() {
        System.out.println("Usage: java -jar bytemine-manager <options>");
        System.out.println("options are:");
        System.out.println("    --dbPath path-to-manager-db: pass the location of manager db");
        System.out.println("    --help: displays this dialog");
        System.out.println("    --debugSSH: displays ssh communication in separate frame");
        System.out.println("    --debugLogging: enables a finer logging");
        System.out.println("    --export-config: exports the configuration to filesystem");
    }

}
