/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.action;

import java.io.FileNotFoundException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.db.ConfigurationQueries;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.LicenceQueries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.CustomJOptionPane;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.update.KeystoreMgmt;
import net.bytemine.utility.FileUtils;

/**
 * Configuration actions
 *
 * @author Daniel Rauer
 */
public class ConfigurationAction {

    private static Logger logger = Logger.getLogger(ConfigurationAction.class.getName());

    /**
     * Save the configurations
     *
     * @return true, if all following dialogs shall not be displayed
     */
    public static boolean saveConfiguration(
            String clientCertExportPath,
            boolean CAEnabled,
            boolean CCEnabled,
            boolean pkcs12,
            boolean noPasswd,
            boolean singlePasswd,
            String dbPath,
            boolean useDefaultDB,
            boolean usePAM,
            boolean createOpenVPNConfigFiles
    ) {
        boolean skipFollowingDialogs = false;
        boolean showRestartDialog = false;
        try {
            Configuration.getInstance().setCertExportPath(clientCertExportPath);
            Configuration.getInstance().setCAEnabled(CAEnabled);
            Configuration.getInstance().setCCEnabled(CCEnabled);
            Configuration.getInstance().setUsePAM(usePAM);
            Configuration.getInstance().setCreateOpenVPNConfigFiles(createOpenVPNConfigFiles);

            int x509Type = Constants.CERTIFICATE_TYPE_BASE64;
            if (pkcs12)
                x509Type = Constants.CERTIFICATE_TYPE_PKCS12;
            Configuration.getInstance().setCertificateType(x509Type);

            if (x509Type == Constants.CERTIFICATE_TYPE_PKCS12) {
                if (noPasswd)
                    Configuration.getInstance().setPkcs12PasswordType(Constants.PKCS12_NO_PASSWORD);
                else if (singlePasswd)
                    Configuration.getInstance().setPkcs12PasswordType(Constants.PKCS12_SINGLE_PASSWORD);
            }

            if (!useDefaultDB && dbPath != null && !"".equals(dbPath)) {
                if (!Configuration.getInstance().JDBC_PATH.equals(dbPath)) {
                    logger.info("Setting new DB Path: " + dbPath);
                    ConfigurationQueries.setValue(ConfigurationQueries.DB_PATH, dbPath);
                    ConfigurationQueries.setValue(ConfigurationQueries.DB_UP_TO_DATE, Constants.DB_OUTDATED);
                    
                    // also store these settings in the internal database
                    ConfigurationQueries.setValue(ConfigurationQueries.DB_PATH, dbPath, DBConnector.getInstance().getBaseConnection());
                    ConfigurationQueries.setValue(ConfigurationQueries.DB_UP_TO_DATE, Constants.DB_OUTDATED, DBConnector.getInstance().getBaseConnection());
                    skipFollowingDialogs = true;
                    showRestartDialog = true;
                }
            }
            
            // setting has changed, restart necessary
            if (Configuration.getInstance().USE_DEFAULT_DB != useDefaultDB) 
                showRestartDialog = true;
                
            Configuration.getInstance().setUseDefaultDB(useDefaultDB);
            
            if (showRestartDialog) {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                CustomJOptionPane.showMessageDialog(
                        ManagerGUI.mainFrame,
                        rb.getString("dialog.configuration.restart.text"),
                        rb.getString("dialog.configuration.restart.title")
                );
                ManagerGUI.mainFrame.dispose();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error storing configurations", e);
        }

        return skipFollowingDialogs;
    }

    
    /**
     * Save the import configurations
     *
     * @return true, if all following dialogs shall not be displayed
     */
    public static boolean saveImportConfiguration(
            String clientCertImportDir,
            String userfile,
            String host,
            String port,
            String dn,
            String objectClass,
            String cn,
            String certAttrName,
            String certImportDir,
            boolean importTypeFile
    ) {
        boolean skipFollowingDialogs = false;
        try {
            Configuration.getInstance().setClientCertImportDir(clientCertImportDir);
            Configuration.getInstance().setClientUserfile(userfile);
            Configuration.getInstance().setLdapHost(host);
            Configuration.getInstance().setLdapPort(port);
            Configuration.getInstance().setLdapDN(dn);
            Configuration.getInstance().setLdapObjectclass(objectClass);
            Configuration.getInstance().setLdapCN(cn);
            Configuration.getInstance().setLdapCertAttrName(certAttrName);
            Configuration.getInstance().setLdapCertImportDir(certImportDir);
            
            int type = Constants.USER_IMPORT_TYPE_FILE;
            if (!importTypeFile)
                type = Constants.USER_IMPORT_TYPE_LDAP;
            Configuration.getInstance().setUserImportType(type);
            skipFollowingDialogs = true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error storing import configurations", e);
        }

        return skipFollowingDialogs;
    }


    /**
     * Save the x509configurations
     * @return boolean wether or not the X509 configuration was successfull
     */
    public static boolean saveX509Configuration(
            String rootSubject,
            String rootValidFrom,
            String rootValidTo,
            String serverValidFrom,
            String serverValidTo,
            String clientValidFrom,
            String clientValidFor,
            String keyStrength
    ) {
        try {
            Configuration.getInstance().setX509RootSubject(rootSubject);
            Configuration.getInstance().setX509ServerSubject(rootSubject);
            Configuration.getInstance().setX509ClientSubject(rootSubject);
            Configuration.getInstance().setX509RootValidFrom(rootValidFrom);
            Configuration.getInstance().setX509RootValidTo(rootValidTo);
            Configuration.getInstance().setX509ServerValidFrom(serverValidFrom);
            Configuration.getInstance().setX509ServerValidFor(serverValidTo);
            Configuration.getInstance().setX509ClientValidFrom(clientValidFrom);
            Configuration.getInstance().setX509ClientValidFor(clientValidFor);
            Configuration.getInstance().setX509KeyStrength(keyStrength);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error storing x509configurations", e);
            return false;
        }

        return true;
    }


    /**
     * Save the update configurations
     *
     * @param automaticUpdate Flag for automatic update
     * @param pemSelected     Flag for pem file or crt/key files
     * @param crtPath         Path to crt/pem file
     * @param keyPath         Path to key file
     * @param serverPath      Path to update server
     * @param repoPath        Path to the update repository
     * @return true, if changes on the certificates occurred
     * @throws Exception Thrown if the certificate could not be stored
     */
    public static boolean saveUpdateConfiguration(
            boolean automaticUpdate,
            boolean pemSelected,
            String crtPath,
            String keyPath,
            String serverPath,
            String repoPath,
            String proxy,
            String proxyPort
    ) throws Exception {

        Configuration.getInstance().setUpdateAutomatically(automaticUpdate);
        if (!"".equals(serverPath))
            Configuration.getInstance().setUpdateServerPath(serverPath);
        else
            Configuration.getInstance().setUpdateServerPath(Constants.UPDATE_HTTPS_SERVER);

        if (!"".equals(repoPath))
            Configuration.getInstance().setUpdateRepository(repoPath);
        else
            Configuration.getInstance().setUpdateRepository(Constants.UPDATE_REPOSITORY);

        if (proxy != null && !"".equals(proxy))
            Configuration.getInstance().setUpdateProxy(proxy);
        if (proxyPort != null && !"".equals(proxyPort))
            Configuration.getInstance().setUpdateProxyPort(proxyPort);

        String crtPathDb = LicenceQueries.getCrtPath();
        String keyPathDb = LicenceQueries.getKeyPath();

        if (crtPath != null && !"".equals(crtPath)) {
            if (crtPath.equals(crtPathDb) && keyPath.equals(keyPathDb))
                return false;


            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            try {
                KeystoreMgmt ksUtils;
                if (pemSelected)
                    ksUtils = new KeystoreMgmt(crtPath);
                else
                    ksUtils = new KeystoreMgmt(crtPath, keyPath);
                ksUtils.store();

            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, "error saving the update configuration", e);
                throw new Exception(rb.getString("dialog.updateconfiguration.errortext3"));
            } catch (VisualException e) {
                throw e;
            } catch (CertificateNotYetValidException e) {
                throw new Exception(rb.getString("dialog.updateconfiguration.errortext5"));
            } catch (CertificateExpiredException e) {
                throw new Exception(rb.getString("dialog.updateconfiguration.errortext6"));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "error saving the update configuration", e);
                throw new Exception(rb.getString("dialog.updateconfiguration.errortext2"));
            }
        }
        return true;
    }


    /**
     * writes all configurations to filesystem
     */
    public static void dumpConfigurationToFile() {
        StringBuffer dump = new StringBuffer();
        dump.append(new Date() + "\n");
        
        if (ConfigurationQueries.areConfigurationsExisiting()) {
            Hashtable<String, String> configs = ConfigurationQueries.getConfigurations();
            for (Iterator<String> iterator = configs.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                String value = configs.get(key);
                dump.append("\n" + key + ": " + value);
            }
        }
        try {
            FileUtils.writeStringToFile(dump.toString(), "configuration.txt");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error dumping the configuration", e);
        }
    }

}
