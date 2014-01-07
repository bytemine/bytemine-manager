/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
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

import net.bytemine.manager.bean.User;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the User.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class UserDAO {

    private static Logger logger = Logger.getLogger(UserDAO.class.getName());

    private static UserDAO userDAO;
    private static Connection dbConnection;


    private UserDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static UserDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                userDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (userDAO == null)
            userDAO = new UserDAO();
        return userDAO;
    }


    /**
     * creates a new user row in the db
     *
     * @param user The user to create
     */
    public void create(User user) {
        try {
            int nextUserId = getNextUserid();
            user.setUserid(nextUserId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO user(userid, username, password, x509id, cn, ou, yubikeyid) VALUES(?,?,?,?,?,?,?)"
            );
            pst.setInt(1, nextUserId);
            pst.setString(2, user.getUsername());
            pst.setString(3, user.getPassword());
            pst.setInt(4, user.getX509id());
            pst.setString(5, user.getCn());
            pst.setString(6, user.getOu());
            pst.setString(7, user.getYubikeyid());
            pst.executeUpdate();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating user", e);
            new VisualException(rb.getString("error.db.user") + " " + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a user from the db
     * identification by userid
     *
     * @param user The user to load
     * @return the loaded user or null
     */
    public User read(User user) {
        User returnUser = null;
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT username, password, x509id, cn, ou, yubikeyid FROM user WHERE userid=?"
            );
            pst.setInt(1, user.getUserid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                user.setUsername(rs.getString(1));
                user.setPassword(rs.getString(2));
                user.setX509id(rs.getInt(3));
                user.setCn(rs.getString(4));
                user.setOu(rs.getString(5));
                user.setYubikeyid(rs.getString(6));

                returnUser = user;
            }
            rs.close();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading user", e);
            new VisualException(rb.getString("error.db.user") + " " + rb.getString("error.db.read"));
        }

        return returnUser;
    }


    /**
     * updates all variables of the user except the userid
     * identification by userid
     *
     * @param user The user to update
     */
    public void update(User user) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE user SET username=?, password=?, x509id=?, cn=?, ou=?, yubikeyid=? " +
                            "WHERE userid=?"
            );
            pst.setInt(7, user.getUserid());
            pst.setString(1, user.getUsername());
            pst.setString(2, user.getPassword());
            pst.setInt(3, user.getX509id());
            pst.setString(4, user.getCn());
            pst.setString(5, user.getOu());
            pst.setString(6, user.getYubikeyid());
            pst.executeUpdate();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating user", e);
            new VisualException(rb.getString("error.db.user") + " " + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the user from the db
     * identification by userid
     *
     * @param user The user to delete
     */
    public void delete(User user) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM user WHERE userid=?"
            );
            pst.setInt(1, user.getUserid());
            pst.executeUpdate();
            pst.close();

            UserQueries.removeUserFromAllServers(user.getUserid());
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting user", e);
            new VisualException(rb.getString("error.db.user") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the user from the db
     * identification by userid
     *
     * @param userId The user to delete
     */
    public void deleteById(String userId) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM user WHERE userid=?"
            );
            pst.setInt(1, Integer.parseInt(userId));
            pst.executeUpdate();
            pst.close();

            UserQueries.removeUserFromAllServers(Integer.parseInt(userId));
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting user", e);
            new VisualException(rb.getString("error.db.user") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next userid from the db
     *
     * @return int th next user id
     */
    private int getNextUserid() throws Exception {
        int userid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(userid) as maxId from user");
            if (rs.next())
                userid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next userid", e);
            throw e;
        }

        return userid;
    }

}
