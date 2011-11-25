/**
 * **********************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 * *
 * http://www.bytemine.net/                                               *
 * ***********************************************************************
 */

package net.bytemine.manager.db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Constants;

public class KnownHostsQueries {

    private static Logger logger = Logger.getLogger(KnownHostsQueries.class.getName());

    /**
     * Inserts or updates the host entry
     *
     * @param hostname    The hostname
     * @param fingerprint The hosts fingerprint
     * @param trusted     True, if the server can be trusted
     * @throws Exception
     */
    public static void updateHost(String hostname, String fingerprint, boolean trusted) throws Exception {
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            Statement st2 = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT fingerprint FROM knownhosts " +
                    "WHERE hostname='" + hostname + "'");

            boolean isExisting = false;
            if (rs.next())
                isExisting = true;
            rs.close();
            st.close();

            String trustedStr = trusted ? "1" : "0";

            if (isExisting) {
                st2.executeUpdate("UPDATE knownhosts " +
                        "SET fingerprint='" + fingerprint + "' " +
                        ", trusted=" + trustedStr + " " +
                        "WHERE hostname='" + hostname + "'");
            } else {
                st2.executeUpdate("INSERT INTO knownhosts (hostname,fingerprint,trusted) " +
                        "VALUES('" + hostname + "','" + fingerprint + "'," + trustedStr + ")");
            }

            st2.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error updating known host: " + hostname, e);
            throw e;
        }

    }


    /**
     * Trust the host
     *
     * @param hostname    The hostname
     * @param fingerprint The fingerprint of the host
     * @throws Exception
     */
    public static void trustHost(String hostname, String fingerprint) throws Exception {
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            st.executeUpdate("UPDATE knownhosts " +
                    "SET fingerprint='" + fingerprint + "' " +
                    ", trusted=1 " +
                    "WHERE hostname='" + hostname + "'");

            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error trusting known host: " + hostname, e);
            throw e;
        }

    }


    /**
     * Mistrust the host
     *
     * @param hostname The hostname
     * @throws Exception
     */
    public static void mistrustHost(String hostname) throws Exception {
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            st.executeUpdate("UPDATE knownhosts " +
                    "SET trusted=0 " +
                    "WHERE hostname='" + hostname + "'");

            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error mistrusting known host: " + hostname, e);
            throw e;
        }

    }


    /**
     * Get the status of this host
     *
     * @param hostname    The hostname
     * @param fingerprint The fingerprint of the host
     * @return A status code as integer
     * @throws Exception
     */
    public static int getConfirmationStatus(String hostname, String fingerprint) throws Exception {
        int status = Constants.KNOWN_HOST_STATUS_NEW;
        if (hostname == null || fingerprint == null)
            return status;

        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT fingerprint, trusted FROM knownhosts " +
                    "WHERE hostname='" + hostname + "'");
            if (rs.next()) {
                String storedFingerprint = rs.getString("fingerprint");
                boolean trusted = rs.getBoolean("trusted");
                if (trusted) {
                    if (storedFingerprint.equals(fingerprint))
                        status = Constants.KNOWN_HOST_STATUS_OK;
                    else
                        status = Constants.KNOWN_HOST_STATUS_CHANGED;

                } else
                    status = Constants.KNOWN_HOST_STATUS_MISTRUSTED;


            } else {
                status = Constants.KNOWN_HOST_STATUS_NEW;
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting known host status for: "+hostname, e);
            throw e;
        }
		return status;
	}

}
