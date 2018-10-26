/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Security;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.action.X509Action;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.UserDAO;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.db.X509Queries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusFrame;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509FileImporter;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.utility.HashtableUtils;
import net.bytemine.utility.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * methods for importing X509 certificates and users from file system
 *
 * @author Daniel Rauer
 */
public class UserImportFile extends UserImport {

    private static Logger logger = Logger.getLogger(UserImportFile.class.getName());
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private String importDir;
    private String userfile;

    private static UserImportFile instance;

    private UserImportFile(boolean createCertificatesForUsers) {
        super(createCertificatesForUsers);
        this.importDir = Configuration.getInstance().CLIENT_CERT_IMPORT_DIR;
        this.userfile = Configuration.getInstance().CLIENT_USERFILE;
    }

    public static UserImportFile getInstance(boolean createCertificatesForUsers) {
        if (instance == null) {
            UserImport.resetCounters();
            instance = new UserImportFile(createCertificatesForUsers);
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
            final StatusFrame statusFrame = new StatusFrame(StatusFrame.TYPE_IMPORT, ManagerGUI.mainFrame);
            statusFrame.show();
            
            SwingWorker<String, Void> importWorker = new SwingWorker<String, Void>() {
                Thread t;

                protected String doInBackground() {
                    try {
                        t = Thread.currentThread();
                        ThreadMgmt.getInstance().addThread(t);

                        importUsersLogic(statusFrame);
                        
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "error while importing users", e);
                        statusFrame.updateStatus(e.getMessage());
                    }
                    return "";
                }


                protected void done() {
                    statusFrame.done();
                    instance = null;

                    if (!Configuration.getInstance().isRootCertExisting()) {
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
     * Does all the import job (no threading)
     *
     */
    
    public void importUsersImmediately() {
    	try {
            // status frame
            final StatusFrame statusFrame = new StatusFrame(StatusFrame.TYPE_IMPORT, ManagerGUI.mainFrame);
            statusFrame.show();

    		try {
                importUsersLogic(statusFrame);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "error while importing users", e);
                statusFrame.updateStatus(e.getMessage());
            }


            statusFrame.done();
            instance = null;

            if (!Configuration.getInstance().isRootCertExisting()) {
                // show special root import dialog
                Dialogs.showRootCertDialog(ManagerGUI.mainFrame);
            }

            if(!(ManagerGUI.mainFrame==null)) {
              ManagerGUI.refreshAllTables();
              ManagerGUI.reloadServerUserTree();

              ManagerGUI.mainFrame.toFront();
            }
            statusFrame.toFront();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error importing users", e);
            throw e;
        }
    }

    
    /**
     * Import the users while showing the status
     *
     * @param statusFrame The frame where import results are shown
     * @throws Exception
     */
    private void importUsersLogic(StatusFrame statusFrame) throws Exception {
    	statusFrame.updateStatus(rb.getString("status.msg.import.initialize"));

        Vector<String> importedX509Ids = importCertificates();
        Hashtable<String, String> importedUsersTable = importUserfile();	// contains only the (new) users
        Hashtable<String, String> usersWithoutCertificate = tryLinkingCertificatesAndUsers(importedX509Ids, importedUsersTable);
        assert usersWithoutCertificate != null;
        if (!usersWithoutCertificate.isEmpty()) {
            if (isCreateCertificatesForUsers())
                createClientCertificates(usersWithoutCertificate);
            else
                UserImport.incNotLinkedUsers(usersWithoutCertificate.size());
        }

        // update messages, show statistic
        statusFrame.updateStatus(rb.getString("status.msg.import.done"));

        String generatedUsersStr = formatStatusNumber(generatedUsers);
        String importedUsersStr = formatStatusNumber(importedUsers);
        String generatedCertsStr = formatStatusNumber(generatedCerts);
        String importedCertsStr = formatStatusNumber(importedCerts);
        String notLinkedCertsStr = formatStatusNumber(notLinkedCerts);
        String notLinkedUsersStr = formatStatusNumber(notLinkedUsers);

        statusFrame.addDetailsText(generatedUsersStr + " " + rb.getString("status.msg.import.newusers"));
        statusFrame.addDetailsText(importedUsersStr + " " + rb.getString("status.msg.import.importedusers"));
        statusFrame.addDetailsText(generatedCertsStr + " " + rb.getString("status.msg.import.newcerts"));
        statusFrame.addDetailsText(importedCertsStr + " " + rb.getString("status.msg.import.importedcerts"));
        statusFrame.addDetailsText(notLinkedUsersStr + " " + rb.getString("status.msg.import.notlinkedusers"));
        statusFrame.addDetailsText(notLinkedCertsStr + " " + rb.getString("status.msg.import.notlinkedcerts"));
        statusFrame.showDetails();
    }

    
    /**
     * Imports certificates and links them with users
     *
     * @return Vector with imported certificates
     * @throws Exception
     */
    private Vector<String> importCertificates() throws Exception {
        Vector<String> importedX509Ids = new Vector<String>();

        if (importDir == null || importDir.isEmpty())
            return importedX509Ids;

        X509FileImporter x509Importer;
        x509Importer = new X509FileImporter(new File(importDir));
        importedX509Ids = x509Importer.importClientCertsAndKeys(createUsersFromCN == YES);

        return importedX509Ids;
    }


    /**
     * tries to link imported users and imported certificates
     *
     * @param importedX509Ids The IDs of the imported certificates
     * @param importedUsers   The imported users with username and ID
     * @return Hashtable with users which could not be mapped to certificates
     */
    private Hashtable<String, String> tryLinkingCertificatesAndUsers(Vector<String> importedX509Ids, Hashtable<String, String> importedUsers) {
        Hashtable<String, String> usersWithoutCertificate = HashtableUtils.copyDeep(importedUsers);

        for (String x509id : importedX509Ids) {
            X509 x509 = X509.getX509ById(Integer.parseInt(x509id));
            String subject = x509.getSubject();
            String username = X509Utils.getCnFromSubject(subject);

            // link users and certificates
            String userid = importedUsers.get(username);
            if (userid != null) {

                // link the user to the cert in _unassigned
                if (linkCertificateAndUser(userid, x509)) {

                    try {
                        X509Action.exportToFilesystem(x509id);
                    } catch (Exception e) {
                        logger.severe("Couldn't export cert to userdir, aborting import process" + e.toString());
                        return null;
                    }


                    String oldX509File = x509.getPath() + x509.getFileName();
                    x509.setPath(Configuration.getInstance().CERT_EXPORT_PATH + File.separator +
                            username + File.separator);
                    x509.setFileName(username + "_" + Integer.toString(userid.hashCode()) + ".crt");

                    // link the user to the cert in his folder
                    if (linkCertificateAndUser(userid, x509)) {

                        usersWithoutCertificate.remove(username);
                        UserImport.decNotLinkedCerts();

                        new File(oldX509File).delete();
                        new File(oldX509File.replace(".crt", ".key")).delete();
                    }
                }
            }

        }
        
        // try to link the users with local existing certificates
        usersWithoutCertificate = tryLinkingWithExistingCertificate(usersWithoutCertificate);

        return usersWithoutCertificate;
    }
    
    
    /**
     * Try to find a local certificate that matches to a users CN
     * @param usersWithoutCertificate The imported users without certificate
     * @return The imported users still without certificate
     */
    private Hashtable<String, String> tryLinkingWithExistingCertificate(Hashtable<String, String> usersWithoutCertificate) {
        // every user has a certificate
        if (usersWithoutCertificate.isEmpty())
            return usersWithoutCertificate;
        
        Hashtable<String, String> usersWithoutCertificateClone = HashtableUtils.copyDeep(usersWithoutCertificate);
        for (String username : usersWithoutCertificateClone.keySet()) {
            int userid = UserQueries.getUserId(username);
            if (userid > -1) {
                User user = new User(userid + "");
                user = UserDAO.getInstance().read(user);
                // try to get a certificate matching the users CN
                int x509id = X509Queries.getCertificateByCN(user.getCn(), X509.X509_TYPE_CLIENT);
                if (x509id != -1) {
                    X509 x509 = X509.getX509ById(x509id);
                    // link them in the database
                    boolean linked = linkCertificateAndUser(userid + "", x509);
                    if (linked) {
                        usersWithoutCertificate.remove(username);
                        UserImport.decNotLinkedCerts();
                    }
                }
            }
        }
        return usersWithoutCertificate;
    }


    /**
     * Link user and certificate
     *
     * @param userid The userId
     * @param x509   The certificate object
     * @return true, if objects could be linked
     */
    private boolean linkCertificateAndUser(String userid, X509 x509) {
        if (userid != null) {
            User user = new User(userid);
            user = UserDAO.getInstance().read(user);
            if (user != null) {
                user.setX509id(x509.getX509id());
                UserDAO.getInstance().update(user);

                x509.setUserId(user.getUserid());
                X509DAO.getInstance().update(x509);

                return true;
            }
        }
        return false;
    }


    /**
     * Reads the userfile and triggers the user import
     *
     * @return Hashtable with <Username,Userid> of the imported users
     * @throws Exception
     */
    private Hashtable<String, String> importUserfile() throws Exception {

        // import userfile
        String content = readUserfile();
        return importUsersFromFile(content);
    }


    /**
     * Generates certificate for the imported users
     *
     * @param usersWithoutCertificate List with usernames
     */
    private void createClientCertificates(Hashtable<String, String> usersWithoutCertificate) throws Exception {
        X509Generator generator = new X509Generator();
        
        if (!Configuration.getInstance().isRootCertExisting()) {
            // show special root import dialog
            Dialogs.showRootCertDialog(ManagerGUI.mainFrame);
        }

        if (!Configuration.getInstance().isRootCertExisting()) {
            throw new VisualException(rb.getString("error.importClients.noroot"));
        }

        for (String username : usersWithoutCertificate.keySet()) {
            try {
                UserImport.incGeneratedCerts();

                User user = new User(usersWithoutCertificate.get(username));
                user = UserDAO.getInstance().read(user);

                logger.info("generating certificate for imported user: " + username);
                generator.createClientCertImmediately(user);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "error generating client certificate", e);
                throw new Exception(rb.getString("error.importClients.generateerror"));
            }
        }

        //#FIXME
        //  - tryLinkingWithExistingCertificate already called by tryLinkingCertificatesAndUsers
        //  - a function called createClientCertificates should maybe only create certs;
        
        // commented out
        
        // tryLinkingWithExistingCertificate(usersWithoutCertificate);
    }


    /**
     * Reads the userfile
     *
     * @return Content as String
     */
    private String readUserfile() throws Exception {
        if (this.userfile == null || "".equals(this.userfile))
            return null;

        logger.info("Reading username/password file: " + this.userfile);

        File certFile = new File(this.userfile);

        byte[] content = new byte[(int) certFile.length()];
        try {
            FileInputStream keyInputStream = new FileInputStream(certFile);
            keyInputStream.read(content);
            keyInputStream.close();

            return StringUtils.bytes2String(content);
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.WARNING, "userfile not found", fnfe);
            throw new Exception(rb.getString("error.importClients.fileerror"));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "userfile could not be read", ioe);
            throw new Exception(rb.getString("error.importClients.fileerror"));
        }
    }


    /**
     * Imports the users from the content
     *
     * @param content The content of the userfile
     * @return Hashtable with <username, userid>
     */
    private Hashtable<String, String> importUsersFromFile(String content) {
        Hashtable<String, String> existingUsers = UserQueries.getUserTable(true);
        Hashtable<String, String> returnTable = new Hashtable<String, String>();

        if (content != null) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line == null || !line.contains(":"))
                    continue;

                String username = line.substring(0, line.indexOf(":"));
                String password = line.substring(line.indexOf(":") + 1);

                User user;
                if (!existingUsers.containsKey(username)) {
                    // user is not existing in database
                    // do not crypt the already crypted password
                    try {
                        user = new User(username, password, -1, false, username, "");    // set cn=username
                        UserImport.incImportedUsers();
                        returnTable.put(username, user.getUserid() + "");
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                } else {
                    // load the existing user
                    String userIdStr = existingUsers.get(username);
                    user = new User(userIdStr);
                    user = UserDAO.getInstance().read(user);

                    // update the crypted password
                    user.updatePasswordWithoutCrypt(password);
                    //   returnTable.put(username, user.getUserid() + ""); #FIXME not necessary
                }
            }
        }
        return returnTable;
    }


    public static void main(String[] args) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            UserImportFile im = new UserImportFile(true);
            if (im.createUsersFromCN == UserImport.NOT_SET) {
                // show dialog
                im.createUsersFromCN = Dialogs.showCreateUserFromCNDialog(null);
            }
            im.importUsers();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
    }
}
