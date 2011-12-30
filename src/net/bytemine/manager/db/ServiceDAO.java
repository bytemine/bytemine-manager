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

import net.bytemine.manager.bean.Service;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * data access object for the Service.class
 * implements CRUD-methods for this class
 * implemented as singleton
 *
 * @author Daniel Rauer
 */
public class ServiceDAO {

    private static Logger logger = Logger.getLogger(ServiceDAO.class.getName());

    private static ServiceDAO serviceDAO;
    private static Connection dbConnection;


    private ServiceDAO() {
        dbConnection = DBConnector.getInstance().getConnection();
    }


    public static ServiceDAO getInstance() {
        try {
            if (dbConnection != null && dbConnection.isClosed())
                serviceDAO = null;
        } catch (SQLException e) {
            // XXX TODO: remove, currently here as a test
            e.printStackTrace();
        }
        if (serviceDAO == null)
            serviceDAO = new ServiceDAO();
        return serviceDAO;
    }


    /**
     * creates a new server row in the db
     *
     * @param service The service to create
     */
    public void create(Service service) {
        try {
            logger.info("start creating service");
            int nextServiceId = getNextServiceid();
            service.setServiceid(nextServiceId);

            PreparedStatement pst = dbConnection.prepareStatement(
                    "INSERT INTO service(serviceid, servicename, protocol, port) " +
                    "VALUES(?,?,?,?)"
            );
            pst.setInt(1, nextServiceId);
            pst.setString(2, service.getServicename());
            pst.setString(3, service.getProtocol());
            pst.setInt(4, service.getPort());
            pst.executeUpdate();
            pst.close();

            logger.info("end creating service");
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error creating service", e);
            new VisualException(rb.getString("error.db.service") + " " + rb.getString("error.db.create"));
        }
    }


    /**
     * loads a server from the db
     * identification by serverid
     *
     * @param service The service to load
     * @return the service
     */
    public Service read(Service service) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "SELECT * FROM service WHERE serviceid=?"
            );
            pst.setInt(1, service.getServiceid());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                service.setServicename(rs.getString(3));
                rs.close();
                pst.close();

                return service;
            } else {
                rs.close();
                pst.close();
            }

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error reading service", e);
            new VisualException(rb.getString("error.db.service") + " " + rb.getString("error.db.read"));
        }

        return null;
    }


    /**
     * updates all variables of the service except the serviceid
     * identification by serviceid
     *
     * @param service The service to update
     */
    public void update(Service service) {
        try {
            dbConnection.setAutoCommit(false);
            PreparedStatement pst = dbConnection.prepareStatement(
                    "UPDATE service SET " +
                            "servicename=?, " +
                            "protocol=?, " +
                            "port=? " +
                            "WHERE serviceid=?"
            );

            pst.setString(1, service.getServicename());
            pst.setString(2, service.getProtocol());
            pst.setInt(3, service.getPort());
            pst.setInt(4, service.getServiceid());

            pst.executeUpdate();
            pst.close();
            dbConnection.commit();

        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error updating service", e);
            new VisualException(rb.getString("error.db.service") + " " + rb.getString("error.db.save"));
        }
    }


    /**
     * deletes the service from the db
     * identification by serviceid
     *
     * @param service The service to delete
     */
    public void delete(Service service) {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM service WHERE serviceid=?"
            );
            pst.setInt(1, service.getServiceid());
            pst.executeUpdate();
            pst.close();

            ServerQueries.removeServerFromAllUsers(service.getServiceid());
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting service", e);
            new VisualException(rb.getString("error.db.service") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * deletes the service from the db
     * identification by serviceid
     *
     * @param serviceId The service to delete
     */
    public void deleteById(String serviceId) throws Exception {
        try {
            PreparedStatement pst = dbConnection.prepareStatement(
                    "DELETE FROM service WHERE serviceid=?"
            );
            pst.setInt(1, Integer.parseInt(serviceId));
            pst.executeUpdate();
            pst.close();

            ServerQueries.removeServerFromAllUsers(Integer.parseInt(serviceId));
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error deleting service", e);
            new VisualException(rb.getString("error.db.service") + " " + rb.getString("error.db.delete"));
        }
    }


    /**
     * retrieves the next serverid from the db
     *
     * @return int the next server id
     */
    private int getNextServiceid() throws Exception {
        int serviceid = 0;
        try {
            Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(serviceid) as maxId from service");
            if (rs.next())
                serviceid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next serviceid", e);
            throw e;
        }

        return serviceid;
    }

}
