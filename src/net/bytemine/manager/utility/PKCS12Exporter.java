/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.PKCS12;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.db.PKCS12DAO;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.FileUtils;

/**
 * methods for exporting PKCS#12 certificates
 *
 * @author Daniel Rauer
 */
public class PKCS12Exporter {

    private static Logger logger = Logger.getLogger(PKCS12Exporter.class.getName());

    private Certificate rootCertificate;
    private Certificate clientCertificate;
    private PrivateKey clientKey;
    private String friendlyName;
    private String userId;
    private String exportPath;
    private String password;

    
    public PKCS12Exporter(X509Certificate clientCertificate, PrivateKey clientKey,
                          X509Certificate rootCertificate, User user, String username)
            throws Exception {
        this.clientCertificate = clientCertificate;
        this.clientKey = clientKey;
        this.rootCertificate = rootCertificate;
        if (user != null) {
            this.friendlyName = user.getUsername();
            this.userId = user.getUserid() + "";
        } else {
            if (username == null) {
                String subject = clientCertificate.getSubjectDN().toString();
                this.friendlyName = X509Utils.getCnFromSubject(subject);
            } else
                this.friendlyName = username;
            this.userId = System.currentTimeMillis() + "";
        }

        this.exportPath = Configuration.getInstance().CERT_EXPORT_PATH;
        if (user != null)
            this.exportPath = this.exportPath + "/" + user.getUsername();
        preparePath(this.exportPath);
    }
    
    public PKCS12Exporter(X509Certificate clientCertificate,
            PrivateKey clientKey, X509Certificate rootCertificate, Server server) throws Exception {
        this.clientCertificate = clientCertificate;
        this.clientKey = clientKey;
        this.rootCertificate = rootCertificate;
        if (server != null) {
            this.friendlyName = server.getHostname();
            this.userId = server.getServerid() + "";
        } else {
            String subject = clientCertificate.getSubjectDN().toString();
            this.friendlyName = X509Utils.getCnFromSubject(subject);
            this.userId = "";
        }

        this.exportPath = Configuration.getInstance().CERT_EXPORT_PATH;
        preparePath(this.exportPath);
    }
    

    /**
     * Creates necessary directories for exporting
     *
     * @param path The path to prepare
     * @throws Exception
     */
    private void preparePath(String path) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        path = FileUtils.unifyPath(path);

        if (!new File(path).exists()) {
            boolean success = (new File(path)).mkdirs();
            if (!success)
                throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }


    /**
     * exports the pkcs12 file to filesystem
     *
     * @return filecontent as String
     * @throws Exception
     */
    public String exportPKCS12ToFile() throws Exception {
        return exportPKCS12ToFile("");
    }


    /**
     * exports the pkcs12 file to filesystem
     *
     * @return filecontent as String
     * @throws Exception
     */
    public String exportPKCS12ToFile(String password) throws Exception {
        boolean putRootInBag = true;

        if (password == null)
            password = "";
        char[] passwordArray = password.toCharArray();

        KeyStore store = KeyStore.getInstance(Constants.KEYSTORE_PKCS12, "BC");
        store.load(null, null);

        Certificate[] chain;
        if (putRootInBag) {
            chain = new Certificate[2];
            chain[1] = this.rootCertificate;
        } else {
            chain = new Certificate[1];
        }
        chain[0] = this.clientCertificate;

        store.setKeyEntry(this.friendlyName, this.clientKey, "".toCharArray(), chain);

        String prefix = this.friendlyName + "_" + this.userId.hashCode() + ".";
        String filename = prefix + Constants.DEFAULT_PKCS12_EXTENSION;

        File outputFile = new File(this.exportPath + "/" + filename);
        FileOutputStream fOut = new FileOutputStream(outputFile);

        store.store(fOut, passwordArray);
        fOut.flush();
        fOut.close();
        logger.info(".p12 file written to filesystem");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        store.store(baos, "".toCharArray());
        baos.flush();
        baos.close();

        return baos.toString();
    }


    /**
     * stores the pkcs12 data in the database
     *
     * @param content The pkcs12 file content
     * @param x509id  The x509id
     * @throws Exception
     */
    public void storePKCS12(String content, int x509id) {
        new PKCS12(this.friendlyName, password, content, x509id);
    }


    public static void main(String[] args) {
        PKCS12 p = new PKCS12("10");
        p = PKCS12DAO.getInstance().read(p);
        try {
            FileUtils.writeStringToFile(p.getContent(), "w:/out.p12", "windows-1252");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
