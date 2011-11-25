/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.*;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.DBTasks;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.openvpn.UserImport;
import net.bytemine.openvpn.UserImporter;
import net.bytemine.openvpn.UserImportFile;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class UserImportTest {

	static String userfile_path = ManagerTestSuite.testdirpath+"/passwd";
	
    @Before
    public void setUp() {
        System.err.println("\n\n\n>>> Setting up UserImportTest");
        ManagerTestSuite.setUpTest();
        ManagerTestSuite.rootCreation();
        
        new File(ManagerTestSuite.testdirpath+"/emptydir").mkdir();
        
        Configuration.getInstance().setClientCertImportDir(ManagerTestSuite.testdirpath+"/emptydir");
        Configuration.getInstance().setClientUserfile(userfile_path);
        
       // Configuration.getInstance().CLIENT_CERT_IMPORT_DIR = ManagerTestSuite.testdirpath+"/emptydir";
       // Configuration.getInstance().CLIENT_USERFILE = userfile_path;
    }

    @After
    public void tearDown() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down UserImportTest");
    }
    

    /*
     * testing userimport : cases
     * 
     * - import new user from passwd (with new cert, without new cert) 
     * - import already existing user from passwd 
     * - import from empty passwd
     * - import new certs (with new user, without new user)
     */
    @Test
    public void testUserImport() throws Exception {
        testUserImportFromPasswd();
        testCertImport();
        testUserCertImport();
        testSerialOnImport();
        testSerialOnImportPKCS12();
    }
    
    private void createPasswd(String content) {
    	try {
    		new File(userfile_path);
            BufferedWriter out = new BufferedWriter(new FileWriter(
            		userfile_path));
            out.write(content);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("error while creating passwd-file for UserImport Test");
        }
    }

    public void testUserImportFromPasswd() throws Exception {

    	createPasswd("usr1:password");

        UserImportFile im = (UserImportFile) UserImporter
                .getUserImportImplementation(true); // create cert

        UserImport.resetCounters();
        try {
            // import new user
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(1, UserImport.importedUsers);
        assertEquals(1, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);

        UserImport.resetCounters();
        try {
            // import already existing user
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(0, UserImport.importedUsers);
        assertEquals(0, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);

        // delete usr1 - check if user was really created
        User user = User.getUserByID(UserQueries.getUserId("usr1"));
        assertEquals("usr1", user.getUsername());

        /* do not create cert when importing usr */
        im = (UserImportFile) UserImporter.getUserImportImplementation(false);

        createPasswd("usr2:password");
        UserImport.resetCounters();
        try {
            // import user but do not create certificates
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(1, UserImport.importedUsers);
        assertEquals(0, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(1, UserImport.notLinkedUsers);
        
        /* test with empty passwd */
        createPasswd("");
        UserImport.resetCounters();
        try {
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(0, UserImport.importedUsers);
        assertEquals(0, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);
    }

    public void testCertImport() throws Exception {
        Configuration.getInstance().CLIENT_CERT_IMPORT_DIR = ManagerTestSuite.testdirpath+"/ImportCertUser";

        // import certificates - do not create user
        UserImportFile im = (UserImportFile) UserImporter
                .getUserImportImplementation(true);

        int id = UserAction.createUserAndCertificate("ImportCertUser",
                "123456", "ImportCertUser", "technik", "a", "90");
        UserAction.deleteUser(Integer.toString(id));

        im.createUsersFromCN = UserImport.NO;
        UserImport.resetCounters();
        try {
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(0, UserImport.importedUsers);
        assertEquals(0, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(1, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);

        // import certificates - create User
        im.createUsersFromCN = UserImport.YES;
        UserImport.resetCounters();
        try {
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(1, UserImport.generatedUsers);
        assertEquals(0, UserImport.importedUsers);
        assertEquals(0, UserImport.generatedCerts);
        assertEquals(0, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);

        // clean up
        User user = User.getUserByID(UserQueries.getUserId("ImportCertUser"));
        user.delete();
    }
    
    public void testUserCertImport() throws Exception {
    	String testUser1 = "usrImport1";
    	String importCertDir = ManagerTestSuite.testdirpath+File.separator+"ImportCertDir";
        String testUser1Dir =  ManagerTestSuite.testdirpath+File.separator+testUser1;
        
         if(!(new File(importCertDir)).mkdir())
             System.out.println("Cannot create :"+importCertDir);
        
        Configuration.getInstance().CLIENT_CERT_IMPORT_DIR = importCertDir;       	
        createPasswd(testUser1+":password");
        UserAction.createUserAndCertificate(testUser1, "password", testUser1, "ou", "a", "1");
        DBTasks.resetDB(true);
        
        UserImportFile im = (UserImportFile) UserImporter
            .getUserImportImplementation(false); 	// do not create cert
        im.createUsersFromCN = UserImport.NO;	// do not create user
        
        FileUtils.copyDirectory(new File(testUser1Dir), new File(importCertDir));
        FileUtils.deleteDirectory(new File(testUser1Dir));
        
        UserImport.resetCounters();
        try {
            im.importUsersImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(0, UserImport.generatedUsers);
        assertEquals(1, UserImport.importedUsers);
        assertEquals(1, UserImport.generatedCerts);
        assertEquals(1, UserImport.importedCerts);
        assertEquals(0, UserImport.notLinkedCerts);
        assertEquals(0, UserImport.notLinkedUsers);
        
        int userHash = Integer.toString(UserQueries.getUserId(testUser1)).hashCode();
        String testUser1Cert = testUser1Dir + File.separator + testUser1+"_"+userHash+".crt";
        
        // check if cert is moved to the user-dir
        File f = new File(testUser1Cert);
        assertTrue(f.exists());
    }

    
    /**
     * Check if the serial is correctly imported
     */
    public void testSerialOnImport() {
        String importCertDir = ManagerTestSuite.testdirpath;
        Configuration.getInstance().CLIENT_CERT_IMPORT_DIR = importCertDir;         
        try {
            int userid = UserAction.createUserAndCertificate("testuser_serial", "password", "testuser_serial", "ou", "a", "1");
            
            if(!(new File(importCertDir)).mkdir())
                System.out.println("Cannot create :"+importCertDir);
            new File(importCertDir + File.separator + "passwd").createNewFile();
            FileUtils.copyDirectory(new File(importCertDir + File.separator + "testuser_serial"), new File(Configuration.getInstance().CLIENT_CERT_IMPORT_DIR));
            
            User user = UserDAO.getInstance().read(new User(userid + ""));
            int x509id = user.getX509id();
            X509 x509 = X509DAO.getInstance().read(new X509(x509id));
            String serial = x509.getSerial();
            
            UserDAO.getInstance().delete(user);
            X509DAO.getInstance().delete(x509);
            
            UserImportFile im = (UserImportFile) UserImporter
                .getUserImportImplementation(false);
            im.createUsersFromCN = UserImport.YES;
            im.importUsersImmediately();
            
            assertEquals(1, UserImport.importedCerts);
            
            Vector<String[]> users = UserQueries.getAllUsersFilteredByUsername("testuser_serial");
            assertEquals(1, users.size());
            User importedUser = UserDAO.getInstance().read(new User(users.get(0)[0]));
            X509 importedX509 = X509DAO.getInstance().read(new X509(importedUser.getX509id()));
            
            assertEquals(serial, importedX509.getSerial());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
        
    /**
     * Check if the serial is correctly imported
     */
    public void testSerialOnImportPKCS12() {
        String importCertDir = ManagerTestSuite.testdirpath;
        Configuration.getInstance().CLIENT_CERT_IMPORT_DIR = importCertDir;         
        Configuration.getInstance().CERTIFICATE_TYPE = Constants.CERTIFICATE_TYPE_PKCS12;
        Configuration.getInstance().TEST_PKCS12_PASSWORD_DISABLED = true;
        try {
            int userid = UserAction.createUserAndCertificate("testuser_serial", "password", "testuser_serial", "ou", "", "1");
            
            new File(importCertDir + File.separator + "passwd").createNewFile();
            FileUtils.copyDirectory(new File(importCertDir + File.separator + "testuser_serial"), new File(Configuration.getInstance().CLIENT_CERT_IMPORT_DIR));
            
            User user = UserDAO.getInstance().read(new User(userid + ""));
            int x509id = user.getX509id();
            X509 x509 = X509DAO.getInstance().read(new X509(x509id));
            String serial = x509.getSerial();
            
            UserDAO.getInstance().delete(user);
            X509DAO.getInstance().delete(x509);
            
            UserImportFile im = (UserImportFile) UserImporter
                .getUserImportImplementation(false);
            im.createUsersFromCN = UserImport.YES;
            im.importUsersImmediately();
            
            assertEquals(1, UserImport.importedCerts);
            
            Vector<String[]> users = UserQueries.getAllUsersFilteredByUsername("testuser_serial");
            assertEquals(1, users.size());
            User importedUser = UserDAO.getInstance().read(new User(users.get(0)[0]));
            X509 importedX509 = X509DAO.getInstance().read(new X509(importedUser.getX509id()));
            
            assertEquals(serial, importedX509.getSerial());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        Configuration.getInstance().TEST_PKCS12_PASSWORD_DISABLED = false;
    }
}