/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.baconfig;

public class Startup {

    private boolean sshd;
    private boolean named;
    private boolean ntpd;
    private boolean openvpn;
    private String safecore_flags;
    
    
    public boolean isNamed() {
        return named;
    }
    public void setNamed(boolean named) {
        this.named = named;
    }
    public boolean isNtpd() {
        return ntpd;
    }
    public void setNtpd(boolean ntpd) {
        this.ntpd = ntpd;
    }
    public boolean isOpenvpn() {
        return openvpn;
    }
    public void setOpenvpn(boolean openvpn) {
        this.openvpn = openvpn;
    }
    public String getSafecore_flags() {
        return safecore_flags;
    }
    public void setSafecore_flags(String safecore_flags) {
        this.safecore_flags = safecore_flags;
    }
    public boolean isSshd() {
        return sshd;
    }
    public void setSshd(boolean sshd) {
        this.sshd = sshd;
    }
    
    
}
