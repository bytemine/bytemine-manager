/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.baconfig;

import java.util.List;

public class System {
    
    private Startup startup;
    private String hostname;
    private List<String> ntp_server;
    private String smarthost;
    private String timezone;
    private boolean update_check;
    private String update_check_recipient;

    
    public Startup getStartup() {
        return startup;
    }

    public void setStartup(Startup startup) {
        this.startup = startup;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<String> getNtp_server() {
        return ntp_server;
    }

    public void setNtp_server(List<String> ntp_server) {
        this.ntp_server = ntp_server;
    }

    public String getSmarthost() {
        return smarthost;
    }

    public void setSmarthost(String smarthost) {
        this.smarthost = smarthost;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isUpdate_check() {
        return update_check;
    }

    public void setUpdate_check(boolean update_check) {
        this.update_check = update_check;
    }

    public String getUpdate_check_recipient() {
        return update_check_recipient;
    }

    public void setUpdate_check_recipient(String update_check_recipient) {
        this.update_check_recipient = update_check_recipient;
    }

}
