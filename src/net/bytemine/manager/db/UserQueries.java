/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.UserOverviewTableModel;
import net.bytemine.utility.StringUtils;


/**
 * SQL-Queries for the user table
 *
 * @author Daniel Rauer
 */
public class UserQueries {

    public static final int order_userid = 0;
    public static final int order_username = 1;

    private static Logger logger = Logger.getLogger(UserQueries.class.getName());

    /**
     * Gets all userids and usernames existing in the database
     * returns a vector with String[2]
     *
     * @return Vector
     */
    public static Vector<String[]> getAllUsersAsVector() {
        return getAllUsersAsVector(null, order_username, "");
    }
    
    /**
     * Gets all userids and usernames matching the given search filter
     * returns a vector with String[2]
     *
     * @param filterString A String that is used as a filter
     * @return Vector
     */
    public static Vector<String[]> getAllUsersFilteredByUsername(String filterString) {
        return getAllUsersAsVector(null, order_username, filterString);
    }

    /**
     * Gets all userids and usernames existing in the database
     * returns a vector with String[2]
     *
     * @param model The UserOverviewModel
     * @return Vector
     */
    public static Vector<String[]> getAllUsersAsVector(UserOverviewTableModel model) {
        return getAllUsersAsVector(model, order_userid, "");
    }


    /**
     * Gets all userids and usernames existing in the database
     * returns a vector with String[2]
     *
     * @param order An integer representing the order:
     *              0=order by userid
     *              1=order by username
     * @return Vector
     */
    public static Vector<String[]> getAllUsersAsVector(int order) {
        return getAllUsersAsVector(null, order, "");
    }


    /**
     * Gets all userids and usernames existing in the database
     * returns a vector with String[2]
     *
     * @param model The UserOverviewModel
     * @return Vector
     */
    public static Vector<String[]> getAllUsersAsVectorForTable(UserOverviewTableModel model) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        Vector<String[]> returnList = new Vector<String[]>();

        String orderStr = "username";
        try {
            int rowNr = 0;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT userid, username, password, x509id FROM user " +
            		"ORDER BY " + orderStr);
            while (rs.next()) {
                String[] entry = new String[4];
                entry[0] = rs.getString("username");
                String password = rs.getString("password");
                if (password == null || "".equals(password))
                    entry[1] = rb.getString("user.overview.notset");
                else
                    entry[1] = rb.getString("user.overview.set");
                entry[2] = (rs.getInt("x509id")) < 1
                        ? rb.getString("user.overview.notset")
                        : rb.getString("user.overview.set");
                entry[3] = rs.getString("userid");
                returnList.add(entry);

                // store the mapping for later retrieval
                if (model != null)
                    model.addIdRowMapping(rowNr + "", rs.getString("userid"));
                rowNr++;
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading users as vector", e);
        }

        return returnList;
    }
    
    
    /**
     * Gets all userids and usernames existing in the database
     * returns a vector with String[2]
     *
     * @param model The UserOverviewModel
     * @param order An integer representing the order:
     *              0=order by userid
     *              1=order by username
     * @param filterString A String that is used as a filter
     * @return Vector
     */
    public static Vector<String[]> getAllUsersAsVector(UserOverviewTableModel model, int order, String filterString) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        Vector<String[]> returnList = new Vector<String[]>();

        String orderStr;
        switch (order) {
            case order_userid:
                orderStr = "userid";
                break;
            case order_username:
                orderStr = "username";
                break;
            default:
                orderStr = "userid";
                break;
        }

        try {
            int rowNr = 0;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT userid, username, password, x509id FROM user " +
                    "WHERE username like '%" + filterString +"%' " +
                    "ORDER BY " + orderStr);
            while (rs.next()) {
                String[] entry = new String[4];
                entry[0] = rs.getString("userid");
                entry[1] = rs.getString("username");
                String password = rs.getString("password");
                if (password == null || "".equals(password))
                    entry[2] = rb.getString("user.overview.notset");
                else
                    entry[2] = rb.getString("user.overview.set");
                entry[3] = (rs.getInt("x509id")) < 1
                        ? rb.getString("user.overview.notset")
                        : rb.getString("user.overview.set");

                returnList.add(entry);

                // store the mapping for later retrieval
                if (model != null)
                    model.addIdRowMapping(rowNr + "", rs.getString("userid"));
                rowNr++;
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading users as vector", e);
        }

        return returnList;
    }


    /**
     * Loads a complete row from the user table
     *
     * @param id The id to load
     * @return a stringarray
     */
    public static String[] getUserDetails(String id) {
        String[] detail = new String[8];

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM user " +
                    "where userid = " + id);
            while (rs.next()) {
                detail[0] = rs.getString("userid");
                detail[1] = rs.getString("username");
                detail[2] = rs.getString("password");
                detail[3] = rs.getString("x509id");
                detail[5] = rs.getString("cn");
                detail[6] = rs.getString("ou");
                detail[7] = rs.getString("yubikeyid");

                if (!StringUtils.isEmptyOrWhitespaces(detail[3]) && !"0".equals(detail[3]) && !"-1".equals(detail[3])) {
                    Statement st2 = DBConnector.getInstance().getConnection().createStatement();
                    ResultSet rs2 = st2.executeQuery("SELECT filename FROM x509 " +
                            "where x509id = " + detail[3]);

                    if (rs2.next())
                        detail[4] = rs2.getString("filename");

                    rs2.close();
                    st2.close();
                }
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading user details", e);
        }

        return detail;
    }


    /**
     * Gets all usernames and userids existing in the database
     * returns a Hashtable
     * used for synchronising the userfile
     *
     * @return Hashtable with usernames as key and userid as value
     */
    public static Hashtable<String, String> getAllUsersAsTable() {
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT username, userid from user");
            while (rs.next())
                if (rs.getString("username") != null)
                    returnTable.put(rs.getString("username"), rs.getString("userid"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading all users as table", e);
        }

        return returnTable;
    }


    /**
     * Removes the link between this user and all servers
     *
     * @param userid The id of the user
     */
    public static void removeUserFromAllServers(int userid) {
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM server_user WHERE userid=?");
            pst.setInt(1, userid);
            pst.executeUpdate();
            
            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a user from all servers", e);
        }
    }

    
    /**
     * Removes the link between this user and this server
     *
     * @param userId The id of the user
     * @param serverId The server id
     */
    public static void removeUserFromServer(String userId, String serverId) {
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM server_user WHERE userid=? AND serverid=?");
            pst.setString(1, userId);
            pst.setString(2, serverId);
            pst.executeUpdate();

            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a user from a server", e);
        }
    }

    

    /**
     * Loads all users connected to the given server
     *
     * @param serveridStr The server id
     * @return Vector with userids as String
     */
    public static Vector<String> getUsersForServer(String serveridStr) {
        try {
            int serverid = Integer.parseInt(serveridStr);
            return getUsersForServer(serverid);
        } catch (Exception e) {
            logger.severe("given serverid is not an integer. returning empty list");
            return new Vector<String>();
        }
    }

    /**
     * Loads all users connected to the given server
     *
     * @param serverid The server id
     * @return Vector with userids as String
     */
    public static Vector<String> getUsersForServer(int serverid) {
        Vector<String> userids = new Vector<String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT userid FROM server_user " +
                    "WHERE serverid = " + serverid);
            while (rs.next())
                userids.add(rs.getString("userid"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading the users for server with id " + serverid, e);
        }

        return userids;
    }
    
    
    
    /**
     * Load a user by x509id
     *
     * @param x509id The x509 ID
     * @return user The user
     */
    public static User getUserByX509id(int x509id) {
        User user = null;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT userid FROM user " +
                    "WHERE x509id = " + x509id);
            if (rs.next()) {
                int userid = rs.getInt("userid");
                user = new User(userid + "");
                user = UserDAO.getInstance().read(user);
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading a user by its x509id", e);
        }

        return user;
    }


    /**
     * Loads all users connected to the given server
     *
     * @param server   The server
     * @param importList if true, put username and userid into hashtable
     *                   if false, put username and password into hashtable
     * @return HashTable with username and userid of each user if importList is true
     *         or username and password if importList is false
     */
    public static Hashtable<String, String> getUserTableForServer(Server server, boolean importList) {
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        try {
            Vector<String> userids = getUsersForServer(server.getServerid());

            // convert Vector to String "2,3,5"
            StringBuffer idList = new StringBuffer();
            for (Iterator<String> it = userids.iterator(); it.hasNext();) {
                String id = it.next();
                if (it.hasNext())
                    idList.append(id + ",");
                else
                    idList.append(id);
            }

            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT userid, username, password, yubikeyid FROM user " +
                            "WHERE userid IN (" + idList + ")");
            ResultSet rs = pst.executeQuery();

            while (rs.next())
                if (importList)
                    returnTable.put(rs.getString("username"), rs.getString("userid"));
                else if (rs.getString("password") != null) {
                    if (rs.getString("yubikeyid") != null && !"".equals(rs.getString("yubikeyid")) && (server.getServerType()==Server.SERVER_TYPE_BYTEMINE_APPLIANCE))
                        returnTable.put(rs.getString("username"), rs.getString("password")+":"+rs.getString("yubikeyid"));
                    else
                        returnTable.put(rs.getString("username"), rs.getString("password"));
                }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting users of the server ", e);
        }

        return returnTable;
    }


    /**
     * Loads all users
     *
     * @param importList if true, put username and userid into hashtable
     *                   if false, put username and password into hashtable
     * @return HashTable with username and userid of each user if importList is true
     *         or username and password if importList is false
     */
    public static Hashtable<String, String> getUserTable(boolean importList) {
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT userid, username, password FROM user");
            ResultSet rs = pst.executeQuery();

            while (rs.next())
                if (importList)
                    returnTable.put(rs.getString("username"), rs.getString("userid"));
                else if (rs.getString("password") != null)
                    returnTable.put(rs.getString("username"), rs.getString("password"));

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting users", e);
        }

        return returnTable;
    }


    /**
     * Counts all users
     *
     * @return Integer representing the number of users
     */
    public static int getUserCount() {
        int count = 0;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT count(userid) as number FROM user");
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                count = rs.getInt("number");
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting user count", e);
        }

        return count;
    }


    /**
     * Links the user to the servers
     *
     * @param useridStr The user id
     * @param serverids The server ids
     */
    public static void reconnectServersAndUser(String useridStr, Vector<String> serverids) throws Exception {
        try {
            int userid = Integer.parseInt(useridStr);
            reconnectServersAndUser(userid, serverids);
        } catch (Exception e) {
            logger.severe("cannot connect servers with user, cause userid is not an integer");
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }


    /**
     * Links the servers to the user
     *
     * @param userid    The user id
     * @param serverids The serverids to connect
     */
    public static void reconnectServersAndUser(int userid, Vector<String> serverids) throws Exception {
        try {
        	// temporary save ips
        	Hashtable<String,String> userServerIp = getIpsFromUserServer(Integer.toString(userid));
        	
            // delete
            removeUserFromAllServers(userid);

            // save
            PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO server_user VALUES (?,?,?)");
            pst2.setInt(2, userid);
            
            for (Iterator<String> it = serverids.iterator(); it.hasNext();) {
                String serverid = (String) it.next();
                pst2.setInt(1, Integer.parseInt(serverid));
                pst2.setString(3, userServerIp.get(serverid));
                pst2.executeUpdate();
            }

            pst2.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding servers to this user", e);
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }


    /**
     * Detects if a user with the given username is already existing
     *
     * @param username The username
     * @return true, if the user is existing
     */
    public static boolean isUserExisting(String username) {
        boolean existing = true;

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT userid FROM user " +
                            "WHERE username=?"
            );
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (!rs.next())
                existing = false;

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if user is existing", e);
        }

        return existing;
    }
    

    /**
     * Detects if a user with the given username is already existing
     *
     * @param username The username
     * @return true, if the user is existing
     */
    public static int getUserId(String username) {
        int userId = -1;

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT userid FROM user " +
                            "WHERE username=?"
            );
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                userId = rs.getInt("userid");

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if user is existing", e);
        }

        return userId;
    }

    
    /**
     * Get all ips for a valid UserServer-Combination
     * 
     * @param userId      The user id
     * @return userServerIp  The hashtable containing the UserIp-Combination
     */
    public static Hashtable<String,String> getIpsFromUserServer(String userId) {
    	
    	Hashtable<String,String> userServerIp = new Hashtable<String,String>();
    	
    	Vector<String[]> allUsers = ServerQueries.getServerOverview(ServerQueries.order_name);

        for (Iterator<String[]> it = allUsers.iterator(); it.hasNext();) {
        	String[] strings = it.next();
        	String ip = ServerQueries.getIpFromUserServer(strings[0],userId);
        	if(ip!=null)
        		userServerIp.put(strings[0], ip);
        	else
        		userServerIp.put(strings[0], "");
        }
        
        return userServerIp;
    }

}
