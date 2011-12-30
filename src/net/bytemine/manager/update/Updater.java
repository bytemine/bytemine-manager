/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.update;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;
import net.bytemine.manager.db.LicenceQueries;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.StringUtils;


/**
 * Does all the update communication and download task
 *
 * @author Daniel Rauer
 */
public class Updater {

    private static Logger logger = Logger.getLogger(Updater.class.getName());

    public static SSLSocket socket;
    private String responseHeader;

    public Updater() {
    }


    /**
     * Builds up a ssl socket
     * @throws Exception
     */
    private void connect() throws Exception {
        if (socket != null && socket.isConnected() && !socket.isClosed())
            return;

        String updateServerPath = Configuration.getInstance().UPDATE_SERVER;
        if (updateServerPath == null || "".equals(updateServerPath))
            updateServerPath = Constants.UPDATE_HTTPS_SERVER;

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
        try {

            SSLContext ctx = SSLContext.getInstance("TLS");
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
                char[] passphrase = "".toCharArray();

                ByteArrayInputStream bais = new ByteArrayInputStream(
                        LicenceQueries.getKeystore());
                ks.load(bais, passphrase);
                kmf.init(ks, passphrase);
                ctx.init(kmf.getKeyManagers(), null, null);

            } catch (Exception e) {
                throw new IOException();
            }

            if (Configuration.getInstance().UPDATE_PROXY != null && !"".equals(Configuration.getInstance().UPDATE_PROXY)) {
                // update over proxy server
                SSLTunnelSocketFactory tunnelFactory = new SSLTunnelSocketFactory(
                        Configuration.getInstance().UPDATE_PROXY, Configuration.getInstance().UPDATE_PROXY_PORT);
                try {
                    socket = (SSLSocket) tunnelFactory.createSocket(updateServerPath, Constants.UPDATE_HTTPS_PORT);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error connecting the proxy: " + Configuration.getInstance().UPDATE_PROXY, e);
                    throw new ProxyException(rb.getString("dialog.update.error_unknown_host"));
                }
            } else {
                SSLSocketFactory factory = ctx.getSocketFactory();
                socket = (SSLSocket) factory.createSocket(
                        updateServerPath, Constants.UPDATE_HTTPS_PORT);
                socket.startHandshake();
            }

        } catch (ProxyException e) {
            throw new Exception(rb.getString("dialog.update.error_proxy"));
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "unknown host: " + updateServerPath, e);
            throw new Exception(rb.getString("dialog.update.error_unknown_host"));
        } catch (NoRouteToHostException e) {
            logger.log(Level.SEVERE, "no route to host: " + updateServerPath, e);
            throw new Exception(rb.getString("dialog.update.error_no_route"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error loading the update certificates", e);
            throw new Exception(rb.getString("dialog.update.error_licence"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error connecting the ssl socket: " + updateServerPath, e);
            throw new Exception(rb.getString("dialog.update.error_unknown_host"));
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error disconnecting sslsocket", e);
        }
    }


    /**
     * sends a request to get the repo.yml
     *
     * @return A String cotaining the response
     * @throws IOException
     */
    public String askForUpdate() throws Exception {
        byte[] bytes = sendRequest(Constants.UPDATE_PAGE);
        if (bytes == null)
            return null;
        return StringUtils.bytes2String(bytes);
    }


    /**
     * sends a request to get the chaneglog
     *
     * @param filename The ChangeLog filename
     * @return A String cotaining the response
     * @throws IOException
     */
    public String askForChangelog(String filename) throws Exception {
        byte[] bytes = sendRequest(filename);
        if (bytes == null)
            return null;
        return StringUtils.bytes2String(bytes);
    }


    /**
     * Downloads the given filename
     *
     * @param filename The filename of the file to download
     * @return The response
     * @throws IOException
     */
    public byte[] downloadUpdate(String filename) throws Exception {
        return sendRequest(filename);
    }


    /**
     * Requests the given page
     *
     * @param requestedPage The page that is requested
     * @return The response
     * @throws Exception 
     */
    private byte[] sendRequest(String requestedPage) throws Exception {
        connect();

        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())));
        out.println("GET " + Configuration.getInstance().UPDATE_REPOSITORY + requestedPage + " HTTP/1.0");
        logger.info("GET " + Configuration.getInstance().UPDATE_REPOSITORY + requestedPage + " HTTP/1.0");
        out.println();
        out.flush();

        if (out.checkError()) {
            logger.severe("error getting PrintWriter from sslsocket");
            throw new IOException("error getting PrintWriter from sslsocket");
        }

        /* read response */
        InputStream is = socket.getInputStream();
        byte[] bytes = getBytesFromInputStream(is);

        String response = new String(bytes);
        if (response != null && response.startsWith("HTTP/1.1 404"))
            bytes = null;
        else
            bytes = removeHTTPHeader(bytes);

        disconnect();

        return bytes;
    }


    /**
     * Converts the InputStream into a byte[]
     *
     * @param is The InputStream
     * @return The Stream as byte[]
     * @throws IOException
     */
    private byte[] getBytesFromInputStream(InputStream is)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is);

        int b;
        while ((b = bis.read()) != -1) {
            baos.write(b);
        }

        return baos.toByteArray();
    }


    /**
     * Removes the http header from the response
     *
     * @param response The response as byte[]
     * @return byte[] with content only
     */
    private byte[] removeHTTPHeader(byte[] response) {
        String original = new String(response);

        int startIndex = original.indexOf("Content-Type:");
        if (startIndex > -1) {
            // remove header up to 'Content-Type:'
            String str2 = original.substring(startIndex);
            // detect the header ending
            int headerEndIndex = str2.indexOf("\n") + 3;
            // the header length
            int headerLength = startIndex + headerEndIndex;

            // remove the rest of the header
            str2 = str2.substring(headerEndIndex);

            // store the header
            responseHeader = original.substring(headerEndIndex);

            // create new byte array for the response without the http header
            byte[] returnBytes = new byte[response.length - headerLength];
            for (int i = 0; i < returnBytes.length; i++) {
                returnBytes[i] = response[i + headerLength];
            }
            return returnBytes;
        }
        return response;
    }


    public String getResponseHeader() {
        return responseHeader;
    }

}
