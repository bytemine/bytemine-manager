/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.update;


/**
 * Just do address an exception thrown at establising a proxy
 * connection to the correct receipient
 *
 * @author Daniel Rauer
 */
public class ProxyException extends Exception {

    private static final long serialVersionUID = 3620774567359324870L;

    public ProxyException(String msg) {
        super(msg);
    }

}
