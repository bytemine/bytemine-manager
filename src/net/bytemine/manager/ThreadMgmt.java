/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.util.Hashtable;
import java.util.Iterator;

import net.bytemine.manager.gui.ManagerGUI;


/**
 * A management class for holding active threads running in background
 *
 * @author Daniel Rauer
 */
public class ThreadMgmt {

    private static ThreadMgmt instance = null;

    private Hashtable<Long, Thread> activeThreads = new Hashtable<Long, Thread>();
    private Hashtable<Long, String> messages = new Hashtable<Long, String>();


    private ThreadMgmt() {
    }

    public static ThreadMgmt getInstance() {
        if (instance == null)
            instance = new ThreadMgmt();
        return instance;
    }


    public void addThread(Thread t) {
        addThread(t, null);
    }


    public void addThread(Thread t, String message) {
        Long id = t.getId();
        if (activeThreads.get(id) == null) {
            activeThreads.put(id, t);
            if (activeThreads.size() == 1)
                ManagerGUI.setThreadRunning();
            if (message != null)
                messages.put(id, message);
        }
        generateMessagesForGUI();
    }

    public void removeThread(Thread t) {
        Long id = t.getId();
        activeThreads.remove(id);
        messages.remove(id);
        if (activeThreads.size() == 0) {
            ManagerGUI.unsetThreadRunning();
        }
        generateMessagesForGUI();
    }


    /**
     * returns true, if there a active background processes
     *
     * @return true or false
     */
    public boolean areThreadsRunning() {
        return !activeThreads.isEmpty();
    }


    private void generateMessagesForGUI() {
        StringBuffer messageBuffer = new StringBuffer("<html>");
        for (Iterator<String> iterator = messages.values().iterator(); iterator.hasNext();) {
            String message = (String) iterator.next();
            messageBuffer.append("- " + message);
            if (iterator.hasNext())
                messageBuffer.append("<br />");
            else
                messageBuffer.append("</html>");
        }

        ManagerGUI.setWaitingToolTip(messageBuffer.toString());
	}

}
