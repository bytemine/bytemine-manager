/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.config;

/**
 * Constants for OpenVPN configurations
 *
 * @author Daniel Rauer
 */
public class VPNConfigurationConstants {
    // Path to the VPN config templates
    public static final String TEMPLATE_PATH = "templates";
    public static final String CLIENT_TEMPLATE = "client.ovpn.tpl";
    public static final String SERVER_TEMPLATE = "server.ovpn.tpl";
    
    public static final String SERVER_NAME = "server_name";
    public static final String SERVER_PORT = "server_port";
    public static final String PATH_TO_PASSWD = "path_to_passwd";
    public static final String ROOT_CA = "root_ca";
    public static final String CRT = "crt";
    public static final String KEY = "key";
    public static final String PROTOCOL = "protocol";
    public static final String CCD = "client-config-dir ccd";
    public static final String DH = "server_dh";
    public static final String SERVER_GATEWAY = "push \"redirect-gateway\"";
    public static final String SERVER_DEVICE = "device";
    public static final String NETWORK_ADDRESS = "networkAddress";
    public static final String DUPLICATE_CN = "duplicate-cn";
    public static final String SERVER_USER = "user";
    public static final String SERVER_GROUP = "group";
    public static final String SERVER_KEEP_ALIVE = "keepalive";
}
