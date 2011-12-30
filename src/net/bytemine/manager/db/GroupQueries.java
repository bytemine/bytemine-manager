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
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.GroupToUserEntry;
import net.bytemine.manager.model.GroupToUserModel;
import net.bytemine.utility.StringUtils;


/**
 * SQL-Queries for the group table
 *
 * @author Daniel Rauer
 */
public class GroupQueries {

    private static Logger logger = Logger.getLogger(GroupQueries.class.getName());

    public static final int order_groupid = 0;
    public static final int order_name = 1;


    /**
     * Loads a complete row from the group table
     *
     * @param id The id to load
     * @return a stringarray
     */
    public static String[] getGroupDetails(String id) {
        String[] detail = new String[3];

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT " +
                            "groupid, name, description " +
                            "FROM groups where groupid=?");
            pst.setInt(1, Integer.parseInt(id));
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                detail[0] = rs.getString("groupid");
                detail[1] = rs.getString("name");
                detail[2] = rs.getString("description");
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading group details", e);
        }

        return detail;
    }


    /**
     * Counts all groups
     *
     * @return Integer representing the number of groups
     */
    public static int getGroupCount() {
        int count = 0;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT count(groupid) as number FROM groups");
            ResultSet rs = pst.executeQuery();

            if (rs.next())
                count = rs.getInt("number");

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting group count", e);
        }

        return count;
    }

    /**
     * Gets all groupnames and descriptions existing in the database
     * returns a vector with String[2]
     *
     * @return Vector
     */
    public static Vector<String[]> getAllGroupsAsVector() {
        return GroupQueries.getAllGroupsAsVector(order_groupid);
    }

    /**
     * Gets all groupnames and descriptions existing in the database
     * returns a vector with String[3]: groupid, name, description
     *
     * @param order An integer representing the order
     * @return Vector
     */
    public static Vector<String[]> getAllGroupsAsVector(int order) {
        Vector<String[]> returnList = new Vector<String[]>();

        String orderStr;
        switch (order) {
            case order_groupid:
                orderStr = "groupid";
                break;
            case order_name:
                orderStr = "name, description";
                break;
            default:
                orderStr = "name, description";
                break;
        }

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT groupid, name, description FROM groups ORDER BY " + orderStr);
            while (rs.next()) {
                String[] entry = new String[3];
                entry[0] = rs.getString("groupid");
                entry[1] = rs.getString("name");
                entry[2] = rs.getString("description");
                returnList.add(entry);
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading groups as vector", e);
        }

        return returnList;
    }


    /**
     * Links the given users to the group
     *
     * @param groupidStr The group id
     * @param userids    The ids of the users
     */
    public static void reconnectUsersAndGroup(String groupidStr, Vector<String> userids) throws Exception {
        try {
            int groupid = Integer.parseInt(groupidStr);
            reconnectUsersAndGroup(groupid, userids);
        } catch (Exception e) {
            logger.severe("cannot connect users with group, cause groupid is not an integer");
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }


    /**
     * Links the users to the group
     *
     * @param groupid The group id
     * @param userids The userids to connect
     */
    public static void reconnectUsersAndGroup(int groupid, Vector<String> userids) throws Exception {
        try {
            // delete
            removeGroupFromAllUsers(groupid);

            // save
            PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO groups_user VALUES (?,?)");
            pst2.setInt(1, groupid);

            for (Iterator<String> it = userids.iterator(); it.hasNext();) {
                String userid = it.next();
                pst2.setInt(2, Integer.parseInt(userid));
                pst2.executeUpdate();
            }

            pst2.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding users to this group", e);
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }


    /**
     * Links the user to the group
     *
     * @param groupid The group id
     * @param userid  The user id
     */
    public static void addUserToGroup(int groupid, int userid) {
        try {
            if (!isCombinationExisting(groupid, userid)) {
                DBConnector.getInstance().getConnection().setAutoCommit(false);
                PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                        "INSERT INTO groups_user VALUES (?,?)");
                pst2.setInt(1, groupid);
                pst2.setInt(2, userid);
                pst2.executeUpdate();

                pst2.close();
                DBConnector.getInstance().getConnection().commit();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding a user to this group", e);
        }
    }


    /**
     * Removes the link between the user and the group
     *
     * @param groupid The group id
     * @param userid  The user id
     */
    public static void removeUserFromGroup(String groupid, String userid) throws Exception {
        removeUserFromGroup(Integer.parseInt(groupid), Integer.parseInt(userid));
    }

    /**
     * Removes the link between the user and the group
     *
     * @param groupid The group id
     * @param userid  The user id
     */
    public static void removeUserFromGroup(int groupid, int userid) {
        try {
            if (isCombinationExisting(groupid, userid)) {
                DBConnector.getInstance().getConnection().setAutoCommit(false);
                PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                        "DELETE FROM groups_user WHERE groupid=? and userid=?");
                pst2.setInt(1, groupid);
                pst2.setInt(2, userid);
                pst2.executeUpdate();

                pst2.close();
                DBConnector.getInstance().getConnection().commit();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a user from this group", e);
        }
    }


    /**
     * Checks whether this combination of group and user is already existing
     *
     * @param groupid The groupid
     * @param userid  The userid
     * @return true, if they are already linked
     */
    private static boolean isCombinationExisting(int groupid, int userid) {
        boolean existing = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT groupid, userid FROM groups_user WHERE groupid=? AND userid=?");
            pst.setInt(1, groupid);
            pst.setInt(2, userid);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                existing = true;
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while checking if user is linked to this group", e);
        }

        return existing;
    }


    /**
     * Removes the link between this group and all users
     *
     * @param groupid The id of the group
     */
    public static void removeGroupFromAllUsers(int groupid) {
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM groups_user WHERE groupid=?");
            pst.setInt(1, groupid);
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing a group from all users", e);
        }
    }


    /**
     * Loads all groups connected to the given user
     *
     * @param useridStr The user id
     * @return Vector with groupids as String
     */
    public static Vector<String> getGroupsForUser(String useridStr) {
        try {
            int userid = Integer.parseInt(useridStr);
            return getGroupsForUser(userid);
        } catch (Exception e) {
            logger.severe("given userid is not an integer. returning empty list");
            return new Vector<String>();
        }
    }

    /**
     * Loads all groups connected to the given user
     *
     * @param userid The user id
     * @return Vector with groupids as String
     */
    public static Vector<String> getGroupsForUser(int userid) {
        Vector<String> groupids = new Vector<String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT groupid FROM groups_user " +
                    "WHERE userid = " + userid);
            while (rs.next())
                if (rs.getString("groupid") != null && !"null".equals(rs.getString("groupid")))
                    groupids.add(rs.getString("groupid"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading the groups for user with id " + userid, e);
        }

        return groupids;
    }


    /**
     * Loads all users connected to the given group
     *
     * @param groupidStr The group id
     * @return Vector with usersids as String
     */
    public static Vector<String> getUsersForGroup(String groupidStr) {
        try {
            int groupid = Integer.parseInt(groupidStr);
            return getUsersForGroup(groupid);
        } catch (Exception e) {
            logger.severe("given groupid is not an integer. returning empty list");
            return new Vector<String>();
        }
    }

    /**
     * Loads all users connected to the given group
     *
     * @param groupid The group id
     * @return Vector with userids as String
     */
    public static Vector<String> getUsersForGroup(int groupid) {
        Vector<String> userids = new Vector<String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT userid FROM groups_user " +
                    "WHERE groupid = " + groupid);
            while (rs.next())
                if (!StringUtils.isEmptyOrWhitespaces(rs.getString("userid")))
                    userids.add(rs.getString("userid"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading the users for group with id " + groupid, e);
        }

        return userids;
    }


    /**
     * Loads all users<->group connections
     *
     * @return Vector with GroupToUserEntrys
     */
    public static Vector<GroupToUserEntry> getAllUserToGroupConnections() {
        Vector<GroupToUserEntry> entries = new Vector<GroupToUserEntry>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM groups_user");
            while (rs.next()) {
                String groupid = rs.getString("groupid");
                String userid = rs.getString("userid");
                GroupToUserEntry entry = new GroupToUserEntry(groupid, userid);
                entries.add(entry);
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading all user and group connections", e);
        }

        return entries;
    }


    /**
     * Saves all user<->group connections that have changed
     *
     * @param model A model with all new and to remove connections
     * @throws Exception
     */
    public static void saveUserToGroupConnections(GroupToUserModel model) throws Exception {

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM groups_user WHERE groupid=? AND userid=?");
            for (Iterator<GroupToUserEntry> iter = model.toRemove.iterator(); iter.hasNext();) {
                GroupToUserEntry entry = iter.next();

                pst.setString(1, entry.groupid);
                pst.setString(2, entry.userid);
                pst.executeUpdate();
            }
            pst.close();

            PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO groups_user VALUES(?,?)");
            for (Iterator<GroupToUserEntry> iter = model.toAdd.iterator(); iter.hasNext();) {
                GroupToUserEntry entry = (GroupToUserEntry) iter.next();

                pst2.setString(1, entry.groupid);
                pst2.setString(2, entry.userid);
                pst2.executeUpdate();
            }
            pst2.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error saving user and group connections", e);
            throw e;
        }

    }
    
    
    /**
     * Detects if a group with the given name is already existing
     *
     * @param name The name
     * @return true, if the group is existing
     */
    public static boolean isGroupExisting(String name) {
        boolean existing = true;

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT groupid FROM groups " +
                            "WHERE name=?"
            );
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();
            if (!rs.next())
                existing = false;

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if group is existing", e);
        }

        return existing;
    }
}
