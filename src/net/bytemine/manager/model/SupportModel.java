/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *

 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import net.bytemine.manager.Configuration;


/**
 * Holds the data of a support request
 * @author Daniel Rauer
 *
 */
public class SupportModel {
    
    private String name;
    private String customer;
    private String mail;
    private String phone;
    private String message;
    private Date createDate;
    private int type;
    private String managerVersion;
    private String managerBuild;
    private String systemProperties;

    public static final int TYPE_BUG = 1;
    public static final int TYPE_REQUEST = 2;
    public static final int TYPE_FEEDBACK = 3;
    
    public SupportModel(String name, String customer, String mail, String phone, String message, int type) {
        this.name = name;
        this.customer = customer;
        this.mail = mail;
        this.phone = phone;
        this.message = message;
        this.type = type;
        
        managerVersion = Configuration.getInstance().MANAGER_VERSION;
        managerBuild = Configuration.getInstance().MANAGER_BUILD;
        createDate = new Date();
        
        StringBuffer sb = new StringBuffer();
        Properties sysProps = System.getProperties();
        Enumeration<?> systemPropertiesKeys = sysProps.keys();
        while (systemPropertiesKeys.hasMoreElements()) {
            String prop = (String) systemPropertiesKeys.nextElement();
            sb.append("    " + prop + ": " + sysProps.getProperty(prop) + "\n");
        }
        systemProperties = sb.toString();
    }
    
    
    /**
     * Returns the request in textual form 
     * @return The request as formatted text
     */
    public String printRequest() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMMM yyyy HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("Support request from: " + this.name + "\n");
        sb.append("Date of request: " + sdf.format(this.createDate) + "\n");
        sb.append("Type of request: " + getPrintableType() + "\n");
        sb.append("Customer number: " + this.customer + "\n");
        sb.append("Email address: " + this.mail + "\n");
        sb.append("Phone number: " + this.phone + "\n");
        sb.append("Manager version: " + this.managerVersion + "\n");
        sb.append("Manager build: " + this.managerBuild + "\n");
        sb.append("Message:\n" + this.message + "\n");
        sb.append("\n\n");
        sb.append("System properties:\n" + this.systemProperties);
        return sb.toString();
    }
    
    /**
     * Translates the request type from an int to a readable String
     * @return A String representing the request type
     */
    private String getPrintableType() {
        switch (this.type) {
            case TYPE_BUG: return "bug";
            case TYPE_REQUEST: return "general request";
            case TYPE_FEEDBACK: return "feedback";
            default: return "general request";
        }
    }
    
    public String getName() {
        return name;
    }

    public String getCustomer() {
        return customer;
    }

    public String getMail() {
        return mail;
    }

    public String getPhone() {
        return phone;
    }

    public String getMessage() {
        return message;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public int getType() {
        return type;
    }

    public String getManagerVersion() {
        return managerVersion;
    }

    public String getManagerBuild() {
        return managerBuild;
    }

    public String getSystemProperties() {
        return systemProperties;
    }

}
