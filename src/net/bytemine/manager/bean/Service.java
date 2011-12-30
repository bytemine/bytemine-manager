/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.util.logging.Logger;

import net.bytemine.manager.db.ServiceDAO;

/**
 * Holds all data of a service
 *
 * @author Daniel Rauer
 */
public class Service {

    private static Logger logger = Logger.getLogger(Service.class.getName());

    private int serviceid;
    private String servicename;
    private String protocol;
    private int port;

    // indicates wether the object is persistent (serverDAO!=null) or not
    private ServiceDAO serviceDAO = null;


    public Service(int id) {
        this.serviceid = id;

        serviceDAO = ServiceDAO.getInstance();
    }

    public Service(String servicename, String protocol, int port) {

        initialize(servicename, protocol, port);
    }


    private void initialize(String servicename, String protocol, int port) {

        this.servicename = servicename;
        this.protocol = protocol;
        this.port = port;

        serviceDAO = ServiceDAO.getInstance();
        // write to db
        serviceDAO.create(this);
    }


    /**
     * deletes the server from the db
     */
    public void delete() {
        logger.info("deleting service with id " + this.getServiceid());
        serviceDAO.delete(this);
    }

    public int getServiceid() {
        return serviceid;
    }

    public void setServiceid(int id) {
        this.serviceid = id;
    }

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

}
