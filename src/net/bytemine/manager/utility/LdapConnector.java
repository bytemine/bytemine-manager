/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.Base64;

/**
 * Establishes an LDAP-Connection and provides query methods
 *
 * @author Daniel Rauer
 */
public class LdapConnector {

    private static Logger logger = Logger.getLogger(LdapConnector.class.getName());
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

    private final static String INIT_CTX = "com.sun.jndi.ldap.LdapCtxFactory";
    private String url;
    private String filterDN;
    private String objectclass;
    private String certAttributeName;
    private String certImportDir;
    private boolean loadCertificateFromLDAP = true;

    private DirContext dirContext = null;


    public LdapConnector() throws Exception {
        readProperties();
        setInitialContext();
    }

    private void readProperties() {
        String host = Configuration.getInstance().LDAP_HOST;
        String port = Configuration.getInstance().LDAP_PORT;
        url = "ldap://" + host + ":" + port;

        filterDN = Configuration.getInstance().LDAP_DN;

        String objClass = Configuration.getInstance().LDAP_OBJECTCLASS;
        objectclass = "(objectclass=" + objClass + ")";

        certAttributeName = Configuration.getInstance().LDAP_CERT_ATTRNAME;
        certImportDir = Configuration.getInstance().LDAP_CERT_IMPORTDIR;
        if (certAttributeName == null || "".equals(certAttributeName))
            loadCertificateFromLDAP = false;
    }


    /**
     * Builds a DirContext
     *
     * @throws java.lang.Exception
     */
    public void setInitialContext() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, INIT_CTX);
        env.put(Context.PROVIDER_URL, url);

        try {
            dirContext = new InitialDirContext(env);

            if (dirContext == null)
                throw new Exception(rb.getString("error.ldap.context"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error initializing LDAP Context", e);
            throw new Exception(rb.getString("error.ldap.context"));
        }
        logger.info("successful login to ldap server");
    }


    /**
     * Searches a person by its UID
     *
     * @param uid The uid
     * @return A String with the cn of the person, or null if the person was not found
     * @throws java.lang.Exception
     */
    public String searchByUID(String uid) throws Exception {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results = dirContext.search(
                filterDN,
                "(&" + objectclass + "(uid=" + uid + "))",
                controls);
        while (results.hasMore()) {
            SearchResult searchResult = (SearchResult) results.next();
            Attributes attributes = searchResult.getAttributes();
            Attribute attr = attributes.get("cn");
            String cn = (String) attr.get();
            return cn;
        }
        return null;

    }

    /**
     * @param cert     The certificate as byte array
     * @param filename The filename
     * @deprecated Only for testing puposes, use X509Exporter for exporting a certificate
     */
    private void writeCertToFile(byte[] cert, String filename) {
        try {
            FileOutputStream os = new FileOutputStream(filename);
            StringBuffer contentBuffer = new StringBuffer();
            contentBuffer.append("-----BEGIN CERTIFICATE-----\n");
            contentBuffer.append(Base64.encodeBytes(cert));
            contentBuffer.append("\n-----END CERTIFICATE-----\n");

            // write to file
            Writer wr = new OutputStreamWriter(os, Charset.forName("UTF-8"));
            wr.write(contentBuffer.toString());
            wr.flush();

            os.close();
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "error writing certificate to file", ioe);
        }
    }


    /**
     * Loads all persons from LDAP matching the filter rules
     *
     * @return A Hashtable with <cn,certificate>
     * @throws java.lang.Exception
     */
    public Hashtable<String, byte[]> getAllPersonWithCertificates() throws Exception {
        Hashtable<String, byte[]> returnTable = new Hashtable<String, byte[]>();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            NamingEnumeration<SearchResult> results = dirContext.search(filterDN, objectclass, controls);
            while (results.hasMore()) {
                SearchResult searchResult = (SearchResult) results.next();
                Attributes attributes = searchResult.getAttributes();
                Attribute attr = attributes.get("cn");
                String cn = (String) attr.get();

                // certificate
                Attribute attr2 = attributes.get(certAttributeName);
                if (attr2 != null) {
                    byte[] cert = (byte[]) attr2.get();
                    returnTable.put(cn, cert);
                } else
                    throw new Exception(rb.getString("error.ldap.certattribute"));
            }
        } catch (NameNotFoundException nnfe) {
            logger.log(Level.SEVERE, "error loading all ldap persons", nnfe);
            throw new Exception(rb.getString("error.ldap.filter"));
        }
        return returnTable;
    }


    /**
     * Loads all persons from LDAP matching the filter rules
     *
     * @return A set with cn
     * @throws java.lang.Exception
     */
    public Set<String> getAllPerson() throws Exception {
        Set<String> returnSet = new HashSet<String>();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            NamingEnumeration<SearchResult> results = dirContext.search(filterDN, objectclass, controls);
            while (results.hasMore()) {
                SearchResult searchResult = (SearchResult) results.next();
                Attributes attributes = searchResult.getAttributes();
                Attribute attr = attributes.get("cn");
                String cn = (String) attr.get();
                returnSet.add(cn);
            }
        } catch (NameNotFoundException nnfe) {
            logger.log(Level.SEVERE, "error loading all ldap persons", nnfe);
            throw new Exception(rb.getString("error.ldap.filter"));
        }
        return returnSet;
    }


    public String getCertImportDir() {
        return certImportDir;
    }

    public boolean isLoadCertificateFromLDAP() {
        return loadCertificateFromLDAP;
    }


    public static void main(String[] args) throws NamingException, Exception {
        LdapConnector lc = new LdapConnector();
        lc.searchByUID("11260");
        lc.writeCertToFile(null, null);
    }
}