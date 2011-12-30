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
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.bean.X509;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the X509.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class X509DAO {

    private static Logger logger = Logger.getLogger(X509DAO.class.getName());

    private static X509DAO x509DAO;
    private static Connection dbConnection;


    private X509DAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static X509DAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                x509DAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (x509DAO == null)
            x509DAO = new X509DAO();
        return x509DAO;
    }


    /**
     * creates a new x509 row in the db
     *
     * @param x509 The certificate to create
     */
    public void create(X509 x509) {
        try {
            int nextX509Id = getNextX509Id();

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO x509(x509id, version, filename, path, serial, issuer, subject, content, contentdisplay, " +
                    "certserialized, key, keycontent, type, createdate, validfrom, validto, generated, userid) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            );
            pst.setInt(1, nextX509Id);
            pst.setString(2, x509.getVersion());
            pst.setString(3, x509.getFileName());
            pst.setString(4, x509.getPath());
            pst.setString(5, x509.getSerial());
            pst.setString(6, x509.getIssuer());
            pst.setString(7, x509.getSubject());
            pst.setString(8, x509.getContent());
            pst.setString(9, x509.getContentDisplay());
            pst.setString(10, x509.getCertSerialized());
            pst.setString(11, x509.getKey());
            pst.setString(12, x509.getKeyContent());
            pst.setInt(13, x509.getType());
            pst.setString(14, x509.getCreateDate());
            pst.setString(15, x509.getValidFrom());
            pst.setString(16, x509.getValidTo());
            pst.setBoolean(17, x509.isGenerated());
            pst.setInt(18, x509.getUserId());
            pst.executeUpdate();
            pst.close();

            x509.setX509id(nextX509Id);
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating X509 " + x509.getFileName(), e);
            new VisualException(rb.getString("error.db.cert") + " " + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a x509 from the db
     * identification by x509id
     *
     * @param x509 The x509 to load
     * @return the x509
     */
    public X509 read(X509 x509) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT version, filename, path, serial, issuer, subject, " +
                            "content, contentdisplay, certserialized, key, keycontent, type, " +
                            "createdate, validfrom, validto, generated, userid " +
                            "FROM x509 WHERE x509id=?"
            );

            pst.setInt(1, x509.getX509id());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                x509.setVersion(rs.getString(1));
                x509.setFileName(rs.getString(2));
                x509.setPath(rs.getString(3));
                x509.setSerial(rs.getString(4));
                x509.setIssuer(rs.getString(5));
                x509.setSubject(rs.getString(6));
                x509.setContent(rs.getString(7));
                x509.setContentDisplay(rs.getString(8));
                x509.setCertSerialized(rs.getString(9));
                x509.setKey(rs.getString(10));
                x509.setKeyContent(rs.getString(11));
                x509.setType(rs.getInt(12));
                x509.setCreateDate(rs.getString(13));
                x509.setValidFrom(rs.getString(14));
                x509.setValidTo(rs.getString(15));
                x509.setGenerated(rs.getBoolean(16));
                x509.setUserId(rs.getInt(17));

                rs.close();
                pst.close();

                return x509;
            } else {
                rs.close();
                pst.close();
            }

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading X509", e);
            new VisualException(rb.getString("error.db.cert") + " " + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the x509 except the x509id
     * identification by x509id
     *
     * @param x509 The x509 to update
     */
    public void update(X509 x509) {
        try {

            dbConnection.setAutoCommit(false);
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE x509 SET " +
                            "version=?, filename=?, path=?, serial=?, issuer=?, subject=?, " +
                            "content=?, contentdisplay=?, certserialized=?, " +
                            "key=?, keycontent=?, type=?, createdate=?, " +
                            "validfrom=?, validto=?, generated=?, userid=? " +
                            "WHERE x509id=?"
            );
            pst.setInt(18, x509.getX509id());
            pst.setString(1, x509.getVersion());
            pst.setString(2, x509.getFileName());
            pst.setString(3, x509.getPath());
            pst.setString(4, x509.getSerial());
            pst.setString(5, x509.getIssuer());
            pst.setString(6, x509.getSubject());
            pst.setString(7, x509.getContent());
            pst.setString(8, x509.getContentDisplay());
            pst.setString(9, x509.getCertSerialized());
            pst.setString(10, x509.getKey());
            pst.setString(11, x509.getKeyContent());
            pst.setInt(12, x509.getType());
            pst.setString(13, x509.getCreateDate());
            pst.setString(14, x509.getValidFrom());
            pst.setString(15, x509.getValidTo());
            pst.setBoolean(16, x509.isGenerated());
            pst.setInt(17, x509.getUserId());
            pst.executeUpdate();
            pst.close();

            dbConnection.commit();
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating X509 " + x509.getFileName(), e);
            new VisualException(rb.getString("error.db.cert") + " " + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the x509 from the db
     * identification by x509id
     *
     * @param x509 The x509 to delete
     */
    public void delete(X509 x509) {
        try {

            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM x509 WHERE x509id=?"
            );
            pst.setInt(1, x509.getX509id());
            pst.executeUpdate();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting X509 " + x509.getX509id(), e);
            new VisualException(rb.getString("error.db.cert") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the x509 from the db
     * identification by x509id
     *
     * @param x509Id The x509 to delete
     */
    public void deleteById(String x509Id) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM x509 WHERE x509id=?"
            );
            pst.setInt(1, Integer.parseInt(x509Id));
            pst.executeUpdate();
            pst.close();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting X509", e);
            new VisualException(rb.getString("error.db.cert") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next x509id from the db
     *
     * @return int the next x509id
     */
    private int getNextX509Id() throws Exception {
        int x509id = 0;
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT max(x509id) as maxId from x509"
            );
            ResultSet rs = pst.executeQuery();

            if (rs.next())
                x509id = rs.getInt("maxId") + 1;

            rs.close();
            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next x509id", e);
            throw e;
        }

        return x509id;
    }


    /**
     * checks whether a root certificate is existing or not
     *
     * @return true, if a root certificate is existing
     */
    public static boolean isRootCertExisting() {
        boolean certExisting = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT x509id from x509 where type=?"
            );
            pst.setInt(1, X509.X509_TYPE_ROOT);
            ResultSet rs = pst.executeQuery();

            if (rs.next())
                certExisting = true;

            rs.close();
            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if a root certificate is existing", e);
        }

        return certExisting;
    }


    /**
     * checks if certificates exist
     *
     * @return true, if at least one certificate is in the database
     */
    public static boolean isCertificatesExisting() {
        boolean certExisting = false;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT x509id FROM x509"
            );
            ResultSet rs = pst.executeQuery();

            if (rs.next())
                certExisting = true;

            rs.close();
            pst.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if a certificate is existing", e);
        }

        return certExisting;
    }

}
