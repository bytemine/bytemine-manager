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
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Constants;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * SQL-Queries for the x509 table
 *
 * @author Daniel Rauer
 */
public class CRLQueries {

    private static Logger logger = Logger.getLogger(CRLQueries.class.getName());

    /**
     * retrieves the next crlNumber from the db
     *
     * @return int The next crlNumber
     */
    public static int getNextCrlNumber() throws Exception {
        int crlNumber = 1;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT max(crlnumber) AS maxNumber from crl");
            if (rs.next())
                crlNumber = rs.getInt("maxNumber") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next crlNumber", e);
            throw e;
        }

        return crlNumber;
    }


    /**
     * retrieves the highest ID
     *
     * @return int The highest ID
     */
    public static int getMaxCRLId() throws Exception {
        int crlId = -1;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT max(crlid) AS maxId from crl");
            if (rs.next())
                crlId = rs.getInt("maxId");

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting highest crlId", e);
            throw e;
        }

        return crlId;
    }


    /**
     * Returns the revocation serials matching the crlId
     *
     * @param crlId The crlId
     * @return List of ids
     * @throws Exception
     */
    public static Vector<String> getRevocationSerials(String crlId) throws Exception {
        Vector<String> serials = new Vector<String>();
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT serial " +
                            "FROM crlentry " +
                            "WHERE crlid=" + crlId);
            while (rs.next()) {
                String serial = rs.getString("serial");
                serials.add(serial);
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting all serials", e);
            throw e;
        }

        return serials;
    }


    /**
     * Returns the revocation serials
     *
     * @return List of ids
     * @throws Exception
     */
    public static Vector<String> getRevocationSerials() throws Exception {
        Vector<String> serials = new Vector<String>();
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT serial " +
                            "FROM crlentry");
            while (rs.next()) {
                String serial = rs.getString("serial");
                serials.add(serial);
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting all serials", e);
            throw e;
        }

        return serials;
    }


    /**
     * Decides whether the certificate with the given serial is revoked or not
     *
     * @param serial The serial to test
     * @return true, if certificate is revoked
     * @throws Exception
     */
    public static boolean isCertificateRevoked(String serial) throws Exception {
        boolean revoked = false;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT crlid " +
                            "FROM crlentry " +
                            "WHERE serial=" + serial);
            if (rs.next()) {
                int crlId = rs.getInt("crlid");
                if (crlId > -1)
                    revoked = true;
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error querying if certificate is revoked", e);
            throw e;
        }

        return revoked;
    }
    
    
    
    /**
     * Gets a crlentryid by a x509 serial number
     *
     * @param serial The x509 serial 
     * @return the id of the crlentry, or -1
     * @throws Exception
     */
    public static int getCRLEntryId(String serial) throws Exception {
        int crlEntryId = -1;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT crlentryid " +
                            "FROM crlentry " +
                            "WHERE serial=" + serial);
            if (rs.next()) {
                crlEntryId = rs.getInt("crlentryid");
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error querying a crlentryid by a x509 serial number", e);
            throw e;
        }

        return crlEntryId;
    }


    /**
     * Reads some details from the latest CRL
     *
     * @return String[] with crl details
     */
    public static String[] getCrlDetails() {
        String[] details = new String[7];
        try {

            int maxCrlId = getMaxCRLId();
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT crlid, crlnumber, filename, path, issuer, " +
                            "createdate, validfrom, nextupdate " +
                            "FROM crl WHERE crlid=?"
            );

            pst.setInt(1, maxCrlId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                details[0] = rs.getInt(1) + "";
                details[1] = rs.getInt(2) + "";
                details[2] =
                        (rs.getString(4).endsWith("/") ? rs.getString(4) : rs.getString(4) + "/")
                                + rs.getString(3);
                details[3] = rs.getString(5);
                details[4] = rs.getString(6);
                details[5] = rs.getString(7);
                details[6] = rs.getString(8);
            }
            rs.close();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error while loading crl details", e);
            new VisualException(rb.getString("error.db.crl") + rb.getString("error.db.read"));
        }

        return details;
    }


    /**
     * Retrieves the entries of the given CRL
     * @param crlId The ID of the CRL
     */
    public static Vector<String[]> getCRLEntries(String crlId) {
        Vector<String[]> crlEntries = new Vector<String[]>();
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT serial, revocationdate, x509id, username " +
                            "FROM crlentry " +
                            "WHERE crlid=" + crlId + " ORDER BY username");
            while (rs.next()) {
                String[] entry = new String[4];
                entry[0] = rs.getString("serial");
                try {
                    Date revocationDate = Constants.parseDetailedFormat(rs.getString("revocationdate"));
                    entry[1] = Constants.getShowFormatForCurrentLocale().format(revocationDate);
                } catch (Exception e) {
                    logger.warning("revocationdate cannot be formatted or is null");
                    entry[1] = rs.getString("revocationdate");
                }
                entry[2] = rs.getString("x509id");
                entry[3] = rs.getString("username");
                crlEntries.add(entry);
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting crl entries", e);
        }

        return crlEntries;
    }


}
