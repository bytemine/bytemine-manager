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

    private static final int TYPE_BUG = 1;
    public static final int TYPE_REQUEST = 2;
    private static final int TYPE_FEEDBACK = 3;
    
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
        
        StringBuilder sb = new StringBuilder();
        Properties sysProps = System.getProperties();
        Enumeration<?> systemPropertiesKeys = sysProps.keys();
        while (systemPropertiesKeys.hasMoreElements()) {
            String prop = (String) systemPropertiesKeys.nextElement();
            sb.append("    ").append(prop).append(": ").append(sysProps.getProperty(prop)).append("\n");
        }
        systemProperties = sb.toString();
    }
    
    
    /**
     * Returns the request in textual form 
     * @return The request as formatted text
     */
    public String printRequest() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMMM yyyy HH:mm:ss");
        return ("Support request from: " + this.name + "\n") +
                "Date of request: " + sdf.format(this.createDate) + "\n" +
                "Type of request: " + getPrintableType() + "\n" +
                "Customer number: " + this.customer + "\n" +
                "Email address: " + this.mail + "\n" +
                "Phone number: " + this.phone + "\n" +
                "Manager version: " + this.managerVersion + "\n" +
                "Manager build: " + this.managerBuild + "\n" +
                "Message:\n" + this.message + "\n" +
                "\n\n" +
                "System properties:\n" + this.systemProperties;
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
