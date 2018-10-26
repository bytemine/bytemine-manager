/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.util.Vector;

import javax.swing.JTextField;

import net.bytemine.openvpn.UserSync;
import java.util.Hashtable;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserQueries;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 
 * @author fre
 *
 * Simple Testclass to validate UserServer-Assignments
 * 		- test UserServer Assignment
 * 		- test UserServer Ip assigning
 * 		- test ClientConfigurationCreation
 */
public class ServerUserTest {
    
	private final String networkAddress = "10.1.1.";
	private final String ipUser1 = "2";
	private final String ipUser2 = "3";
	private final String ccUser1 = "ifconfig-push 10.1.1.2 10.1.1.3\n";
	private final String ccUser2 = "ifconfig-push 10.1.1.3 10.1.1.4\n";
	
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up ServerUserTest");
        ManagerTestSuite.setUpTest();
        ManagerTestSuite.rootCreation();
	}
	
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down ServerUserTest");
	}
    
    // testing server : creation,update,removal   
    @Test
    public void testServerUser() throws Exception {
    	// create server,user
    	int serverId = ServerAction.createServerAndCertificate("username", "keyfile",
    			true, "servername", "server_cn", "server_ou", "hostname", "userfile", "exportpath", "123",
    			"0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
    			networkAddress, 24, 0, false, false, "", "", "");
    	int userId1 = UserAction.createUserAndCertificate("test1", "123456", "test1", "technik", "a", "90");
    	int userId2 = UserAction.createUserAndCertificate("test2", "123456", "test2", "technik", "a", "90");
    	
    	// testing
    	testUserServerAssignment(serverId, userId1, userId2);
    	testUserServerIp(serverId,userId1, userId2);					  // requries testUserServerAssignment
    	try {
    		testClientConfigurationCreation(serverId, userId1, userId2);// requires testClientConfigurationCreation
    	} catch(Exception e) {
    		System.out.println("Couldn't create ClientConfiguration: " + e.toString());
    		fail();
    	}
    	
    	// delete server,user
    	ServerAction.deleteServer(Integer.toString(serverId));
    	UserAction.deleteUser(Integer.toString(userId1));
    	UserAction.deleteUser(Integer.toString(userId2));
    }
    
    /*
     * tests assigning user to server
     */
    private void testUserServerAssignment(int serverId, int userId1, int userId2) {
    	// connect
    	Vector<String> connectUsers = new Vector<String>();
    	
    	connectUsers.add(Integer.toString(userId1));
    	connectUsers.add(Integer.toString(userId2));

    	try {
    		ServerQueries.reconnectUsersAndServer(Integer.toString(serverId), connectUsers);
    	} catch(Exception e) {
    		System.out.println("ServerQueries.reconnectUsersAndServer(); failed !");
    		fail();
    	}
    	
    	// check

		Vector<String> connectedUsers = new Vector<>(UserQueries.getUsersForServer(serverId));

        if(!connectedUsers.containsAll(connectUsers))
        	fail();
    }
    
    /*
     * checks ip assigning to ServerUser,
     *  requires that testUserServerAssignment already run
     */
    private void testUserServerIp(int serverId, int userId1, int userId2) {
    	Hashtable<String,JTextField> UserServerIp = new Hashtable<String,JTextField>();
    	JTextField ip1 = new JTextField(20);
    	JTextField ip2 = new JTextField(20);
    	
    	ip1.setText(ipUser1);
    	ip2.setText(ipUser2);
    	
    	UserServerIp.put(Integer.toString(userId1), ip1);
    	UserServerIp.put(Integer.toString(userId2), ip2);
    	
    	ServerQueries.reassignIpToUserServer(Integer.toString(serverId), UserServerIp);
    	
    	//check
    	assertEquals(ipUser1, ServerQueries.getIpFromUserServer(serverId, userId1));
    	assertEquals(ipUser2, ServerQueries.getIpFromUserServer(serverId, userId2));
    }
    
    /*
     * checks ClientConfigurationCreation,
     * 	requires that testUserServerIp already run
     */
    private void testClientConfigurationCreation(int serverId, int userId1, int userId2) throws Exception {
    	System.out.println("testClientConfigurationCreation");
    	assertEquals(ccUser1, UserSync.createCC(Integer.toString(serverId), Integer.toString(userId1)));
    	assertEquals(ccUser2, UserSync.createCC(Integer.toString(serverId), Integer.toString(userId2)));
    }
}