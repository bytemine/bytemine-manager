/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SQL-Queries for licence and update issues
 *
 * @author Daniel Rauer
 */
public class LicenceQueries {

    private static Logger logger = Logger.getLogger(LicenceQueries.class.getName());


    /**
     * Loads the keystore of the update certificates from the database
     *
     * @return byte[] with the keystore, or null
     */
    public static byte[] getKeystore() {
        byte[] keystore = null;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT keystore FROM licence"
            );
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                keystore = rs.getBytes("keystore");
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while retrieving keystore", e);
        }

        return keystore;
    }


    /**
     * Stores the update certificates as keystore
     *
     * @param keystore The keystore
     * @param crtpath  The path to the crt or pem file
     * @param keypath  The path to the key file
     */
    public static void storeKeystore(byte[] keystore, String crtpath, String keypath) {
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);

            PreparedStatement pstDelete = DBConnector.getInstance().getConnection().prepareStatement(
                    "DELETE FROM licence");
            pstDelete.executeUpdate();
            pstDelete.close();
            
            PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                    "INSERT INTO licence (licid, keystore, crtpath, keypath) VALUES(?,?,?,?)"
            );
            pst2.setInt(1, 1);
            pst2.setBytes(2, keystore);
            pst2.setString(3, crtpath);
            pst2.setString(4, keypath);
            pst2.executeUpdate();
            pst2.close();
        
            DBConnector.getInstance().getConnection().commit();
            DBConnector.getInstance().getConnection().setAutoCommit(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while storing keystore", e);
        }
    }

    /**
     * Returns the path to the crt file
     *
     * @return The crt file path
     */
    public static String getCrtPath() {
        return getAttribute("crtpath");
    }

    /**
     * Returns the path to the key file
     *
     * @return The key file path
     */
    public static String getKeyPath() {
        return getAttribute("keypath");
    }

    /**
     * Returns the string value of the given attribute name
     *
     * @param attName The attribute name
     * @return The value stored in the attribute
     */
    private static String getAttribute(String attName) {
        String att = "";
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT " + attName + " FROM licence WHERE licid=1"
            );
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                att = rs.getString(attName);
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while retrieving " + attName, e);
        }

        return att;
    }

}
