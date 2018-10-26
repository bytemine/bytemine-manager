/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import java.io.File;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusFrame;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.LdapConnector;
import net.bytemine.manager.utility.X509FileImporter;
import net.bytemine.manager.utility.X509LdapImporter;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.utility.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * methods for importing X509 certificates and users from LDAP
 *
 * @author Daniel Rauer
 */
public class UserImportLdap extends UserImport {

    private static Logger logger = Logger.getLogger(UserImportLdap.class.getName());
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();


    private static UserImportLdap instance;
    private boolean errorOccured = false;

    private UserImportLdap(boolean createCertificatesForUsers) {
        super(createCertificatesForUsers);
    }

    public static UserImport getInstance(boolean createCertificatesForUsers) {
        if (instance == null) {
            UserImport.resetCounters();
            instance = new UserImportLdap(createCertificatesForUsers);
        }
        return instance;
    }


    /**
     * Does all the import job
     *
     */

    public void importUsers() {
        try {
            // status frame
            final StatusFrame statusFrame = new StatusFrame(StatusFrame.TYPE_LDAP, ManagerGUI.mainFrame);
            statusFrame.show();

            SwingWorker<String, Void> importWorker = new SwingWorker<String, Void>() {
                Thread t;

                protected String doInBackground() {
                    try {
                        t = Thread.currentThread();
                        ThreadMgmt.getInstance().addThread(t);

                        statusFrame.updateStatus(rb.getString("status.msg.ldap.initialize"));

                        //import
                        loadAndImport();

                        // update messages, show statistic
                        statusFrame.updateStatus(rb.getString("status.msg.ldap.done"));

                        String generatedUsersStr = formatStatusNumber(generatedUsers);
                        String importedUsersStr = formatStatusNumber(importedUsers);
                        String generatedCertsStr = formatStatusNumber(generatedCerts);
                        String importedCertsStr = formatStatusNumber(importedCerts);
                        String notLinkedCertsStr = formatStatusNumber(notLinkedCerts);
                        String notLinkedUsersStr = formatStatusNumber(notLinkedUsers);

                        statusFrame.addDetailsText(generatedUsersStr + " " + rb.getString("status.msg.ldap.newusers"));
                        statusFrame.addDetailsText(importedUsersStr + " " + rb.getString("status.msg.ldap.importedusers"));
                        statusFrame.addDetailsText(generatedCertsStr + " " + rb.getString("status.msg.ldap.newcerts"));
                        statusFrame.addDetailsText(importedCertsStr + " " + rb.getString("status.msg.ldap.importedcerts"));
                        statusFrame.addDetailsText(notLinkedUsersStr + " " + rb.getString("status.msg.ldap.notlinkedusers"));
                        statusFrame.addDetailsText(notLinkedCertsStr + " " + rb.getString("status.msg.ldap.notlinkedcerts"));

                        statusFrame.showDetails();

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "", e);
                        statusFrame.updateStatus(e.getMessage());
                        errorOccured = true;
                    }

                    return "";
                }


                protected void done() {
                    statusFrame.done();
                    instance = null;

                    if (!Configuration.getInstance().isRootCertExisting() && !errorOccured) {
                        // show special root import dialog
                        Dialogs.showRootCertDialog(ManagerGUI.mainFrame);
                    }

                    ManagerGUI.refreshAllTables();
                    ManagerGUI.reloadServerUserTree();

                    ManagerGUI.mainFrame.toFront();
                    statusFrame.toFront();

                    ThreadMgmt.getInstance().removeThread(t);
                }
            };

            importWorker.execute();


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error importing users", e);
            throw e;
        }
    }


    /**
     * Loads users and certificates from ldap
     * and imports them
     *
     * @throws java.lang.Exception
     */
    private void loadAndImport() throws Exception {
        LdapConnector lc = new LdapConnector();
        if (lc.isLoadCertificateFromLDAP()) {
            Hashtable<String, byte[]> usersAndCertificates = lc.getAllPersonWithCertificates();
            Hashtable<String, String> users = importUsersFromLdap(usersAndCertificates.keySet());
            importCertificates(users, usersAndCertificates);
        } else {
            Set<String> usernames = lc.getAllPerson();
            Hashtable<String, String> users = importUsersFromLdap(usernames);
            importCertificatesFromFilesystem(users, lc.getCertImportDir());
        }

    }


    /**
     * Imports the certificates
     *
     * @param users                Hashtable with <username,userid>
     * @param usersAndCertificates Hashtable with <username,certificate>
     * @throws java.lang.Exception
     */
    private void importCertificates(
            Hashtable<String, String> users,
            Hashtable<String, byte[]> usersAndCertificates
    ) throws Exception {

        for (String username : usersAndCertificates.keySet()) {
            byte[] cert = usersAndCertificates.get(username);

            String userid = users.get(username);
            User user = new User(userid);
            user = UserDAO.getInstance().read(user);

            String content = Base64.encodeBytes(cert);
            X509LdapImporter importer = new X509LdapImporter(content);
            int x509id = importer.importCertificate(user);

            if (x509id > -1) {
                user.setX509id(x509id);
                UserDAO.getInstance().update(user);
            }
        }
    }


    /**
     * Imports the certificates from filesystem
     *
     * @param users     Hashtable with <username,userid>
     * @param importDir The import directory
     * @throws java.lang.Exception
     */
    private void importCertificatesFromFilesystem(
            Hashtable<String, String> users,
            String importDir
    ) throws Exception {

        // load certificate files and retrieve the cns from the subjects
        Hashtable<String, String> cnAndContent = readCertificates(importDir);

        // iterate over loaded users
        for (String username : users.keySet()) {
            try {
                String userid = users.get(username);
                User user = new User(userid);
                user = UserDAO.getInstance().read(user);

                String content = cnAndContent.get(username);
                if (content != null) {
                    X509LdapImporter importer = new X509LdapImporter(content);
                    int x509id = importer.importCertificate(user);

                    if (x509id > -1) {
                        logger.info("user " + username + " and certificate " + x509id + " matched");
                        user.setX509id(x509id);
                        UserDAO.getInstance().update(user);
                    }
                }
            } catch (Exception e) {
                logger.info("for user " + username + " could no certificate be found");
            }
        }

    }


    /**
     * Read a certificate from file system
     *
     * @param importDir The directory to import the certificates from
     * @return A Hastable with <CN, Content>
     */
    private Hashtable<String, String> readCertificates(String importDir) throws Exception {
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        if (importDir == null || "".equals(importDir))
            return returnTable;

        File importDirectory = new File(importDir);
        if (!importDirectory.exists() || !importDirectory.isDirectory())
            throw new Exception(rb.getString("error.ldap.importdirectory"));

        Arrays.stream(Objects.requireNonNull(importDirectory.listFiles())).forEach(file -> {
            try {
                X509FileImporter importer = new X509FileImporter(file);
                String content = importer.readCertificate();
                X509Certificate cert = X509Utils.regainX509Certificate(content);
                if (cert != null) {
                    String subject = cert.getSubjectDN().getName();
                    String cn = X509Utils.getCnFromSubject(subject);
                    returnTable.put(cn, content);
                    logger.info("Subject in certificate file: " + subject);
                }
            } catch (Exception e) {
                logger.warning("potential certificate file could not be processed: " + file.getName());
            }
        });

        return returnTable;
    }


    /**
     * Imports the users from the given Set
     *
     * @param ldapUsers A Set with usernames from LDAP
     * @return A Hashtable with <username,userid>
     */
    private Hashtable<String, String> importUsersFromLdap(Set<String> ldapUsers) {
        Hashtable<String, String> existingUsers = UserQueries.getUserTable(true);
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        for (String username : ldapUsers) {
            User user;
            if (!existingUsers.containsKey(username)) {
                // user is not existing in database
                // do not crypt the already crypted password
                user = new User(username, -1);
                UserImport.incGeneratedUsers();
                returnTable.put(username, user.getUserid() + "");
            } else {
                // load the existing user
                String userIdStr = existingUsers.get(username);
                user = new User(userIdStr);
                user = UserDAO.getInstance().read(user);
                returnTable.put(username, user.getUserid() + "");
            }
        }
        return returnTable;
    }


    public static void main(String[] args) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            UserImportLdap im = new UserImportLdap(true);
            im.importUsers();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
    }

}
