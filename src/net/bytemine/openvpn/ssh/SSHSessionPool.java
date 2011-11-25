/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;

import java.util.Hashtable;
import java.util.logging.Logger;

import com.jcraft.jsch.Session;


/**
 * Manages a pool of ssh session
 *
 * @author Daniel Rauer
 */
public class SSHSessionPool {

    private static Logger logger = Logger.getLogger(SSHSessionPool.class.getName());

    private static SSHSessionPool instance = null;
    private Hashtable<String, Session> pool = new Hashtable<String, Session>();

    private SSHSessionPool() {
    }

    public static SSHSessionPool getInstance() {
        if (instance == null)
            instance = new SSHSessionPool();

        return instance;
    }


    public void addSession(String name, Session session) {
        logger.info("adding sshsession with key: " + name);
        pool.put(name, session);
    }

    public void removeSession(String name) {
        logger.info("removing sshsession with key: " + name);
        pool.remove(name);
    }

    public Session getSession(String name) {
        return pool.get(name);
    }

}
