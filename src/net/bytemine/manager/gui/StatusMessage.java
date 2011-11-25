/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;


/**
 * A class for status messages
 * @author Daniel Rauer
 */
public class StatusMessage {
    
    private int type;
    private String message;
    
    public static final int TYPE_ERROR = 0;
    public static final int TYPE_INFO = 1;
    public static final int TYPE_CONFIRM = 2;
    
    public StatusMessage(String message) {
        this.message = message;
        this.type = TYPE_INFO;
    }

    public StatusMessage(String message, int type) {
        this.message = message;
        this.type = type;
    }
    
    
    public String getMessage() {
        return message;
    }
    
    public int getType() {
        return type;
    }
    
    public String toString() {
        return message;
    }
}
