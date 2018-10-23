/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.gui.ManagerGUI;


/**
 * A management class for holding active threads running in background
 *
 * @author Daniel Rauer
 */
public class ThreadMgmt {
    
    private static Logger logger = Logger.getLogger(ThreadMgmt.class.getName());

    private static ThreadMgmt instance = null;

    private Hashtable<Long, Thread> activeThreads = new Hashtable<>();
    private Hashtable<Long, String> messages = new Hashtable<>();


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
        try {
            Long id = t.getId();
            if (activeThreads.get(id) == null) {
                activeThreads.put(id, t);
                if (activeThreads.size() == 1)
                    ManagerGUI.setThreadRunning();
                if (message != null)
                    messages.put(id, message);
            }
            generateMessagesForGUI();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Error on adding current thread to queue.", e);
        }
    }

    public void removeThread(Thread t) {
        try {
            Long id = t.getId();
            activeThreads.remove(id);
            messages.remove(id);
            if (activeThreads.size() == 0) {
                ManagerGUI.unsetThreadRunning();
            }
            generateMessagesForGUI();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Error on removing current thread from queue.", e);
        }
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
        StringBuilder messageBuffer = new StringBuilder("<html>");
        for (Iterator<String> iterator = messages.values().iterator(); iterator.hasNext();) {
            String message = iterator.next();
            messageBuffer.append("- ").append(message);
            if (iterator.hasNext())
                messageBuffer.append("<br />");
            else
                messageBuffer.append("</html>");
        }

        ManagerGUI.setWaitingToolTip(messageBuffer.toString());
	}

}
