/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.Group;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the Group.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class GroupDAO {

    private static Logger logger = Logger.getLogger(GroupDAO.class.getName());

    private static GroupDAO groupDAO;
    private static Connection dbConnection;


    private GroupDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static GroupDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                groupDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (groupDAO == null)
            groupDAO = new GroupDAO();
        return groupDAO;
    }


    /**
     * creates a new group row in the db
     *
     * @param group The group to create
     */
    public void create(Group group) {
        try {
            int nextGroupId = getNextGroupid();
            group.setGroupid(nextGroupId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO groups(groupid, name, description) " +
                    "VALUES(?,?,?)"
            );
            pst.setInt(1, nextGroupId);
            pst.setString(2, group.getName());
            pst.setString(3, group.getDescription());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating group", e);
            new VisualException(rb.getString("error.db.group") + " " + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a group from the db
     * identification by groupid
     *
     * @param group The group to load
     * @return the loaded group or null
     */
    public Group read(Group group) {
        Group returnGroup = null;
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT name, description FROM groups WHERE groupid=?"
            );
            pst.setInt(1, group.getGroupid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                group.setName(rs.getString(1));
                group.setDescription(rs.getString(2));

                returnGroup = group;
            }
            rs.close();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading group", e);
            new VisualException(rb.getString("error.db.group") + " " + rb.getString("error.db.read"));
        }

        return returnGroup;
    }


    /**
     * updates all variables of the group except the groupid
     * identification by groupid
     *
     * @param group The group to update
     */
    public void update(Group group) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE groups SET name=?, description=? " +
                            "WHERE groupid=?"
            );
            pst.setInt(3, group.getGroupid());
            pst.setString(1, group.getName());
            pst.setString(2, group.getDescription());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating group", e);
            new VisualException(rb.getString("error.db.group") + " " + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the group from the db
     * identification by groupid
     *
     * @param group The group to delete
     */
    public void delete(Group group) {
        try {
            int groupid = group.getGroupid();
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM groups WHERE groupid=?"
            );
            pst.setInt(1, groupid);
            pst.executeUpdate();
            pst.close();

            GroupQueries.removeGroupFromAllUsers(groupid);
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting group", e);
            new VisualException(rb.getString("error.db.group") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the group from the db
     * identification by groupid
     *
     * @param groupid The group to delete
     */
    public void deleteById(String groupid) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM groups WHERE groupid=?"
            );
            pst.setInt(1, Integer.parseInt(groupid));
            pst.executeUpdate();
            pst.close();

            GroupQueries.removeGroupFromAllUsers(Integer.parseInt(groupid));
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting group", e);
            new VisualException(rb.getString("error.db.group") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next groupid from the db
     *
     * @return int the next group id
     */
    private int getNextGroupid() throws Exception {
        int groupid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(groupid) as maxId from groups");
            if (rs.next())
                groupid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next groupid", e);
            throw e;
        }

        return groupid;
    }

}
