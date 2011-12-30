/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.ResourceBundle;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.X509Queries;


/**
 * methods for importing X509 certificates from LDAP
 *
 * @author Daniel Rauer
 */
public class X509LdapImporter {

    private String content;


    public X509LdapImporter(String content) throws Exception {
        this.content = X509Utils.modifyContent(content);
    }


    /**
     * Reads and imports a certificate from a content String
     *
     * @param user The User to import the certificate for
     * @return int with the ID of the new X509 entry
     * @throws java.lang.Exception
     */
    public int importCertificate(User user) throws Exception {
        X509Certificate cert = X509Utils.regainX509Certificate(content);
        long currentTime = System.currentTimeMillis();

        ResourceBundle clientCertBundle = ResourceBundle.getBundle(Constants.CLIENT_BUNDLE_NAME);
        String issuer = "";
        String subject = "";
        Date validFrom = null;
        Date validTo = null;
        String filename = clientCertBundle.getString("export_cert_file");
        String path = Configuration.getInstance().CERT_EXPORT_PATH;
        int x509id = -1;

        if (cert != null) {
            issuer = cert.getIssuerDN().toString();
            subject = cert.getSubjectDN().toString();
            validFrom = cert.getNotBefore();
            validTo = cert.getNotAfter();

            // is certificate already existing
            x509id = X509Queries.getCertificateBySubject(subject, X509.X509_TYPE_CLIENT);

            // create new certificate
            X509Exporter exporter = new X509Exporter(X509.X509_TYPE_CLIENT, filename, "", path, cert, user);
            String contentStr = exporter.generateContent(false, false);
            exporter.exportCertToFile(contentStr, false);

            x509id = exporter.storeCertificate(
                    currentTime, issuer, subject, content, null, null, validFrom, validTo, false, x509id);

        }

        return x509id;
    }


}
