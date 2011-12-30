/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.File;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.utility.FileUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.sql.PreparedStatement;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.fail;

/**
 * 
 * Tests if the OpenVPN Userconfig are created
 * 
 */
public class UserConfigTest {
    
    private String dbDir = ManagerTestSuite.testdirpath;
    
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up UserConfigTest");
        ManagerTestSuite.setUpTest();
        ManagerTestSuite.rootCreation();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down UserConfigTest");
    }
    
    @Test
    public void testUserConfig() throws Exception {
        // assign users to servers
        Server server1 = createServer("server1"); 
        Server server2 = createServer("server2");
        Server server3 = createServer("server3");
    	
    	User user1 = User.getUserByID(UserAction.createUserAndCertificate("user1", "1", "user1", "technik", "a", "9"));
    	User user2 = User.getUserByID(UserAction.createUserAndCertificate("user2", "1", "user2", "technik", "a", "9"));
    	User user3 = User.getUserByID(UserAction.createUserAndCertificate("user3", "1", "user3", "technik", "a", "9"));
    	X509 user2x509 = X509.getX509ById(user3.getX509id());
        user2x509.delete();
    	
        // server1 - all 3 clients connected
        // server2 - only first client connected
        // server3 - no client connected
        ServerQueries.addUserToServer(server1.getServerid(), user1.getUserid());
        ServerQueries.addUserToServer(server1.getServerid(), user2.getUserid());
        ServerQueries.addUserToServer(server1.getServerid(), user3.getUserid());
        
        ServerQueries.addUserToServer(server2.getServerid(), user1.getUserid());
        
        // create configs
        ServerAction.createVPNUserConfigFile(server1);
        ServerAction.createVPNUserConfigFile(server2);
        ServerAction.createVPNUserConfigFile(server3);
        
        // check configs for validity 
        File f = new File(dbDir);
        if (!f.exists()) 
            fail(); 

    	if (!checkFile(dbDir + "/user1/server1.conf", server1))
    	    fail();
    	if (!checkFile(dbDir + "/user1/server2.conf", server2))
            fail();
    	if (!checkFile(dbDir + "/user2/server1.conf", server1))
            fail();
    	
    	// files should not created when not associated
    	f = new File(dbDir + "/user1/server3.conf");
    	if (f.exists())
    	    fail();
    	
    	f = new File(dbDir + "/user2/server3.conf");
    	if (f.exists())
    	    fail();
    	
    	
    	// delete a users directory and try to write the config again
    	File userDir = new File(dbDir + "/user1/");
    	FileUtils.deleteDirectory(userDir);
    	ServerAction.createVPNUserConfigFile(server1);
    	
    	// config file has to be present
    	if (!checkFile(dbDir + "/user1/server1.conf", server1))
            fail();
    	
    	user1.delete();
    	user2.delete();
    	user3.delete();
    }
    
    @Test
    public void testUserConfigFail() throws Exception {
        Server server1 = createServer("server20");
        User user1 = User.getUserByID(UserAction.createUserAndCertificate("user20", "1", "user20", "technik", "a", "9"));
        // assign user to server
        ServerQueries.addUserToServer(server1.getServerid(), user1.getUserid());
        
        // remove vpnport to let the config creation fail
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "UPDATE server SET vpnport=? WHERE serverid=?"
            );
    
            pst.setNull(1, java.sql.Types.INTEGER);
            pst.setInt(2, server1.getServerid());        
            pst.executeUpdate();
            pst.close();
            DBConnector.getInstance().getConnection().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        
        // try to create the config
        ServerAction.createVPNUserConfigFile(server1);
        
        // check configs for validity 
        File f = new File(dbDir);
        if (!f.exists()) 
            fail(); 

        // config file should not be present
        if (checkFile(dbDir + "/user20/server20.conf", server1, true))
            fail();
    }
    
    private boolean checkFile(String pathToFile, Server server) {
        return checkFile(pathToFile, server, false);
    }
    
    private boolean checkFile(String pathToFile, Server server, boolean noException) {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(pathToFile))); 
            StringBuffer contentOfFile = new StringBuffer();
            String line; 
            while ((line = br.readLine()) != null)
                contentOfFile.append(line);
            checkContentFile(contentOfFile.toString(),server);
        } catch (Exception e) {
            if (!noException) {
                System.err.println("(" + pathToFile + ")");
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
    
    private void checkContentFile(String content, Server server) throws Exception {
        if (!content.contains(server.getHostname())) // check hostname
            throw new Exception("Config not written properly - hostname missing");
        if (!content.contains("" + server.getVpnPort())) // check vpn-port
            throw new Exception("Config not written properly - vpn-port missing");       
    }
    
    private Server createServer(String servername) throws Exception {
        Server s = new Server(servername, "server_cn", "server_ou", "hostname", Server.AUTH_TYPE_KEYFILE, "username", "keyfile", "userfile", 
        					  "exportpath", 123, Server.STATUS_TYPE_TCPIP, 0, 22, 0, "", -1, 0, 1190, false, "ccd (vpn)",
        					  "vpnNetworkAddress", 24, 0, false, false, "", "", "");
        X509Generator gen = new X509Generator();
        try {
            gen.createServerCertImmediately(s, "180");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return Server.getServerById(s.getServerid());
    }
}