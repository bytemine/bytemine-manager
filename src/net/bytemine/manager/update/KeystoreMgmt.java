/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.update;


import java.security.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.db.LicenceQueries;
import net.bytemine.manager.utility.X509Utils;


/**
 * Provides functions for keystore operations
 *
 * @author Daniel Rauer
 */
public class KeystoreMgmt {

    private static Logger logger = Logger.getLogger(KeystoreMgmt.class.getName());

    private String crtFile;
    private String keyFile;
    private String keypass = "";
    private String defaultalias = "manager";
    private Certificate[] certificates = new Certificate[1];
    private KeyStore store;
    private PrivateKey key;
    private ByteArrayOutputStream keystoreOutStream = new ByteArrayOutputStream();


    public KeystoreMgmt(String pemFile) {
        this.crtFile = pemFile;
    }

    public KeystoreMgmt(String crtFile, String keyFile) {
        this.crtFile = crtFile;
        this.keyFile = keyFile;
    }


    /**
     * Store certificates in keystore
     *
     * @throws Exception
     */
    public void store() throws CertificateExpiredException, CertificateNotYetValidException, Exception {
        if (keyFile == null || "".equals(keyFile))
            storePEM();
        else
            storeCrtAndKey();
    }


    /**
     * Prepares the keystore
     *
     * @throws Exception
     */
    private void prepareKeystore() throws Exception {
        try {
            store = KeyStore.getInstance("JKS");
            store.load(null, keypass.toCharArray());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error preparing keystore", e);
        }
    }


    /**
     * closes the keystore
     *
     * @throws Exception
     */
    private void closeKeystore() throws Exception {
        try {
            store.setKeyEntry(defaultalias, key,
                    keypass.toCharArray(),
                    certificates);
            store.store(keystoreOutStream,
                    keypass.toCharArray());

            LicenceQueries.storeKeystore(
                    keystoreOutStream.toByteArray(), crtFile, keyFile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error closing keystore", e);
            throw e;
        }
    }


    /**
     * Stores separate crt and key files into keystore
     *
     * @throws Exception
     */
    private void storeCrtAndKey() throws Exception {
        prepareKeystore();

        try {
            key = X509Utils.regainPrivateKey(
                    new File(keyFile));

            X509Certificate certificate = X509Utils.regainX509Certificate(
                    new File(crtFile));
            certificate.checkValidity();
            certificates[0] = certificate;
        } catch (CertificateNotYetValidException e) {
            logger.log(Level.SEVERE, "certificate is not yet valid", e);
            throw e;
        } catch (CertificateExpiredException e) {
            logger.log(Level.SEVERE, "certificate has expired", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error storing crt and key in keystore", e);
            throw e;
        }

        closeKeystore();
    }


    /**
     * Stores pem file into keystore
     *
     * @throws Exception
     */
    private void storePEM() throws Exception {
        prepareKeystore();
        try {
            key = X509Utils.regainPrivateKeyFromPEM(new File(crtFile));
            X509Certificate certificate = X509Utils.regainX509CertificateFromPEM(new File(crtFile));
            certificates[0] = certificate;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error storing pem in keystore", e);
            throw e;
        }
        closeKeystore();
    }
}
