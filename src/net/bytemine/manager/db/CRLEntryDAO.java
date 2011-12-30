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

import net.bytemine.manager.bean.CRLEntry;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the CRLEntry.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class CRLEntryDAO {

    private static Logger logger = Logger.getLogger(CRLEntryDAO.class.getName());

    private static CRLEntryDAO crlEntryDAO;
    private static Connection dbConnection;


    private CRLEntryDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static CRLEntryDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                crlEntryDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (crlEntryDAO == null)
            crlEntryDAO = new CRLEntryDAO();
        return crlEntryDAO;
    }


    /**
     * creates a new crlEntry row in the db
     *
     * @param crlEntry The crlEntry to create
     */
    public void create(CRLEntry crlEntry) {
        try {
            int nextCrlEntryId = getNextCrlEntryid();
            crlEntry.setCrlEntryid(nextCrlEntryId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO crlentry VALUES(?,?,?,?,?,?)"
            );
            pst.setInt(1, nextCrlEntryId);
            pst.setString(2, crlEntry.getSerial());
            pst.setString(3, crlEntry.getRevocationDate());
            pst.setInt(4, crlEntry.getX509id());
            pst.setInt(5, crlEntry.getCrlid());
            pst.setString(6, crlEntry.getUsername());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating crlEntry", e);
            new VisualException(rb.getString("error.db.crlentry") + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a crlEntry from the db
     * identification by crlentryid
     *
     * @param crlEntry The crlEntry to load
     * @return the crlEntry
     */
    public CRLEntry read(CRLEntry crlEntry) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT serial, revocationdate, crlid, x509id, username " +
                            "FROM crlentry WHERE crlentryid=?"
            );

            pst.setInt(1, crlEntry.getCrlEntryid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                crlEntry.setSerial(rs.getString(1));
                crlEntry.setRevocationDate(rs.getString(2));
                crlEntry.setCrlid(rs.getInt(3));
                crlEntry.setX509id(rs.getInt(4));
                crlEntry.setUsername(rs.getString(5));

                rs.close();
                pst.close();

                return crlEntry;
            } else {
                rs.close();
                pst.close();
            }

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading crlEntry", e);
            new VisualException(rb.getString("error.db.crlentry") + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the crlentry except the crlentryid
     * identification by crlentryid
     *
     * @param crlEntry The crlentry to update
     */
    public void update(CRLEntry crlEntry) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE crlentry SET " +
                            "serial=?, revocationdate=?, crlid=?, x509id=?, username=? " +
                            "WHERE crlentryid=?"
            );
            pst.setInt(6, crlEntry.getCrlEntryid());
            pst.setString(1, crlEntry.getSerial());
            pst.setString(2, crlEntry.getRevocationDate());
            pst.setInt(3, crlEntry.getCrlid());
            pst.setInt(4, crlEntry.getX509id());
            pst.setString(5, crlEntry.getUsername());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating crlentry", e);
            new VisualException(rb.getString("error.db.crlentry") + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the crlEntry from the db
     * identification by crlEntryId
     *
     * @param crlEntry The crlEntry to delete
     */
    public void delete(CRLEntry crlEntry) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM crlentry WHERE crlentryid=?"
            );
            pst.setInt(1, crlEntry.getCrlEntryid());
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting crlentry", e);
            new VisualException(rb.getString("error.db.crlentry") + rb.getString("error.db.delete"));
        }
    }

    /**
     * deletes the crlEntry from the db
     * identification by crlEntryId
     *
     * @param crlEntryId The user to delete
     */
    public void deleteById(String crlEntryId) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM crlentry WHERE crlEntryId=?"
            );
            pst.setInt(1, Integer.parseInt(crlEntryId));
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting crlentry", e);
            new VisualException(rb.getString("error.db.crlentry") + rb.getString("error.db.delete"));
        }
    }

    /**
     * retrieves the next crlentryid from the db
     *
     * @return int The next crlentry id
     */
    private int getNextCrlEntryid() throws Exception {
        int crlid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(crlentryid) AS maxId from crlentry");
            if (rs.next())
                crlid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next crlentryid", e);
            throw e;
        }

        return crlid;
    }

}
