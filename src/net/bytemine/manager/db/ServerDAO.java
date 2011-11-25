/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Server;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the Server.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class ServerDAO {

    private static Logger logger = Logger.getLogger(ServerDAO.class.getName());

    private static ServerDAO serverDAO;
    private static Connection dbConnection;


    private ServerDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static ServerDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                serverDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (serverDAO == null)
            serverDAO = new ServerDAO();
        return serverDAO;
    }


    /**
     * creates a new server row in the db
     *
     * @param server The server to create
     */
    public void create(Server server) {
        try {
            logger.info("start creating server");
            int nextServerId = getNextServerid();
            server.setServerid(nextServerId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO server(serverid, name, hostname, authtype, username, keyfilepath, " +
                    "userfilepath, exportpath, statusport, statustype, statusinterval, sshport, " +
                    "servertype, wrappercommand, x509id, vpnport, vpnprotocol, vpncc, vpnccpath, " +
                    "vpnNetworkAddress, vpnSubnetMask, vpnDevice, vpnRedirectGateway," +
                    "vpnDuplicateCN, vpnUser, vpnGroup, vpnKeepAlive, cn, ou) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            );
            pst.setInt(1, nextServerId);
            pst.setString(2, server.getName());
            pst.setString(3, server.getHostname());
            pst.setInt(4, server.getAuthType());
            pst.setString(5, server.getUsername());
            pst.setString(6, server.getKeyfilePath());
            pst.setString(7, server.getUserfilePath());
            pst.setString(8, server.getExportPath());
            pst.setInt(9, server.getStatusPort());
            pst.setInt(10, server.getStatusType());
            pst.setInt(11, server.getStatusInterval());
            pst.setInt(12, server.getSshPort());
            pst.setInt(13, server.getServerType());
            pst.setString(14, server.getWrapperCommand());
            pst.setInt(15, server.getX509id());
            pst.setInt(16, server.getVpnPort());
            pst.setInt(17, server.getVpnProtocol());
            pst.setBoolean(18, server.getVpncc());
            pst.setString(19, server.getVpnccpath());
            pst.setString(20, server.getVpnNetworkAddress());
            pst.setInt(21, server.getVpnSubnetMask());
            pst.setInt(22, server.getVpnDevice());
            pst.setBoolean(23, server.getVpnRedirectGateway());
            pst.setBoolean(24, server.getVpnDuplicateCN());
            pst.setString(25, server.getVpnUser());
            pst.setString(26, server.getVpnGroup());
            pst.setString(27, server.getVpnKeepAlive());
            pst.setString(28, server.getCn());
            pst.setString(29, server.getOu());
            pst.executeUpdate();
            pst.close();

            logger.info("end creating server");
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating server", e);
            new VisualException(rb.getString("error.db.server") + " " + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a server from the db
     * identification by serverid
     *
     * @param server The server to load
     * @return the server
     */
    public Server read(Server server) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT serverid, name, hostname, authtype, username, keyfilepath, " +
                    "userfilepath, exportpath, statusport, statustype, statusinterval, sshport, " +
                    "servertype, wrappercommand, x509id, vpnport, vpnprotocol, vpncc, vpnccpath, " +
                    "vpnNetworkAddress, vpnSubnetMask, vpnDevice, vpnRedirectGateway, " +
                    "vpnDuplicateCN, vpnUser, vpnGroup, vpnKeepAlive, cn, ou " +
                    "FROM server WHERE serverid=?"
            );
            pst.setInt(1, server.getServerid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                server.setName(rs.getString(2));
                server.setHostname(rs.getString(3));
                server.setAuthType(rs.getInt(4));
                server.setUsername(rs.getString(5));
                server.setKeyfilePath(rs.getString(6));
                server.setUserfilePath(rs.getString(7));
                server.setExportPath(rs.getString(8));
                server.setStatusPort(rs.getInt(9));
                server.setStatusType(rs.getInt(10));
                server.setStatusInterval(rs.getInt(11));
                server.setSshPort(rs.getInt(12));
                server.setServerType(rs.getInt(13));
                server.setWrapperCommand(rs.getString(14));
                server.setX509id(rs.getInt(15));
                server.setVpnPort(rs.getInt(16));
                server.setVpnProtocol(rs.getInt(17));
                server.setVpncc(rs.getBoolean(18));
                server.setVpnccpath(rs.getString(19));
                server.setVpnNetworkAddress(rs.getString(20));
                server.setVpnSubnetMask(rs.getInt(21));
                server.setVpnDevice(rs.getInt(22));
                server.setVpnRedirectGateway(rs.getBoolean(23));
                server.setVpnDuplicateCN(rs.getBoolean(24));
                server.setVpnUser(rs.getString(25));
                server.setVpnGroup(rs.getString(26));
                server.setVpnKeepAlive(rs.getString(27));
                server.setCn(rs.getString(28));
                server.setOu(rs.getString(29));
                
                rs.close();

                pst.close();

                return server;
            } else {
                rs.close();
                pst.close();
            }

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading server", e);
            new VisualException(rb.getString("error.db.server") + " " + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the server except the serverid
     * identification by serverid
     *
     * @param server The server to update
     */
    public void update(Server server) {
        try {
            dbConnection.setAutoCommit(false);
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE server SET " +
                            "name=?, " +
                            "hostname=?, " +
                            "authtype=?, " +
                            "username=?, " +
                            "keyfilepath=?, " +
                            "userfilepath=?, " +
                            "exportpath=?, " +
                            "statusport=?, " +
                            "statustype=?, " + "" +
                            "statusinterval=?, " +
                            "sshport=?, " +
                            "servertype=?, " +
                            "wrappercommand=?, " +
                            "x509id=?, " +
                            "vpnport=?, " +
                            "vpnprotocol=?, " +
                            "vpncc=?, " +
                            "vpnccpath=?, " +
                            "vpnNetworkAddress=?, " +
                            "vpnSubnetMask=?, " +
                            "vpnDevice=?, " +
                            "vpnRedirectGateway=?, " +
                            "vpnDuplicateCN=?, " +
                            "vpnUser=?, " +
                            "vpnGroup=?, " +
                            "vpnKeepAlive=?, " +
                            "cn=?, " +
                            "ou=? " +
                            "WHERE serverid=?"
            );

            pst.setString(1, server.getName());
            pst.setString(2, server.getHostname());
            pst.setInt(3, server.getAuthType());
            pst.setString(4, server.getUsername());
            pst.setString(5, server.getKeyfilePath());
            pst.setString(6, server.getUserfilePath());
            pst.setString(7, server.getExportPath());
            pst.setInt(8, server.getStatusPort());
            pst.setInt(9, server.getStatusType());
            pst.setInt(10, server.getStatusInterval());
            pst.setInt(11, server.getSshPort());
            pst.setInt(12, server.getServerType());
            pst.setString(13, server.getWrapperCommand());
            pst.setInt(14, server.getX509id());
            pst.setInt(15, server.getVpnPort());
            pst.setInt(16, server.getVpnProtocol());
            pst.setBoolean(17, server.getVpncc());
            pst.setString(18, server.getVpnccpath());
            pst.setString(19, server.getVpnNetworkAddress());
            pst.setInt(20, server.getVpnSubnetMask());
            pst.setInt(21, server.getVpnDevice());
            pst.setBoolean(22, server.getVpnRedirectGateway());
            pst.setBoolean(23, server.getVpnDuplicateCN());
            pst.setString(24, server.getVpnUser());
            pst.setString(25, server.getVpnGroup());
            pst.setString(26, server.getVpnKeepAlive());
            pst.setString(27, server.getCn());
            pst.setString(28, server.getOu());
            
            pst.setInt(29, server.getServerid());
            
            pst.executeUpdate();
            pst.close();
            dbConnection.commit();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating server", e);
            new VisualException(rb.getString("error.db.server") + " " + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the server from the db
     * identification by serverid
     *
     * @param server The server to delete
     */
    public void delete(Server server) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM server WHERE serverid=?"
            );
            pst.setInt(1, server.getServerid());
            pst.executeUpdate();
            pst.close();
            
            ServerQueries.removeServerFromAllUsers(server.getServerid());
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting server", e);
            new VisualException(rb.getString("error.db.server") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the server from the db
     * identification by serverid
     *
     * @param serverId The server to delete
     */
    public void deleteById(String serverId) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM server WHERE serverid=?"
            );
            pst.setInt(1, Integer.parseInt(serverId));
            pst.executeUpdate();
            pst.close();

            ServerQueries.removeServerFromAllUsers(Integer.parseInt(serverId));
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting server", e);
            new VisualException(rb.getString("error.db.server") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next serverid from the db
     *
     * @return int the next server id
     */
    private int getNextServerid() throws Exception {
        int serverid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(serverid) as maxId from server");
            if (rs.next())
                serverid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next serverid", e);
            throw e;
        }

        return serverid;
    }

}
