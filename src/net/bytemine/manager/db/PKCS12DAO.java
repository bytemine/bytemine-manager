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

import net.bytemine.manager.bean.PKCS12;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the PKCS12.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class PKCS12DAO {

    private static Logger logger = Logger.getLogger(PKCS12DAO.class.getName());

    private static PKCS12DAO pkcs12DAO;
    private static Connection dbConnection;


    private PKCS12DAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static PKCS12DAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                pkcs12DAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (pkcs12DAO == null)
            pkcs12DAO = new PKCS12DAO();
        return pkcs12DAO;
    }


    /**
     * creates a new pkcs12 row in the db
     *
     * @param pkcs12 The pkcs12 to create
     */
    public void create(PKCS12 pkcs12) {
        try {
            int nextPKCS12Id = getNextPKCS12id();
            pkcs12.setPkcs12id(nextPKCS12Id);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO pkcs12(pkcs12id, friendlyname, password, content, x509id) " +
                    "VALUES(?,?,?,?,?)"
            );
            pst.setInt(1, nextPKCS12Id);
            pst.setString(2, pkcs12.getFriendlyName());
            pst.setString(3, pkcs12.getPassword());
            pst.setString(4, pkcs12.getContent());
            pst.setInt(5, pkcs12.getX509id());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating pkcs12", e);
            new VisualException(rb.getString("error.db.pkcs12") + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a pkcs12 from the db
     * identification by pkcs12id
     *
     * @param pkcs12 The pkcs12 to load
     * @return the pkcs12
     */
    public PKCS12 read(PKCS12 pkcs12) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT friendlyName, password, content, x509id FROM pkcs12 WHERE pkcs12id=?"
            );
            pst.setInt(1, pkcs12.getPkcs12id());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                pkcs12.setFriendlyName(rs.getString(1));
                pkcs12.setPassword(rs.getString(2));
                pkcs12.setContent(rs.getString(3));
                pkcs12.setX509id(rs.getInt(4));

                rs.close();
                pst.close();

                return pkcs12;
            } else {
                rs.close();
                pst.close();
            }

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading pkcs12", e);
            new VisualException(rb.getString("error.db.pkcs12") + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the pkcs12 except the pkcs12id
     * identification by pkcs12id
     *
     * @param pkcs12 The pkcs12 to update
     */
    public void update(PKCS12 pkcs12) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE pkcs12 SET friendlyname=?, password=?, content=?, x509id=? " +
                            "WHERE pkcs12id=?"
            );
            pst.setInt(4, pkcs12.getPkcs12id());
            pst.setString(1, pkcs12.getFriendlyName());
            pst.setString(2, pkcs12.getPassword());
            pst.setString(3, pkcs12.getContent());
            pst.setInt(4, pkcs12.getX509id());
            pst.executeUpdate();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating pkcs12", e);
            new VisualException(rb.getString("error.db.pkcs12") + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the pkcs12 from the db
     * identification by pkcs12id
     *
     * @param pkcs12 The pkcs12 to delete
     */
    public void delete(PKCS12 pkcs12) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM pkcs12 WHERE pkcs12id=?"
            );
            pst.setInt(1, pkcs12.getPkcs12id());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting pkcs12", e);
            new VisualException(rb.getString("error.db.pkcs12") + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the pkcs12 from the db
     * identification by pkcs12id
     *
     * @param pkcs12Id The pkcs12 to delete
     */
    public void deleteById(String pkcs12Id) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM pkcs12 WHERE pkcs12id=?"
            );
            pst.setInt(1, Integer.parseInt(pkcs12Id));
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting pkcs12", e);
            new VisualException(rb.getString("error.db.pkcs12") + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next pkcs12id from the db
     *
     * @return int th next pkcs12 id
     */
    private int getNextPKCS12id() throws Exception {
        int pkcs12id = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(pkcs12id) as maxId from pkcs12");
            if (rs.next())
                pkcs12id = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next pkcs12id", e);
            throw e;
        }

        return pkcs12id;
    }

}
