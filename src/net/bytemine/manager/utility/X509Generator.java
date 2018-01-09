/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.security.Security;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.security.PrivateKey;
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
import net.bytemine.manager.db.CRLQueries;
import net.bytemine.manager.db.ServerDAO;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusMessage;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.DNUtil;
import net.bytemine.utility.DateUtils;
import net.bytemine.utility.StringUtils;

import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * Generators for keys and certificates
 *
 * @author Daniel Rauer
 */
public class X509Generator {

    private static Logger logger = Logger.getLogger(X509Generator.class.getName());
    private static ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    net.bytemine.crypto.x509.X509Generator generator = null;

    // the bundles to load data from
    ResourceBundle rootCertBundle = null;
    ResourceBundle interCertBundle = null;
    ResourceBundle serverCertBundle = null;
    ResourceBundle clientCertBundle = null;

    // export to file?
    private boolean exportCert = true;
    // export the key?
    private boolean exportKey = true;
    // export as binary?
    private boolean exportBinary = false;
    // export plain text additionally?
    private boolean exportText = false;
    // key strength
    int keyStrength = 1024;


    public X509Generator() {

        // read the root properties file
        try {
            rootCertBundle = ResourceBundle.getBundle(Constants.ROOT_BUNDLE_NAME);
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: root_cert.properties could not be read", mre);
        }

        // read the intermediate properties file
        try {
            interCertBundle = ResourceBundle.getBundle(Constants.INTERMEDIATE_BUNDLE_NAME);
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: inter_cert.properties could not be read", mre);
        }

        // read the server properties file
        try {
            serverCertBundle = ResourceBundle.getBundle(Constants.SERVER_BUNDLE_NAME);
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: server_cert.properties could not be read", mre);
        }

        // read the client properties file
        try {
            clientCertBundle = ResourceBundle.getBundle(Constants.CLIENT_BUNDLE_NAME);
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: client_cert.properties could not be read", mre);
        }


        try {
            keyStrength = Integer.parseInt(Configuration.getInstance().X509_KEY_STRENGTH);
        } catch (NumberFormatException nfe) {
            logger.warning("no valid key strength specified. using default value");
            keyStrength = Integer.parseInt(rootCertBundle.getString("key_strength"));
        }

        String generatorIdentifier = 
                "Manager version: " + Configuration.getInstance().MANAGER_VERSION + ", " +
                "build: " + Configuration.getInstance().MANAGER_BUILD;
        generator = new net.bytemine.crypto.x509.X509Generator(keyStrength, generatorIdentifier);
    }


    /**
     * Creates a root certificate
     * can export it to file system
     *
     * @throws java.lang.Exception
     */
    public void createRootCert() throws Exception {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.rootcert.tooltip"));

                createRootCertImmediately();

                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);

                if (ManagerApp.intermediate)
                    try {
                        createIntermediateCert(X509Utils.loadRootCertificate());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "error generating intermediate certificate", e);
                    }
            }

        };
        generateWorker.execute();
    }


    /**
     * Creates a root certificate in a new Thread
     *
     * @throws java.lang.Exception
     */
    public void createRootCertImmediately() throws Exception {
        try {
            String subject = Configuration.getInstance().X509_ROOT_SUBJECT;
            String validFrom = Configuration.getInstance().X509_ROOT_VALID_FROM;
            String validTo = Configuration.getInstance().X509_ROOT_VALID_TO;

            X509Certificate cert = generator.createRootCert(subject,
                        Constants.PROPERTIES_DATE_FORMAT.parse(validFrom),
                        Constants.PROPERTIES_DATE_FORMAT.parse(validTo));
            if (cert == null)
                throw new Exception(rb.getString("error.x509.generate.root"));
            
            PrivateKey rootPrivKey = generator.getRootPrivateKey();

            exportBinary = Boolean.parseBoolean(rootCertBundle.getString("export_binary"));
            exportText = Boolean.parseBoolean(rootCertBundle.getString("export_text"));
            exportKey = Boolean.parseBoolean(rootCertBundle.getString("export_key"));

            String contentStr = null;
            String keyStr = null;
            String issuer = PrincipalUtil.getIssuerX509Principal(cert).toString();

            X509Exporter exporter = new X509Exporter(
                    X509.X509_TYPE_ROOT,
                    rootCertBundle.getString("export_cert_file"),
                    rootCertBundle.getString("export_key_file"),
                    Configuration.getInstance().CERT_EXPORT_PATH,
                    cert,
                    null
            );

            contentStr = exporter.generateContent(exportBinary, exportText);

            // export the certificate to file system
            if (exportCert) {
                try {
                    exporter.exportCertToFile(contentStr, exportBinary);
                } catch (Exception e) {
                    new VisualException(e);
                }
            }

            // add a header to the key
            keyStr = X509Utils.addKeyHeader(rootPrivKey.getEncoded());
            // export the private key to file system
            if (exportKey)
                exporter.exportKeyToFile(keyStr);

            // store the certificate in db
            exporter.storeCertificate(
                    cert.getSerialNumber().longValue(), issuer, subject, contentStr,
                    rootPrivKey, keyStr, cert.getNotBefore(), cert.getNotAfter(), true);

            // create empty certificate revocation list
            createCRL();

            ManagerGUI.refreshX509Table();
            ManagerGUI.addStatusMessage(new StatusMessage(rb.getString("statusBar.rootca.generated"), StatusMessage.TYPE_CONFIRM));
        } catch (Exception e) {
            new VisualException(e);
        }

    }


    /**
     * Creates an intermediate certificate
     * can export it to file system
     *
     * @param rootCert The root certificate
     * @throws java.lang.Exception
     */
    public void createIntermediateCert(final X509Certificate rootCert) throws Exception {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.intercert.tooltip"));

                createIntermediateCertImmediately(rootCert);

                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();
    }


    /**
     * Creates an intermediate certificate
     * can export it to file system
     *
     * @param rootCert The root certificate
     * @throws java.lang.Exception
     */
    public void createIntermediateCertImmediately(final X509Certificate rootCert) throws Exception {
        try {
            X509 rootX509 = X509Utils.loadRootX509();
            PrivateKey rootPrivKey = X509Utils.extractRootPrivateKey(rootX509);

            X509Certificate cert = generator.createIntermediateCert(rootCert, rootPrivKey);
            if (cert == null)
                throw new Exception(rb.getString("error.x509.generate.intermediate"));
            
            PrivateKey privKey = generator.getPrivateKey();

            String contentStr = null;
            String keyStr = null;
            String issuer = PrincipalUtil.getIssuerX509Principal(cert).toString();
            String subject = PrincipalUtil.getSubjectX509Principal(cert).toString();

            exportBinary = Boolean.parseBoolean(interCertBundle.getString("export_binary"));
            exportText = Boolean.parseBoolean(interCertBundle.getString("export_text"));

            X509Exporter exporter = new X509Exporter(
                    X509.X509_TYPE_INTERMEDIATE,
                    interCertBundle.getString("export_cert_file"),
                    interCertBundle.getString("export_key_file"),
                    Configuration.getInstance().CERT_EXPORT_PATH,
                    cert,
                    null
            );

            contentStr = exporter.generateContent(exportBinary, exportText);

            // export the certificate to file system
            if (exportCert) {
                try {
                    exporter.exportCertToFile(contentStr, exportBinary);
                } catch (Exception e) {
                    new VisualException(e);
                }
            }

            // add a header to the key
            keyStr = X509Utils.addKeyHeader(privKey.getEncoded());
            // export the private key to file system
            if (exportKey)
                exporter.exportKeyToFile(keyStr);

            // store the certificate in db
            exporter.storeCertificate(
                    cert.getSerialNumber().longValue(), issuer, subject, contentStr,
                    privKey, keyStr, cert.getNotBefore(), cert.getNotAfter(), true);

            ManagerGUI.refreshX509Table();
        } catch (Exception e) {
            new VisualException(e);
        }
    }


    /**
     * Creates a server certificate
     * can export it to file system
     *
     * @param server The server to create a certificate for
     * @param validFor Number of days the certificate will be valid
     * @throws java.lang.Exception
     */
    public void createServerCert(final Server server, final String validFor) throws Exception {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.servercert.tooltip"));

                createServerCertImmediately(server, validFor);
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();
    }


    /**
     * Creates a server certificate
     * can export it to file system
     *
     * @param server The server to create a certificate for
     * @param validFor Number of days the certificate will be valid
     * @throws java.lang.Exception
     */
    public void createServerCertImmediately(Server server, String validFor) throws Exception {
        try {
            X509 rootX509 = X509Utils.loadRootX509();
            X509Certificate rootCert = (X509Certificate)
                    X509Serializer.getInstance().fromXML(
                            rootX509.getCertSerialized()
                    );
            PrivateKey rootPrivKey = X509Utils.extractRootPrivateKey(rootX509);

            String subject = Configuration.getInstance().X509_SERVER_SUBJECT;
            subject = modifySubject(subject, StringUtils.isEmptyOrWhitespaces(server.getCn()) ? server.getName() : server.getCn(), (server.getOu()==null || server.getOu().equals("")) ? null : server.getOu());
            String validFrom = Constants.PROPERTIES_DATE_FORMAT.format(new Date());
            
            try {
                Integer.parseInt(validFor);
            } catch(Exception e) {
                validFor = null;
            }
            if (validFor == null || StringUtils.isEmptyOrWhitespaces(validFor))
                validFor = Configuration.getInstance().X509_SERVER_VALID_FOR;

            X509Certificate cert = generator.createServerCert(rootCert, rootPrivKey,
                                            Constants.PROPERTIES_DATE_FORMAT.parse(validFrom),
                                            Constants.PROPERTIES_DATE_FORMAT.parse(DateUtils.addDaysToDate(validFrom, validFor)), subject);
            if (cert == null)
                throw new Exception(rb.getString("error.x509.generate.server"));
            
            PrivateKey privKey = generator.getPrivateKey();

            String issuer = PrincipalUtil.getIssuerX509Principal(rootCert).toString();
            String contentStr = null;
            String keyStr = null;

            exportBinary = Boolean.parseBoolean(serverCertBundle.getString("export_binary"));
            exportText = Boolean.parseBoolean(serverCertBundle.getString("export_text"));

            X509Exporter exporter = new X509Exporter(
                    X509.X509_TYPE_SERVER,
                    serverCertBundle.getString("export_cert_file"),
                    serverCertBundle.getString("export_key_file"),
                    Configuration.getInstance().CERT_EXPORT_PATH,
                    cert,
                    null,
                    server.getServerid()
            );

            contentStr = exporter.generateContent(exportBinary, exportText);

            // export the certificate to file system
            if (exportCert) {
                try {
                    exporter.exportCertToFile(contentStr, exportBinary);
                } catch (Exception e) {
                    new VisualException(e);
                }
            }

            keyStr = X509Utils.addKeyHeader(privKey.getEncoded());

            // export the private key to file system
            if (exportKey)
                exporter.exportKeyToFile(keyStr);

            // store the certificate in db
            int x509id = exporter.storeCertificate(
                    cert.getSerialNumber().longValue(), issuer, subject, contentStr,
                    privKey, keyStr, cert.getNotBefore(), cert.getNotAfter(), true);

            server.setX509id(x509id);
            ServerDAO.getInstance().update(server);

            ManagerGUI.refreshX509Table();
            ManagerGUI.refreshServerTable();
        } catch (Exception e) {
            new VisualException(e);
        }
    }


    /**
     * Creates Diffie Hellman parameters
     * Runs in own Thread
     *
     * @throws Exception
     */
    public void createAndExportDHParameters() throws Exception {

        SwingWorker<String, Void> generatorWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                try {
                    t = Thread.currentThread();
                    ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.dhparameters.tooltip"));

                    String content = generator.createDHParameters();
                    X509Exporter.exportDHParameters(content, keyStrength);

                    ManagerGUI.addStatusMessage(new StatusMessage(rb.getString("statusBar.dhparameters.generated"), StatusMessage.TYPE_CONFIRM));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error generating dh parameters", e);
                }
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };

        if (!X509Exporter.isDHParametersExisting(keyStrength))
            generatorWorker.execute();
    }


    /**
     * Creates a client certificate
     * can export it to file system
     *
     * @param u The user to create the certificate for
     * @throws java.lang.Exception
     */
    public void createClientCert(final User u) throws Exception {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.clientcert.tooltip"));

                createClientCertImmediately(u);
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();
    }
    
    
    /**
     * Creates a client certificate
     * can export it to file system
     *
     * @param user The user to create the certificate for
     * @throws java.lang.Exception
     */
    public void createClientCertImmediately(User user) throws Exception {
        String password = null;
        if (Configuration.getInstance().PKCS12_PASSWORD_TYPE
                        == Constants.PKCS12_SINGLE_PASSWORD) {
            // show password input dialog
            while (password == null) {
                String dialogHeadline = rb.getString("dialog.pkcs12password.headline1");
                dialogHeadline += " " + user.getUsername();
                dialogHeadline += " " + rb.getString("dialog.pkcs12password.headline2");
                
                password = Dialogs.showPKCS12PasswordDialog(ManagerGUI.mainFrame, dialogHeadline);

                if (password == null)
                    if (Dialogs.showReallySkipPkcs12Password(ManagerGUI.mainFrame))
                        break;
            }
        }
        
        createClientCertImmediately(user, password);
    }
    
    
    /**
     * Creates a client certificate
     * can export it to file system
     *
     * @param user The user to create the certificate for
     * @param pkcs12Password The PKCS12 password or null
     * @throws java.lang.Exception
     */
    public void createClientCertImmediately(User user, String pkcs12Password) throws Exception {
        createClientCertImmediately(user, pkcs12Password, null);
    }


    /**
     * Creates a client certificate
     * can export it to file system
     *
     * @param user The user to create the certificate for
     * @param pkcs12Password The PKCS12 password or null
     * @param validFor Number of days the certificate will be valid
     * @throws java.lang.Exception
     */
    public void createClientCertImmediately(User user, String pkcs12Password, String validFor) throws Exception {
        try {
            X509 signingX509 = null;
            if (ManagerApp.intermediate) {
                signingX509 = X509Utils.loadIntermediateX509();
            } else {
                signingX509 = X509Utils.loadRootX509();
            }
            X509Certificate signingCert = (X509Certificate)
                    X509Serializer.getInstance().fromXML(
                            signingX509.getCertSerialized()
                    );
            PrivateKey signingPrivKey = X509Utils.extractRootPrivateKey(signingX509);

            String issuer = PrincipalUtil.getSubjectX509Principal(signingCert).toString();
            String subject = Configuration.getInstance().X509_CLIENT_SUBJECT;
            subject = modifySubject(subject, StringUtils.isEmptyOrWhitespaces(user.getCn()) ? user.getUsername() : user.getCn(), (user.getOu()==null || user.getOu().equals("")) ? null : user.getOu());
            String validFrom = Constants.PROPERTIES_DATE_FORMAT.format(new Date());
            
            try {
                Integer.parseInt(validFor);
            } catch(Exception e) {
                validFor = null;
            }
            if (validFor == null || StringUtils.isEmptyOrWhitespaces(validFor))
                validFor = Configuration.getInstance().X509_CLIENT_VALID_FOR;

            X509Certificate cert = generator.createClientCert(
                    signingCert, signingPrivKey,
                    Constants.PROPERTIES_DATE_FORMAT.parse(validFrom),
                    Constants.PROPERTIES_DATE_FORMAT.parse(DateUtils.addDaysToDate(validFrom, validFor)),
                    subject);
            if (cert == null)
                throw new Exception(rb.getString("error.x509.generate.client"));
            
            PrivateKey privKey = generator.getPrivateKey();

            String contentStr = null;
            String keyStr = null;

            exportBinary = Boolean.parseBoolean(clientCertBundle.getString("export_binary"));
            exportText = Boolean.parseBoolean(clientCertBundle.getString("export_text"));

            int type = X509.X509_TYPE_CLIENT;
            if (Configuration.getInstance().CERTIFICATE_TYPE == Constants.CERTIFICATE_TYPE_PKCS12)
                type = X509.X509_TYPE_PKCS12;

            X509Exporter exporter = new X509Exporter(
                    type,
                    clientCertBundle.getString("export_cert_file"),
                    clientCertBundle.getString("export_key_file"),
                    Configuration.getInstance().CERT_EXPORT_PATH,
                    cert,
                    user
            );


            // add a header to the key
            keyStr = X509Utils.addKeyHeader(privKey.getEncoded());
            contentStr = exporter.generateContent(exportBinary, exportText);

            // store the certificate in db
            int x509id = exporter.storeCertificate(
                    cert.getSerialNumber().longValue(), issuer, subject, contentStr,
                    privKey, keyStr, cert.getNotBefore(), cert.getNotAfter(), true);
            // different types of certificates must be exported differently
            if (Configuration.getInstance().CERTIFICATE_TYPE == Constants.CERTIFICATE_TYPE_BASE64) {
                // export the certificate to file system
                if (exportCert) {
                    try {
                        exporter.exportCertToFile(contentStr, exportBinary);
                    } catch (Exception e) {
                        new VisualException(e);
                    }
                }

                // export the private key to file system
                if (exportKey) {
                    try {
                        exporter.exportKeyToFile(keyStr);
                    } catch (Exception e) {
                        new VisualException(e);
                    }
                }
            } else if (Configuration.getInstance().CERTIFICATE_TYPE == Constants.CERTIFICATE_TYPE_PKCS12 && exportCert) {
                try {
                    PKCS12Exporter pkcs12Exporter = new PKCS12Exporter(cert, privKey, signingCert, user, null);
                    String pkcs12Content = pkcs12Exporter.exportPKCS12ToFile(pkcs12Password);
                    pkcs12Exporter.storePKCS12(pkcs12Content, x509id);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error exporting PKCS#12 certificate", e);
                    
                    // store as 'normal' x509 certificate
                    X509 x509 = new X509(x509id);
                    x509 = x509.getX509DAO().read(x509);
                    x509.setType(X509.X509_TYPE_CLIENT);
                    X509DAO.getInstance().update(x509);
                    
                    throw new Exception(rb.getString("error.x509.generate.pkcs12"));
                }
            }

            user.setX509id(x509id);
            UserDAO.getInstance().update(user);

            logger.info("end creating client certificate: " + x509id + "\n");
            ManagerGUI.refreshX509Table();
        } catch (Exception e) {
            new VisualException(e);
        }
    }
    
    
    /**
     * Renews an existing certificate
     * @param x509 The certificate to renew
     * @throws Exception
     */
    public void renewCertificate(final X509 x509) throws Exception {

        SwingWorker<String, Void> renewWorker = new SwingWorker<String, Void>() {
            Thread currentThread;

            protected String doInBackground() throws Exception {

                if ((x509.getType() == X509.X509_TYPE_CLIENT) || (x509.getType() == X509.X509_TYPE_PKCS12)) {
                    currentThread = Thread.currentThread();
                    ThreadMgmt.getInstance().addThread(currentThread, rb.getString("statusBar.clientcert_renew.tooltip"));
                    createClientCertImmediately(UserQueries.getUserByX509id(x509.getX509id()));
                } else if(x509.getType() == X509.X509_TYPE_SERVER) {
                    Server server = ServerQueries.getServerByX509id(x509.getX509id());
                    currentThread = Thread.currentThread();
                    ThreadMgmt.getInstance().addThread(currentThread, rb.getString("statusBar.servercert_renew.tooltip"));
                    createServerCertImmediately(server, x509.validForDays());
                }

                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(currentThread);
                ManagerGUI.addStatusMessage(new StatusMessage(rb.getString("statusBar.cert_renew.msg")));
                try {
                    X509Utils.revokeCertificate(x509);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error at revoking old certificate on renewal", e);
                }
                X509DAO.getInstance().delete(x509);
                ManagerGUI.refreshX509Table();
            }
        };
        
        if (x509 != null) {
            renewWorker.execute();
        } else
            new VisualException(rb.getString("error.x509.generate.no_user"));

    }
    
    
    /**
     * Renews an existing certificate
     * @param x509 The certificate to renew
     */
    public void renewCertificateImmediately(final X509 x509) throws Exception {
        final User user = UserQueries.getUserByX509id(x509.getX509id());
        if (user == null)
            new VisualException(rb.getString("error.x509.generate.no_user"));
        
        if (x509.getType() == X509.X509_TYPE_SERVER) {
            Server server = ServerQueries.getServerByX509id(x509.getX509id());
            createServerCertImmediately(server, x509.validForDays());
        } else
            createClientCertImmediately(user);
        
        ManagerGUI.addStatusMessage(new StatusMessage(rb.getString("statusBar.cert_renew.msg")));
        try {
            X509Utils.revokeCertificate(x509);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error at revoking old certificate on renewal", e);
        }
        X509DAO.getInstance().delete(x509);
        ManagerGUI.refreshX509Table();
    }


    /**
     * Inserts the name and ou into the subject, between 'CN=' and ',E' and for OU
     *
     * @param subject  The subject of the certificate
     * @param username The username
     * @param ou       The users OU
     * @return Returns the modified subject
     */
    private String modifySubject(String subject, String username, String ou) {
        try {
            DNUtil dnUtil = new DNUtil();
            dnUtil.split(subject);
            dnUtil.setCn(username);
            if (ou != null)
                dnUtil.setOu(ou);
            String newDN = dnUtil.merge();
            return newDN;
        } catch (Exception e) {
            logger.warning("unable to modify client subject String, 'cn=' not found: " + subject);
            return subject;
        }
    }


    /**
     * creates a Certificate Revocation List
     */
    public void createCRL() {
        SwingWorker<String, Void> generateWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.crl.tooltip"));

                createCRLImmediately();
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);
            }
        };
        generateWorker.execute();
    }


    /**
     * creates a Certificate Revocation List
     */
    public void createCRLImmediately() {
        try {
            // load root certificate
            X509 rootX509 = X509Utils.loadRootX509();
            if (rootX509 == null) {
                new VisualException(rb.getString("error.general.text"));
                return;
            }
            X509Certificate rootCert = (X509Certificate)
                    X509Serializer.getInstance().fromXML(
                            rootX509.getCertSerialized()
                    );
            // extract the private key from the certificate
            PrivateKey rootPrivKey = X509Utils.extractRootPrivateKey(rootX509);
            String issuer = PrincipalUtil.getSubjectX509Principal(rootCert).toString();

            Date now = new Date();
            Calendar nowCal = Calendar.getInstance();
            nowCal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(Configuration.getInstance().X509_CLIENT_VALID_FOR));
            Date nextUpdate = nowCal.getTime();

            String createDateStr = Constants.formatDetailedFormat(now);
            String nextUpdateStr = Constants.formatDetailedFormat(nextUpdate);

            int crlNumber = CRLQueries.getNextCrlNumber();
            // certificates to revoke
            Vector<String> revocationSerials = CRLQueries.getRevocationSerials();

            // generate
            X509CRL x509crl = generator.createCRLImmediately(rootCert, rootPrivKey, crlNumber, revocationSerials, Integer.parseInt(Configuration.getInstance().X509_CLIENT_VALID_FOR));

            // export
            String contentStr = CRLExporter.exportCRLToFile(x509crl);

            // create crl object
            CRLExporter.storeCRL(crlNumber, contentStr, x509crl, createDateStr, nextUpdateStr, issuer);

            ManagerGUI.addStatusMessage(new StatusMessage(rb.getString("statusBar.crl.update")));
        } catch (Exception e) {
            new VisualException(e);
        }
    }


    public static void main(String[] args) {

        try {
            Security.addProvider(new BouncyCastleProvider());

            logger.info("main: starting to generate certificates");

            X509Generator gen = new X509Generator();
            gen.createRootCert();

            //X509Certificate rootCert = X509Utils.loadRootCertificate();
            //gen.createIntermediateCertImmediately(rootCert);

            logger.info("main: end generating certificates");


            //gen.createServerCertTEST();
            //gen.createServerCert(X509Generator.loadRootCertificate());
            //gen.createServerCert(rootCert);

            gen.createCRLImmediately();

            /*
            String subject = "C=DE,ST=NDS,L=Oldenburg,O=example,CN=exampleCA,E=root@example.org";
            String subjectNew = gen.modifySubjectCN(subject, "Peter Meier");
            System.out.println(subjectNew);
            */
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
    }

}
