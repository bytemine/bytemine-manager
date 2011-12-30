/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import java.io.File;

import junit.framework.*;

public class CustomAssert extends Assert {
    protected CustomAssert() {
    }

    /**
     * Tests if the file at the given location exists
     * @param filename The path and name of the file
     */
    public static void assertFileExists(String filename) {
        try {
            assertTrue(new File(filename).exists());
        } catch(Exception e) {
            System.err.println("FAIL: File " + filename + " does not exist.");
            fail();
        }
    }
}
