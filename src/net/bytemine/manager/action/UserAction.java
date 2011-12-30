/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.action;

import java.io.File;

import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Exporter;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.config.ClientConfig;
import net.bytemine.utility.FileUtils;
import net.bytemine.manager.db.ServerQueries;


/**
 * User actions
 *
 * @author Daniel Rauer
 */
public class UserAction {

    private static Logger logger = Logger.getLogger(UserAction.class.getName());
    
    /**
     * Create new user and client certificate
     *
     * @param username The users username
     * @param password The users password
     * @param cn The users CN
     * @param ou The users OU
     * @param pkcs12Password The (optional) PKCS12 password
     * @param validFor Number of days the certificate will be valid
     * @return the new userid
     * @throws java.lang.Exception
     */
    public static int createUserAndCertificate(String username, String password, String cn, String ou, String pkcs12Password, String validFor)
            throws Exception {

        // create new user
        User newUser = new User(username, password, -1, true, cn, ou);

        // create client certificate
        if (Configuration.getInstance().CA_ENABLED) {
            X509Generator gen = new X509Generator();
            try {
                gen.createClientCertImmediately(newUser, pkcs12Password, validFor);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "error while creating client certificate. Message: " + ex.getMessage(), ex);
                throw ex;
            }
        }

        return newUser.getUserid();
    }
    
    /**
     * Creates a client OpenVPN configuration file for the user
     *
     * @param user The user to create the OpenVPN config file for
     */
    public static void createVPNConfigFile(User user) {
        try {
        	 Vector<String> serverIds = ServerQueries.getServersForUser(user.getUserid());
            new ClientConfig(user, serverIds);
        } catch (Exception e) {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            logger.log(Level.SEVERE, "error while creating client OpenVPN config file. Message: " + e.getMessage(), e);
            new VisualException(rb.getString("dialog.newuser.vpnconfigerror"));
        }
    }



    /**
     * delete the user with the given id
     *
     * @param userId
     * @throws java.lang.Exception
     */
    public static void deleteUser(String userId) throws Exception {
        X509Utils.revokeCertificateByUserID(userId);

        UserDAO.getInstance().deleteById(userId);
    }


    /**
     * Updates username and password of the user
     *
     * @param userId
     * @param username
     * @param newPasswordPlain
     * @throws java.lang.Exception
     */
    public static void updateUser(
            String userId, String username, String newPasswordPlain, String cn, String ou)
            throws Exception {
        logger.info("Update User with id: " + userId);

        User user = new User(userId);
        user = UserDAO.getInstance().read(user);

        boolean newCn = !user.getCn().equals(cn);
        user.setCn(cn);
        user.setOu(ou);
        if (newCn)
            renameUser(user, username);

        // update password only if a new password is entered
        if (newPasswordPlain != null && !"".equals(newPasswordPlain))
            user.updatePassword(newPasswordPlain);

        UserDAO.getInstance().update(user);
    }


    /**
     * Does what is necessary to rename a user
     *
     * @param user        The user
     * @param newUsername The new username
     */
    private static void renameUser(User user, String newUsername) throws Exception {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        String userDirPath = exportPath + user.getUsername() + "/";

        // delete old directory
        File userDir = new File(userDirPath);
        FileUtils.deleteDirectory(userDir);

        // create new directory and copy ca.crt
        prepareFilesystem(newUsername);

        user.setUsername(newUsername);

        // delete old certificate
        if (user.getX509id() != -1) {
            X509 x509 = new X509(user.getX509id());
            x509 = X509DAO.getInstance().read(x509);
            X509DAO.getInstance().delete(x509);
        }

        // create new certificate
        if (Configuration.getInstance().CA_ENABLED) {
            X509Generator gen = new X509Generator();
            try {
                gen.createClientCertImmediately(user);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "error while creating client certificate. Message: " + ex.getMessage(), ex);
                throw ex;
            }
        }
    }


    /**
     * prepares the filesystem for the user
     * creates a directory for the user
     *
     * @throws Exception
     */
    public static void prepareFilesystem(String username) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        exportPath = exportPath + username + "/";

        if (!new File(exportPath).exists()) {
            boolean success = (new File(exportPath)).mkdirs();
            if (!success)
                throw new Exception(rb.getString("dialog.cert.exporterror"));
        }

        addCaToFilesystem(username);
    }
    
    
    /**
     * Sets cn and ou of the given user with data of its certificate
     * @param user The user to update
     */
    public static void reassignToX509(User user) {
        X509 x509 = new X509(user.getX509id());
        x509 = x509.getX509DAO().read(x509);
        
        if (x509 != null) {
            user.setCn(X509Utils.getCnFromSubject(x509.getSubject()));
            user.setOu(X509Utils.getOuFromSubject(x509.getSubject()));
            UserDAO.getInstance().update(user);
        }
    }


    /**
     * Copies the root CA to the user directory
     *
     * @param username The username
     * @throws Exception
     */
    private static void addCaToFilesystem(String username) throws Exception {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        exportPath = FileUtils.unifyPath(exportPath);
        exportPath = exportPath + username + "/";

        X509 rootX509 = X509Utils.loadRootX509();
        X509Exporter.exportCertToFile(rootX509, exportPath);
    }

    
    public static void updateSyncedServers(String userId) {
    	Vector<String> serverIds = null;
    	
    	try {
    		serverIds = ServerQueries.getServersForUser(Integer.parseInt(userId));
    	} catch (Exception e) {
    		System.out.println("Can't add process NOT_SYNCED_SERVERS list: "+e.toString());
    		logger.warning("Can't add process NOT_SYNCED_SERVERS list: "+e.toString());
    		return;
    	}
    	
    	for ( String server : serverIds ) {
    		if(!Configuration.getInstance().NOT_SYNCED_SERVERS.contains(server))
    			Configuration.getInstance().NOT_SYNCED_SERVERS.addElement(server);  	
    	}
    }
}
