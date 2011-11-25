/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.openvpn.ssh;

import java.util.StringTokenizer;


/**
 * Holds information of a single VPN user
 * @author Daniel Rauer
 *
 */
public class SSHStatusModel {

    private String commonName;
    private String realAddress;
    private String bytesReceived;
    private String bytesSent;
    private String connectedSince;
    private String virtualAddress;
    private String lastRef;
    
    public SSHStatusModel(String line) {
        initialize(line);
    }
    
    private void initialize(String line) {
        // cut off the prefix
        line = line.substring(5);

        StringTokenizer t = new StringTokenizer(line, ",");
        int i = 0;
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            switch (i) {
                case 0: setCommonName(token); break;
                case 1: setRealAddress(token); break;
                case 2: setBytesReceived(token); break;
                case 3: setBytesSent(token); break;
                case 4: setConnectedSince(token); break;
                default: break;
            }
            i++;
        }
    }
    
    /**
     * Adds a line with routing information to the model 
     * @param line The line
     */
    public void addRoutingInformation(String line) {
        // cut off the prefix
        line = line.substring(5);

        StringTokenizer t = new StringTokenizer(line, ",");
        int i = 0;
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            switch (i) {
                case 0: setVirtualAddress(token); break;
                case 3: setLastRef(token); break;
                default: break;
            }
            i++;
        }        
    }
    
    public String[] toStringArray() {
        String[] array = new String[6];
        array[0] = getCommonName();
        array[1] = getRealAddress();
        array[2] = getVirtualAddress();
        array[3] = getBytesReceived();
        array[4] = getBytesSent();
        array[5] = getConnectedSince();
        return array;
    }

    
    
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getRealAddress() {
        return realAddress;
    }

    public void setRealAddress(String realAddress) {
        this.realAddress = realAddress;
    }

    public String getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(String bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public String getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(String bytesSent) {
        this.bytesSent = bytesSent;
    }

    public String getConnectedSince() {
        return connectedSince;
    }

    public void setConnectedSince(String connectedSince) {
        this.connectedSince = connectedSince;
    }

    public String getVirtualAddress() {
        return virtualAddress;
    }

    public void setVirtualAddress(String virtualAddress) {
        this.virtualAddress = virtualAddress;
    }

    public String getLastRef() {
        return lastRef;
    }

    public void setLastRef(String lastRef) {
        this.lastRef = lastRef;
    }
    
    
    
}
