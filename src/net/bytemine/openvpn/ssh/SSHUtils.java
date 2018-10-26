/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;


import java.util.Arrays;

/**
 * Some useful methods for ssh communication
 *
 * @author Daniel Rauer
 */
public class SSHUtils {


    /**
     * Determines whether the given service type is known to
     * the application
     *
     * @param serviceCode
     * @return true, if the code is known
     */
    public static boolean isServiceCodeKnown(String serviceCode) {
        return Arrays.stream(SSHConstants.KNOWN_SERVICE_CODES).anyMatch(knownCode -> knownCode.equals(serviceCode));
    }

}
