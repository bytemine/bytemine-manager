/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

/**
 * basic server object
 *
 * @author fkr
 */
public class Server {

    private String identifier;
    private String hostname;

    /**
     * port ssh listens on
     */
    private int port;

    /**
     * Username we use for connecting to the vpn conncentrator
     */
    private String username;


    /**
     * @param identifier
     * @param hostname
     * @param port
     * @param username
     */
    public Server(String identifier, String hostname, int port, String username) {
        this.identifier = identifier;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the username used to connect via ssh
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

}
