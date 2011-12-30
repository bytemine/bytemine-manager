/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.action;

import java.io.File;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.db.ServerDAO;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.openvpn.config.ClientConfig;
import net.bytemine.openvpn.config.ServerConfig;
import net.bytemine.utility.FileUtils;
import net.bytemine.manager.db.UserQueries;


/**
 * Server actions
 *
 * @author Daniel Rauer
 */
public class ServerAction {

    private static Logger logger = Logger.getLogger(ServerAction.class.getName());


    /**
     * Create new server and server certificate
     *
     * @param username          The username used for auth
     * @param keyfile           The file containing the key
     * @param authTypeKeyfile   true, if auth uses a keyfile
     * @param name              The server name
     * @param cn                The server cn
     * @param ou                The server ou
     * @param hostname          The servers hostname
     * @param userfile          The passwd file
     * @param exportPath        The path to export the certificates to
     * @param statusPortStr     The port for polling status information
     * @param statusType        The type of status polling
     * @param statusIntervalStr The interval for status polling
     * @param sshPortStr        The ssh port
     * @param serverType        The type of the server
     * @param wrapperCommand    The command to call the wrapper
     * @param validFor          Number of days the certificate will be valid
     * @param vpnPortStr        The openvpn port
     * @param vpnProtocol		The openvpn protocol which is used
     * @param vpncc				true, if user wants cc(vpn)
     * @param vpnccpath			the path to the cc-dir, disabled if cc not wanted
     * @param vpnNetworkAddress	the network address used by the vpn server
     * @param vpnSubnetMask		the network mask in CIDR-Notation (/)
     * @param vpnDevice         the vpn device used by the vpn server
     * @param vpnRedirectGateway option for enabling RedirectGateway
     * @param vpnDuplicateCN    option for enabling DuplicateCN
     * @param vpnUser           the user for vpn to reduce privileges
     * @param vpnGroup          the group for vpn to reduce privileges
     * @param vpnKeepAlive      the keep alive statement for vpn
     * @return the serverId of the new server
     * @throws java.lang.Exception
     */
    public static int createServerAndCertificate(String username, String keyfile,
                                                 boolean authTypeKeyfile, String name, String cn, String ou,
                                                 String hostname, String userfile, String exportPath, String statusPortStr,
                                                 String statusType, String statusIntervalStr, String sshPortStr,
                                                 int serverType, String wrapperCommand, String validFor, String vpnPortStr,
                                                 int vpnProtocol, boolean vpncc, String vpnccpath, String vpnNetworkAddress,
                                                 int vpnSubnetMask, int vpnDevice, boolean vpnRedirectGateway,
                                                 boolean vpnDuplicateCN, String vpnUser, String vpnGroup, String vpnKeepAlive)
            throws Exception {

        int authType = Server.AUTH_TYPE_PASSWORD;
        if (authTypeKeyfile) {
            authType = Server.AUTH_TYPE_KEYFILE;
        }


        int statusPort = 0;
        int statusInterval = 0;
        int sshPort = 0;
        int vpnPort = 0;
        try {
            statusPort = Integer.parseInt(statusPortStr);
        } catch (Exception e) {
        }
        try {
            statusInterval = Integer.parseInt(statusIntervalStr);
        } catch (Exception e) {
        }
        try {
            sshPort = Integer.parseInt(sshPortStr);
        } catch (Exception e) {
        }
        try {
            vpnPort = Integer.parseInt(vpnPortStr);
        } catch (Exception e) {
        }
        
        if (Configuration.getInstance().USE_PAM)
            userfile = "";
        
        // create new server
        Server newServer = new Server(
                name,
                cn,
                ou,
                hostname,
                authType,
                username,
                keyfile,
                userfile,
                exportPath,
                statusPort,
                Server.STATUS_TYPE_TCPIP,
                statusInterval,
                sshPort,
                serverType,
                wrapperCommand,
                -1,
                vpnPort,
                vpnProtocol,
                vpncc,
                vpnccpath,
                vpnNetworkAddress,
                vpnSubnetMask,
                vpnDevice,
                vpnRedirectGateway,
                vpnDuplicateCN,
                vpnUser,
                vpnGroup,
                vpnKeepAlive);


        // create server certificate
        if (Configuration.getInstance().CA_ENABLED) {
            X509Generator gen = new X509Generator();
            try {
                gen.createServerCert(newServer, validFor);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "error while creating server certificate. Message: " + ex.getMessage(), ex);
                throw ex;
            }
        }

        return newServer.getServerid();
    }


    /**
     * delete the server with the given id
     *
     * @param serverId
     * @throws java.lang.Exception
     */
    public static void deleteServer(String serverId) throws Exception {
        ServerDAO.getInstance().deleteById(serverId);
    }


    /**
     * delete the server with the given id
     *
     * @param serverId
     * @throws java.lang.Exception
     */
    public static void deleteServer(String serverId, boolean deleteCertificate) throws Exception {
        logger.info("deleting server with id " + serverId + "");
        if (deleteCertificate) {
            Server server = new Server(serverId);
            server = ServerDAO.getInstance().read(server);
            int x509Id = server.getX509id();
            logger.info("deleting corresponding certificate with id " + x509Id + "");
            X509DAO.getInstance().deleteById(x509Id + "");
        }

        ServerDAO.getInstance().deleteById(serverId);
    }


    /**
     * Updates the server object
     *
     * @param serverId
     * @param name
     * @param cn
     * @param ou
     * @param hostname
     * @param username
     * @param authTypeKeyfile
     * @param keyfile
     * @param userfile
     * @param exportPath
     * @param statusport
     * @param statusType
     * @param statusinterval
     * @param sshport
     * @param vpnport
     * @param vpnProtocol
     * @param vpncc
     * @param vpnccpath
     * @param vpnDevice
     * @param vpnRedirectGateway
     * @param vpnDuplicateCN
     * @param vpnUser
     * @param vpnGroup
     * @param vpnKeepAlive
     * @throws ValidationException
     * @throws Exception
     */
    public static void updateServer(String serverId, String name, String cn, String ou, String hostname,
                                    String username, boolean authTypeKeyfile, String keyfile, String userfile,
                                    String exportPath, String statusport, String statusType, String statusinterval,
                                    String sshport, int serverType, String wrapperCommand, String vpnport,
                                    int vpnProtocol, boolean vpncc, String vpnccpath, String vpnNetworkAddress,
                                    int vpnSubnetMask, int vpnDevice, boolean vpnRedirectGateway,
                                    boolean vpnDuplicateCN, String vpnUser, String vpnGroup, String vpnKeepAlive)
            throws ValidationException, Exception {
        logger.info("Update server with id: " + serverId);

        Server server = new Server(serverId);
        server = ServerDAO.getInstance().read(server);

        if (!name.equals(server.getName()))
            renameServer(server,name);
        server.setName(name);
        server.setCn(cn);
        server.setOu(ou);
        server.setHostname(hostname);
        server.setUsername(username);
        server.setKeyfilePath(keyfile);
        if (Configuration.getInstance().USE_PAM)
            userfile = "";
        server.setUserfilePath(userfile);
        server.setExportPath(exportPath);
        server.setStatusPort(Server.STATUS_TYPE_TCPIP);
        server.setStatusInterval(Integer.parseInt(statusinterval));
        server.setSshPort(Integer.parseInt(sshport));
        server.setServerType(serverType);
        server.setVpnPort(Integer.parseInt(vpnport));
        server.setWrapperCommand(wrapperCommand);
        if (authTypeKeyfile)
            server.setAuthType(Server.AUTH_TYPE_KEYFILE);
        else
            server.setAuthType(Server.AUTH_TYPE_PASSWORD);
        server.setVpnProtocol(vpnProtocol);
        server.setVpncc(vpncc);
        server.setVpnccpath(vpnccpath);
        
        server.setVpnNetworkAddress(vpnNetworkAddress);
        server.setVpnSubnetMask(vpnSubnetMask);
        server.setVpnDevice(vpnDevice);
        server.setVpnRedirectGateway(vpnRedirectGateway);
        server.setVpnDuplicateCN(vpnDuplicateCN);
        server.setVpnUser(vpnUser);
        server.setVpnGroup(vpnGroup);
        server.setVpnKeepAlive(vpnKeepAlive);
        
        ServerDAO.getInstance().update(server);
    }

    /**
     * Creates OpenVPN client config files for all users connected to the server
     *
     * @param server The server for which client OpenVpn config files are desired
     */
    public static void createVPNUserConfigFile(Server server) {
        try {
        	Vector<String> serverlist = new Vector<String>();
        	serverlist.add("" + server.getServerid());
            
            Vector<String> userlist = new Vector<String>();
            userlist = UserQueries.getUsersForServer(server.getServerid());
            
            // loop through every (connected) user, and create a config for each one
    		for (String id : userlist){
    		    User user = User.getUserByID(Integer.parseInt(id));
    		    new ClientConfig(user, serverlist);
    		}
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error while creating the OpenVPN user configuration", e);
            new VisualException(rb.getString("dialog.newuser.vpnconfigerror"));
        }
    }
    
    
    /**
     * prepares the filesystem for the server
     * creates a directory for the server
     *
     * @throws Exception
     */
    public static void prepareFilesystem(String servername) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        exportPath = exportPath + "_" + servername + "/";

        if (!new File(exportPath).exists()) {
            boolean success = (new File(exportPath)).mkdirs();
            if (!success)
                throw new Exception(rb.getString("dialog.cert.exporterror"));
        }
    }
    
    
    /**
     * Does what is necessary to rename a server
     *
     * @param server        The server
     * @param newServername The new servername
     */
    private static void renameServer(Server server, String newServername) throws Exception {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        File oldPath = new File(exportPath + "_" + server.getName() + "/");
        File newPath = new File(exportPath + "_" + newServername + "/");

        org.apache.commons.io.FileUtils.copyDirectory(oldPath, newPath);
        
        FileUtils.deleteDirectory(oldPath);
    }
    
    
    
    /**
     * Create an OpenVPN-Serverconfig file for the given server
     *
     * @param server The server for which the config should be created
     */
    public static void createVPNServerConfigFile(Server server) {
        try {
            
            ServerConfig conf = new ServerConfig(server);
            conf.createConfig();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error while creating the OpenVPN server configuration", e);
            new VisualException(rb.getString("dialog.newuser.vpnconfigerror"));
        }
    }
    
}
