/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;


/**
 * Handles notifications;
 * Implemented as singleton
 *
 * @author Daniel Rauer
 */
public class Notifier {

    private static Notifier instance = null;

    private HashMap<String, ActionListener> actionListeners = new HashMap<String, ActionListener>();

    private Notifier() {
    }

    /**
     * Returns the instance
     *
     * @return the Notifier instance
     */
    public static Notifier getInstance() {
        if (instance == null) {
            instance = new Notifier();
        }
        return instance;
    }


    public void addActionListener(String listenerName, ActionListener listener) {
        actionListeners.put(listenerName, listener);
    }

    public void removeActionListener(String listenerName) {
        if (actionListeners.containsKey(listenerName))
            actionListeners.remove(listenerName);
    }

    public void fireAction(Object src, int id, String actionName) {
        ActionListener listener = actionListeners.get(actionName);

        if (listener != null) {
            ActionEvent event = new ActionEvent(src, id, actionName);
            listener.actionPerformed(event);
        }
    }

}
