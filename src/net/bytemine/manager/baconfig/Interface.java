/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.baconfig;

import java.util.List;

public class Interface {

    private List<String> aliases;
    private String ipv4_main;
    private String lladdr;
    private String name;
    private String pppoe;
    private boolean dhcp;
    private boolean route_needed;
    
    
    public boolean isDhcp() {
        return dhcp;
    }
    public void setDhcp(boolean dhcp) {
        this.dhcp = dhcp;
    }
    public String getLladdr() {
        return lladdr;
    }
    public void setLladdr(String lladdr) {
        this.lladdr = lladdr;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPppoe() {
        return pppoe;
    }
    public void setPppoe(String pppoe) {
        this.pppoe = pppoe;
    }
    public boolean isRoute_needed() {
        return route_needed;
    }
    public void setRoute_needed(boolean route_needed) {
        this.route_needed = route_needed;
    }
    public List<String> getAliases() {
        return aliases;
    }
    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
    public String getIpv4_main() {
        return ipv4_main;
    }
    public void setIpv4_main(String ipv4_main) {
        this.ipv4_main = ipv4_main;
    }
}
