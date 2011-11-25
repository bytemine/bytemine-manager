/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;


import net.bytemine.manager.db.CRLDAO;


/**
 * Holds all data of the CRL
 *
 * @author Daniel Rauer
 */
public class CRL {

    private int crlid;
    private int crlNumber;
    private String version;
    private String fileName;
    private String path;
    private String issuer;
    private String content;
    private String contentDisplay;
    private String crlSerialized;
    private String createDate;
    private String validFrom;
    private String nextUpdate;

    // indicates wether the object is persistent (crlDAO!=null) or not
    private CRLDAO crlDAO = null;


    private CRL() {
    }


    public CRL(int crlId) {
        this.crlid = crlId;

        this.crlDAO = CRLDAO.getInstance();
    }


    public CRL(String createDate) {
        this.createDate = createDate;

        this.crlDAO = CRLDAO.getInstance();
        // write to db
        this.crlDAO.create(this);

    }


    /**
     * deletes the CRL from the db
     */
    public void delete() {
        crlDAO.delete(this);
    }


    /**
     * loads the CRL from the db
     * identification by crlid
     *
     * @param crlid The crlid of the CRL to load
     * @return the loaded CRL
     */
    public static CRL getCRLByID(int crlid) {
        CRL crl = new CRL();
        crl.setCrlid(crlid);
        crl = CRLDAO.getInstance().read(crl);

        if (crl != null)
            crl.crlDAO = CRLDAO.getInstance();
        return crl;
    }


    public int getCrlid() {
        return crlid;
    }


    public void setCrlid(int crlid) {
        this.crlid = crlid;
    }


    public int getCrlNumber() {
        return crlNumber;
    }


    public void setCrlNumber(int crlNumber) {
        this.crlNumber = crlNumber;
    }


    public String getVersion() {
        return version;
    }


    public void setVersion(String version) {
        this.version = version;
    }


    public String getFileName() {
        return fileName;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getPath() {
        return path;
    }


    public void setPath(String path) {
        this.path = path;
    }


    public String getIssuer() {
        return issuer;
    }


    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }


    public String getContent() {
        return content;
    }


    public void setContent(String content) {
        this.content = content;
    }


    public String getContentDisplay() {
        return contentDisplay;
    }


    public void setContentDisplay(String contentDisplay) {
        this.contentDisplay = contentDisplay;
    }


    public String getCrlSerialized() {
        return crlSerialized;
    }


    public void setCrlSerialized(String crlSerialized) {
        this.crlSerialized = crlSerialized;
    }


    public String getCreateDate() {
        return createDate;
    }


    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }


    public String getValidFrom() {
        return validFrom;
    }


    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }


    public String getNextUpdate() {
        return nextUpdate;
    }


    public void setNextUpdate(String nextUpdate) {
        this.nextUpdate = nextUpdate;
    }


    /*
    * getter and setter of the attributes
    */


}
