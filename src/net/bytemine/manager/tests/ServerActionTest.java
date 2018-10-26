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
import net.bytemine.manager.bean.Server;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import net.bytemine.manager.Configuration;
import net.bytemine.utility.FileUtils;

import static org.junit.Assert.*;

public class ServerActionTest {
    
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up ServerActionTest");
        ManagerTestSuite.setUpTest();
	}
	
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down ServerActionTest");
	}
    
    // testing server : creation,update,removal
    
    @Test
    public void testServer() throws Exception {
    	int id = testServerCreation();
        checkForDirectory(Server.getServerById(id));
        testServerUpdate(id);
        checkForDirectory(Server.getServerById(id));
        testServerRemoval(id);
    }
    
    private int testServerCreation() throws Exception {
    	int id = ServerAction.createServerAndCertificate("username", "keyfile",
    			true, "servername", "server_cn", "server_ou", "hostname", "userfile", "exportpath", "123",
    			"0", "456", "22", 0, "", "90", "1190", 0, false, "ccd (vpn)",
    			"10.1.1.", 24, 1, false, false, "c", "d", "10 120");
    	Server server = Server.getServerById(id);
    	
        assertEquals("username", server.getUsername());
        assertEquals("servername", server.getName());
        assertEquals("server_cn", server.getCn());
        assertEquals("server_ou", server.getOu());
        assertEquals("hostname", server.getHostname());
        
        assertEquals("keyfile", server.getKeyfilePath());
        assertEquals("userfile", server.getUserfilePath());
        assertEquals("exportpath", server.getExportPath());
        assertEquals("123", Integer.toString(server.getStatusPort()));
        
        assertEquals("0", Integer.toString(server.getVpnProtocol()));
        assertEquals("1190", Integer.toString(server.getVpnPort()));
        
        assertEquals("0", Integer.toString(server.getServerType()));
        assertEquals("456", Integer.toString(server.getStatusInterval()));
        assertEquals("1", Integer.toString(server.getAuthType()));
        assertEquals("0", Integer.toString(server.getStatusType()));
        assertEquals("22", Integer.toString(server.getSshPort()));

        assertFalse(server.getVpncc());
        assertEquals("ccd (vpn)", server.getVpnccpath());
        assertEquals("10.1.1.", server.getVpnNetworkAddress());
        assertEquals("24", Integer.toString(server.getVpnSubnetMask()));
        assertEquals("1", Integer.toString(server.getVpnDevice()));

        assertFalse(server.getVpnRedirectGateway());
        assertFalse(server.getVpnDuplicateCN());
        assertEquals("c", server.getVpnUser());
        assertEquals("d", server.getVpnGroup());
        assertEquals("10 120", server.getVpnKeepAlive());
        
        return id;
    }
    
    private void testServerUpdate(int id) throws Exception {
    	ServerAction.updateServer(Integer.toString(id), "name", "cn_of_server", "ou_of_server",
    			"hostna", "userna", true, "keyfil", "userfil", "exportPat", "0",
    			"statusType", "1", "22", 1, "wrapperCommand",
    			"1199", 1, true, "CCD", "10.1.2.", 25, 2, true,
                true, "a", "b", "11 121");
    	Server server = Server.getServerById(id);
    	
    	assertEquals("userna", server.getUsername());
        assertEquals("name", server.getName());
        assertEquals("cn_of_server", server.getCn());
        assertEquals("ou_of_server", server.getOu());
        assertEquals("hostna", server.getHostname());
        
        assertEquals("keyfil", server.getKeyfilePath());
        assertEquals("userfil", server.getUserfilePath());
        assertEquals("exportPat", server.getExportPath());
        assertEquals("0", Integer.toString(server.getStatusPort()));
        
        assertEquals("1", Integer.toString(server.getVpnProtocol()));
        assertEquals("1199", Integer.toString(server.getVpnPort()));
        
        assertEquals("1", Integer.toString(server.getServerType()));
        assertEquals("1", Integer.toString(server.getStatusInterval()));
        assertEquals("1", Integer.toString(server.getAuthType()));
        assertEquals("0", Integer.toString(server.getStatusType()));
        assertEquals("22", Integer.toString(server.getSshPort()));

        assertTrue(server.getVpncc());
        assertEquals("CCD", server.getVpnccpath());     
        assertEquals("10.1.2.", server.getVpnNetworkAddress());
        assertEquals("25", Integer.toString(server.getVpnSubnetMask()));
        assertEquals("2", Integer.toString(server.getVpnDevice()));
        assertTrue(server.getVpnRedirectGateway());
        assertTrue(server.getVpnDuplicateCN());
        assertEquals("a", server.getVpnUser());
        assertEquals("b", server.getVpnGroup());
        assertEquals("11 121", server.getVpnKeepAlive());
    }
    
    private void testServerRemoval(int id) throws Exception{
    	ServerAction.deleteServer(Integer.toString(id));
    	Server server = Server.getServerById(id);
    	if (server != null)
    		fail();
    }
    
    private void checkForDirectory(Server server) {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        exportPath = exportPath + "_" + server.getName() + "/";

        if (!new File(exportPath).exists())
            fail();
    }
}