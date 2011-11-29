package net.bytemine.manager.tests;

import java.io.File;

import junit.framework.*;

public class CustomAssert extends Assert {
    protected CustomAssert() {
    }

    /**
     * Tests if the fiel at the given location exists
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
