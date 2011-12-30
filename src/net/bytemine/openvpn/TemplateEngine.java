/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.openvpn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.openvpn.config.VPNConfigurationConstants;
import net.bytemine.utility.FileUtils;


/**
 * A template engine that merges templates and data
 *
 * @author Daniel Rauer
 */
public class TemplateEngine {

    private Logger logger = Logger.getLogger(TemplateEngine.class.getName());

    private String content = null;
    private String templateDir = null;
    private HashMap<String, String> params = null;

    public TemplateEngine(String templateFilename) throws Exception {
        params = new HashMap<String, String>();
        templateDir = VPNConfigurationConstants.TEMPLATE_PATH;
        
        loadTemplate(templateFilename);
    }

    /**
     * loads the template
     *
     * @param templateFilename The filename of the template to load
     * @throws Exception
     */
    private void loadTemplate(String templateFilename) throws Exception {
        try {
            content = FileUtils.readFile(templateDir + "/" + templateFilename);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error loading the template: " + templateDir + "/" + templateFilename, e);
            throw e;
        }
    }


    /**
     * Merges template and data
     *
     * @throws Exception
     */
    public String processTemplate() throws Exception {
        for (Iterator<String> iter = params.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            content = content.replaceAll("\\$\\{" + key + "}", params.get(key));
        }

        if (content.indexOf("${") > -1) {
            logger.severe("after processing the template there are still unreplaced marks left");
            throw new Exception("processTemplate - unreplaced marks left");
        }
        return content;
    }

    /**
     * Adds a parameter to the map that will be put into to template
     *
     * @param key   The param key
     * @param value The param value
     */
    public void addParam(String key, String value) {
        params.put(key, value);
    }

    public void setParams(HashMap<String, String> params) {
        if (params == null)
            params = new HashMap<String, String>();
        this.params = params;
    }

    public static void main(String[] args) {
        try {
            TemplateEngine tEng = new TemplateEngine("client.tpl");
        
            tEng.addParam("server_name", "127.0.0.2");
            tEng.addParam("server_port", "8080");
            tEng.addParam("root_ca", "bla");
            tEng.addParam("crt", "bla");
            tEng.addParam("key", "bla");
            tEng.addParam("protocol", "udp");

            tEng.processTemplate();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
