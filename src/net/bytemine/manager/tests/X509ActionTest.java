/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.security.cert.CertificateException;

import net.bytemine.manager.db.CRLQueries;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.utility.X509FileImporter;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.utility.DNUtil;
import net.bytemine.utility.DateUtils;
import net.bytemine.utility.StringUtils;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static net.bytemine.manager.tests.CustomAssert.*;

public class X509ActionTest {

    private static String username = "testuser";
    
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up X509ActionTest");
        ManagerTestSuite.setUpTest();
        ManagerTestSuite.rootCreation();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down X509ActionTest");
    }
    
    @Test
    public void testX509() throws Exception {
        testRootCreation();
        testClientCertificate();
        testServerCertificate();
        testExportToFilesystem();
        testExportAllToFilesystem();
        X509 x509 = testRevokeCertificate();
        testReenableCertificate(x509);
        testRenewCertificate();
        testRenewServerCertificate();
    }
    
    private void testClientCertificate() throws Exception {
        int userid = UserAction.createUserAndCertificate(username, "123456", "test", "technik", "a", "90");
        User user = User.getUserByID(userid);
        X509 x509 = X509.getX509ById(user.getX509id());

        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + user.getUsername() + File.separator + x509.getFileName());
    }
    
    private void testServerCertificate() throws Exception {
        DNUtil dnUtil = new DNUtil();
        X509Generator gen = new X509Generator();

        // CN and OU not set
        int id = ServerAction.createServerAndCertificate("username", "keyfile",
                true, "servername", null, null, "hostname", "userfile", "exportpath", "123",
                "0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
                "10.1.1.", 24, 1, false, false, "c", "d", "10 120");
        Server server = Server.getServerById(id);

        gen.createServerCertImmediately(server, "365");
        
        int x509id = server.getX509id();
        X509 serverCert = X509.getX509ById(x509id);
        
        String issuer = serverCert.getIssuer();
        String subject = issuer;
        // servers name is set as CN
        dnUtil.split(subject);
        dnUtil.setCn(server.getName());
        subject = dnUtil.merge();
        assertEquals(subject, serverCert.getSubject());
        
        // check certificate export
        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + "_"+server.getName() + File.separator + serverCert.getFileName());
        
        
        // CN and OU set to blank
        id = ServerAction.createServerAndCertificate("username", "keyfile",
                true, "servername", "", "", "hostname", "userfile", "exportpath", "123",
                "0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
                "10.1.1.", 24, 1, false, false, "c", "d", "10 120");
        server = Server.getServerById(id);

        gen = new X509Generator();
        gen.createServerCertImmediately(server, "365");
        
        x509id = server.getX509id();
        serverCert = X509.getX509ById(x509id);
        
        issuer = serverCert.getIssuer();
        // servers CN is set to servers name
        dnUtil.split(subject);
        dnUtil.setCn(server.getName());
        subject = dnUtil.merge();
        assertEquals(subject, serverCert.getSubject());
        
        // check certificate export
        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + "_"+server.getName() + File.separator + serverCert.getFileName());
        
        // CN set
        id = ServerAction.createServerAndCertificate("username", "keyfile",
                true, "servername", "server_cn", null, "hostname", "userfile", "exportpath", "123",
                "0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
                "10.1.1.", 24, 1, false, false, "c", "d", "10 120");
        server = Server.getServerById(id);

        gen = new X509Generator();
        gen.createServerCertImmediately(server, "365");
        
        x509id = server.getX509id();
        serverCert = X509.getX509ById(x509id);
        
        issuer = serverCert.getIssuer();
        // servers CN is set as CN
        dnUtil.split(subject);
        dnUtil.setCn(server.getCn());
        subject = dnUtil.merge();
        assertEquals(subject, serverCert.getSubject());
        
        // check certificate export
        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + "_"+server.getName() + File.separator + serverCert.getFileName());
        
        
        // OU set
        id = ServerAction.createServerAndCertificate("username", "keyfile",
                true, "servername", null, "servers_ou", "hostname", "userfile", "exportpath", "123",
                "0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
                "10.1.1.", 24, 1, false, false, "c", "d", "10 120");
        server = Server.getServerById(id);

        gen = new X509Generator();
        gen.createServerCertImmediately(server, "365");
        
        x509id = server.getX509id();
        serverCert = X509.getX509ById(x509id);
        
        issuer = serverCert.getIssuer();
        // servers name is set as CN, servers OU is set as OU
        dnUtil.split(subject);
        dnUtil.setCn(server.getName());
        dnUtil.setOu(server.getOu());
        subject = dnUtil.merge();
        assertEquals(subject, serverCert.getSubject());
        
        // check certificate export
        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + "_"+server.getName() + File.separator + serverCert.getFileName());
        
        
        // CN and OU set
        id = ServerAction.createServerAndCertificate("username", "keyfile",
                true, "servername", "servers_cn", "servers_ou", "hostname", "userfile", "exportpath", "123",
                "0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
                "10.1.1.", 24, 1, false, false, "c", "d", "10 120");
        server = Server.getServerById(id);

        gen = new X509Generator();
        gen.createServerCertImmediately(server, "365");
        
        x509id = server.getX509id();
        serverCert = X509.getX509ById(x509id);
        
        issuer = serverCert.getIssuer();
        // servers CN is set as CN, servers OU is set as OU
        dnUtil.split(subject);
        dnUtil.setCn(server.getCn());
        dnUtil.setOu(server.getOu());
        subject = dnUtil.merge();
        assertEquals(subject, serverCert.getSubject());
        
        // check certificate export
        assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + "_"+server.getName() + File.separator + serverCert.getFileName());
    }
    
    private void testRootCreation() {
        X509Generator gen = new X509Generator();
        try {
            gen.createRootCertImmediately();
            X509 rootX509 = X509Utils.loadRootX509();
            
            assertEquals(rootX509.getType(), X509.X509_TYPE_ROOT);
            assertTrue(rootX509.isGenerated());
            
            // check certificate export
            assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + rootX509.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void testExportToFilesystem() {
        User user = User.getUserByID(UserQueries.getUserId(username));
        X509 x509 = X509.getX509ById(user.getX509id());
        String path = null;
        try {
            path = X509Action.exportToFilesystem(x509.getX509id()+"") + x509.getFileName();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        
        X509Certificate cert;
        try {
            assert path != null;
            File certFile = new File(path);

            byte[] encodedCert = new byte[(int) certFile.length()];
            FileInputStream keyInputStream = new FileInputStream(certFile);
            keyInputStream.read(encodedCert);
            keyInputStream.close();

            String certContent = StringUtils.bytes2String(encodedCert);
            cert = X509Utils.regainX509Certificate(certContent);
            
            assertEquals(cert.getType(), "X.509");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void testExportAllToFilesystem() {
        String path;
        try {
            X509Action.exportAllCertificatesToFilesystem();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        
        X509Certificate cert;
        try {
            path = Configuration.getInstance().CERT_EXPORT_PATH;
            X509FileImporter im = new X509FileImporter(new File(path+"/"+username));
            Vector<String> ids = im.importClientCertsAndKeys(false);
            for (String x509id : ids) {
                X509 x509 = X509.getX509ById(Integer.parseInt(x509id));
                cert = X509Utils.regainX509Certificate(x509.getContent());
                assertEquals(cert.getType(), "X.509");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private X509 testRevokeCertificate() {
        try {
            int userid = UserAction.createUserAndCertificate(username, "123456", "test", "technik", "a", "90");
            User user = User.getUserByID(userid);
            int x509id = user.getX509id();
            X509 x509 = X509.getX509ById(x509id);
            
            assertFalse(CRLQueries.isCertificateRevoked(x509.getSerial()));
            
            X509Utils.revokeCertificate(x509);
            
            assertTrue(CRLQueries.isCertificateRevoked(x509.getSerial()));
            
            // check CRL export
            assertFileExists(Configuration.getInstance().CERT_EXPORT_PATH + File.separator + Constants.DEFAULT_CRL_FILENAME);
            
            return x509;
        } catch (Exception e) {
            e.printStackTrace();
            fail("error creating user and certificate");
        }
        return null;
    }
    
    /**
     * @param x509 A revoked certificate
     */
    private void testReenableCertificate(X509 x509) {
        try {
            assertTrue(CRLQueries.isCertificateRevoked(x509.getSerial()));
            
            X509Utils.reEnableCertificate(x509);
            
            assertFalse(CRLQueries.isCertificateRevoked(x509.getSerial()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("error re-enabling a certificate");
        }
    }
    
    private void testRenewCertificate() {
        try {
            int userid = UserAction.createUserAndCertificate(username, "123456", "test", "technik", "a", "1");
            User user = User.getUserByID(userid);
            int x509id = user.getX509id();
            X509 x509 = X509.getX509ById(x509id);
            String serial = x509.getSerial();
            
            // certificate should be valid until midnight
            Date validTo = Constants.parseDetailedFormat(x509.getValidTo());
            Calendar cal = Calendar.getInstance();
            cal.setTime(validTo);
            assertEquals(0, DateUtils.daysBetween(Calendar.getInstance(), cal));
            
            X509Generator gen = new X509Generator();
            gen.renewCertificateImmediately(x509);
            
            // user is connected with the new certificate
            user = User.getUserByID(userid);
            int newX509id = user.getX509id();
            X509 newX509 = X509.getX509ById(newX509id);
            assertFalse(x509id == newX509id);
            
            // old certificate should be revoked
            assertTrue(CRLQueries.isCertificateRevoked(serial));
            
            // old certificate should be deleted
            x509 = X509.getX509ById(x509id);
            assertNull(x509);
            
            // new certificate should be valid for one year minus one day
            validTo = Constants.parseDetailedFormat(newX509.getValidTo());
            cal = Calendar.getInstance();
            cal.setTime(validTo);
            assertEquals(364, DateUtils.daysBetween(Calendar.getInstance(), cal));
        } catch (Exception e) {
            e.printStackTrace();
            fail("error creating user and certificate");
        }
    }
    
    private void testRenewServerCertificate() {
        X509Generator gen = new X509Generator();
        try {
            Server server = new Server("my_server", "server_cn", "server_ou", "hostname", Server.AUTH_TYPE_KEYFILE, "username", "/etc/openvpn/keys",
                    "/etc/openvpn/passwd", "exportpath", 123, Server.STATUS_TYPE_TCPIP, 0, 22, 0, "", -1, 1194, 0,
                    true, "ccd (vpn)", "10.8.0.", 24, 2, true, true, "", "", "");
            int serverid = server.getServerid();
            
            try {
                gen.createServerCertImmediately(server, "1");
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            
            int x509id = server.getX509id();
            X509 x509 = X509.getX509ById(x509id);
            String serial = x509.getSerial();
            
            // certificate should be valid until midnight
            Date validTo = Constants.parseDetailedFormat(x509.getValidTo());
            Calendar cal = Calendar.getInstance();
            cal.setTime(validTo);
            assertEquals(0, DateUtils.daysBetween(Calendar.getInstance(), cal));
            
            // Set the validity to two years to get a proper test for the validity of the new certificate
            cal.add(Calendar.YEAR, 2);
            SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMMM yyyy HH:mm:ss");
            String strdate = sdf.format(cal.getTime());
            x509.setValidTo(strdate);
            X509DAO.getInstance().update(x509);

            gen.renewCertificateImmediately(x509);
            
            // server is connected with the new certificate
            server = Server.getServerById(serverid);
            int newX509id = server.getX509id();
            X509 newX509 = X509.getX509ById(newX509id);
            assertFalse(x509id == newX509id);
            
            // old certificate should be revoked
            assertTrue(CRLQueries.isCertificateRevoked(serial));
            
            // old certificate should be deleted
            x509 = X509.getX509ById(x509id);
            assertNull(x509);
            
            // new certificate should be valid for two years
            validTo = Constants.parseDetailedFormat(newX509.getValidTo());
            cal = Calendar.getInstance();
            cal.setTime(validTo);
            assertEquals(731, DateUtils.daysBetween(Calendar.getInstance(), cal));
        } catch (Exception e) {
            e.printStackTrace();
            fail("error renewing server certificate");
        }
    }
}