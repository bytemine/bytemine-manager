/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.openvpn.ssh;

import net.bytemine.manager.Constants;
import net.bytemine.manager.db.KnownHostsQueries;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;


/**
 * Known host management for the ssh servers
 *
 * @author Daniel
 */
public class SSHKnownHostsMGMT {

    private String hostname;
    private String fingerprint;

    public SSHKnownHostsMGMT(JSch jsch) {
        HostKeyRepository repo = jsch.getHostKeyRepository();
        if (repo != null) {
            HostKey[] keys = repo.getHostKey();
            if (keys != null) {
                for (int i = 0; i < keys.length; i++) {
                    HostKey key = keys[i];
                    this.hostname = key.getHost();
                    this.fingerprint = key.getFingerPrint(jsch);
                }
            }
        }
    }


    /**
     * Returns the confirmation status for the hostname and fingerprint
     *
     * @return Integer with the status code
     * @throws Exception
     */
    private int getConfirmationStatus() throws Exception {
        return KnownHostsQueries.getConfirmationStatus(hostname, fingerprint);
    }


    /**
     * Evaluate the hostname and fingerprint, whether it is trusted,
     * unknown or known
     *
     * @return true, if the session shall be disconnected
     * @throws Exception
     */
    public boolean evaluateHost() throws Exception {
        boolean disconnect = false;
        boolean trusted = false;
        System.out.println("eval: " + fingerprint);
        int status = getConfirmationStatus();
        if (status == Constants.KNOWN_HOST_STATUS_NEW) {
            // new host
            trusted = Dialogs.showKnownHostNewDialog(ManagerGUI.mainFrame, hostname, fingerprint);
            KnownHostsQueries.updateHost(hostname, fingerprint, trusted);
        } else if (status == Constants.KNOWN_HOST_STATUS_CHANGED) {
            // fingerprint changed!
            trusted = Dialogs.showKnownHostChangedDialog(ManagerGUI.mainFrame, hostname, fingerprint);
            KnownHostsQueries.updateHost(hostname, fingerprint, trusted);
        } else if (status == Constants.KNOWN_HOST_STATUS_OK) {
            trusted = true;
        } else if (status == Constants.KNOWN_HOST_STATUS_MISTRUSTED) {
            // host is untrusted. ask user if it shall be trusted
            trusted = Dialogs.showKnownHostMistrustDialog(ManagerGUI.mainFrame, hostname, fingerprint);
        }

        if (trusted)
            KnownHostsQueries.trustHost(hostname, fingerprint);
        else {
            KnownHostsQueries.mistrustHost(hostname);
            disconnect = true;
        }

        return disconnect;
    }

}
