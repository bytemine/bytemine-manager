/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.test;

import net.bytemine.openvpn.Server;
import net.bytemine.openvpn.StatusCollector;

/**
 * @author Daniel Rauer
 */
public class StatusTester {

    public static void main(String[] args) {
        StatusCollector sc = new StatusCollector(new Server("Server name", "192.168.1.10", 22, "username"));
        sc.run();
    }

}
