/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.security.PrivateKey;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.ResourceBundle;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.CRLEntry;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.CRLEntryDAO;
import net.bytemine.manager.db.CRLQueries;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * General utils for x509 objects
 *
 * @author Daniel Rauer
 */
public class X509Utils extends net.bytemine.utility.X509Utils {
    
    /**
     * generates the root certificate
     */
    public static void createRootCertificate() {
        X509Generator gen = new X509Generator();
        try {
            gen.createRootCert();
            Configuration.getInstance().setRootCertExisting(true);
        } catch (Exception e) {
            new VisualException(e);
        }
    }

    
    /**
     * generates DH parameters
     */
    public static void createDHParameters() {
        X509Generator gen = new X509Generator();
        try {
            gen.createAndExportDHParameters();
        } catch (Exception e) {
            new VisualException(e);
        }

    }
    

    /**
     * withdrawes privileges from the certificate of the user with the given id
     *
     * @param userid The ID of the user
     */
    public static void revokeCertificateByUserID(String userid) throws Exception {
        User user = new User(userid);
        user = UserDAO.getInstance().read(user);

        int x509id = user.getX509id();
        if (x509id != -1)
            revokeCertificateByID(x509id);
    }
    
    /**
     * withdrawes privileges from the certificate with the given id
     *
     * @param x509id The ID of the certificate
     */
    private static void revokeCertificateByID(int x509id) throws Exception {
        X509 x509 = new X509(x509id);
        x509 = X509DAO.getInstance().read(x509);

        revokeCertificate(x509);
    }

    /**
     * withdraws privileges from the given certificate
     *
     * @param x509 The x509 to revoke
     */
    public static void revokeCertificate(X509 x509) throws Exception {
        int crlId = CRLQueries.getMaxCRLId();

        String subject = x509.getSubject();
        String cn = getCnFromSubject(subject);
        
        Date now = new Date();
        String createDateStr = Constants.formatDetailedFormat(now);

        CRLEntry entry = new CRLEntry(x509.getSerial());
        entry.setRevocationDate(createDateStr);
        entry.setX509id(x509.getX509id());
        entry.setCrlid(crlId);
        entry.setUsername(cn);
        CRLEntryDAO.getInstance().update(entry);

        X509Generator gen = new X509Generator();
        gen.createCRL();
    }
    
    
    /**
     * Re-enable a revoked certificate
     * @param x509 The x509
     */
    public static void reEnableCertificate(X509 x509) throws Exception {
        int crlEntryId = CRLQueries.getCRLEntryId(x509.getSerial());
        if (crlEntryId>-1) {
            CRLEntry entry = new CRLEntry(crlEntryId);
            entry = CRLEntryDAO.getInstance().read(entry);
            CRLEntryDAO.getInstance().delete(entry);
            
            X509Generator gen = new X509Generator();
            gen.createCRL();
        }
    }
    
    
    /**
     * Renew an existing certificate
     * @param x509 The x509
     */
    public static void renewCertificate(X509 x509) throws Exception {
        X509Generator gen = new X509Generator();
        gen.renewCertificate(x509);
    }


    /**
     * loads the root certificate
     *
     * @return The root certificate as X509Certificate
     */
    static X509Certificate loadRootCertificate() throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String rootId = X509Queries.getRootCertId();
        if (rootId == null)
            throw new Exception(rb.getString("dialog.newuser.certerror.text"));

        return loadCertificate(Integer.parseInt(rootId));
    }


    /**
     * loads the root certificates private key
     *
     * @return The root certificates private key
     */
    public static PrivateKey loadRootPrivateKey() throws Exception {
        String rootId = X509Queries.getRootCertId();
        if (rootId == null)
            throw new Exception("no root certificate could be found");

        X509 rootX509 = X509.getX509ById(Integer.parseInt(rootId));

        return (PrivateKey)
                X509Serializer.getInstance().fromXML(
                        rootX509.getKey()
                );
    }


    /**
     * loads the intermediate certificates private key
     *
     * @return The intermediate certificates private key
     */
    public static PrivateKey loadIntermediatePrivateKey() throws Exception {
        String interId = X509Queries.getIntermediateCertId();
        if (interId == null)
            throw new Exception("no intermdiate certificate could be found");

        X509 interX509 = X509.getX509ById(Integer.parseInt(interId));

        return (PrivateKey)
                X509Serializer.getInstance().fromXML(
                        interX509.getKey()
                );
    }


    /**
     * extracts the root certificates private key from the rootX509
     *
     * @return The root certificates private key
     */
    public static PrivateKey extractRootPrivateKey(X509 rootX509) throws Exception {

        return (PrivateKey)
                X509Serializer.getInstance().fromXML(
                        rootX509.getKey()
                );
    }


    /**
     * loads the certificate with the given id
     *
     * @param x509Id The id of the certificate to load
     * @return The certificate as X509Certificate
     */
    private static X509Certificate loadCertificate(int x509Id) throws Exception {
        X509 rootX509 = X509.getX509ById(x509Id);

        return (X509Certificate)
                X509Serializer.getInstance().fromXML(
                        rootX509.getCertSerialized()
                );
    }


    /**
     * loads the root x509 object
     *
     * @return The root certificate as X509
     */
    public static X509 loadRootX509() throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String rootId = X509Queries.getRootCertId();
        if (rootId == null)
            throw new Exception(rb.getString("dialog.newuser.certerror.text"));

        return X509.getX509ById(Integer.parseInt(rootId));
    }


    /**
     * loads the intermediate x509 object
     *
     * @return The intermediate certificate as X509
     */
    public static X509 loadIntermediateX509() throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        String interId = X509Queries.getIntermediateCertId();
        if (interId == null)
            throw new Exception(rb.getString("dialog.newuser.certerror.text"));

        return X509.getX509ById(Integer.parseInt(interId));
    }

}
