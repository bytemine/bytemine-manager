/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.config;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.action.ServerAction;
import net.bytemine.manager.bean.Server;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.exception.VisualException;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.openvpn.TemplateEngine;
import net.bytemine.utility.FileUtils;
import net.bytemine.utility.StringUtils;

/**
 * Creates a OpenVPN server config file
 * 
 * @author Florian Reichel
 */
public class ServerConfig {
    
    private static Logger logger = Logger.getLogger(ClientConfig.class.getName());
    private String exportPath;
    private String configFilename;
    private HashMap<String, String> params = null;
    private String content = null;
    private Server server;

    public ServerConfig(Server server) {
        this.server = server;
        this.exportPath = FileUtils.appendPathSeparator(Configuration.getInstance().CERT_EXPORT_PATH) + 
                            "_" + server.getName() + File.separator;
        this.configFilename = server.getName() + ".conf";
    }
    
    /**
     * Retrieves all necessary data and puts it into a HashMap
     */
    private void prepareParams() throws Exception {
        this.params = new HashMap<>();

        X509 rootX509 = X509Utils.loadRootX509();
        this.params.put(VPNConfigurationConstants.ROOT_CA, rootX509 == null ? "" : FileUtils.appendPathSeparator(server.getExportPath()) + rootX509.getFileName());

        String keyfilepath = FileUtils.appendPathSeparator(server.getExportPath());
        this.params.put(VPNConfigurationConstants.CRT,
                keyfilepath + "server.crt");
        this.params.put(VPNConfigurationConstants.KEY,
                keyfilepath + "server.key");
        this.params.put(VPNConfigurationConstants.CRL,
                keyfilepath + Constants.DEFAULT_CRL_FILENAME);
        
        try {
            this.params.put(VPNConfigurationConstants.SERVER_PORT,
                    Integer.toString(server.getVpnPort()));
            this.params.put(VPNConfigurationConstants.PROTOCOL, 
                    Server.transformVpnProtocolToString((server.getVpnProtocol())));
            this.params.put(VPNConfigurationConstants.NETWORK_ADDRESS, server.getVpnNetworkAddress());
            this.params.put(VPNConfigurationConstants.SERVER_DEVICE, 
                    Server.transformVpnDeviceToString(server.getVpnDevice()));
            this.params.put(VPNConfigurationConstants.DH, 
                    FileUtils.appendPathSeparator(server.getExportPath()) + "dh"+Configuration.getInstance().X509_KEY_STRENGTH+".pem");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error preparing params to merge into OpenVPN config template: ", e);
            throw e;
        }
        
        // determine whether cc should be activated and if, set the path
        this.params.put(VPNConfigurationConstants.CCD, server.getVpncc() ? "client-config-dir " + server.getVpnccpath() : ";client-config-dir ccd");

        this.params.put(VPNConfigurationConstants.SERVER_GATEWAY, server.getVpnRedirectGateway() ? "push \"redirect-gateway\"" : ";push \"redirect-gateway\"");

        this.params.put(VPNConfigurationConstants.DUPLICATE_CN, server.getVpnDuplicateCN() ? "duplicate-cn" : ";duplicate-cn");
        
        this.params.put(VPNConfigurationConstants.SERVER_USER, "user "+server.getVpnUser());
        this.params.put(VPNConfigurationConstants.SERVER_GROUP, "group "+server.getVpnGroup());
        this.params.put(VPNConfigurationConstants.SERVER_KEEP_ALIVE, "keepalive "+server.getVpnKeepAlive());
        this.params.put(VPNConfigurationConstants.PATH_TO_PASSWD, server.getUserfilePath());
    }
    
    
    /**
     * Returns true if all data needed for config creation is existing
     * @param server The server
     * @return True, if all data is present
     */
    private boolean checkDataComplete(Server server) {
        String[] necessaryData = new String[5];
        necessaryData[0] = server.getVpnProtocol() + "";
        necessaryData[1] = server.getVpnPort() + "";
        necessaryData[2] = server.getVpnNetworkAddress();
        necessaryData[3] = server.getVpnDevice() + "";
        necessaryData[4] = server.getUserfilePath();

        return Arrays.stream(necessaryData).noneMatch(StringUtils::isEmptyOrWhitespaces);
    }
    
    
    /**
     * Creates the server configuration
     * @return the generated server config
     * @throws Exception
     */
    public String createConfig() throws Exception {
        if (checkDataComplete(server)) {
            prepareParams();
            TemplateEngine tEng = new TemplateEngine(
                    VPNConfigurationConstants.SERVER_TEMPLATE);  
            tEng.setParams(params);
            content = tEng.processTemplate();
            
            writeConfig();
        } else {
            ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
            new VisualException(rb.getString("dialog.userconfig.error") + " " + server.getName());
        }
        return content;
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

            ServerAction.prepareFilesystem(server.getName());
            FileUtils.writeStringToFile(content, FileUtils.appendPathSeparator(this.exportPath)
                    + configFilename);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error writing the merged template: "
                    + configFilename, e);
            throw e;
        }
    }
}
