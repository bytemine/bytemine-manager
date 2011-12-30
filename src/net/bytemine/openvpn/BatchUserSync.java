/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.openvpn;

import java.util.Iterator;
import java.util.Vector;

import net.bytemine.manager.db.ServerQueries;


/**
 * Batch worker for user synchronization
 * @author Daniel Rauer
 *
 */
public class BatchUserSync {
    
    private static BatchUserSync instance = null;
    private Vector<String> serversToSync = new Vector<String>();
    
    private BatchUserSync() {
    }
    
    
    public static BatchUserSync getInstance() {
        if (instance == null)
            instance = new BatchUserSync();
        
        return instance;
    }
    
    public void startBatchSync() {
        Vector<String[]> servers = ServerQueries.getServerOverview();
        for (Iterator<String[]> iterator = servers.iterator(); iterator.hasNext();) {
            String[] server = (String[]) iterator.next();
            String serverId = server[0];
            serversToSync.add(serverId);
        }
        startNextSync();
    }

    public void startNextSync() {
        if (serversToSync.isEmpty())
            return;
        
        String serverId = serversToSync.get(0);
        try {
            new UserSync(serverId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        serversToSync.remove(0);
    }
}
