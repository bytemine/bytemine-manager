/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.security.cert.X509Certificate;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.FileUtils;
import net.bytemine.utility.HexUtils;
import net.bytemine.utility.StringUtils;


/**
 * methods for exporting X509 certificates and keys
 *
 * @author Daniel Rauer
 */
public class X509Exporter {

    private static Logger logger = Logger.getLogger(X509Exporter.class.getName());

    private int type;
    private String seqNumber;
    private String newCertFilename;
    private String newKeyFilename;
    private String path;
    private X509Certificate certificate;
    private boolean exportBinary = false;

    
    X509Exporter(int type, String certFilename, String keyFilename, String path, X509Certificate cert, User user)
            throws Exception {
        this(type, certFilename, keyFilename, path, cert, user, -1);
    }

    public X509Exporter(int type, String certFilename, String keyFilename, String path, X509Certificate cert, User user, int serverid)
            throws Exception {

        this.type = type;
        this.certificate = cert;
        
        if (!path.endsWith(File.separator))
            path = path + File.separator;
        this.path = path;

        this.newCertFilename = certFilename;
        this.newKeyFilename = keyFilename;

        // add sequential number
        switch (this.type) {
            case X509.X509_TYPE_CLIENT:
                if (user != null) {
                    this.path = this.path + user.getUsername() + File.separator;
                    generateNewClientFilenames(user);
                } else {
                    this.path = this.path + File.separator + Constants.EXPORT_UNASSIGNED_DIRECTORY;
                    this.seqNumber = X509Queries.retrieveCurrentSeqNumber(this.type);
                    this.newCertFilename = generateNewFilename(certFilename);
                    this.newKeyFilename = generateNewFilename(keyFilename);
                    logger.info("storing client certificate without user: " + this.path + File.separator + newCertFilename);
                    logger.info("storing client key without user: " + this.path + File.separator + newKeyFilename);
                }
                break;
            case X509.X509_TYPE_PKCS12:
                if (user != null) {
                    this.path = this.path + user.getUsername() + File.separator;
                    generateNewPKCS12ClientFilenames(user);
                } else {
                    this.path = this.path + File.separator + Constants.EXPORT_UNASSIGNED_DIRECTORY;
                    this.seqNumber = X509Queries.retrieveCurrentSeqNumber(this.type);
                    this.newCertFilename = generateNewFilename(certFilename);
                    this.newKeyFilename = "";
                    logger.info("storing PKCS#12 client certificate without user: " + this.path + File.separator + newCertFilename);
                    logger.info("storing PKCS#12 client key without user: " + this.path + File.separator + newKeyFilename);
                }
                break;
            case X509.X509_TYPE_SERVER:
                this.seqNumber = serverid + "";
                this.path = this.path + "_" + Server.getServerById(serverid).getName() + File.separator;
                String prefix = certFilename;
                if (prefix.contains("_"))
                    prefix = prefix.substring(0, prefix.indexOf("_"));
                if (prefix.contains("."))
                    prefix = prefix.substring(0, prefix.indexOf("."));
                this.newCertFilename = generateNewFilename(certFilename, prefix + "_");
                this.newKeyFilename = generateNewFilename(keyFilename, prefix + "_");
                break;
        }
        preparePath(this.path);

        String bundleName = "";
        try {
            switch (type) {
                case X509.X509_TYPE_ROOT:
                    bundleName = Constants.ROOT_BUNDLE_NAME;
                    break;
                case X509.X509_TYPE_INTERMEDIATE:
                    bundleName = Constants.INTERMEDIATE_BUNDLE_NAME;
                    break;
                case X509.X509_TYPE_SERVER:
                    bundleName = Constants.SERVER_BUNDLE_NAME;
                    break;
                case X509.X509_TYPE_CLIENT:
                    bundleName = Constants.CLIENT_BUNDLE_NAME;
                    break;
                case X509.X509_TYPE_PKCS12:
                    bundleName = Constants.CLIENT_BUNDLE_NAME;
                    break;
                default:
                    bundleName = Constants.CLIENT_BUNDLE_NAME;
                    break;
            }
            ResourceBundle certBundle = ResourceBundle.getBundle(bundleName);

            String exportBinaryStr = certBundle.getString("export_binary");
            try {
                this.exportBinary = Boolean.parseBoolean(exportBinaryStr);
            } catch (Exception e) {
                logger.warning("Invalid option export_text or export_binary in " + bundleName + ".properties");
            }
        } catch (MissingResourceException mre) {
            logger.log(Level.SEVERE, "Error: " + bundleName + ".properties could not be read", mre);
        }
    }


    /**
     * Creates necessary directories for exporting
     *
     * @param path The path to prepare
     * @throws Exception
     */
    private void preparePath(String path) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        this.path = FileUtils.unifyPath(path);

        if (!new File(this.path).exists()) {
            boolean success = (new File(this.path)).mkdirs();
            if (!success)
                throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }


    /**
     * stores a new certificate into the db
     *
     * @param currentTime the time of creation
     * @param issuer      The issuer
     * @param subject     The subject
     * @param content     The certificate as written to file
     * @param privKey     The private key
     * @param keyContent  The key as written to file
     * @param validFrom   The date the certificate is valid from
     * @param validTo     The date the certificate is valid to
     * @param generated   true, if the certificate is generated, false if it is imported
     * @return the x509id
     * @throws java.security.cert.CertificateEncodingException
     *
     */
    int storeCertificate(
            long currentTime, String issuer, String subject,
            String content, PrivateKey privKey, String keyContent,
            Date validFrom, Date validTo, boolean generated) {

        return storeCertificate(
                Constants.DEFAULT_X509_VERSION, currentTime, issuer, subject,
                content, privKey, keyContent, validFrom, validTo, generated
        );
    }


    /**
     * stores a new certificate into the db
     *
     * @param currentTime the time of creation
     * @param issuer      The issuer
     * @param subject     The subject
     * @param content     The certificate as written to file
     * @param privKey     The private key
     * @param keyContent  The key as written to file
     * @param validFrom   The date the certificate is valid from
     * @param validTo     The date the certificate is valid to
     * @param generated   true, if the certificate is generated, false if it is imported
     * @return the x509id
     * @throws java.security.cert.CertificateEncodingException
     *
     */
    int storeCertificate(
            long currentTime, String issuer, String subject,
            String content, PrivateKey privKey, String keyContent,
            Date validFrom, Date validTo, boolean generated, int x509id) {

        return storeCertificate(
                Constants.DEFAULT_X509_VERSION, currentTime, issuer, subject,
                content, privKey, keyContent, validFrom, validTo, generated, x509id
        );
    }


    /**
     * stores a new certificate into the db
     *
     * @param version     The version of the certificate
     * @param currentTime the time of creation
     * @param issuer      The issuer
     * @param subject     The subject
     * @param content     The certificate as written to file
     * @param privKey     The private key
     * @param keyContent  The key as written to file
     * @param validFrom   The date the certificate is valid from
     * @param validTo     The date the certificate is valid to
     * @param generated   true, if the certificate is generated, false if it is imported
     * @return the x509id
     * @throws java.security.cert.CertificateEncodingException
     *
     */
    private int storeCertificate(
            int version, long currentTime, String issuer, String subject,
            String content, PrivateKey privKey, String keyContent,
            Date validFrom, Date validTo, boolean generated) {
        return storeCertificate(
                version, currentTime, issuer, subject, content, privKey, keyContent,
                validFrom, validTo, generated, -1);
    }


    /**
     * stores a new certificate into the db
     *
     * @param version     The version of the certificate
     * @param currentTime the time of creation
     * @param issuer      The issuer
     * @param subject     The subject
     * @param content     The certificate as written to file
     * @param privKey     The private key
     * @param keyContent  The key as written to file
     * @param validFrom   The date the certificate is valid from
     * @param validTo     The date the certificate is valid to
     * @param generated   true, if the certificate is generated, false if it is imported
     * @return the x509id
     * @throws java.security.cert.CertificateEncodingException
     *
     */
    private int storeCertificate(
            int version, long currentTime, String issuer, String subject,
            String content, PrivateKey privKey, String keyContent,
            Date validFrom, Date validTo, boolean generated, int x509id) {


        Date createDate = new Date(currentTime);
        String createDateStr = Constants.formatDetailedFormat(createDate);
        String validFromStr = Constants.formatDetailedFormat(validFrom);
        String validToStr = Constants.formatDetailedFormat(validTo);

        String certSerialized = X509Serializer.getInstance().toXML(certificate);
        String key = "";
        if (privKey != null)
            key = X509Serializer.getInstance().toXML(privKey);

        issuer = issuer.replaceAll(", ", ",");
        subject = subject.replaceAll(", ", ",");

        X509 x509;
        if (x509id == -1) {
            // create new x509 and store it to db
            x509 = new X509(String.valueOf(currentTime));
        } else {
            // update the x509
            x509 = new X509(x509id);
            x509 = X509DAO.getInstance().read(x509);
        }

        x509.setVersion(String.valueOf(version));
        x509.setFileName(newCertFilename);
        x509.setPath(path);
        x509.setIssuer(issuer);
        x509.setSubject(subject);
        x509.setContent(content);
        x509.setContentDisplay(certificate.toString());
        x509.setCertSerialized(certSerialized);
        x509.setKey(key);
        x509.setKeyContent(keyContent);
        x509.setType(type);
        x509.setCreateDate(createDateStr);
        x509.setValidFrom(validFromStr);
        x509.setValidTo(validToStr);
        x509.setGenerated(generated);

        // update 
        x509.update();

        return x509.getX509id();
    }


    /**
     * Exports the certificate to a file
     *
     * @param content to write to file
     */
    public void exportCertToFile(String content) throws Exception {
        exportCertToFile(content, exportBinary);
    }


    /**
     * Exports the certificate to a file
     *
     * @param content      The content to export
     * @param exportBinary binary certificate will be exported
     *                     as binary(true) or
     *                     as base64-text(false)
     */
    public void exportCertToFile(String content, boolean exportBinary) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        try {
            FileWriter fw = new FileWriter(this.path + newCertFilename);

            if (exportBinary) {
                // write in binary form
                String encoded = StringUtils.bytes2String(certificate.getEncoded());
                fw.write(encoded);
            } else {
                // write to file
                fw.write(content);
            }
            fw.close();
        } catch (IOException | CertificateEncodingException e) {
            logger.log(Level.SEVERE, "Error writing key to filesystem", e);
            throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }

    
    /**
     * Generate the certificates content
     * @param exportBinary binary certificate will be exported
     *                     as binary(true) or
     *                     as base64-text(false)
     * @param exportText export certificate data as human readable text
     * @return The certificates content
     * @throws Exception
     */
    String generateContent(boolean exportBinary, boolean exportText) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        StringBuilder contentBuffer = new StringBuilder();
        try {
            if (exportText)
                contentBuffer.append(certificate.toString());

            String content = X509Utils.addCertificateHeader(certificate.getEncoded());
            contentBuffer.append(content);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing key to filesystem", e);
            throw new Exception(rb.getString("dialog.cert.exporterror"));
        }

        return contentBuffer.toString();
    }


    /**
     * Exports the given certificate to a file
     *
     * @param x509 The x509 object to export
     * @param path The path to export to
     */
    public static void exportCertToFile(X509 x509, String path) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        try {
            FileWriter fw = new FileWriter(path + "/" + x509.getFileName());

            // write to file
            fw.write(x509.getContent());
            fw.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing certificate to filesystem: " +
                    path + "/" + x509.getFileName(), e);
            throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }


    /**
     * Checks whether the dh.pem exists or not
     *
     * @param keyStrength The keyStrength to test for
     * @return true, if the file is existing. false if not
     */
    static boolean isDHParametersExisting(int keyStrength) {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        String filename = "dh" + keyStrength + ".pem";
        File dhFile = new File(exportPath + "/" + filename);
        return dhFile.exists();

    }


    /**
     * Exports Diffie Hellman parameters to file system
     *
     * @param content     The DH parameters content
     * @param keyStrength The keyStrength
     * @throws Exception
     */
    static void exportDHParameters(String content, int keyStrength) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        String filename = "dh" + keyStrength + ".pem";
        try {
            FileWriter fw = new FileWriter(exportPath + "/" + filename);
            fw.write(content);
            fw.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing certificate to filesystem: " +
                    exportPath + "/" + filename, e);
            throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }


    /**
     * Exports the private key to a file
     *
     * @param content the key as String to export
     */
    public void exportKeyToFile(String content) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        try {
            FileWriter fw = new FileWriter(this.path + this.newKeyFilename);
            fw.write(content);
            fw.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing key to filesystem", e);
            throw new Exception(rb.getString("dialog.key.exporterror"));
        }
    }


    /**
     * Generates the client certificate and key name
     *
     * @param user The user
     */
    private void generateNewClientFilenames(User user) {
        String userId = user.getUserid() + "";
        String prefix = user.getUsername() + "_" + userId.hashCode() + ".";
        this.newCertFilename = prefix + Constants.DEFAULT_CERT_EXTENSION;
        this.newKeyFilename = prefix + Constants.DEFAULT_KEY_EXTENSION;

    }


    /**
     * Generates the client certificate name for pkcs#12 certificates
     *
     * @param user The user
     */
    private void generateNewPKCS12ClientFilenames(User user) {
        String userId = user.getUserid() + "";
        String prefix = user.getUsername() + "_" + userId.hashCode() + ".";
        this.newCertFilename = prefix + Constants.DEFAULT_PKCS12_EXTENSION;
        this.newKeyFilename = "";

    }


    /**
     * Adds an incremented sequential number to the filename
     *
     * @param filename the filename
     * @return The modified filename
     */
    private String generateNewFilename(String filename) {
        return generateNewFilename(filename, "");
    }


    /**
     * Adds an incremented sequential number to the filename
     *
     * @param filename the filename
     * @param prefix   The prefix to add to the filename
     * @return The modified filename
     */
    private String generateNewFilename(String filename, String prefix) {
        try {
            String nextSeqNuber = HexUtils.incrementHex(seqNumber);
            nextSeqNuber = StringUtils.fillLeadingZeros(nextSeqNuber);
            return addSeqNumber(filename, nextSeqNuber, prefix);
        } catch (Exception e) {
            logger.log(Level.WARNING, "error while incrementing sequential number. using timestamp to unify filename", e);
            return addSeqNumber(filename, System.currentTimeMillis() + "");
        }
    }


    /**
     * Adds the given sequential number to the filename
     * or a timestamp if an error occurs
     *
     * @param fileName  The filename to modify
     * @param seqNumber The sequential number to add
     * @return the new filename
     */
    private static String addSeqNumber(String fileName, String seqNumber) {
        return addSeqNumber(fileName, seqNumber, "");
    }


    /**
     * Adds the given sequential number to the filename
     * or a timestamp if an error occurs
     *
     * @param fileName  The filename to modify
     * @param seqNumber The sequential number to add
     * @param prefix    The prefix to ad to the filename
     * @return the new filename
     */
    private static String addSeqNumber(String fileName, String seqNumber, String prefix) {
        try {
            String extension = FileUtils.getExtension(fileName);
            if (prefix == null)
                prefix = "";
            fileName = prefix + seqNumber + "." + extension;
        } catch (Exception e) {
            logger.warning(
                    "error while adding sequential number to filename: " + fileName + ". " +
                            "using timestamp to unify the filename");
            fileName = System.currentTimeMillis() + "-" + fileName;
        }

        return fileName;
    }


    public String getPath() {
        return path;
    }

}
