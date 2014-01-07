/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.action;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextField;

import net.bytemine.crypto.utility.CryptoUtils;
import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.db.GroupQueries;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.exception.ValidationException;
import net.bytemine.utility.DBUtils;
import net.bytemine.utility.StringUtils;


/**
 * methods for ui-validations
 *
 * @author Daniel Rauer
 */
public class ValidatorAction {
    
    private static Logger logger = Logger.getLogger(ValidatorAction.class.getName());

    /**
     * validates the username and password from the user creation dialog
     *
     * @param username The content of the username field
     * @param password The content of the password field
     * @param yubikeyid The content of the yubikeyid field
     * @return true, if the entries are valid
     */
    public static boolean validateUserCreation(String username, String password, String yubikeyid) throws ValidationException {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        int minimumPasswordLength = Configuration.getInstance().USER_PASSWORD_LENGTH;

        if (username == null || "".equals(username))
            throw new ValidationException(
                    rb.getString("dialog.newuser.error.text"),
                    rb.getString("dialog.newuser.error.title"),
                    1
            );
        else if (UserQueries.isUserExisting(username))
            throw new ValidationException(
                    rb.getString("dialog.newuser.validationerror2.text"),
                    rb.getString("dialog.newuser.validationerror2.title"),
                    1
            );
        else if (password == null || "".equals(password) ||
                password.length() < minimumPasswordLength)
            throw new ValidationException(
                    rb.getString("dialog.newuser.validationerror.text") +
                            " " + minimumPasswordLength + " " +
                            rb.getString("dialog.newuser.validationerror.text2"),
                    rb.getString("dialog.newuser.validationerror.title"),
                    2
            );
        else if (!CryptoUtils.testCryptCharacters(password))
            throw new ValidationException(
                    rb.getString("error.password.usascii"),
                    rb.getString("error.general.title"),
                    2);
        else if (!username.matches("[A-Za-z0-9 ]+"))
            throw new ValidationException(
                    rb.getString("error.username.characters"),
                    rb.getString("error.general.title"), 
                    1);
        
        else if (yubikeyid != null && !"".equals(yubikeyid)) {
            if (yubikeyid.length() != 12) {
                throw new ValidationException(
                        rb.getString("error.yubikeyid.characters"),
                        rb.getString("error.general.title"),
                        3);
            }
        }
        return true;
    }


    /**
     * validates the username from the user update dialog
     *
     * @param username    The content of the username field
     * @param oldUsername The username before the update
     * @param password    The content of the password field
     * @param yubikeyid   The content of the yubikeyid field
     * @return true, if username and password are valid
     * @throws NoSuchAlgorithmException, ValidationException
     */
    public static boolean validateUserUpdate(
            String username, String oldUsername, String password, String yubikeyid)
            throws NoSuchAlgorithmException, ValidationException {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        int minimumPasswordLength = Configuration.getInstance().USER_PASSWORD_LENGTH;

        if (username == null || "".equals(username))
            throw new ValidationException(
                    rb.getString("dialog.newuser.error.text"),
                    rb.getString("dialog.newuser.error.title"),
                    1
            );
        else if (!username.equals(oldUsername) && UserQueries.isUserExisting(username))
            throw new ValidationException(
                    rb.getString("dialog.newuser.validationerror2.text"),
                    rb.getString("dialog.newuser.validationerror2.title"),
                    1
            );
        else if (!username.matches("[A-Za-z0-9 ]+"))
            throw new ValidationException(
                    rb.getString("error.username.characters"),
                    rb.getString("error.general.title"),
                    1);

        else if (password == null || "".equals(password))
            return true;
        else if (password.length() < minimumPasswordLength)
            throw new ValidationException(
                    rb.getString("dialog.newuser.validationerror.text") +
                            " " + minimumPasswordLength + " " +
                            rb.getString("dialog.newuser.validationerror.text2"),
                    rb.getString("dialog.newuser.validationerror.title"),
                    2
            );
        else if (!CryptoUtils.testCryptCharacters(password))
            throw new ValidationException(
                    rb.getString("error.password.usascii"),
                    rb.getString("error.general.title"),
                    2);
        else if (yubikeyid != null && !"".equals(yubikeyid)) {
            if (yubikeyid.length() != 12) {
                throw new ValidationException(
                        rb.getString("error.yubikeyid.characters"),
                        rb.getString("error.general.title"), 
                        3);
            } else
                return true;
        }
        else
            return true;
    }


    /**
     * validates some input values from the server creation dialog
     *
     * @param name     The provided name
     * @param oldName  The name before an update (null for a new server)
     * @param hostname The provided hostname
     * @param username The provided username
     * @param authTypeKeyfile The provided auth type of keyfiles
     * @param keyfile The provided keyfile
     * @param userfilePath The provided path to the userfile 
     * @param exportPath The provided export path
     * @param statusPort The provided status port
     * @param statusInterval The provided status interval
     * @param sshPort The provided ssh port
     * @param newServer The provided status of the server object: new or existing
     * @param vpnPort The provided openvpn port
     * @param vpnCC The provided vpn cc status
     * @param vpnCCPath The provided vpn cc path
     * @param networkAddress The provided network address
     * @return true, if the entries are valid
     * @throws ValidationException
     */
    public static boolean validateServerCreation(String name, String oldName, String hostname,
                                                String username, boolean authTypeKeyfile, String keyfile,
                                                String userfilePath, String exportPath,
                                                String statusPort, String statusInterval, String sshPort,
                                                boolean newServer, String vpnPort, boolean vpnCC,
                                                String vpnCCPath, String networkAddress)
            throws ValidationException {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if ("".equals(name))
            throw new ValidationException(
                    rb.getString("server.details.errorName"),
                    rb.getString("server.details.errortitle"),
                    1
            );
        else if(!name.equals(oldName) &&  ServerQueries.isServerExisting(name)) {
        	throw new ValidationException(
                    rb.getString("server.details.duplicateName"),
                    rb.getString("server.details.duplicateTitle"),
                    1
            );
        }    
        if ("".equals(hostname))
            throw new ValidationException(
                    rb.getString("server.details.errorHostname"),
                    rb.getString("server.details.errortitle"),
                    2
            );
        if ("".equals(username))
            throw new ValidationException(
                    rb.getString("server.details.errorUsername"),
                    rb.getString("server.details.errortitle"),
                    3
            );
        if ("".equals(keyfile) && authTypeKeyfile)
            throw new ValidationException(
                    rb.getString("server.details.errorKeyfile"),
                    rb.getString("server.details.errortitle"),
                    4
            );
        if (!Configuration.getInstance().USE_PAM && "".equals(userfilePath))
            throw new ValidationException(
                    rb.getString("server.details.errorUserfile"),
                    rb.getString("server.details.errortitle"),
                    5
            );
        if ("".equals(exportPath))
            throw new ValidationException(
                    rb.getString("server.details.errorExportPath"),
                    rb.getString("server.details.errortitle"),
                    6
            );

        try {
            Integer.parseInt(statusInterval);
        } catch (NumberFormatException nfe) {
            throw new ValidationException(
                    rb.getString("server.details.errorSshinterval"),
                    rb.getString("server.details.errortitle"),
                    7
            );
        }

        try {
            Integer.parseInt(sshPort);
        } catch (NumberFormatException nfe) {
            throw new ValidationException(
                    rb.getString("server.details.errorSshport"),
                    rb.getString("server.details.errortitle"),
                    8
            );
        }

        try {
            Integer.parseInt(vpnPort);
        } catch (NumberFormatException nfe) {
            throw new ValidationException(
                    rb.getString("server.details.errorVpnport"),
                    rb.getString("server.details.errortitle"),
                    9
            );
        }
        
        if (vpnCC && "".equals(vpnCCPath))
            throw new ValidationException(
                    rb.getString("server.details.errorVpnCCPath"),
                    rb.getString("server.details.errortitle"),
                    10
            );
        
        if (Configuration.getInstance().CREATE_OPENVPN_CONFIG_FILES) {
            if (!networkAddress.trim().matches("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}")) {
                throw new ValidationException(
                        rb.getString("server.details.errorUserIp"),
                        rb.getString("server.details.errortitle"), 12);
            }
        }
        
        return true;
    }

    
    /**
     * validates the ip for each user connected to the server
     *
     * @param userServerIp A hashtable containing user Ids and client IPs
     * @return the text field containing the error
     */
    public static JTextField validateServerCreationUserServerIp(Hashtable<String,JTextField> userServerIp) {
    	
    	for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();) {
    		String userId = e.nextElement();
    		String ip = (userServerIp.get(userId)).getText();
            if (userServerIp.get(userId).isEditable()) {
        		int clientIP = 0;
                try {
        		    clientIP = Integer.parseInt(ip);
                } catch (Exception ex) {
                    return ((JTextField) userServerIp.get(userId));
                }
                
        		if(clientIP > 255) {
        			return ((JTextField) userServerIp.get(userId));
        		}
            }
    	}
        
        for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();) {
            String userId = e.nextElement();
             
            for (Enumeration<String> en = userServerIp.keys(); en.hasMoreElements();) {
                String userID = en.nextElement();
             
                 if (!userId.equals(userID)) {
                     if (userServerIp.get(userID).getText().length()>0 && userServerIp.get(userId).getText().length()>0)
                         if (userServerIp.get(userID).getText().equals(userServerIp.get(userId).getText())) {
                             return userServerIp.get(userID);
                         }
                 }
            }
         }
        
    	return null;
    }
    
    /**
     * validates the OpenVPN-Ip-Compatibility for each user connected to the server
     *
     * According to the OpenVPN-Webpage for ClientConfiguration, the last octet in
     * the ip address have to be choosen out of a restricted ip pool, otherwise
     * some win32-drivers have problems handling the client.
     * The pool is [1,2], [3,4], +=4
     *
     *
     * @param userServerIp A hashtable containing user Ids and client IPs
     * @return the client IP if not Windows compatible, or null
     */
    public static String validateStaticIpForWindowsClients(Hashtable<String,JTextField> userServerIp) {
    	for (Enumeration<String> e = userServerIp.keys(); e.hasMoreElements();)	{
    		String userId = e.nextElement();
    		String ip = (userServerIp.get(userId)).getText();
    		if (userId == null || StringUtils.isEmptyOrWhitespaces(ip))
    		    continue;
    		
    		String lastOctet;
    		int checkIp = 255;
    		
    		try {
    			lastOctet = ip;
    			checkIp = Integer.parseInt(lastOctet);
    		} catch (Exception ex) {
    		    logger.log(Level.SEVERE, "empty static OpenVPN IP detected for user with ID: " + userId);
    			return ip;
    		}
    		
    		int i = 1;
    		while (i < 255) {
    			if (checkIp == i)
    				break;
    			i += 4;
    		}
    		if (i >= 255)
    			return ip;
    	}
    	return null;
    }
    
    
    /**
     * validates the name and description from the group creation dialog
     *
     * @param name The content of the name field
     * @param description The content of the description field
     * @return true, if the entries are valid
     */
    public static boolean validateGroupCreation(String name, String description) throws ValidationException {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        
        if (name == null || "".equals(name))
            throw new ValidationException(
                    rb.getString("dialog.newgroup.error.text"),
                    rb.getString("dialog.newgroup.error.title")
            );
        else if (GroupQueries.isGroupExisting(name))
            throw new ValidationException(
                    rb.getString("dialog.newgroup.validationerror2.text"),
                    rb.getString("dialog.newgroup.validationerror2.title")
            );
        return true;
    }
    
    
    /**
     * validates the name and description from the group creation dialog
     *
     * @param name The content of the name field
     * @param oldName The name of the group before the update
     * @param description The content of the description field
     * @return true, if the entries are valid
     */
    public static boolean validateGroupUpdate(String name, String oldName, String description) throws ValidationException {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (name == null || "".equals(name))
            throw new ValidationException(
                    rb.getString("dialog.newgroup.error.text"),
                    rb.getString("dialog.newgroup.error.title")
            );
        else if (!oldName.equals(name) && GroupQueries.isGroupExisting(name))
            throw new ValidationException(
                    rb.getString("dialog.newgroup.validationerror2.text"),
                    rb.getString("dialog.newgroup.validationerror2.title")
            );
        return true;
    }
    

    /**
     * validates some fields from the configuration dialog
     *
     * @return A String with the error message
     */
    public static String validateConfiguration(
            String clientCertExportPath,
            boolean CAEnabled,
            boolean CCEnabled,
            String dbPath
    ) throws ValidationException {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        // database path has changed
        if (dbPath != null && !"".equals(dbPath) && !dbPath.toLowerCase().equals(Configuration.getInstance().JDBC_PATH.toLowerCase())) {
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                // copy database to new location
                String internalDBPath = Configuration.getInstance().JDBC_PATH;
                boolean success = DBUtils.copyDatabase(internalDBPath, dbPath);
                if (!success)
                    return rb.getString("dialog.configuration.error.dbcopy");
            }
        } else {
            if (CAEnabled && "".equals(clientCertExportPath))
                return rb.getString("dialog.configuration.error.exportPath");

            if (!CAEnabled && !CCEnabled)
                return rb.getString("dialog.configuration.error.module");
        }

        return null;
    }
    
    
    /**
     * validates some fields from the import configuration dialog
     *
     * @return A String with the error message
     */
    public static String validateImportConfiguration(
            String clientCertImportDir,
            String userfile,
            String host,
            String port,
            String dn,
            String objectClass,
            String cn,
            String certAttrName,
            String certImportDir,
            boolean importTypeFile
    ) throws ValidationException {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (!importTypeFile) {
            if("".equals(host) ||
               "".equals(port) ||
               "".equals(dn) ||
               "".equals(objectClass) ||
               "".equals(cn) ||
               ("".equals(certAttrName) && "".equals(certImportDir))
            )
                return rb.getString("dialog.configuration.error.ldap");
            
            else
                try {
                    Integer.parseInt(port);
                } catch(NumberFormatException nfe) {
                    return rb.getString("dialog.configuration.error.ldap");
                }
        }
        return null;
    }


    /**
     * validates some fields from the x509configuration dialog
     *
     * @return true, if the entries are valid
     */
    public static String validateX509Configuration(
            String dnString,
            String rootValidFrom,
            String rootValidTo,
            String serverValidFrom,
            String serverValidTo,
            String clientValidFrom,
            String clientValidTo,
            String keyStrength
    ) throws ValidationException {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        StringBuffer message = new StringBuffer();

        if (dnString == null || "".equals(dnString))
            message.append(rb.getString("dialog.x509configuration.errorDN") + "\n");

        if (rootValidFrom == null)
            message.append(rb.getString("dialog.x509configuration.errorRootValidFrom") + "\n");
        else {
            try {
                Constants.PROPERTIES_DATE_FORMAT.parse(rootValidFrom);
            } catch (ParseException e) {
                message.append(rb.getString("dialog.x509configuration.errorRootValidFrom") + "\n");
            }
        }

        if (rootValidTo == null)
            message.append(rb.getString("dialog.x509configuration.errorRootValidTo") + "\n");
        else {
            try {
                Constants.PROPERTIES_DATE_FORMAT.parse(rootValidTo);
            } catch (ParseException e) {
                message.append(rb.getString("dialog.x509configuration.errorRootValidTo") + "\n");
            }
        }

        if (serverValidTo == null)
            message.append(rb.getString("dialog.x509configuration.errorServerValidTo") + "\n");
        else {
            if (!StringUtils.isDigit(serverValidTo)) {
                message.append(rb.getString("dialog.x509configuration.errorServerValidTo") + "\n");
            }
        }

        if (clientValidTo == null)
            message.append(rb.getString("dialog.x509configuration.errorClientValidTo") + "\n");
        else {
            if (!StringUtils.isDigit(clientValidTo)) {
                message.append(rb.getString("dialog.x509configuration.errorClientValidTo") + "\n");
            }
        }

        if (keyStrength == null)
            message.append(rb.getString("dialog.x509configuration.errorKeyStrength") + "\n");
        else {
            try {
                Integer.parseInt(keyStrength);
            } catch (NumberFormatException e) {
                message.append(rb.getString("dialog.x509configuration.errorKeyStrength") + "\n");
            }
        }

        return message.toString();
    }


    /**
     * Validates the update configuration
     *
     * @return null if everything was fine, an error message if not
     */
    public static String validateUpdateConfiguration(boolean pemSelected, String crtPath, String keyPath,
                                                     String serverPath, String repoPath, String proxy, String proxyPort) {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (crtPath == null)
            return rb.getString("dialog.updateconfiguration.noCrt");
        else {
            File f = new File(crtPath);
            if (!f.exists())
                return rb.getString("dialog.updateconfiguration.missingCrt");
        }
        if (!pemSelected) {
            if (keyPath == null)
                return rb.getString("dialog.updateconfiguration.noKey");
            else {
                File f = new File(keyPath);
                if (!f.exists())
                    return rb.getString("dialog.updateconfiguration.missingKey");
            }
        }
        return null;
    }


    /**
     * Validates the password and the confirmation password
     *
     * @param pwd1 The first password
     * @param pwd2 The second password
     * @return null if everything was fine, an error message if not
     */
    public static String validatePKCS12Password(String pwd1, String pwd2) {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (pwd1.equals(pwd2)) {
            return null;
        } else {
            return rb.getString("dialog.pkcs12password.error");
        }
    }
    
    
    /**
     * validates some fields from the support form
     *
     * @param name The name of the requestor
     * @param mail The mail address
     * @param message The message
     * @return true, if the entries are valid
     */
    public static boolean validateSupportRequest(String name, String mail, String message) throws ValidationException {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        if (name == null || "".equals(name))
            throw new ValidationException(
                    rb.getString("supportformular.error.name"),
                    rb.getString("supportformular.error.title"),
                    1
            );
        if (mail == null || "".equals(mail) || !isValidEmailAddress(mail))
            throw new ValidationException(
                    rb.getString("supportformular.error.mail"),
                    rb.getString("supportformular.error.title"),
                    2
            );
        if (message == null || "".equals(message))
            throw new ValidationException(
                    rb.getString("supportformular.error.message"),
                    rb.getString("supportformular.error.title"),
                    3
            );
        return true;
    }

    /**
     * Validates if this email address is valid
     * 
     * @param emailAddress
     *            The email address a user entered
     * @return true, if the email is valid
     */
    private static boolean isValidEmailAddress(String emailAddress) {
        if (emailAddress != null) {
            Pattern p = Pattern.compile("[a-z0-9_.-]+@+[a-z0-9.-]+.[a-z]{2,4}");
            Matcher m = p.matcher(emailAddress);
            return m.matches();
        } else
            return false;
    }
}
