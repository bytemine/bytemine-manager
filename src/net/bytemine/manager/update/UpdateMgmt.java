/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.update;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.db.LicenceQueries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.UpdateDialog;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.ChecksumUtils;
import net.bytemine.utility.FileUtils;
import net.bytemine.utility.StringUtils;
import net.sourceforge.yamlbeans.YamlReader;


/**
 * Triggers the update search, and displays a dialog
 *
 * @author Daniel Rauer
 */
public class UpdateMgmt {

    private static Logger logger = Logger.getLogger(UpdateMgmt.class.getName());

    private boolean updateSuccess= false;

    private String newVersion;
    private String downloadFilename;
    private String checksumFromYaml;
    private String[] jars;

    private static UpdateMgmt instance;
    private boolean displayNoUpdate;
    private boolean emptyYaml = false;
    private Updater updater;
    private List<RepoEntry> repoEntries = new ArrayList<>();
    private String changeLog = null;


    private UpdateMgmt(boolean displayNoUpdate) {
        this.displayNoUpdate = displayNoUpdate;

        updater = new Updater();
    }

    public static UpdateMgmt getInstance() {
        if (instance == null)
            instance = new UpdateMgmt(true);
        return instance;
    }

    public static UpdateMgmt getInstance(boolean displayNoUpdate) {
        if (instance == null)
            instance = new UpdateMgmt(displayNoUpdate);
        return instance;
    }


    public void cancelUpdate() {
        updater.disconnect();

        File downloadedFile = new File(Constants.UPDATE_PATH + downloadFilename);
        if (downloadedFile.exists())
            downloadedFile.delete();
    }


    /**
     * Searches for Updates
     */
    public void searchForUpdates() {

        SwingWorker<String, Void> httpsWorker = new SwingWorker<String, Void>() {
            Thread t;
            boolean errorOccurred = false;

            protected String doInBackground() {
                try {
                    ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                    t = Thread.currentThread();
                    ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.update.tooltip"));

                    // send request to server
                    String response = updater.askForUpdate();
                    if (response == null || response.length() == 0)
                        emptyYaml = true;
                    else {
                        // process the responded yaml
                        processYAML(response);

                        // get changelogs
                        getAllChangeLogs();
                    }

                } catch (Exception e) {
                    errorOccurred = true;
                    logger.log(Level.SEVERE, "error searching for a new update", e);
                    new VisualException(e.getMessage());
                }

                return "";
            }

            /**
             * Processes the responded yml file
             * @param content The yml files content
             * @throws Exception
             */
            private void processYAML(String content) throws Exception {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                try {
                    YamlReader reader = new YamlReader(content);
                    Object yaml = reader.read();
                    if (yaml instanceof List<?>) {
                        List<Object> list = (List<Object>) yaml;
                        list.stream().map(obj -> (HashMap<String, Object>) obj).map(RepoEntry::new).forEach(entry -> {
                            repoEntries.add(entry);
                            if (entry.getFilename().startsWith("bytemine-manager")) {
                                newVersion = entry.getVersion();
                                downloadFilename = entry.getFilename();
                                checksumFromYaml = entry.getChecksum();
                                jars = entry.getJars();
                            }
                        });
                    } else {
                        throw new Exception(rb.getString("dialog.update.error_update_location"));
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error processing update yaml", e);
                    throw new Exception(rb.getString("dialog.update.error_update_location"));
                }
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);

                if (!errorOccurred && (isUpdateAvailable() || displayNoUpdate))
                    UpdateDialog.getInstance().showUpdateDialog(
                            ManagerGUI.mainFrame, instance, changeLog);

            }
        };
        httpsWorker.execute();

    }


    /**
     * Searches for Updates
     */
    public void downloadUpdates() {

        SwingWorker<String, Void> httpsWorker = new SwingWorker<String, Void>() {
            Thread t;

            protected String doInBackground() throws Exception {

                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                t = Thread.currentThread();
                ThreadMgmt.getInstance().addThread(t, rb.getString("statusBar.update.tooltip"));

                // send request to server
                byte[] file = updater.downloadUpdate(downloadFilename);
                if (ChecksumUtils.checkSHA1Checksum(file, checksumFromYaml)) {

                    File downloadDir = new File(Constants.UPDATE_PATH );

                    // assure our download directory exists
                    if (!downloadDir.isDirectory())
                        downloadDir.mkdir();

                    FileUtils.writeBytesToFile(file, Constants.UPDATE_PATH + downloadFilename);
                } else {
                    // throw an exception

                }

                if (jars.length > 0) {
                    // assure our lib directory exists
                    File jarDir = new File(Constants.UPDATE_JAR_PATH );
                    if (!jarDir.isDirectory())
                        jarDir.mkdir();
                    
                    // new jars to grab
                    for (String jar : jars) {
                        boolean entryFound = false;
                        byte[] downloadJar = updater.downloadUpdate(jar);

                        for (RepoEntry entry : repoEntries) {
                            if (entry.getFilename().equals(jar)) {
                                entryFound = true;
                                if (ChecksumUtils.checkSHA1Checksum(downloadJar, entry.getChecksum())) {
                                    FileUtils.writeBytesToFile(file, Constants.UPDATE_JAR_PATH +
                                            System.getProperty("file.separator") +
                                            entry.getFilename());
                                } else {
                                    // throw Exception
                                }
                            }
                        }

                        if (!entryFound) {
                            // throw exception
                        }

                    }

                }

                updateSuccess= true;
                return "";
            }


            protected void done() {
                ThreadMgmt.getInstance().removeThread(t);

                UpdateDialog updateDialog = UpdateDialog.getInstance();
                if (updateSuccess) {
                    updateDialog.downloadFinished();
                } else {
                    updateDialog.dispose();
                    ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                    new VisualException(rb.getString("dialog.update.error_checksum"));
                }
            }
        };
        httpsWorker.execute();

    }



    /**
     * detects if an update is available
     *
     * @return true, if an update is available
     */
    public boolean isUpdateAvailable() {
        String currentVersion = Configuration.getInstance().MANAGER_VERSION;
        return !currentVersion.equals(newVersion) && !emptyYaml;

    }


    /**
     * Fetches all changeLogs for this particular update
     *
     * @throws Exception
     */
    private void getAllChangeLogs() throws Exception {
        StringBuilder changeLogs = new StringBuilder();
        String currentVersion = Configuration.getInstance().MANAGER_VERSION;
        String[] currentTokens = StringUtils.tokenize(currentVersion);

        Collections.sort(repoEntries);

        for (RepoEntry entry : repoEntries) {
            if ((entry.isNewerThanCurrentVersion(currentTokens)) &&
                    (entry.getChangelog() != null))
                changeLogs.append(updater.askForChangelog(entry.getChangelog()));
        }

        changeLog = changeLogs.toString();
    }


    /**
     * Checks if a keystore is stored in the database
     *
     * @return true if the keystrore exists
     */
    public boolean isUpdateKeystoreExisting() {
        byte[] keystore = LicenceQueries.getKeystore();
        return keystore != null;
    }


    public String getDownloadFilename() {
        return downloadFilename;
    }

    public String getNewVersion() {
        return newVersion;
    }

}
