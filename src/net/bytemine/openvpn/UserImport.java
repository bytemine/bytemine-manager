/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;


/**
 * Abstract superclass for the UserImportImplementation classes
 *
 * @author Daniel Rauer
 */
public abstract class UserImport {


    public int createUsersFromCN = 0;
    public static final int NOT_SET = 0;
    public static final int NO = 1;
    public static final int YES = 2;

    public static int importedUsers = 0;
    public static int generatedUsers = 0;
    public static int importedCerts = 0;
    public static int generatedCerts = 0;
    public static int notLinkedUsers = 0;
    public static int notLinkedCerts = 0;
    
    private boolean createCertificatesForUsers = true;

    UserImport(boolean createCertificatesForUsers) {
        this.createCertificatesForUsers = createCertificatesForUsers;
    }

    public abstract void importUsers() throws Exception;


    public static void incGeneratedUsers() {
        generatedUsers++;
    }

    static void incImportedUsers() {
        importedUsers++;
    }

    public static void incNotLinkedUsers() {
        notLinkedUsers++;
    }

    static void incNotLinkedUsers(int number) {
        notLinkedUsers += number;
    }

    public static void incGeneratedCerts() {
        generatedCerts++;
    }

    public static void incImportedCerts() {
        importedCerts++;
    }

    public static void incNotLinkedCerts() {
        notLinkedCerts++;
    }
    
    static void decNotLinkedCerts() {
        notLinkedCerts--;
    }

    public static void resetCounters() {
        generatedUsers = 0;
        importedUsers = 0;
        notLinkedUsers = 0;
        generatedCerts = 0;
        importedCerts = 0;
        notLinkedCerts = 0;
    }


    /**
     * Formats a number for the status frame
     *
     * @param number The number to format
     * @return a formatted String
     */
    String formatStatusNumber(int number) {
        return number < 10 ? "  " + number : "" + number;
    }

    boolean isCreateCertificatesForUsers() {
        return createCertificatesForUsers;
    }


}

