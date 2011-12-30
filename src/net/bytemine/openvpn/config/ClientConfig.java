/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.config;

import java.io.File;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.TemplateEngine;
import net.bytemine.utility.FileUtils;

/**
 * Creates a OpenVPN client config file
 * 
 * @author Daniel Rauer
 */
public class ClientConfig {

    private static Logger logger = Logger.getLogger(ClientConfig.class.getName());
    private User user = null;
    private X509 clientX509 = null;
    private HashMap<String, String> params = null;
    private String content = null;
    private String exportPath = null;
    private String configFilename = null;
    private Vector<String> serverlist = null;

    public ClientConfig(User user, Vector<String> serverlist) throws Exception {
        this.user = user;

        this.serverlist = serverlist;
        this.exportPath = Configuration.getInstance().CERT_EXPORT_PATH;
        this.exportPath = FileUtils.unifyPath(this.exportPath);
        this.exportPath = this.exportPath + this.user.getUsername() + "/";
        
        // return if no certificate is existing for this user
        clientX509 = X509.getX509ById(user.getX509id());
        if (clientX509 == null)
            return;
        
        prepareParams();
        createConfig();
    }

    /**
     * Retrieves all necessary data and puts it into a HashMap
     */
    private void prepareParams() throws Exception {
        this.params = new HashMap<String, String>();

        try {
            X509 rootX509 = X509Utils.loadRootX509();
            this.params.put(VPNConfigurationConstants.ROOT_CA, rootX509
                    .getFileName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error preparing params to merge into OpenVPN config template: ", e);
            throw e;
        }

        try {
            this.params.put(VPNConfigurationConstants.CRT, clientX509.getFileName());
            this.params.put(VPNConfigurationConstants.KEY, FileUtils
                    .replaceExtension(clientX509.getFileName(),
                            Constants.DEFAULT_KEY_EXTENSION));
            this.params.put(VPNConfigurationConstants.SERVER_NAME, "");
            this.params.put(VPNConfigurationConstants.SERVER_PORT, "");
            this.params.put(VPNConfigurationConstants.PROTOCOL, "");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error preparing params to merge into OpenVPN config template: ", e);
            throw e;
        }
    }

    /**
     * Creates the client configuration
     * 
     * @throws Exception
     */
    private void createConfig() throws Exception {

        // loop through every (connected) server, and create for each one a
        // different config
        String[] server = new String[serverlist.size()];
        for (String id : serverlist) {
            server = ServerQueries.getServerDetails(id);
            if (checkDataComplete(server)) {
                TemplateEngine tEng = new TemplateEngine(
                        VPNConfigurationConstants.CLIENT_TEMPLATE);
                try {
                    this.params.put(VPNConfigurationConstants.SERVER_NAME, server[2]); // server[2] = hostname
                    this.params.put(VPNConfigurationConstants.SERVER_PORT, server[15]);
                    this.params
                            .put(VPNConfigurationConstants.PROTOCOL, Server
                                    .transformVpnProtocolToString(Integer
                                            .parseInt(server[16])));
        
                    tEng.setParams(params);
                    this.configFilename = server[1] + ".conf"; // server[1] = name
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error on extracting server details for merging them into OpenVPN client config params", e);
                    throw e;
                }
        
                content = tEng.processTemplate();
        
                writeConfig();
            } else {
                ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
                new VisualException(rb.getString("dialog.userconfig.error") + " " + server[1]);
            }
        }
    }
    
    /**
     * Returns true if all data needed for config creation is existing
     * @param server The data of the server
     * @return True, if all data is present
     */
    private boolean checkDataComplete(String[] server) {
        String[] necessaryData = new String[4];
        necessaryData[0] = server[1];
        necessaryData[1] = server[2];
        necessaryData[2] = server[15];
        necessaryData[3] = server[16];
        
        for (int i = 0; i < necessaryData.length; i++) {
            String data = necessaryData[i];
            if (data == null)
                return false;
        }
        
        return true;
    }

    /**
     * Writes the configuration to filesystem
     * 
     * @throws Exception
     */
    private void writeConfig() throws Exception {
        try {
            File exportDir = new File(this.exportPath);
            // create whole path, if this path does not exist
            if (!exportDir.exists())
                exportDir.mkdirs();

            FileUtils.writeStringToFile(content, FileUtils.appendPathSeparator(this.exportPath)
                    + configFilename);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error writing the merged template: "
                    + configFilename, e);
            throw e;
        }
    }
}
