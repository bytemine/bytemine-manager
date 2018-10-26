/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.utility;

import java.io.File;

import java.io.FileWriter;
import java.security.cert.X509CRL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.CRL;
import net.bytemine.manager.db.CRLDAO;
import net.bytemine.manager.db.CRLQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.FileUtils;


/**
 * methods for exporting CRLs
 *
 * @author Daniel Rauer
 */
public class CRLExporter {

    private static Logger logger = Logger.getLogger(CRLExporter.class.getName());


    /**
     * Creates and stores a CRL list
     *
     * @param crlNumber     The number of the CRL
     * @param contentStr    The content written to the file
     * @param x509crl       The X509CRL-object
     * @param createDateStr The date of creation
     * @param nextUpdateStr The date of the next update
     * @param issuer        The issuer
     * @return The stored CRL
     * @throws Exception
     */
    static CRL storeCRL(
            int crlNumber, String contentStr, X509CRL x509crl,
            String createDateStr, String nextUpdateStr, String issuer)
            throws Exception {
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;

        String crlSerialized = X509Serializer.getInstance().toXML(x509crl);

        CRL crl;
        int crlId = CRLQueries.getMaxCRLId();
        if (crlId <= 0)
            crl = new CRL(createDateStr);
        else {
            crl = new CRL(crlId);
            crl = CRLDAO.getInstance().read(crl);
        }
        crl.setCrlNumber(crlNumber);
        crl.setValidFrom(createDateStr);
        crl.setNextUpdate(nextUpdateStr);
        crl.setFileName("crl.pem");
        crl.setPath(exportPath);
        crl.setIssuer(issuer);
        crl.setVersion(Constants.DEFAULT_CRL_VERSION + "");
        crl.setCrlSerialized(crlSerialized);
        crl.setContent(contentStr);
        crl.setContentDisplay(x509crl.toString());

        CRLDAO.getInstance().update(crl);

        return crl;
    }

    
    /**
     * Exports the current CRL to file
     * 
     * @return The content written to file
     * @throws Exception
     */
    public static String exportCRLToFile() throws Exception {
        int crlId = CRLQueries.getMaxCRLId();
        CRL crl = new CRL(crlId);
        crl = CRLDAO.getInstance().read(crl);
        String content = crl.getContent();
        return exportCRLToFile(content);
    }
    
    /**
     * Exports the given crl to file
     *
     * @param crl The X509CRL object to export
     * @throws Exception
     */
    static String exportCRLToFile(X509CRL crl) throws Exception {
        String contentBuffer = crl.toString() +
                X509Utils.addCRLHeader(crl.getEncoded());
        return exportCRLToFile(contentBuffer);
    }

    /**
     * Exports the given content to file
     *
     * @param content The content to export
     * @throws Exception
     */
    private static String exportCRLToFile(String content) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        String exportPath = Configuration.getInstance().CERT_EXPORT_PATH;
        preparePath(exportPath);
        
        try {
            FileWriter fw = new FileWriter(exportPath + File.separator + Constants.DEFAULT_CRL_FILENAME);
            // write to file
            fw.write(content);
            fw.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing crl to filesystem: " +
                    exportPath + "/crl.pem", e);
            throw new Exception(rb.getString("dialog.crl.exporterror"));
        }
        return content;
    }
    
    
    /**
     * Creates necessary directories for exporting
     *
     * @param path The path to prepare
     * @throws Exception
     */
    private static void preparePath(String path) throws Exception {
        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        path = FileUtils.unifyPath(path);

        if (!new File(path).exists()) {
            boolean success = (new File(path)).mkdirs();
            if (!success)
                throw new Exception(rb.getString("dialog.crl.exporterror"));
        }
    }

}
