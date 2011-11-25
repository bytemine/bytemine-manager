/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.util.logging.Logger;

import net.bytemine.crypto.utility.CryptoUtils;
import net.bytemine.manager.db.PKCS12DAO;


/**
 * Holds all data of the pkcs12
 *
 * @author Daniel Rauer
 */
public class PKCS12 {

    private static Logger logger = Logger.getLogger(PKCS12.class.getName());

    private int pkcs12id;
    private String friendlyName;
    private String password;
    private String content;
    private int x509id;

    // indicates wether the object is persistent (pkcs12DAO!=null) or not
    private PKCS12DAO pkcs12DAO = null;


    private PKCS12() {
    }


    public PKCS12(String id) {
        this.pkcs12id = Integer.parseInt(id);

        pkcs12DAO = PKCS12DAO.getInstance();
    }


    public PKCS12(String friendlyName, String password, String content, int x509id) {

        initialize(friendlyName, password, content, x509id);
    }


    private void initialize(String friendlyName, String password, String content, int x509id) {
        logger.info("creating new pkcs12: " + friendlyName);

        this.friendlyName = friendlyName;
        this.content = content;
        this.x509id = x509id;

        if (password != null) {
            try {
                // crypt the password
                this.password = CryptoUtils.unixCrypt(password);
            } catch (Exception e) {
                this.password = null;
            }
        } else {
            this.password = null;
        }
        pkcs12DAO = PKCS12DAO.getInstance();
        // write to db
        pkcs12DAO.create(this);

    }


    /**
     * deletes the user from the db
     */
    public void delete() {
        pkcs12DAO.delete(this);
    }


    /**
     * loads the pkcs12 from the db
     * identification by pkcs12id
     *
     * @param pkcs12id The pkcs12id of the pkcs12 to load
     * @return the loaded pkcs12
     */
    public static PKCS12 getPKCS12ByID(int pkcs12id) {
        PKCS12 pkcs12 = new PKCS12();
        pkcs12.setPkcs12id(pkcs12id);
        pkcs12 = PKCS12DAO.getInstance().read(pkcs12);

        if (pkcs12 != null)
            pkcs12.pkcs12DAO = PKCS12DAO.getInstance();
        return pkcs12;
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

    public int getPkcs12id() {
        return pkcs12id;
    }

    public void setPkcs12id(int pkcs12id) {
        this.pkcs12id = pkcs12id;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getX509id() {
        return x509id;
    }

    public void setX509id(int x509id) {
        this.x509id = x509id;
    }

}
