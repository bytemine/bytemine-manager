/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.bean;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.db.ServerDAO;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * Holds all data of the servers
 *
 * @author Daniel Rauer
 */
public class Server {

    private static Logger logger = Logger.getLogger(Server.class.getName());

    public static final int AUTH_TYPE_PASSWORD = 0;
    public static final int AUTH_TYPE_KEYFILE = 1;

    public static final int STATUS_TYPE_TCPIP = 0;
    private static final int STATUS_TYPE_UNIX_DOMAIN = 1;

    public static final int SERVER_TYPE_REGULAR_OPENVPN = 0;
    public static final int SERVER_TYPE_BYTEMINE_APPLIANCE = 1;

    public static final int SERVER_OPENVPN_PROTOCOL_TCP = 0;
    public static final int SERVER_OPENVPN_PROTOCOL_UDP = 1;
    
    public static final int SERVER_OPENVPN_DEVICE_TUN = 0;
    private static final int SERVER_OPENVPN_DEVICE_TUN0 = 1;
    private static final int SERVER_OPENVPN_DEVICE_TUN1 = 2;
    
    private static final int OPTION_DISABLED = 0;
    private static final int OPTION_ENABLED = 1;
    
    private int serverid;
    private String name;
    private String cn;
    private String ou;
    private String hostname;
    private int authType;
    private String username;
    private String keyfilePath;
    private String userfilePath;
    private String exportPath;
    private int statusPort;
    private int statusType;
    private int statusInterval;
    private int sshPort;
    private int serverType;
    private String wrapperCommand;
    private int x509id;
    private int vpnPort;
    private int vpnProtocol;
    private boolean vpncc;
    private String vpnccpath;
    private String vpnNetworkAddress;
    private int vpnSubnetMask;
    private int vpnDevice;
    private boolean vpnRedirectGateway;
    private boolean vpnDuplicateCN;
    private String vpnUser;
    private String vpnGroup;
    private String vpnKeepAlive;


    // indicates wether the object is persistent (serverDAO!=null) or not
    private ServerDAO serverDAO = null;


    public Server(String id) {
        this.serverid = Integer.parseInt(id);

        serverDAO = ServerDAO.getInstance();
    }


    public Server(String name, String cn, String ou, String hostname, int authType, String username,
                  String keyfilePath, String userfilePath, String exportPath,
                  int statusPort, int statusType, int statusInterval, int sshPort,
                  int serverType, String wrapperCommand, int x509id, int vpnPort, 
                  int vpnProtocol, boolean vpncc, String vpnccpath, String vpnNetworkAddress,
                  int vpnSubnetMask, int vpnDevice, boolean vpnRedirectGateway,
                  boolean vpnDuplicateCN, String vpnUser, String vpnGroup, String vpnKeepAlive) {
        initialize(name, cn, ou, hostname, authType, username, keyfilePath, userfilePath, exportPath,
                statusPort, statusType, statusInterval, sshPort, serverType,
                wrapperCommand, x509id, vpnPort, vpnProtocol, vpncc, vpnccpath,
                vpnNetworkAddress, vpnSubnetMask, vpnDevice, vpnRedirectGateway,
                vpnDuplicateCN, vpnUser, vpnGroup, vpnKeepAlive
                );
    }


    private void initialize(String name, String cn, String ou, String hostname, int authType, String username,
                            String keyfilePath, String userfilePath, String exportPath, int statusPort, int statusType,
                            int statusInterval, int sshPort, int serverType, String wrapperCommand, int x509id,
                            int vpnPort, int vpnProtocol, boolean vpncc, String vpnccpath, String vpnNetworkAddress,
                            int vpnSubnetMask, int vpnDevice, boolean vpnRedirectGateway,
                            boolean vpnDuplicateCN, String vpnUser, String vpnGroup, String vpnKeepAlive) {

        this.name = name;
        this.cn = cn;
        this.ou = ou;
        this.hostname = hostname;
        this.authType = authType;
        this.username = username;
        this.keyfilePath = keyfilePath;
        this.userfilePath = userfilePath;
        this.exportPath = exportPath;
        this.statusPort = statusPort;
        this.statusType = statusType;
        this.statusInterval = statusInterval;
        this.sshPort = sshPort;
        this.serverType = serverType;
        this.wrapperCommand = wrapperCommand;
        this.x509id = x509id;
        this.vpnPort = vpnPort;
        this.vpnProtocol = vpnProtocol;
        this.vpncc = vpncc;
        this.vpnccpath = vpnccpath;
        this.vpnNetworkAddress = vpnNetworkAddress;
        this.vpnSubnetMask = vpnSubnetMask;
        this.vpnDevice = vpnDevice;
        this.vpnRedirectGateway = vpnRedirectGateway;
        this.vpnDuplicateCN = vpnDuplicateCN;
        this.vpnUser = vpnUser;
        this.vpnGroup = vpnGroup;
        this.vpnKeepAlive = vpnKeepAlive;
        
        // prepare certificate directory
        try {
            ServerAction.prepareFilesystem(this.name);
        } catch (Exception e) {
            logger.warning("cannot prepare filesystem for server: "+e.toString());
        }
        
        serverDAO = ServerDAO.getInstance();
        // write to db
        serverDAO.create(this);
    }


    /**
     * deletes the server from the db
     */
    public void delete() {
        logger.info("deleting server with id " + this.getServerid());
        serverDAO.delete(this);
    }
    
    public static String transformVpnDeviceToString(int device) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        switch (device) {
            case SERVER_OPENVPN_DEVICE_TUN:
                return rb.getString("server.vpn.device.tun");
            case SERVER_OPENVPN_DEVICE_TUN0:
                return rb.getString("server.vpn.device.tun0");
            case SERVER_OPENVPN_DEVICE_TUN1:
                return rb.getString("server.vpn.device.tun1");
            default:
                return rb.getString("server.type.password");
        }
    }
    
    public static String transformAuthTypeToString(int type) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        switch (type) {
            case AUTH_TYPE_PASSWORD:
                return rb.getString("server.type.password");
            case AUTH_TYPE_KEYFILE:
                return rb.getString("server.type.keyfile");
            default:
                return rb.getString("server.type.password");
        }
    }


    public static String transformStatusTypeToString(int type) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        return getRBString(type, rb);
    }

    private static String getRBString(int type, ResourceBundle rb) {
        switch (type) {
            case STATUS_TYPE_TCPIP:
                return rb.getString("server.statustype.tcpip");
            case STATUS_TYPE_UNIX_DOMAIN:
                return rb.getString("server.statustype.unix");
            default:
                return rb.getString("server.statustype.tcpip");
        }
    }


    public static String transformVpnProtocolToString(int type) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        switch (type) {
            case SERVER_OPENVPN_PROTOCOL_TCP:
                return rb.getString("server.vpn.protocol.tcp");
            case SERVER_OPENVPN_PROTOCOL_UDP:
                return rb.getString("server.vpn.protocol.udp");
            default:
                return rb.getString("server.vpn.protocol.tcp");
        }
    }
    
    
    public static String transformStatusTypeToString(String typeStr) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        int type = Server.STATUS_TYPE_TCPIP;
        try {
            type = Integer.parseInt(typeStr);
        } catch (NumberFormatException e) {
            logger.warning("status type is not an int!");
        }
        return getRBString(type, rb);
    }
    
    public static boolean transformBooleanOption(String option) {
    	int type = Server.OPTION_DISABLED;

    	// new server
    	if (option == null)
    		return false;
    	
    	// update server
        try {
            type = Integer.parseInt(option);
        } catch (Exception e) {
        	logger.severe("option is not an int!: "+option);
        }
        
    	return type == Server.OPTION_ENABLED;
    }

    /**
     * loads the server from the db
     * identification by serverid
     *
     * @param serverid The serverid of the server to load
     * @return the loaded user
     */
    public static Server getServerById(int serverid) {
        Server server = new Server(Integer.toString(serverid));
        server.setServerid(serverid);
        server = ServerDAO.getInstance().read(server);

        if (server != null)
            server.serverDAO = ServerDAO.getInstance();
        return server;
    }
    
    
    
    /*
    * getter and setter of the attributes
    */


    public int getAuthType() {
        return authType;
    }

    public void setAuthType(int authType) {
        this.authType = authType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getKeyfilePath() {
        return keyfilePath;
    }

    public void setKeyfilePath(String keyfilePath) {
        this.keyfilePath = keyfilePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServerDAO getServerDAO() {
        return serverDAO;
    }

    public void setServerDAO(ServerDAO serverDAO) {
        this.serverDAO = serverDAO;
    }

    public int getServerid() {
        return serverid;
    }

    public void setServerid(int serverid) {
        this.serverid = serverid;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public int getStatusPort() {
        return statusPort;
    }

    public void setStatusPort(int statusPort) {
        this.statusPort = statusPort;
    }

    public String getUserfilePath() {
        return userfilePath;
    }

    public void setUserfilePath(String userfilePath) {
        this.userfilePath = userfilePath;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getX509id() {
        return x509id;
    }

    public void setX509id(int x509id) {
        this.x509id = x509id;
    }

    public int getStatusInterval() {
        return statusInterval;
    }

    public void setStatusInterval(int statusInterval) {
        this.statusInterval = statusInterval;
    }

    public int getStatusType() {
        return statusType;
    }

    public void setStatusType(int statusType) {
        this.statusType = statusType;
    }

    public int getServerType() {
        return serverType;
    }

    public void setServerType(int serverType) {
        this.serverType = serverType;
    }

    public String getWrapperCommand() {
        return wrapperCommand;
    }

    public void setWrapperCommand(String wrapperCommand) {
        this.wrapperCommand = wrapperCommand;
    }

    public int getVpnPort() {
        return this.vpnPort;
    }

    public void setVpnPort(int vpnPort) {
        this.vpnPort = vpnPort;
    }

    public int getVpnProtocol() {
    	return this.vpnProtocol;
    }
    
    public void setVpnProtocol(int vpnProtocol) {
    	this.vpnProtocol = vpnProtocol;
    }
    
    public boolean getVpncc() {
    	return this.vpncc;
    }
    
    public void setVpncc(boolean vpncc) {
    	this.vpncc = vpncc;
    }
    
    public String getVpnccpath() {
    	return this.vpnccpath;
    }
    
    public void setVpnccpath(String vpnccpath) {
    	this.vpnccpath = vpnccpath;
    }
    
    public String getVpnNetworkAddress() {
    	return this.vpnNetworkAddress;
    }
    
    public void setVpnNetworkAddress(String vpnNetworkAddress) {
    	this.vpnNetworkAddress = vpnNetworkAddress;
    }
    
    public int getVpnSubnetMask() {
    	return this.vpnSubnetMask;
    }
    
    public void setVpnSubnetMask(int vpnSubnetMask) {
    	this.vpnSubnetMask = vpnSubnetMask;
    }
    
    public void setVpnDevice(int vpnDevice) {
        this.vpnDevice = vpnDevice;
    }
    
    public int getVpnDevice() {
        return this.vpnDevice;
    }
    
    public void setVpnRedirectGateway(boolean vpnRedirectGateway) {
        this.vpnRedirectGateway = vpnRedirectGateway;
    }
    
    public boolean getVpnRedirectGateway() {
        return this.vpnRedirectGateway;
    }
    
    public void setVpnDuplicateCN(boolean vpnDuplicateCN) {
        this.vpnDuplicateCN = vpnDuplicateCN;
    }
    
    public boolean getVpnDuplicateCN() {
        return this.vpnDuplicateCN;
    }
    
    public String getVpnUser() {
        return this.vpnUser;
    }
    
    public void setVpnUser(String vpnUser) {
        this.vpnUser = vpnUser;
    }

    public String getVpnGroup() {
        return this.vpnGroup;
    }
    
    public void setVpnGroup(String vpnGroup) {
        this.vpnGroup = vpnGroup;
    }   
    
    public String getVpnKeepAlive() {
        return this.vpnKeepAlive;
    }
    
    public void setVpnKeepAlive(String vpnKeepAlive) {
        this.vpnKeepAlive = vpnKeepAlive;
    }
    
    public void addUser(int userid) {
        ServerQueries.addUserToServer(this.serverid, userid);
    }

    public void removeUser(int userid) {
        ServerQueries.removeUserFromServer(this.serverid, userid);
    }
    
    public void setCn(String cn) {
        this.cn = cn;
    }   
    
    public String getCn() {
        return this.cn;
    }
    
    public void setOu(String ou) {
        this.ou = ou;
    }   
    
    public String getOu() {
        return this.ou;
    }

}
