/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.util.logging.Logger;

import net.bytemine.manager.db.CRLEntryDAO;


/**
 * Holds all data of the CRLEntry
 *
 * @author Daniel Rauer
 */
public class CRLEntry {

    private static Logger logger = Logger.getLogger(CRLEntry.class.getName());

    private int crlEntryid;
    private String serial;
    private String revocationDate;
    private int x509id;
    private int crlid;
    private String username;

    // indicates wether the object is persistent (crlEntryDAO!=null) or not
    private CRLEntryDAO crlEntryDAO = null;


    private CRLEntry() {
    }

    
    public CRLEntry(int crlEntryId) {
        this.crlEntryid = crlEntryId;

        this.crlEntryDAO = CRLEntryDAO.getInstance();
    }

    public CRLEntry(String serial) {
        logger.info("creating new crlEntry");

        this.serial = serial;

        this.crlEntryDAO = CRLEntryDAO.getInstance();
        // write to db
        this.crlEntryDAO.create(this);

    }


    /**
     * deletes the CRLEntry from the db
     */
    public void delete() {
        crlEntryDAO.delete(this);
    }


    /**
     * loads the CRLEntry from the db
     * identification by crlentryid
     *
     * @param crlentryid The crlEtryid of the CRLEntry to load
     * @return the loaded CRLEntry
     */
    public static CRLEntry getCRLEntryByID(int crlentryid) {
        CRLEntry crlEntry = new CRLEntry();
        crlEntry.setCrlEntryid(crlentryid);
        crlEntry = CRLEntryDAO.getInstance().read(crlEntry);

        if (crlEntry != null)
            crlEntry.crlEntryDAO = CRLEntryDAO.getInstance();
        return crlEntry;
    }


    /*
    * getter and setter of the attributes
    */


    public int getCrlEntryid() {
        return crlEntryid;
    }


    public void setCrlEntryid(int crlEntryid) {
        this.crlEntryid = crlEntryid;
    }


    public String getSerial() {
        return serial;
    }


    public void setSerial(String serial) {
        this.serial = serial;
    }


    public String getRevocationDate() {
        return revocationDate;
    }


    public void setRevocationDate(String revocationDate) {
        this.revocationDate = revocationDate;
    }


    public int getCrlid() {
        return crlid;
    }


    public void setCrlid(int crlid) {
        this.crlid = crlid;
    }


    public int getX509id() {
        return x509id;
    }


    public void setX509id(int x509id) {
        this.x509id = x509id;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }

}
