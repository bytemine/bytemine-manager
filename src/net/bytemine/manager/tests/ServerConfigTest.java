/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.config.ServerConfig;
import net.bytemine.utility.FileUtils;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.*;

/**
 * 
 * Tests if the OpenVPN Userconfig are created
 * 
 */
public class ServerConfigTest {
    private String dbDir = ManagerTestSuite.testdirpath;

    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up ServerConfigTest");
        ManagerTestSuite.setUpTest();
        ManagerTestSuite.rootCreation();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        ManagerTestSuite.tearDownTest();
        System.err.println("\n\n\n>>> Teared down ServerConfigTest");
    }
    
    @Test
    public void testServerConfig() throws Exception {
        checkContent(false, false, false);
        checkContent(true, true, true);
        
        checkContentFail(true, true, true);
    }
    
    private void checkContent(boolean clientConfigDir, boolean vpnRedirectGateway, boolean vpnDuplicateCN) throws Exception {
        Server server = createServer("server1", clientConfigDir, vpnRedirectGateway, vpnDuplicateCN);
        String config = new ServerConfig(server).createConfig();

        assertTrue(config.contains("" + server.getVpnPort()));
        assertTrue(config.contains("" + server.getVpnProtocol()));
        assertTrue(config.contains(FileUtils.appendPathSeparator(server.getExportPath()) + "dh"+Configuration.getInstance().X509_KEY_STRENGTH+".pem"));
            
        // check cert-fields
        String keyfilepath = FileUtils.appendPathSeparator(server.getExportPath());

        assertTrue(config.contains(keyfilepath + "server.crt"));
        assertTrue(config.contains(keyfilepath + "server.key"));
        
        X509 rootX509 = X509Utils.loadRootX509();
        assertTrue(config.contains(FileUtils.appendPathSeparator(server.getExportPath()) +
            rootX509.getFileName()));
        
        // check ccd
        if (server.getVpncc())
            assertTrue(config.contains(server.getVpnccpath()));
        
        assertTrue(config.contains(server.getVpnNetworkAddress()));
        assertTrue(config.contains(Server.transformVpnDeviceToString(server.getVpnDevice())));
        assertTrue(config.contains(server.getVpnUser()));
        assertTrue(config.contains(server.getVpnGroup()));
        assertTrue(config.contains(server.getVpnKeepAlive()));
        
        if (clientConfigDir)
            assertTrue(config.contains("client-config-dir " + server.getVpnccpath()));
        else
            assertFalse(config.contains("client-config-dir " + server.getVpnccpath()));
        
        if (vpnRedirectGateway) {
            assertTrue(config.contains("push \"redirect-gateway\""));
            assertFalse(config.contains(";push \"redirect-gateway\""));
        } else
            assertTrue(config.contains(";push \"redirect-gateway\""));
        
        if (vpnDuplicateCN) {
            assertTrue(config.contains("duplicate-cn"));
            assertFalse(config.contains(";duplicate-cn"));
        } else
            assertTrue(config.contains(";duplicate-cn"));
        
        // config file is present
        if (!checkFile(dbDir + "/_server1/server1.conf", server, false))
            fail();
    }
    
    private void checkContentFail(boolean clientConfigDir, boolean vpnRedirectGateway, boolean vpnDuplicateCN) throws Exception {
        Server server = createServer("server33", clientConfigDir, vpnRedirectGateway, vpnDuplicateCN);
        // remove vpnnetworkaddress to let the config creation fail
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "UPDATE server SET vpnnetworkaddress=? WHERE serverid=?"
            );
    
            pst.setNull(1, java.sql.Types.VARCHAR);
            pst.setInt(2, server.getServerid());        
            pst.executeUpdate();
            pst.close();
            DBConnector.getInstance().getConnection().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        
        server = Server.getServerById(server.getServerid());
        ServerConfig serverConfig = new ServerConfig(server);
        String config = serverConfig.createConfig();
        
        // config is not created
        assertNull(config);
        
        // config file should not be present
        if (checkFile(dbDir + "/_server33/server33.conf", server, true))
            fail();
    }
    
    private Server createServer(String servername, boolean clientConfigDir, boolean vpnRedirectGateway, boolean vpnDuplicateCN) throws Exception {
        Server s = new Server(servername, "server_cn", "server_ou", "hostname", Server.AUTH_TYPE_KEYFILE, "username", "/etc/openvpn/keys",
                  "/etc/openvpn/passwd", "exportpath", 123, Server.STATUS_TYPE_TCPIP, 0, 22, 0, "", -1, 1194, 0,
                  clientConfigDir, "ccd (vpn)", "10.8.0.", 24, 2, vpnRedirectGateway, vpnDuplicateCN, "", "", "");
        X509Generator gen = new X509Generator();
        try {
            gen.createServerCertImmediately(s, "180");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return Server.getServerById(s.getServerid());
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
            checkContentFile(contentOfFile.toString(), server);
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
        if (!content.contains(Server.transformVpnProtocolToString((server.getVpnProtocol())))) // check vpnprotocol
            throw new Exception("Config not written properly - vpnprotocol missing");
        if (!content.contains("" + server.getVpnPort())) // check vpn-port
            throw new Exception("Config not written properly - vpn-port missing");       
    }
}