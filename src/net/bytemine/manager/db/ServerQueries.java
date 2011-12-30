/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.util.Hashtable;
import java.util.Enumeration;
import java.sql.PreparedStatement;

import javax.swing.JTextField;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Server;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.ServerOverviewTableModel;


/**
 * SQL-Queries for the server table
 *
 * @author Daniel Rauer
 */
public class ServerQueries {

    private static Logger logger = Logger.getLogger(ServerQueries.class.getName());

    public static final int order_serverid = 0;
    public static final int order_name = 1;

    /**
     * Gets some data from the server table, where hostname is matching the given search filter
     * returns a vector with String[]
     *
     * @param filterString A String that is used as a filter
     * @return Vector
     */
    public static Vector<String[]> getAllServersFilteredByName(String filterString) {
        return getServerOverview(null, order_name, filterString);
    }
    
    /**
     * Loads some data from the server table
     *
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServerOverview() {
        return getServerOverview(null, order_name, "");
    }
    
    /**
     * Loads some data from the server table, filtered by name 
     *
     * @param filterString A String 
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServersFilteredByName(String filterString) {
        return getServerOverview(null, order_name, filterString);
    }

    /**
     * Loads some data from the server table
     *
     * @param model the table model
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServerOverview(ServerOverviewTableModel model) {
        return getServerOverview(model, order_serverid, "");
    }


    /**
     * Loads some data from the server table
     *
     * @param order The order of the servers
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServerOverview(int order) {
        return getServerOverview(null, order, "");
    }


    /**
     * Loads some data from the server table
     *
     * @param model the table model
     * @param order The order of the servers
     * @param filterString A String used as a filter
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServerOverview(ServerOverviewTableModel model, int order, String filterString) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Vector<String[]> all = new Vector<String[]>();

        String orderStr;
        switch (order) {
            case order_serverid:
                orderStr = "serverid";
                break;
            case order_name:
                orderStr = "name, hostname";
                break;
            default:
                orderStr = "serverid";
                break;
        }

        try {
            int rowNr = 0;
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT serverid, name, hostname, x509id FROM server " +
                    "WHERE name like '%" + filterString +"%' OR " + 
                    "hostname like '%" + filterString +"%' " +
                    "ORDER BY " + orderStr
            );
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String[] row = new String[4];
                row[0] = rs.getString("serverid");
                row[1] = rs.getString("name");
                row[2] = rs.getString("hostname");
                row[3] = (rs.getInt("x509id")) < 1
                        ? rb.getString("server.overview.notset")
                        : rb.getString("server.overview.set");
                all.add(row);

                // store the mapping for later retrieval
                if (model != null)
                    model.addIdRowMapping(rowNr + "", rs.getString("serverid"));
                rowNr++;
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading server overview", e);
        }

        return all;
    }

    
    /**
     * Loads some data from the server table
     *
     * @param model the table model
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getServerOverviewForTable(ServerOverviewTableModel model) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Vector<String[]> all = new Vector<String[]>();

        String orderStr = "name";
        try {
            int rowNr = 0;
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT serverid, name, hostname, x509id FROM server " +
                    "ORDER BY " + orderStr
            );
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String[] row = new String[4];
                row[0] = rs.getString("name");
                row[1] = rs.getString("hostname");
                row[2] = (rs.getInt("x509id")) < 1
                        ? rb.getString("server.overview.notset")
                        : rb.getString("server.overview.set");
                row[3] = rs.getString("serverid");
                all.add(row);

                // store the mapping for later retrieval
                if (model != null)
                    model.addIdRowMapping(rowNr + "", rs.getString("serverid"));
                rowNr++;
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading server overview", e);
        }

        return all;
    }


    /**
     * Loads a complete row from the server table
     *
     * @param id The id to load
     * @return a stringarray
     */
    public static String[] getServerDetails(String id) {
        String[] detail = new String[29];

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT " +
                            "serverid, name, hostname, authtype, username, keyfilepath, " +
                            "userfilepath, exportpath, statusport, statustype, statusinterval, " +
                            "sshport, servertype, wrappercommand, x509id, vpnport, vpnprotocol, " +
                            "vpncc, vpnccpath, vpnNetworkAddress, vpnSubnetMask, vpnDevice, " +
                            "vpnRedirectGateway, vpnDuplicateCN, vpnUser, vpnGroup, vpnKeepAlive, cn, ou " +
                            "FROM server where serverid=?");
            pst.setInt(1, Integer.parseInt(id));
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                detail[0] = rs.getString("serverid");
                detail[1] = rs.getString("name");
                detail[2] = rs.getString("hostname");
                detail[3] = rs.getString("authtype");
                detail[4] = rs.getString("username");
                detail[5] = rs.getString("keyfilepath");
                detail[6] = rs.getString("userfilepath");
                detail[7] = rs.getString("exportpath");
                detail[8] = rs.getString("statusport");
                detail[9] = rs.getString("statustype");
                detail[10] = rs.getString("statusinterval");
                detail[11] = rs.getString("sshport");
                detail[12] = rs.getString("servertype");
                detail[13] = rs.getString("wrappercommand");
                detail[14] = rs.getString("x509id");
                detail[15] = rs.getString("vpnport");
                detail[16] = rs.getString("vpnprotocol");
                detail[17] = rs.getString("vpncc");
                detail[18] = rs.getString("vpnccpath");
                detail[19] = rs.getString("vpnNetworkAddress");
                detail[20] = rs.getString("vpnSubnetMask");
                detail[21] = rs.getString("vpnDevice");
                detail[22] = rs.getString("vpnRedirectGateway");
                detail[23] = rs.getString("vpnDuplicateCN");
                detail[24] = rs.getString("vpnUser");
                detail[25] = rs.getString("vpnGroup");
                detail[26] = rs.getString("vpnKeepAlive");
                detail[27] = rs.getString("cn");
                detail[28] = rs.getString("ou");
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading server details", e);
        }

        return detail;
    }


    /**
     * Counts all servers
     *
     * @return Integer representing the number of servers
     */
    public static int getServerCount() {
        int count = 0;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT count(serverid) as number FROM server");
            ResultSet rs = pst.executeQuery();

            if (rs.next())
                count = rs.getInt("number");

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting server count", e);
        }

        return count;
    }
    
    
    /**
     * Load a server by x509id
     *
     * @param x509id The x509 ID
     * @return server The server
     */
    public static Server getServerByX509id(int x509id) {
        Server server = null;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT serverid FROM server " +
                    "WHERE x509id = " + x509id);
            if (rs.next()) {
                int serverid = rs.getInt("serverid");
                server = new Server(serverid + "");
                server = ServerDAO.getInstance().read(server);
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading a server by its x509id", e);
        }

        return server;
    }



    /**
     * Links the given users to the server
     *
     * @param serveridStr The server id
     * @param userids     The ids of the users
     */
    public static void reconnectUsersAndServer(String serveridStr, Vector<String> userids) throws Exception {
        try {
            int serverid = Integer.parseInt(serveridStr);
            reconnectUsersAndServer(serverid, userids);
        } catch (Exception e) {
            logger.severe("cannot connect users with server, cause serverid is not an integer");
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }


    /**
     * Links the users to the server
     *
     * @param serverid The server id
     * @param userids  The userids to connect
     */
    public static void reconnectUsersAndServer(int serverid, Vector<String> userids) throws Exception {
        try {
            // delete
            removeServerFromAllUsers(serverid);

            // save
            PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO server_user VALUES (?,?,?)");
            pst2.setInt(1, serverid);

            for (Iterator<String> it = userids.iterator(); it.hasNext();) {
                String userid = (String) it.next();
                pst2.setInt(2, Integer.parseInt(userid));
                pst2.setString(3, "");
                pst2.executeUpdate();
            }

            pst2.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding users to this server", e);
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }

    
    /**
     * Links the user to the server
     *
     * @param serverid The server id
     * @param userid   The user id
     */
    public static void addUserToServer(String serverid, String userid) {
        try {
            int sid = Integer.parseInt(serverid);
            int uid = Integer.parseInt(userid);
            addUserToServer(sid, uid);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "error parsing server and user IDs", e);
        }
    }

    /**
     * Links the user to the server
     *
     * @param serverid The server id
     * @param userid   The user id
     */
    public static void addUserToServer(int serverid, int userid) {
        try {
            if (!isCombinationExisting(serverid, userid)) {
                DBConnector.getInstance().getConnection().setAutoCommit(false);
                PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                        "INSERT INTO server_user VALUES (?,?,?)");
                pst2.setInt(1, serverid);
                pst2.setInt(2, userid);
                pst2.setString(3, "");
                pst2.executeUpdate();

                pst2.close();
                DBConnector.getInstance().getConnection().commit();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding a user to this server", e);
        }
    }


    /**
     * Removes the link between the user and the server
     *
     * @param serverid The server id
     * @param userid   The user id
     */
    public static void removeUserFromServer(int serverid, int userid) {
        try {
            if (isCombinationExisting(serverid, userid)) {
                DBConnector.getInstance().getConnection().setAutoCommit(false);
                PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                        "DELETE FROM server_user WHERE serverid=? and userid=?");
                pst2.setInt(1, serverid);
                pst2.setInt(2, userid);
                pst2.executeUpdate();

                pst2.close();
                DBConnector.getInstance().getConnection().commit();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a user from this server", e);
        }
    }


    /**
     * Checks whether this combination of server and user is already existing
     *
     * @param serverid The serverid
     * @param userid   The userid
     * @return true, if they are already linked
     */
    private static boolean isCombinationExisting(int serverid, int userid) {
        boolean existing = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT serverid, userid FROM server_user WHERE serverid=? AND userid=?");
            pst.setInt(1, serverid);
            pst.setInt(2, userid);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                existing = true;
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while checking if user is linked to this server", e);
        }

        return existing;
    }


    /**
     * Removes the link between this server and all users
     *
     * @param serverid The id of the server
     */
    public static void removeServerFromAllUsers(int serverid) {
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM server_user WHERE serverid=?");
            pst.setInt(1, serverid);
            pst.executeUpdate();

            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a server from all users", e);
        }
    }


    /**
     * Loads all servers connected to the given user
     *
     * @param useridStr The user id
     * @return Vector with serverids as String
     */
    public static Vector<String> getServersForUser(String useridStr) {
        try {
            int userid = Integer.parseInt(useridStr);
            return getServersForUser(userid);
        } catch (Exception e) {
            logger.severe("given userid is not an integer. returning empty list");
            return new Vector<String>();
        }
    }

    /**
     * Loads all servers connected to the given user
     *
     * @param userid The user id
     * @return Vector with serverids as String
     */
    public static Vector<String> getServersForUser(int userid) {
        Vector<String> serverids = new Vector<String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT serverid FROM server_user " +
                    "WHERE userid = " + userid);
            while (rs.next())
                if (rs.getString("serverid") != null && !"null".equals(rs.getString("serverid")))
                    serverids.add(rs.getString("serverid"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading the servers for user with id " + userid, e);
        }

        return serverids;
    }
    
    /**
     * Detects if a server with the given name is already existing
     *
     * @param name The servername
     * @return true, if the server is existing
     */
    public static boolean isServerExisting(String name) {
        boolean existing = true;

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT serverid FROM server " +
                            "WHERE name=?"
            );
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();
            if (!rs.next())
                existing = false;

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if server is existing", e);
        }

        return existing;
    }
    
    
    /**
     * Takes UserIp-Hashtable and assign them to the given server
     * 
     * @param serverId      The server id
     * @param userServerIp  The hashtable containing the UserIp-Combination
     */
    public static void reassignIpToUserServer(String serverId, Hashtable<String,JTextField> userServerIp) {

    	int sId = -1;
    	int uId = -1;
    	Object userId = null;
    	String ip = "";
    	
    	for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();) {
    		try {
    			userId = e.nextElement();
    			ip = (userServerIp.get(userId)).getText();
    		} catch(Exception ex) {
    			logger.log(Level.SEVERE, "error while processing UserServerIp-Hashtable", ex);
    		}
    		
    		try {
    			sId = Integer.parseInt(serverId);
    			uId = Integer.parseInt(userId.toString());
    		} catch (Exception ex) {
    			logger.log(Level.SEVERE, "error while parsing uId and sId", ex);
    		}
    		addIpToUserServer(sId, uId, ip);
    	}
    }
    
    
    /**
     * Add a Ip to the given UserServer-Combination
     * 
     * @param serverid The server id
     * @param userid   The user id
     * @param userIp   The ip for the combination
     */
    public static void addIpToUserServer(int serverid, int userid, String userIp) {
    	
    	if(isCombinationExisting(serverid,userid)) {
    		
    		try {
    			PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
    				"UPDATE server_user set ip=? where serverid=? AND userid=?");
    			pst.setString(1, userIp);
    			pst.setInt(2, serverid);
    			pst.setInt(3, userid);
    			pst.executeUpdate();
                pst.close();
    			
    		} catch(Exception e) {
    			logger.log(Level.SEVERE, "error while inserting the ip to the db", e);
    		}
    		
    	}
    }
    
    
    /**
     * Get the Ip of a given UserServer-Combination
     * 
     * @param serverid The server id
     * @param userid   The user id
     * 
     * @return The ip
     */
    public static String getIpFromUserServer(String serverid, String userid) {
    	int sId, uId;
    	try {
    		sId = Integer.parseInt(serverid);
    		uId = Integer.parseInt(userid);
    		return getIpFromUserServer(sId,uId);
    	} catch(Exception e) {
    		logger.log(Level.SEVERE, "error while parsing serverId and userId", e);
    	}
    	return null;
    }
    
    
    /**
     * Get the Ip of a given UserServer-Combination
     * 
     * @param serverid The server id
     * @param userid   The user id
     * 
     * @return The ip
     */
    public static String getIpFromUserServer(int serverid, int userid) {
    	
    	String ip = null;
    	
    	if(isCombinationExisting(serverid,userid)) {
    		
    		try {
    			PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
    				"SELECT ip FROM server_user where serverid=? AND userid=?");
    			pst.setInt(1, serverid);
    			pst.setInt(2, userid);
    			ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                	ip = rs.getString("ip");
                }
                rs.close();
                pst.close();
                
    		} catch(Exception e) {
    			ip=null;
    			logger.log(Level.SEVERE, "error while inserting the ip to the db", e);
    		}
    		
    	}
    	
    	return ip;
    }
}