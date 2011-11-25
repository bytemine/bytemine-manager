/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.baconfig;

import java.util.List;

public class Network {

    private List<Interface> interfaces;
    private List<IPAddress> ip_addresses;
    private List<PPPoEInterface> pppoe_interfaces;
    private List<Route> routes;
    
    private List<String> nameserver;
    private String domain_name;
    private int pppoe_count;
    private int tun_count;
    
    
    public List<Interface> getInterfaces() {
        return interfaces;
    }
    public void setInterfaces(List<Interface> interfaces) {
        this.interfaces = interfaces;
    }
    public List<String> getNameserver() {
        return nameserver;
    }
    public void setNameserver(List<String> nameserver) {
        this.nameserver = nameserver;
    }
    public int getPppoe_count() {
        return pppoe_count;
    }
    public void setPppoe_count(int pppoe_count) {
        this.pppoe_count = pppoe_count;
    }
    public int getTun_count() {
        return tun_count;
    }
    public void setTun_count(int tun_count) {
        this.tun_count = tun_count;
    }
    public String getDomain_name() {
        return domain_name;
    }
    public void setDomain_name(String domain_name) {
        this.domain_name = domain_name;
    }
    public List<IPAddress> getIp_addresses() {
        return ip_addresses;
    }
    public void setIp_addresses(List<IPAddress> ip_addresses) {
        this.ip_addresses = ip_addresses;
    }
    public List<PPPoEInterface> getPppoe_interfaces() {
        return pppoe_interfaces;
    }
    public void setPppoe_interfaces(List<PPPoEInterface> pppoe_interfaces) {
        this.pppoe_interfaces = pppoe_interfaces;
    }
    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
    public List<Route> getRoutes() {
        return routes;
    }
}
