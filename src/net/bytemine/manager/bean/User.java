/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.util.logging.Logger;

import net.bytemine.crypto.utility.CryptoUtils;
import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.UserDAO;


/**
 * Holds all data of the user
 *
 * @author Daniel Rauer
 */
public class User {

    private static Logger logger = Logger.getLogger(User.class.getName());

    private int userid;
    private String username;
    private String password;
    private String cn;
    private String ou;
    private int x509id;

    // indicates wether the object is persistent (userDAO!=null) or not
    private UserDAO userDAO = null;


    private User() {
    }


    public User(String id) {
        this.userid = Integer.parseInt(id);

        userDAO = UserDAO.getInstance();
    }


    public User(String username, int x509id) {
        try {
            initialize(username, null, x509id, false, "", "");
        } catch (Exception e) {
        }
    }

    public User(String username, String password, int x509id)
            throws Exception {
        initialize(username, password, x509id, true, "", "");
    }

    public User(String username, String password, int x509id, boolean cryptPassword)
            throws Exception {
        initialize(username, password, x509id, cryptPassword, "", "");
    }

    public User(String username, String password, int x509id, boolean cryptPassword, String cn, String ou)
            throws Exception {
        initialize(username, password, x509id, cryptPassword, cn, ou);
    }


    private void initialize(String username, String password, int x509id, boolean cryptPassword, String cn, String ou)
            throws Exception {
        logger.info("creating new user: " + username);

        this.username = username;
        this.x509id = x509id;
        this.cn = cn;
        this.ou = ou;

        if (password != null) {
            if (cryptPassword) {
                try {
                    // crypt the password
                    this.password = CryptoUtils.unixCrypt(password);
                } catch (Exception e) {
                    this.password = null;
                }
            } else {
                // do not crypt the password 
                this.password = password;
            }
        } else {
            this.password = null;
        }
        userDAO = UserDAO.getInstance();
        // write to db
        userDAO.create(this);

        // prepare certificate directory
        UserAction.prepareFilesystem(this.username);
    }


    /**
     * deletes the user from the db
     */
    public void delete() {
        userDAO.delete(this);
    }


    /**
     * @param newPassword the new password
     */
    public void updatePassword(String newPassword) throws Exception {

        if (newPassword != null) {
            try {
                // crypt the password
                this.password = CryptoUtils.unixCrypt(newPassword);
            } catch (Exception e) {
                this.password = null;
            }
        } else
            this.password = null;

        // only update if object is already persistent
        if (userDAO != null)
            userDAO.update(this);
    }


    /**
     * @param newPassword the new password
     */
    public void updatePasswordWithoutCrypt(String newPassword) {
        if (newPassword != null) {
            this.password = newPassword;

            // only update if object is already persistent
            if (userDAO != null) {
                userDAO.update(this);
            }
        }
    }


    /**
     * loads the user from the db
     * identification by userid
     *
     * @param userid The userid of the user to load
     * @return the loaded user
     */
    public static User getUserByID(int userid) {
        User user = new User();
        user.setUserid(userid);
        user = UserDAO.getInstance().read(user);

        if (user != null)
            user.userDAO = UserDAO.getInstance();
        return user;
    }


    /*
    * getter and setter of the attributes
    */


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
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


    public String getCn() {
        return cn;
    }


    public void setCn(String cn) {
        this.cn = cn;
    }


    public String getOu() {
        return ou;
    }


    public void setOu(String ou) {
        this.ou = ou;
    }


    public void addServer(int serverid) {
        ServerQueries.addUserToServer(serverid, this.userid);
    }

    public void removeServer(int serverid) {
        ServerQueries.removeUserFromServer(serverid, this.userid);
    }


}
