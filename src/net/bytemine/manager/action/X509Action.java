/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.action;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ManagerApp;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.CustomJOptionPane;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.PKCS12Exporter;
import net.bytemine.manager.utility.X509Exporter;
import net.bytemine.manager.utility.X509Serializer;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.utility.FileUtils;


/**
 * Some actions for X509 objects
 * @author Daniel Rauer
 *
 */
public class X509Action {
    
    private static Logger logger = Logger.getLogger(X509Action.class.getName());
    
    /**
     * Exports all certificates into the export path
     */
    public static void exportAllCertificatesToFilesystem() {
        final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        SwingWorker<String, Void> exportWorker = new SwingWorker<String, Void>() {
            Thread t;
            int count = 0;

            protected String doInBackground() {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.certExport"));

                Vector<String> ids = X509Queries.getAllX509Ids();
                ids.forEach(id -> {
                    try {
                        String result = exportToFilesystem(id);
                        if (result != null)
                            count++;
                    } catch (Exception e) {
                        new VisualException(e.getMessage());
                    }
                });

                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
                
                String message = count + " " + 
                    rb.getString("export.message") + 
                    "\n" +
                    Configuration.getInstance().CERT_EXPORT_PATH;
                CustomJOptionPane.showMessageDialog(ManagerGUI.mainFrame, 
                        message, rb.getString("export.title"));
            }

        };
        exportWorker.execute();
    }
    
    
    /**
     * Exports a certificate to the filesystem
     * @param x509idstr The x509id as String
     * @return Path where the certificates where exported
     * @throws Exception
     */
    public static String exportToFilesystem(String x509idstr) throws Exception {
        String friendlyName;
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        try {
            int x509id = Integer.parseInt(x509idstr);
            X509 x509 = new X509(x509id);
            x509 = X509DAO.getInstance().read(x509);
            
            if (x509 == null)
                return null;
            
            User user = null;
            Server server = null;
            int serverid = -1;
            if (x509.getType() == X509.X509_TYPE_CLIENT || x509.getType() == X509.X509_TYPE_PKCS12) {
                user = UserQueries.getUserByX509id(x509id);
                // create export directory and export the root crt
                if (user != null)
                    UserAction.prepareFilesystem(user.getUsername());
            } else if (x509.getType() == X509.X509_TYPE_SERVER) {
                server = ServerQueries.getServerByX509id(x509id);
                if (server == null)
                    return null;
                ServerAction.prepareFilesystem(server.getName());
                serverid = server.getServerid();
            }
            
            X509Serializer xs = X509Serializer.getInstance();
            X509Certificate cert = (X509Certificate)xs.fromXML(x509.getCertSerialized());
            
            X509Exporter exporter = new X509Exporter(
                        x509.getType(),
                        x509.getFileName(),
                        FileUtils.replaceExtension(x509.getFileName(), Constants.DEFAULT_KEY_EXTENSION),
                        Configuration.getInstance().CERT_EXPORT_PATH,
                        cert,
                        user,
                        serverid
                );
            
            String subject = cert.getSubjectDN().toString();
            friendlyName = X509Utils.getCnFromSubject(subject);
            
            // different types of certificates must be exported differently
            if (Configuration.getInstance().CERTIFICATE_TYPE == Constants.CERTIFICATE_TYPE_BASE64) {
                boolean exportBinary = true;
                try {
                    ResourceBundle clientCertBundle = ResourceBundle.getBundle(Constants.CLIENT_BUNDLE_NAME);
                    exportBinary = Boolean.parseBoolean(clientCertBundle.getString("export_binary"));
                } catch (MissingResourceException mre) {
                    logger.log(Level.SEVERE, "Error: client_cert.properties could not be read", mre);
                }
                // export the certificate to file system
                try {
                    exporter.exportCertToFile(x509.getContent(), exportBinary);
                } catch (Exception e) {
                    new VisualException(e);
                }

                // export the private key to file system
                try {
                    exporter.exportKeyToFile(x509.getKeyContent());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error exporting private key", e);
                    throw new Exception(rb.getString("dialog.cert.exporterror") + ": " + friendlyName);
                }
            } else if (Configuration.getInstance().CERTIFICATE_TYPE == Constants.CERTIFICATE_TYPE_PKCS12) {
                try {
                    X509 signingX509;
                    signingX509 = ManagerApp.intermediate ? X509Utils.loadIntermediateX509() : X509Utils.loadRootX509();
                    X509Certificate signingCert = (X509Certificate)
                            X509Serializer.getInstance().fromXML(
                                    signingX509.getCertSerialized()
                            );
                    PrivateKey privKey = (PrivateKey)X509Serializer.getInstance().fromXML(x509.getKey());

                    String dialogHeadline = rb.getString("dialog.pkcs12password.headline1");
                    dialogHeadline += " " + friendlyName;
                    dialogHeadline += " " + rb.getString("dialog.pkcs12password.headline2");
                    
                    String pkcs12Password = null;
                    if (Configuration.getInstance().PKCS12_PASSWORD_TYPE == Constants.PKCS12_SINGLE_PASSWORD)
                        pkcs12Password = Dialogs.showPKCS12PasswordDialog(ManagerGUI.mainFrame, dialogHeadline);
                    PKCS12Exporter pkcs12Exporter = null;
                    if (user != null)
                        pkcs12Exporter = new PKCS12Exporter(cert, privKey, signingCert, user, null);
                    else if (server != null)
                        pkcs12Exporter = new PKCS12Exporter(cert, privKey, signingCert, server);
                    else
                        pkcs12Exporter = new PKCS12Exporter(cert, privKey, signingCert, null);
                    
                    String pkcs12Content = pkcs12Exporter.exportPKCS12ToFile(pkcs12Password);
                    pkcs12Exporter.storePKCS12(pkcs12Content, x509id);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error exporting PKCS#12 certificate", e);
                    throw new Exception(rb.getString("dialog.cert.exporterror") + ": " + friendlyName);
                }
            }
            
            return exporter.getPath();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error exporting a certificate to the filesystem", e);
            throw e;
        }
    }
}
