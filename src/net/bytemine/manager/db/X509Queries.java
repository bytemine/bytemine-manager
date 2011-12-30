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
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.model.X509OverviewTableModel;
import net.bytemine.manager.utility.X509Utils;


/**
 * SQL-Queries for the x509 table
 *
 * @author Daniel Rauer
 */
public class X509Queries {

    private static Logger logger = Logger.getLogger(X509Queries.class.getName());

    /**
     * Loads all data from the x509 table
     *
     * @param model the table model
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getX509Overview(X509OverviewTableModel model) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Vector<String[]> all = new Vector<String[]>();
        
        try {
            Vector<String> revokationSerials = CRLQueries.getRevocationSerials();
            
            // check if the user wants revoked certificates to be displayed
            boolean showRevoked = true;
            if(ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_CR_X509) != null)
                showRevoked = ConfigurationQueries.getValue(ConfigurationQueries.GUI_SHOW_CR_X509).equals("true");

            int rowNr = 0;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            
            ResultSet rs = st.executeQuery(
           			"SELECT x509id, subject, type, createdate, validfrom, validto, serial " +
           			"FROM x509 ORDER BY type, x509id");
            while (rs.next()) {
                boolean isRevoked = false;
                String[] row = new String[5];
                row[0] = X509Utils.getCnFromSubject(rs.getString("subject"));
                row[1] = X509.transformTypeToString(rs.getInt("type"));
                
                try {
                    Date createDate = Constants.parseDetailedFormat(rs.getString("createdate"));
                    row[2] = Constants.getShowFormatForCurrentLocale().format(createDate);
                } catch (Exception e) {
                    logger.warning("createdate cannot be formatted or is null");
                    row[2] = rs.getString("createdate");
                }
                try {
                    String validity = X509.transformValidityToString(
                            rs.getString("validfrom"), rs.getString("validto")
                    );
                    String serial = rs.getString("serial");
                    if (revokationSerials.contains(serial)) {
                    	isRevoked = true;
                        validity = rb.getString("x509.overview.revoked").toUpperCase();
                        
                        // mark revoked users
                        row[0] = "--- " + row[0] + " ---";
                    }
                    row[3] = validity;
                } catch (Exception e) {
                    logger.warning("validto or validfrom cannot be formatted or is null");
                    row[3] = rb.getString("x509.overview.undefined");
                }
                row[4] = rs.getString("x509id");
                
                if(showRevoked || !isRevoked)
                	all.add(row);

                // store the mapping for later retrieval
                model.addIdRowMapping(rowNr + "", rs.getString("x509id"));
                rowNr++;
            }
            rs.close();
            st.close();

            // add servername to the 'issued for' column
            for (Iterator<String[]> iterator = all.iterator(); iterator
                    .hasNext();) {
                String[] row = (String[]) iterator.next();
                if (row[1].equals(rb.getString("x509.type.server"))) {
                    Server server = ServerQueries.getServerByX509id(Integer.parseInt(row[4]));
                    if (server != null)
                        row[0] += " (" + server.getName() + ")";
                }
                
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading x509 overview", e);
        }

        return all;
    }
    
    /**
     * Loads all data from the x509 table
     *
     * @param x509id the id of the x509-cert
     * @return a string containing the username
     */
    public static String getX509Username(String x509id) {
        String username = null;
        
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            
            ResultSet rs = st.executeQuery(
           			"SELECT x509id, subject, type, createdate, validfrom, validto, serial " +
           			"FROM x509 WHERE x509id="+x509id);
           
            username = X509Utils.getCnFromSubject(rs.getString("subject"));               

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading x509 overview", e);
        }

        return username;
    }
    
    
    /**
     * Loads all x509ids from the x509 table
     *
     * @return a vector with x509ids
     */
    public static Vector<String> getAllX509Ids() {
        Vector<String> allIds = new Vector<String>();

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT x509id FROM x509");
            while (rs.next()) 
                allIds.add(rs.getString("x509id"));

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading all x509 ids", e);
        }

        return allIds;
    }


    /**
     * Returns a vector with id and subject of all non-revoked certificates 
     * of the type
     *
     * @param type The X509 type: client, server, root
     * @return a vector with stringarrays
     */
    public static Vector<String[]> getX509TypeOverview(int type) {
        Vector<String[]> all = new Vector<String[]>();

        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT x509.x509id, x509.subject, x509.validfrom, x509.validto " +
                    "FROM x509 " +
                    "WHERE x509.type=? AND x509.x509id NOT IN " +
                        "(SELECT x509id FROM crlentry)");
            pst.setInt(1, type);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String[] row = new String[3];
                row[0] = rs.getString("x509id");
                row[1] = X509Utils.getCnFromSubject(rs.getString("subject"));
                
                if (type == X509.X509_TYPE_SERVER) {
                    Server server = ServerQueries.getServerByX509id(rs.getInt("x509id"));
                    if (server != null)
                        row[1] += " (" + server.getName() + ")";
                }
                
                row[2] = X509.transformValidityToString(
                        rs.getString("validfrom"), rs.getString("validto")
                );

                all.add(row);
            }

            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading x509 type overview", e);
        }

        return all;
    }


    /**
     * Lists all x509Ids which are assigned to a server or user
     *
     * @param type The type to listthe assigned x509Ids for
     * @return A Vector with ids as integers
     */
    public static Vector<Integer> getAssignedX509Ids(int type) {
        Vector<Integer> all = new Vector<Integer>();

        try {

            // retrieve to a server assigned x509Ids
            if (type == X509.X509_TYPE_SERVER) {
                PreparedStatement pst2 = DBConnector.getInstance().getConnection().prepareStatement(
                        "SELECT x509id FROM server WHERE x509id>0");
                ResultSet rs2 = pst2.executeQuery();
                while (rs2.next()) {
                    all.add(rs2.getInt("x509id"));
                }
            }
            // retrieve to a user assigned x509Ids
            if ((type == X509.X509_TYPE_CLIENT) || (type == X509.X509_TYPE_PKCS12)) {
                PreparedStatement pst3 = DBConnector.getInstance().getConnection().prepareStatement(
                        "SELECT x509id FROM user WHERE x509id>0");
                ResultSet rs3 = pst3.executeQuery();
                while (rs3.next()) {
                    all.add(rs3.getInt("x509id"));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading assigned x509 ", e);
        }

        return all;

    }


    /**
     * Deletes assignment for this x509id
     *
     * @param x509id The x509 id
     * @param type   The X509 type
     */
    public static void deleteX509Assignment(int x509id, int type) throws Exception {
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);

            String tableName = null;
            if ((type == X509.X509_TYPE_CLIENT) || (type == X509.X509_TYPE_PKCS12))
                tableName = "user";
            else if (type == X509.X509_TYPE_SERVER)
                tableName = "server";
            else
                throw new Exception();

            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "UPDATE " + tableName + " SET x509id=-1 WHERE x509id=?");
            pst.setInt(1, x509id);
            pst.execute();
            pst.close();

            DBConnector.getInstance().getConnection().commit();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while deleting assignment for x509id " + x509id, e);
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            throw new Exception(rb.getString("error.general.text"));
        }
    }

    
    /**
     * Loads a complete row from the x509 table
     *
     * @param id The id to load
     * @return a stringarray
     */
    public static String[] getX509Details(String id) {
        return getX509Details(id, false);
    }

    /**
     * Loads a complete row from the x509 table
     *
     * @param id The id to load
     * @param showPKCS12Content true: displays nonsense PKCS#12 content
     *      false: display x509 content
     * @return a stringarray
     */
    public static String[] getX509Details(String id, boolean showPKCS12Content) {
        String[] detail = new String[18];

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM x509 " +
                    "where x509id = " + id);
            while (rs.next()) {
                detail[0] = rs.getString("x509id");
                detail[1] = rs.getString("version");
                detail[2] = rs.getString("filename");
                detail[3] = rs.getString("path");
                detail[4] = rs.getString("serial");
                detail[5] = rs.getString("issuer");
                detail[6] = rs.getString("content");
                detail[7] = rs.getString("contentdisplay");
                detail[8] = rs.getString("certserialized");
                detail[9] = rs.getString("key");
                detail[10] = rs.getString("keycontent");
                detail[11] = rs.getString("type");
                detail[12] = rs.getString("createdate");
                detail[13] = rs.getString("userid");
                detail[14] = rs.getString("subject");
                detail[15] = rs.getString("validfrom");
                detail[16] = rs.getString("validto");
                detail[17] = rs.getString("generated");
            }

            
            int type = Integer.parseInt(detail[11]);
            if (showPKCS12Content && type == X509.X509_TYPE_PKCS12) {
                Statement st2 = DBConnector.getInstance().getConnection().createStatement();
                ResultSet rs2 = st2.executeQuery("SELECT content FROM pkcs12 " +
                        "where x509id = " + id);
                while (rs2.next()) {
                    detail[6] = rs2.getString("content");
                }
                rs2.close();
                st2.close();
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading x509 details", e);
        }

        return detail;
    }


    /**
     * returns the root certificate id
     *
     * @return a String with the root id
     */
    public static String getRootCertId() {
        String rootId = null;

        try {
            int type = X509.X509_TYPE_ROOT;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT x509id from x509 where type=" + type);
            if (rs.next())
                rootId = rs.getString("x509id");

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading root cert id", e);
        }

        return rootId;
    }


    /**
     * returns the intermediate certificate id
     *
     * @return a String with the intermediate id
     */
    public static String getIntermediateCertId() {
        String rootId = null;

        try {
            int type = X509.X509_TYPE_INTERMEDIATE;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT x509id from x509 where type=" + type);
            if (rs.next())
                rootId = rs.getString("x509id");

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading intermediate cert id", e);
        }

        return rootId;
    }


    /**
     * returns the root certificate id
     *
     * @return a String with the root id
     * @deprecated
     */
    public static String getX509IdToUser(String userId) {
        String x509Id = null;

        try {
            int type = X509.X509_TYPE_CLIENT;
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT x509id FROM x509 " +
                            "WHERE type=" + type + " " +
                            "AND userid=" + userId
            );
            if (rs.next())
                x509Id = rs.getString("x509id");

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while loading x509id to user", e);
        }

        return x509Id;
    }


    /**
     * Retrieves the current sequential number for the certificates filename
     * for example: last stored cert has filename client_2B.crt, seqNumber will be 2B
     *
     * @param type The type to retrieve the number for
     * @return sequential number out of the filename
     */
    public static String retrieveCurrentSeqNumber(int type) {
        String seqNumber = null;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT MAX(x509id) AS maxid, filename FROM x509 WHERE type=" + type);
            if (rs.next()) {
                seqNumber = rs.getString("filename");
                if (seqNumber == null || "".equals(seqNumber))
                    seqNumber = "0";
                else {
                    seqNumber = seqNumber.substring(seqNumber.lastIndexOf("_") + 1);
                    seqNumber = seqNumber.substring(0, seqNumber.indexOf("."));
                }
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next sequential number from database", e);
        }
        return seqNumber;
    }


    /**
     * Detects if a certificate is already existing for this subject
     *
     * @param subject The subject, by which the certificate is identified
     * @param type    The certificate type
     * @return The x509id of the found certificate, or -1
     */
    public static int getCertificateBySubject(String subject, int type) {
        int x509id = -1;
        String subjectWS = subject;
        subject = subject.replaceAll(", ", ",");
        try {
            String query = 
                "SELECT x509id FROM x509 " +
                "WHERE (subject LIKE '" + subject + "' " +
                "OR subject LIKE '" + subjectWS + "') " +
                "AND type=" + type;
            
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                x509id = rs.getInt("x509id");
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while checking if certificate is existing", e);
        }
        return x509id;
    }

    /**
     * Detects if a certificate is already existing for this subject
     *
     * @param cn The cn, by which the certificate is identified
     * @param type    The certificate type
     * @return The x509id of the found certificate, or -1
     */
    public static int getCertificateByCN(String cn, int type) {
        int x509id = -1;
        String cnWS = cn;
        cn = cn.replaceAll(", ", ",");
        try {
            String query = 
                "SELECT x509id FROM x509 " +
                "WHERE (subject LIKE '%cn=" + cn + "%' " +
                "OR subject LIKE '%cn=" + cnWS + "%') " +
                "AND type=" + type;
            
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                x509id = rs.getInt("x509id");
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while checking if certificate is existing", e);
        }
        return x509id;
    }

}
