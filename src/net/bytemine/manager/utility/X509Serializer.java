/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.utility;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * Serializer for X509Certificates;
 * Uses DomDriver;
 * Implemented as singleton;
 * Extends XStream;
 *
 * @author Daniel Rauer
 */
public class X509Serializer extends XStream {

    private static X509Serializer instance = null;

    private X509Serializer() {
    }

    private X509Serializer(DomDriver driver) {
        super(driver);
    }


    public static X509Serializer getInstance() {
        if (instance == null)
            instance = new X509Serializer(new DomDriver());

        return instance;
    }

}
