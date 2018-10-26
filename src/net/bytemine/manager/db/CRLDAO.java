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
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.CRL;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the User.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class CRLDAO {

    private static Logger logger = Logger.getLogger(CRLDAO.class.getName());

    private static CRLDAO crlDAO;
    private static Connection dbConnection;


    private CRLDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static CRLDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                crlDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (crlDAO == null)
            crlDAO = new CRLDAO();
        return crlDAO;
    }


    /**
     * creates a new CRL row in the db
     *
     * @param crl The CRL to create
     */
    public void create(CRL crl) {
        try {
            int nextCrlId = getNextCrlid();
            crl.setCrlid(nextCrlId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO crl(crlid, crlnumber, version, filename, path, issuer, " +
                    "content, contentdisplay, crlserialized, createdate, validfrom, nextupdate) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
            );
            pst.setInt(1, nextCrlId);
            pst.setInt(2, crl.getCrlNumber());
            pst.setString(3, crl.getVersion());
            pst.setString(4, crl.getFileName());
            pst.setString(5, crl.getPath());
            pst.setString(6, crl.getIssuer());
            pst.setString(7, crl.getContent());
            pst.setString(8, crl.getContentDisplay());
            pst.setString(9, crl.getCrlSerialized());
            pst.setString(10, crl.getCreateDate());
            pst.setString(11, crl.getValidFrom());
            pst.setString(12, crl.getNextUpdate());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a CRL from the db
     * identification by crlid
     *
     * @param crl The CRL to load
     * @return the CRL
     */
    public CRL read(CRL crl) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT crlnumber, version, filename, path, issuer, " +
                            "content, contentdisplay, crlserialized, " +
                            "createdate, validfrom, nextupdate " +
                            "FROM crl WHERE crlid=?"
            );

            pst.setInt(1, crl.getCrlid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                crl.setCrlNumber(rs.getInt(1));
                crl.setVersion(rs.getString(2));
                crl.setFileName(rs.getString(3));
                crl.setPath(rs.getString(4));
                crl.setIssuer(rs.getString(5));
                crl.setContent(rs.getString(6));
                crl.setContentDisplay(rs.getString(7));
                crl.setCrlSerialized(rs.getString(8));
                crl.setCreateDate(rs.getString(9));
                crl.setValidFrom(rs.getString(10));
                crl.setNextUpdate(rs.getString(11));

                rs.close();
                pst.close();

                return crl;
            }
            rs.close();
            pst.close();

        } catch (SQLException e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the CRL except the crlid
     * identification by crlid
     *
     * @param crl The CRL to update
     */
    public void update(CRL crl) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE crl SET " +
                            "crlnumber=?, version=?, filename=?, path=?, issuer=?, " +
                            "content=?, contentdisplay=?, crlserialized=?, " +
                            "createdate=?, validfrom=?, nextupdate=? " +
                            "WHERE crlid=?"
            );
            pst.setInt(12, crl.getCrlid());
            pst.setInt(1, crl.getCrlNumber());
            pst.setString(2, crl.getVersion());
            pst.setString(3, crl.getFileName());
            pst.setString(4, crl.getPath());
            pst.setString(5, crl.getIssuer());
            pst.setString(6, crl.getContent());
            pst.setString(7, crl.getContentDisplay());
            pst.setString(8, crl.getCrlSerialized());
            pst.setString(9, crl.getCreateDate());
            pst.setString(10, crl.getValidFrom());
            pst.setString(11, crl.getNextUpdate());
            pst.executeUpdate();
            pst.close();
        } catch (SQLException e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the CRL from the db
     * identification by crlid
     *
     * @param crl The CRL to delete
     */
    public void delete(CRL crl) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM crl WHERE crlid=?"
            );
            pst.setInt(1, crl.getCrlid());
            pst.executeUpdate();
            pst.close();
        } catch (SQLException e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the user from the db
     * identification by userid
     *
     * @param crlId The user to delete
     */
    public void deleteById(String crlId) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM crl WHERE crlid=?"
            );
            pst.setInt(1, Integer.parseInt(crlId));
            pst.executeUpdate();
            pst.close();
        } catch (NumberFormatException e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.delete"));
        } catch (SQLException e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting crl", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next crlid from the db
     *
     * @return int The next crl id
     */
    private int getNextCrlid() throws Exception {
        int crlid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(crlid) AS maxId from crl");
            if (rs.next())
                crlid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting next crlid", e);
            throw e;
        }

        return crlid;
    }

}
