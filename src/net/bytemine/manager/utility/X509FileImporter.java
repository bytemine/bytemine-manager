/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.crypto.utility.PrincipalUtil;
import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.openvpn.UserImport;
import net.bytemine.utility.FileUtils;
import net.bytemine.utility.StringUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * methods for importing X509 certificates and keys from file system
 *
 * @author Daniel Rauer
 */
public class X509FileImporter {

    private static Logger logger = Logger.getLogger(X509FileImporter.class.getName());
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private String rootCertName = "ca.crt";
    private String rootKeyName = "ca.key";
    private String serverCertName = "server.crt";
    private String serverKeyName = "server.key";
    private String clientCertExtension = "crt";
    private String clientKeyExtension = "key";
    private String currentCertFilename;
    private String currentKeyFilename;

    private String path;
    private Vector<String> certExtensions = new Vector<String>();
    private Vector<String> importedX509ids = new Vector<String>();

    private File selectedFile;
    private boolean certExisting = false;
    private boolean keyExisting = false;
    private boolean exportRootKey = false;


    public X509FileImporter(File selectedFile) throws Exception {
        this.selectedFile = selectedFile;

        String pathAndFilename = this.selectedFile.getPath();
        if (!this.selectedFile.isDirectory()) {
            if (pathAndFilename.indexOf(File.separator) > -1)
                this.path = pathAndFilename.substring(0, pathAndFilename.lastIndexOf(File.separator));
            else
                throw new Exception(rb.getString("error.importCert.noDirFound"));
        } else {
            this.path = this.selectedFile.getPath();
        }
        currentCertFilename = this.selectedFile.getName();
        initBundles();
    }


    /**
     * initializes the ResourceBundles
     * sets cert and key names
     * detects valid extensions
     */
    private void initBundles() {
        try {
            ResourceBundle rootCertBundle = ResourceBundle.getBundle(Constants.ROOT_BUNDLE_NAME);
            ResourceBundle serverCertBundle = ResourceBundle.getBundle(Constants.SERVER_BUNDLE_NAME);
            ResourceBundle clientCertBundle = ResourceBundle.getBundle(Constants.CLIENT_BUNDLE_NAME);

            rootCertName = rootCertBundle.getString("export_cert_file");
            rootKeyName = rootCertBundle.getString("export_key_file");
            try {
                exportRootKey = Boolean.parseBoolean(rootCertBundle.getString("export_key"));
            } catch (Exception e) {
                logger.warning("export_key in root_cert.properties could not be read as boolean");
            }
            serverCertName = serverCertBundle.getString("export_cert_file");
            serverKeyName = serverCertBundle.getString("export_key_file");
            clientCertExtension = FileUtils.getExtension(clientCertBundle.getString("export_cert_file"));
            clientKeyExtension = FileUtils.getExtension(clientCertBundle.getString("export_key_file"));

            certExtensions.add(FileUtils.getExtension(rootCertName));
            certExtensions.add(FileUtils.getExtension(serverCertName));
            certExtensions.add(clientCertExtension);
            certExtensions.add(Constants.DEFAULT_PKCS12_EXTENSION);
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: cert.properties could not be read", mre);
        }
    }


    /**
     * Detects the type (root, server, client, pkcs12) by the filename
     * Also sets the corresponding key or certificate filename
     *
     * @param filename The filename of the currently reading file
     * @return The type as int
     */
    private int detectType(String filename) {
        if (rootCertName.equals(filename)) {
            currentCertFilename = filename;
            currentKeyFilename =
                    FileUtils.removeExtension(filename) +
                            "." +
                            FileUtils.getExtension(rootKeyName);

            return X509.X509_TYPE_ROOT;
        } else if (rootKeyName.equals(filename)) {
            currentKeyFilename = filename;
            currentCertFilename =
                    FileUtils.removeExtension(filename) +
                            "." +
                            FileUtils.getExtension(rootCertName);
            return X509.X509_TYPE_ROOT;
        } else if (serverCertName.equals(filename)) {
            currentCertFilename = filename;
            currentKeyFilename =
                    FileUtils.removeExtension(filename) +
                            "." +
                            FileUtils.getExtension(serverKeyName);
            return X509.X509_TYPE_SERVER;
        } else if (serverKeyName.equals(filename)) {
            currentKeyFilename = filename;
            currentCertFilename =
                    FileUtils.removeExtension(filename) +
                            "." +
                            FileUtils.getExtension(serverCertName);
            return X509.X509_TYPE_SERVER;
        } else {
            String extension = FileUtils.getExtension(filename);
            if (clientCertExtension.equals(extension)) {
                currentCertFilename = filename;
                currentKeyFilename =
                        FileUtils.removeExtension(filename) +
                                "." +
                                clientKeyExtension;
                return X509.X509_TYPE_CLIENT;
            } else if (clientKeyExtension.equals(extension)) {
                currentKeyFilename = filename;
                currentCertFilename =
                        FileUtils.removeExtension(filename) +
                                "." +
                                clientCertExtension;
                return X509.X509_TYPE_CLIENT;
            } else if (Constants.DEFAULT_PKCS12_EXTENSION.equals(extension)) {
                currentKeyFilename = "";
                currentCertFilename = filename;
                return X509.X509_TYPE_PKCS12;
            } else
                return -1;
        }
    }


    /**
     * Detects by its extension if the file is a certificate file
     *
     * @param filename The filename
     * @return true or false
     */
    private boolean fileIsCertificate(String filename) {
        String ext = FileUtils.getExtension(filename);
        if (certExtensions.contains(ext))
            return true;
        else
            return false;
    }


    /**
     * Does all the import jobs for importing client certs and keys
     *
     * @return A vector with all imported x509 ids
     * @throws java.lang.Exception
     */
    public Vector<String> importClientCertsAndKeys(boolean createUsersFromCN) throws Exception {

        if (selectedFile.isDirectory()) {
            if (!importRootCertAndKey()) {
                boolean isRootExisting = Configuration.getInstance().isRootCertExisting();
                if (!isRootExisting) {
                    X509Generator gen = new X509Generator();
                    try {
                        gen.createRootCertImmediately();
                        UserImport.incGeneratedCerts();
                    } catch (Exception ex) {
                        new VisualException(ex);
                    }
                }
            }


            String[] filenames = selectedFile.list();
            for (String filename : filenames) {

                int type = detectType(filename);

                if (type != -1 && fileIsCertificate(filename)) {
                    boolean isRootExisting = Configuration.getInstance().isRootCertExisting();
                    if (type != X509.X509_TYPE_ROOT || !isRootExisting) {
                        int x509id = importCertAndKey(type, createUsersFromCN);
                        if (x509id != -1) {
                            importedX509ids.add(x509id + "");
                        }
                    }
                } else
                    logger.info("File " + filename + " is not recognized as certificate");
            }

            boolean isRootExisting = Configuration.getInstance().isRootCertExisting();
            if (!isRootExisting) {
                X509Generator gen = new X509Generator();
                try {
                    gen.createRootCertImmediately();
                    UserImport.incGeneratedCerts();
                } catch (Exception ex) {
                    new VisualException(ex);
                }
            }

        } else {
            throw new Exception(rb.getString("error.importCert.noDirSpecified"));
        }

        return importedX509ids;

    }


    /**
     * Does all the import jobs for importing root cert and key
     *
     * @throws java.lang.Exception
     */
    public boolean importRootCertAndKey() throws Exception {
        if (searchRootCert()) {
            currentCertFilename = rootCertName;
            currentKeyFilename = rootKeyName;

            importCertAndKey(X509.X509_TYPE_ROOT, false);
            return true;
        }
        return false;
    }


    /**
     * Reads and imports certificate and key files
     *
     * @param type The X509.type
     * @return Int with the ID of the new X509 entry
     * @throws java.lang.Exception
     */
    private int importCertAndKey(int type, boolean createUsersFromCN) throws Exception {
        String content = null;
        String keyContent = null;
        String password = "";
        X509Certificate cert = null;
        PrivateKey privKey = null;
        if (type == X509.X509_TYPE_PKCS12) {
            String filename = this.path + File.separator + currentCertFilename;
            content = readCertificatePKCS12();

            String pwd = "";
            String dialogHeadline = rb.getString("dialog.pkcs12password.headline1");
            dialogHeadline += " " + filename;
            dialogHeadline += " " + rb.getString("dialog.pkcs12password.headline2");
            Vector<Object> pkcs12 = null;

            // maximum 3 tries to enter the correct password
            // return -1 on cancel
            int counter = 0;
            while (counter < 3) {
                if (!Configuration.getInstance().TEST_PKCS12_PASSWORD_DISABLED) {
                    pwd = Dialogs.showPKCS12PasswordDialog(ManagerGUI.mainFrame, dialogHeadline);
                }
                if (pwd == null)
                    return -1;
                try {
                    pkcs12 = X509Utils.regainX509CertificateFromPKCS12(content, filename, pwd);
                    break;
                } catch (Exception e) {
                    dialogHeadline = rb.getString("dialog.pkcs12password.headline1.fail");
                    dialogHeadline += " " + filename;
                    dialogHeadline += " " + rb.getString("dialog.pkcs12password.headline2.fail");
                }
                counter++;
            }

            if (pkcs12 != null) {
                cert = (X509Certificate) pkcs12.get(0);
                privKey = (PrivateKey) pkcs12.get(1);
                keyContent = StringUtils.bytes2String(privKey.getEncoded());
                password = (String) pkcs12.get(2);
            }
        } else {
            content = readCertificate();
            cert = X509Utils.regainX509Certificate(content);
            keyContent = readKey();
            privKey = X509Utils.regainPrivateKey(keyContent);
        }


        String issuer = "";
        String subject = "";
        Date validFrom = null;
        Date validTo = null;
        int x509id = -1;
        User user = null;

        if (cert != null) {
            issuer = cert.getIssuerDN().toString();
            subject = cert.getSubjectDN().toString();

            String username = X509Utils.getCnFromSubject(subject);
            int userId = UserQueries.getUserId(username);

            // load user
            if (userId != -1) {
                user = new User(userId + "");
                user = UserDAO.getInstance().read(user);
            }

            // create user 
            if (createUsersFromCN && user == null && (type == X509.X509_TYPE_PKCS12 || type == X509.X509_TYPE_CLIENT)) {
                user = new User(username, null, -1);
                UserImport.incGeneratedUsers();
            }

            validFrom = cert.getNotBefore();
            validTo = cert.getNotAfter();

            String keyWithHeader = "";

            if (privKey != null && !"".equals(privKey))
                keyWithHeader = X509Utils.addKeyHeader(privKey.getEncoded());

            String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

            X509Exporter exporter = new X509Exporter(type, currentCertFilename, currentKeyFilename, exportPath, cert, user);
            // is certificate already existing
            x509id = X509Queries.getCertificateBySubject(subject, type);
            if (x509id == -1)
                UserImport.incImportedCerts();

            x509id = exporter.storeCertificate(cert.getSerialNumber().longValue(), issuer, subject, content, privKey, keyContent, validFrom, validTo, false, x509id);

            String contentStr = exporter.generateContent(false, false);


            // different types of certificates must be exported differently
            if (type == X509.X509_TYPE_PKCS12) {
                try {
                    X509Certificate rootCert = X509Utils.loadRootCertificate();
                    PKCS12Exporter pkcs12Exporter = new PKCS12Exporter(cert, privKey, rootCert, user, username);
                    String pkcs12Content = pkcs12Exporter.exportPKCS12ToFile(password);
                    pkcs12Exporter.storePKCS12(pkcs12Content, x509id);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "pkcs12 certificate could not be imported", e);
                    return -1;
                }
            } else {
                exporter.exportCertToFile(contentStr, false);
                if (type != X509.X509_TYPE_ROOT || exportRootKey)
                    // do not export empty keys
                    if (keyWithHeader != null && !"".equals(keyWithHeader))
                        exporter.exportKeyToFile(keyWithHeader);
            }

            if (type == X509.X509_TYPE_ROOT) {
                // create revocation list
                X509Generator gen = new X509Generator();
                gen.createCRL();

                // extract key strength
                try {
                    int keyStrength = X509Utils.regainKeyStrengthFromPublicKey(cert.getPublicKey());
                    Configuration.getInstance().setX509KeyStrength(keyStrength + "");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error regaining key strength from root certificate", e);
                }
                
                // set root details as default certificate settings
                Configuration.getInstance().setX509RootSubject(
                        PrincipalUtil.getIssuerX509Principal(cert).toString());
                Configuration.getInstance().setX509RootValidFrom(
                        Constants.PROPERTIES_DATE_FORMAT.format(
                                cert.getNotBefore()));
                Configuration.getInstance().setX509RootValidTo(
                        Constants.PROPERTIES_DATE_FORMAT.format(
                                cert.getNotAfter()));
            }

            if (user != null && user.getUserid() != -1) {
                user.setX509id(x509id);
                UserDAO.getInstance().update(user);
            } else {
                UserImport.incNotLinkedCerts();
            }
        }

        return x509id;
    }


    /**
     * Reads a certificate from the given file
     *
     * @return String with the content of the crt file
     * @throws java.lang.Exception
     */
    public String readCertificate() throws Exception {
        try {
            logger.info("Reading certificate: " + this.path + File.separator + currentCertFilename);
            File certFile = new File(this.path + File.separator + currentCertFilename);

            byte[] encodedCert = new byte[(int) certFile.length()];
            FileInputStream keyInputStream = new FileInputStream(certFile);
            keyInputStream.read(encodedCert);
            keyInputStream.close();

            String certContent = StringUtils.bytes2String(encodedCert);
            return certContent;
        } catch (Exception e) {
            String errorMessage = rb.getString("error.importCert.readCert");
            logger.log(Level.SEVERE, errorMessage, e);
            throw new Exception(errorMessage);
        }
    }


    /**
     * Reads a certificate from the given file
     *
     * @return String with the content of the crt file
     * @throws java.lang.Exception
     */
    public String readCertificatePKCS12() throws Exception {
        try {
            String filename = this.path + File.separator + currentCertFilename;
            logger.info("Reading certificate: " + filename);
            File certFile = new File(filename);

            String certContent = FileUtils.readFilePKCS12(certFile);
            return certContent;
        } catch (Exception e) {
            String errorMessage = rb.getString("error.importCert.readCert");
            logger.log(Level.SEVERE, errorMessage, e);
            throw new Exception(errorMessage);
        }
    }


    /**
     * Reads a PrivateKey from the given file
     *
     * @return String with the content of the key file
     * @throws java.lang.Exception
     */
    private String readKey() throws Exception {
        try {
            logger.info("Reading key: " + this.path + File.separator + currentKeyFilename);
            File keyFile = new File(this.path + File.separator + currentKeyFilename);

            byte[] encodedKey = new byte[(int) keyFile.length()];
            FileInputStream keyInputStream = new FileInputStream(keyFile);
            keyInputStream.read(encodedKey);
            keyInputStream.close();

            String keyContent = StringUtils.bytes2String(encodedKey);

            return keyContent;
        } catch (Exception e) {
            String errorMessage = "Key " + this.path + File.separator + currentKeyFilename + " could not be found";
            logger.warning(errorMessage);
            return "";
        }
    }


    /**
     * Searches for ca.crt and ca.key in the directory
     * or only for the key, if crt-file is specified
     *
     * @throws java.lang.Exception
     */
    private boolean searchRootCert() throws Exception {
        // a directory was selected
        if (selectedFile.isDirectory()) {
            String[] filenames = selectedFile.list();
            for (String filename : filenames) {
                if (filename.equals(rootCertName))
                    certExisting = true;
                else if (filename.equals(rootKeyName))
                    keyExisting = true;
            }
        }
        // the certificate file was selected
        else {
            certExisting = true;

            // is key also existing            
            String filename = selectedFile.getName();
            rootCertName = filename;
            filename = filename.substring(0, filename.lastIndexOf("."));
            filename = filename + "." + Constants.DEFAULT_KEY_EXTENSION;

            File keyFile = new File(path + File.separator + filename);
            rootKeyName = filename;
            if (keyFile.exists())
                keyExisting = true;
            else
                throw new Exception(rb.getString("error.importCert.keyNotFound"));

        }
        if (certExisting && keyExisting)
            return true;
        else
            return false;
    }


    public static void main(String[] args) {
        try {

            Security.addProvider(new BouncyCastleProvider());

            String searchDirString = "ca.crt";

            X509FileImporter importer = new X509FileImporter(new File(searchDirString));
            importer.searchRootCert();

            String content = importer.readCertificate();
            X509Certificate cert = X509Utils.regainX509Certificate(content);
            if (cert != null) {
                String issuer = cert.getIssuerDN().getName();
                String subject = cert.getSubjectDN().getName();
                logger.info("Subject: " + subject);
                logger.info("Issuer: " + issuer);

                logger.info("key strength: " + X509Utils.regainKeyStrengthFromPublicKey(cert.getPublicKey()));

            }

            importer.currentKeyFilename = importer.rootKeyName;
            String keyContent = importer.readKey();
            PrivateKey privKey = X509Utils.regainPrivateKey(keyContent);
            privKey.getAlgorithm();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error reading cert and key file", e);
        }
    }


}
