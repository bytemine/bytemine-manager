/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

import net.bytemine.manager.Constants;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.DateUtils;


/**
 * Holds all data of the certificate
 *
 * @author Daniel Rauer
 */
public class X509 {

    public static final int X509_TYPE_ROOT = 0;
    public static final int X509_TYPE_INTERMEDIATE = 1;
    public static final int X509_TYPE_SERVER = 2;
    public static final int X509_TYPE_CLIENT = 3;
    public static final int X509_TYPE_PKCS12 = 4;

    private int x509id;
    private String version;
    private String fileName;
    private String path;
    private String serial;
    private String issuer;
    private String subject;
    private String content;
    private String contentDisplay;
    private String certSerialized;
    private String key;
    private String keyContent;
    private int type;
    private String createDate;
    private String validFrom;
    private String validTo;
    private boolean generated;
    private int userId = -1;


    // indicates wether the object is persistent (x509DAO!=null) or not
    private X509DAO x509DAO = null;


    private X509() {
    }


    public X509(int id) {
        this.x509id = id;

        x509DAO = X509DAO.getInstance();
    }


    public X509(String serial) {
        this.serial = serial;
        this.x509DAO = X509DAO.getInstance();

        // write to db
        x509DAO.create(this);
    }


    /**
     * deletes the certificate from the db
     */
    public void delete() {
        x509DAO.delete(this);
    }


    /**
     * loads the x509 from the db
     * identification by x509id
     *
     * @param x509id The x509id of the user to load
     * @return the loaded user
     */
    public static X509 getX509ById(int x509id) {
        X509 x509 = new X509();
        x509.x509id = x509id;
        x509 = X509DAO.getInstance().read(x509);

        if (x509 != null)
            x509.x509DAO = X509DAO.getInstance();
        return x509;
    }


    /**
     * updates the x509
     */
    public void update() {
        if (x509DAO != null)
            x509DAO.update(this);
    }


    /**
     * returns the displayable name of the type
     *
     * @param type
     * @return displayable name of the type
     */
    public static String transformTypeToString(int type) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        switch (type) {
            case X509_TYPE_ROOT:
                return rb.getString("x509.type.root");
            case X509_TYPE_INTERMEDIATE:
                return rb.getString("x509.type.inter");
            case X509_TYPE_SERVER:
                return rb.getString("x509.type.server");
            case X509_TYPE_CLIENT:
                return rb.getString("x509.type.client");
            case X509_TYPE_PKCS12:
                return rb.getString("x509.type.client");
            default:
                return rb.getString("x509.type.client");
        }
    }


    /**
     * returns the displayable name of the generation type
     *
     * @param generated 1 if generated; 0 if imported
     * @return displayable name of the generation type
     */
    public static String transformGenerationTypeToString(String generated) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        if (generated != null && "1".equals(generated))
            return rb.getString("detailsFrame.generated");
        else
            return rb.getString("detailsFrame.imported");
    }


    /**
     * returns the validity as String
     *
     * @param validFromStr The date from the cert is valid
     * @param validToStr   The date till the cert is valid
     * @return displayable validity
     */
    public static String transformValidityToString(String validFromStr, String validToStr) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        Date validFrom = Constants.parseDetailedFormat(validFromStr);
        Date validTo = Constants.parseDetailedFormat(validToStr);
        Date now = new Date();
        if (validFrom.before(now) && validTo.after(now))
            return rb.getString("x509.overview.valid");
        else
            return rb.getString("x509.overview.invalid");
    }

    /**
     * Returns for how many days this certificate is valid 
     * @return the days of validity as String
     * @throws ParseException if the Date Strings in the x509 object could not be parsed properly
     */
    public String validForDays() throws ParseException {
        long days = 0;
        SimpleDateFormat dateFormat = null;

        if (isFormatDe())
            dateFormat = Constants.DETAILED_FORMAT_DE;
        else
            dateFormat = Constants.DETAILED_FORMAT_EN;

        try {
            Date validFromDate = dateFormat.parse(this.validFrom);
            Date validToDate = dateFormat.parse(this.validTo);
            Calendar cFrom = Calendar.getInstance();
            cFrom.setTime(validFromDate);
            Calendar cTo = Calendar.getInstance();
            cTo.setTime(validToDate);
    
            days = DateUtils.daysBetween(cFrom, cTo);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), 1);
        }

        return Long.toString(days);
    }

    /**
     * ugly little helper, since we don't know what to expect as DateFormat in the database
     * @return True, if the date format is german 
     */
    private boolean isFormatDe() {
        SimpleDateFormat dateFormat = Constants.DETAILED_FORMAT_DE;
        try {
            dateFormat.parse(this.validFrom);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    /*
    * getter and setter of the attributes
    */

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

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public String getKeyContent() {
        return keyContent;
    }

    public void setKeyContent(String keyContent) {
        this.keyContent = keyContent;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public X509DAO getX509DAO() {
        return x509DAO;
    }

    public void setX509DAO(X509DAO x509DAO) {
        this.x509DAO = x509DAO;
    }

    public int getX509id() {
        return x509id;
    }

    public void setX509id(int x509id) {
        this.x509id = x509id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCertSerialized() {
        return certSerialized;
    }

    public void setCertSerialized(String certSerialized) {
        this.certSerialized = certSerialized;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

}
