/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


/**
 * A simple test for the UserDAO-class and the db actions
 * @author Daniel Rauer
 */
package net.bytemine.manager.db.test;

import net.bytemine.manager.bean.X509;


public class X509Test {

    public static void main(String[] args) {
        createRootCertificate();
    }


    public static void createRootCertificate() {
        try {
            // create new certificate
            //X509Generator generator = new X509Generator();
            //generator.createRootCert();

            int id = 1;

            // load this x509 by id
            X509 x509 = X509.getX509ById(id);
            if (x509 != null)
                System.out.println("Success: loaded x509 with id " + id + ": " + x509.getSerial());
            else
                System.out.println("Error: could not load x509 with id " + id);

            // show some attributes
            System.out.println("    Issuer: " + x509.getIssuer());


            // delete the x509
            x509.delete();

            // try to load, should fail
            x509 = X509.getX509ById(id);

            if (x509 != null)
                System.out.println("Error: x509 with id " + id + " could not be deleted");
            else
                System.out.println("Success: x509 with id " + id + " has been deleted");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
