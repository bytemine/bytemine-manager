/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import java.io.File;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.bean.CRL;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.*;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.StatusFrame;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Generator;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.ssh.SSHTool;
import net.bytemine.utility.*;


/**
 * syncs users from local db with user file on server
 *
 * @author Daniel Rauer
 */
public class UserSync {

    private static Logger logger = Logger.getLogger(UserSync.class.getName());

    private Server server;
    private ScpTool scpTool;
    private SSHTool sshTool;
    private String serverId;

    private int newUsers = 0;        // users created
    private int updatedUsers = 0;
    private int addUsers = 0;        // (new) users added to a server
    private int delUsers = 0;        // users deleted from a server

    private int exportedCrls = 0;
    private int exportedRootCerts = 0;
    private int exportedServerCerts = 0;
    private int exportedPasswds = 0;

    private int ccfiles = 0;

    final StatusFrame statusFrame;
    final ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    // XXX TODO
    final boolean updatePassword = false;


    /**
     * Method to sync users to the provided server.
     *
     * @param serverIdStr of the server to be synced.
     */
    public UserSync(String serverIdStr) {
        try {
            serverId = serverIdStr;
            server = new Server(serverId);
            server = ServerDAO.getInstance().read(server);

            statusFrame = new StatusFrame(StatusFrame.TYPE_SYNC, ManagerGUI.mainFrame, server);

            // status frame
            statusFrame.show();

            logger.info("Synching server: " + serverId);

            SwingWorker<String, Void> copyWorker = new SwingWorker<String, Void>() {
                Thread t;

                protected String doInBackground() {
                    try {
                        t = Thread.currentThread();
                        ThreadMgmt.getInstance().addThread(t);

                        statusFrame.updateStatus(rb.getString("status.msg.sync.initialize") + server.getHostname() + " ...");
                        scpTool = new ScpTool(server);
                        sshTool = new SSHTool(server);
                        statusFrame.setScpTool(scpTool);

                        // prepare for synchronisation
                        prepareServerFileSystem();

                        // do all the synchronisation
                        syncUsers();

                        // FIXME: atm: only for testing
                        // updateServerConfigOnServer(new ServerConfig(server).createConfig());

                        // update messages, show statistic
                        statusFrame.updateStatus(rb.getString("status.msg.sync.done"));

                        String newUsersStr = (newUsers < 10 ? "  " + newUsers : newUsers + "");
                        String updatedUsersStr = (updatedUsers < 10 ? "  " + updatedUsers : updatedUsers + "");

                        statusFrame.addDetailsText(newUsersStr + " " + rb.getString("status.msg.sync.newusers"));
                        statusFrame.addDetailsText("  " + addUsers + " " + rb.getString("status.msg.sync.addusers"));
                        statusFrame.addDetailsText("  " + delUsers + " " + rb.getString("status.msg.sync.delusers"));
                        if (updatePassword)
                            statusFrame.addDetailsText(updatedUsersStr + " " + rb.getString("status.msg.sync.updatedusers"));
                        statusFrame.addDetailsText("");
                        statusFrame.addDetailsText("  " + rb.getString("status.msg.sync.exportedfiles"));
                        statusFrame.addDetailsText("     " + exportedRootCerts + " " + rb.getString("status.msg.sync.exportedRootcerts"));
                        statusFrame.addDetailsText("     " + exportedServerCerts + " " + rb.getString("status.msg.sync.exportedservercerts"));
                        statusFrame.addDetailsText("     " + exportedCrls + " " + rb.getString("status.msg.sync.exportedcrls"));
                        if (exportedPasswds > 0)
                            statusFrame.addDetailsText("     " + exportedPasswds + " " + rb.getString("status.msg.sync.exportedPasswds"));
                        if (server.getVpncc())
                            statusFrame.addDetailsText("     " + ccfiles + " " + rb.getString("status.msg.sync.ccfiles"));
                        statusFrame.showDetails();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "", e);
                        String error = "<html>" + rb.getString("error.syncusers.scpTo") + ":<br><br><small>" +
                                e.getMessage() + "</small></html>";
                        statusFrame.updateStatus(error);
                    }

                    return "";
                }


                protected void done() {
                    statusFrame.done();
                    if (scpTool != null)
                        scpTool.disconnectSession();

                    ManagerGUI.refreshAllTables();
                    ManagerGUI.reloadServerUserTree();

                    ThreadMgmt.getInstance().removeThread(t);

                    BatchUserSync userSync = BatchUserSync.getInstance();
                    userSync.startNextSync();
                }
            };

            copyWorker.execute();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "UserSync Exception", e);
            throw e;
        }

        Configuration.getInstance().NOT_SYNCED_SERVERS.remove(serverIdStr);
    }


    /**
     * syncs users from local db with user file on server
     *
     * @throws Exception
     */
    private void syncUsers() throws Exception {
        boolean usePAM = Configuration.getInstance().USE_PAM;
        boolean importUsers = Configuration.getInstance().IMPORT_USERS_ON_SYNCHRONISATION;

        if (!usePAM && isPasswdExisting() && importUsers) {
            try {
                String content = getUserfileFromServer();
                importUsersFromFile(content);
            } catch (Exception e) {
                logger.log(Level.WARNING, "error while reading passwd-file", e);
                statusFrame.addDetailsText(rb.getString("status.msg.sync.passwdmissing"));
            }
        }

        exportCertsAndKeys();

        // if staticIp is enabled, push it
        if (server.getVpncc())
            pushToCCD();

        if (!usePAM) {
            String newContent = generateNewUserfile();
            compareUserfiles(newContent);
            updateUserfileOnServer(newContent);
        }
    }

    /**
     * prepares the server filesystem for syncing
     *
     * @throws Exception if anything goes wrong on the server
     *                   currently only for boa
     */
    private void prepareServerFileSystem() throws Exception {
        if (Server.SERVER_TYPE_BYTEMINE_APPLIANCE == server.getServerType()) {
            String passwdPath = server.getUserfilePath();
            sshTool.exec("mkdir -p " + server.getExportPath());
            sshTool.exec("mkdir -p " + passwdPath.substring(0, passwdPath.lastIndexOf("/")));
            sshTool.exec("touch " + server.getUserfilePath());
            if (server.getVpncc())
                sshTool.exec("mkdir -p " + server.getVpnccpath());
        }
    }

    /**
     * checks if a passwd-file was specified
     *
     * @return true or false
     */
    private boolean isPasswdExisting() {
        if (server.getUserfilePath() == null)
            return false;
        return !"".equals(server.getUserfilePath());
    }


    /**
     * loads the file from the server and returns the content as string
     *
     * @return The content of the file
     * @throws Exception
     */
    private String getUserfileFromServer() throws Exception {
        statusFrame.updateStatus(rb.getString("status.msg.sync.getfile") + ": " + server.getUserfilePath());
        byte[] content = scpTool.getFromServer(server.getUserfilePath());
        return (StringUtils.bytes2String(content));
    }


    /**
     * Copies the content to the specified file on the server
     *
     * @param content The new content of the file
     * @throws Exception
     */
    private void updateUserfileOnServer(String content) throws Exception {
        statusFrame.updateStatus(rb.getString("status.msg.sync.postfile") + ": " + server.getUserfilePath());
        scpTool.postToServer(content, server.getUserfilePath());
        exportedPasswds++;
    }


    /**
     * Compare the content of two userfiles
     *
     * @param userfileLocal The new content of the userfile
     * @throws Exception
     */
    private void compareUserfiles(String userfileLocal) throws Exception {

        byte[] tmp = scpTool.getFromServer(server.getUserfilePath());
        String userfileServer = new String(tmp);

        String userStrLocal = null;
        String userStrServer = null;
        boolean newUser = false;

        try {
            // detect 'added' users
            for (String userEntryLocal : userfileLocal.split("\n")) {
                newUser = true;
                userStrLocal = userEntryLocal.split(":")[0];
                for (String userEntryServer : userfileServer.split("\n")) {
                    userStrServer = userEntryServer.split(":")[0];
                    if (userStrLocal.equals(userStrServer)) {
                        newUser = false;
                        break;
                    }
                }
                if (newUser)
                    addUsers++;
            }

            // detect 'deleted' users
            for (String userEntryServer : userfileServer.split("\n")) {
                newUser = true;
                userStrServer = userEntryServer.split(":")[0];
                for (String userEntryLocal : userfileLocal.split("\n")) {
                    userStrLocal = userEntryLocal.split(":")[0];
                    if (userStrLocal.equals(userStrServer)) {
                        newUser = false;
                        break;
                    }
                }
                if (newUser)
                    delUsers++;
            }
        } catch (Exception e) {
            logger.warning("Error in synchronisation - analyse passwds: " + e.toString());
        }
    }


    /**
     * Exports client crt and key files to the server
     *
     * @throws Exception
     */
    private void exportCertsAndKeys() throws Exception {
        // export path
        String path = server.getExportPath();
        if (path != null && !path.endsWith("/"))
            path = path + "/";

        // export server.crt
        X509 serverX509 = X509.getX509ById(server.getX509id());
        if (serverX509 != null) {
            ResourceBundle serverCertBundle = ResourceBundle.getBundle(Constants.SERVER_BUNDLE_NAME);
            String certFilename = serverCertBundle.getString("export_cert_file");
            String keyFilename = certFilename.substring(0, certFilename.lastIndexOf(".")) + "." + Constants.DEFAULT_KEY_EXTENSION;

            // export certificate
            logger.info("Sending server certificate to server: " + path + certFilename);
            scpTool.postToServer(serverX509.getContent(), path + certFilename);

            // export server key
            logger.info("Sending server key to server: " + path + keyFilename);
            scpTool.postToServer(serverX509.getKeyContent(), path + keyFilename);
            exportedServerCerts++;
        } else
            // no server certificate existing!
            throw new Exception(rb.getString("error.syncusers.server"));

        // export root.crt
        X509 rootX509 = X509Utils.loadRootX509();
        if (rootX509 != null) {
            String x509Filename = rootX509.getFileName();
            String certFilename = x509Filename.substring(x509Filename.lastIndexOf("/") + 1);

            // export certificate
            logger.info("Sending root certificate to server: " + path + certFilename);
            scpTool.postToServer(rootX509.getContent(), path + certFilename);
            exportedRootCerts++;
        } else
            // no root certificate existing!
            throw new Exception(rb.getString("error.syncusers.root"));

        // re-generate the CRL
        X509Generator g = new X509Generator();
        g.createCRLImmediately();

        // export CRL
        int crlId = CRLQueries.getMaxCRLId();
        if (crlId > 0) {
            CRL crl = new CRL(crlId);
            crl = CRLDAO.getInstance().read(crl);
            if (crl != null && crl.getContent() != null) {
                String filename = Constants.DEFAULT_CRL_FILENAME;
                logger.info("Sending crl.pem to server: " + path + filename);
                scpTool.postToServer(crl.getContent(), path + filename);
                exportedCrls++;
            }
        }

        // export DH
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;
        String keyStrength = Configuration.getInstance().X509_KEY_STRENGTH;
        String filename = "dh" + keyStrength + ".pem";
        File dhFile = new File(exportPath + "/" + filename);
        if (dhFile.exists()) {
            logger.info("Sending DH parameters '" + filename + "' to server: " + path + filename);
            String content = FileUtils.readFile(dhFile);
            scpTool.postToServer(content, path + filename);
        }
    }


    /**
     * Copies the content to the specified file on the server
     *
     * @param content The new content of the file
     * @throws Exception
     */
    private void updateServerConfigOnServer(String content) throws Exception {
        //  statusFrame.updateStatus(rb.getString("status.msg.sync.postfile") + ": " + server.getUserfilePath());
        scpTool.postToServer(content, FileUtils.appendPathSeparator(server.getExportPath()) + "server.conf"); //#FIXME: should it be posted to the export-path ?
        //   exportedPasswds++;
    }


    /**
     * Push the client-configuration-file to the given directory
     *
     * @throws Exception
     */
    private void pushToCCD() throws Exception {
        Vector<String> connectedUsers = UserQueries.getUsersForServer(server.getServerid());

        for (String userId : connectedUsers) {
            String cn = User.getUserByID(Integer.parseInt(userId)).getCn();
            String file = createCC(Integer.toString(server.getServerid()), userId);

            // copy the file to the server   
            scpTool.postToServer(file, FileUtils.appendPathSeparator(server.getVpnccpath()) + cn);

            ccfiles++;
        }
    }


    /**
     * Create the client-configuration-file
     *
     * @param serverId The ID of the server
     * @param userId   The ID of the user
     * @return A String representing the 'ifconfig-push' option
     */
    public static String createCC(String serverId, String userId) {
        /*
            * Creates the following file:
            *
            * filename (common-name):
            * ifconfig-push  x.x.x.y x.x.x.z
            *
            */
        int lastIpNumber = 0;
        String ip = ServerQueries.getIpFromUserServer(serverId, userId);
        String networkAddress = ServerQueries.getServerDetails(serverId)[19];

        try {
            lastIpNumber = Integer.parseInt(ip);
        } catch (Exception e) {
            logger.severe("ip " + ip + " is not valid / wrong formatted" + e.toString());
        }

        lastIpNumber++;
        String lastIp = networkAddress + Integer.toString(lastIpNumber);
        return "ifconfig-push " + networkAddress + ip + " " + lastIp + "\n";
    }


    /**
     * Imports the users from the content
     *
     * @param content The content of the file
     */
    private void importUsersFromFile(String content) {
        statusFrame.updateStatus(rb.getString("status.msg.sync.syncusers"));

        Hashtable<String, String> existingUsers = UserQueries.getUserTable(true);

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
                    user = new User(username, password, -1, false);
                    UserImport.incImportedUsers();
                    newUsers++;

                    //link server and user
                    user.addServer(server.getServerid());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                logger.info("Updating user: " + username);
                // load the existing user
                String userIdStr = existingUsers.get(username);
                user = new User(userIdStr);
                user = UserDAO.getInstance().read(user);

                if (updatePassword) {
                    // update password
                    user.updatePasswordWithoutCrypt(password);
                }
                updatedUsers++;

                //link server and user
                user.addServer(server.getServerid());
            }
        }
    }


    /**
     * Generates the content of the new userfile
     *
     * @return the new content as String
     */
    private String generateNewUserfile() {
        Hashtable<String, String> existingUsers = UserQueries.getUserTableForServer(
                server, false, false
        );
        StringBuilder output = new StringBuilder();
        existingUsers.keySet().forEach(username -> {
            String password = existingUsers.get(username);
            if (password != null && !"".equals(password)) {
                String newLine = username + ":" + password + "\n";
                output.append(newLine);
            } else {
                // password is empty, do not export user!
            }
        });

        return output.toString();
    }


    public static void main(String[] args) {
        try {
            UserSync sync = new UserSync("1");
            sync.generateNewUserfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
