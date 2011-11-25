/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Felix Kronlage                   E-Mail: kronlage@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/
package net.bytemine.manager;

import java.io.File;
import java.io.IOException;

import net.bytemine.utility.StringUtils;

/**
 * Starts the manager application
 *
 * @author Felix Kronlage
 */
public class ManagerStarter {

    public static void main(String[] args) throws IOException {

        File appDir = new File(".");
        String managerJar= "";
        String majorVersion = "0";
        String minorVersion = "0";

        String[] dirContents = appDir.list();

        for (int i = 0; i < dirContents.length; i++) {
            if ((dirContents[i].startsWith("bytemine-manager") &&
                        (dirContents[i].endsWith(".jar")))) {
                String version = StringUtils.extractVersionFromString(dirContents[i]);
                if (version != null && version.length() > 2) {
                    String minorVersionT = version.substring(2);
                    String majorVersionT = version.substring(0,1);
    
                    if (majorVersionT.compareTo(majorVersion) > 0) {
                        majorVersion = majorVersionT;
                        minorVersion = minorVersionT;
                    } else if ((minorVersionT.compareTo(minorVersion) > 0) &&
                               (majorVersionT.compareTo(majorVersion) >= 0)) {
                        minorVersion = minorVersionT;
                    }
                }
            }
        }
        
        managerJar = "bytemine-manager-"+ majorVersion +"."+ minorVersion +".jar";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++)
            sb.append(args[i] + " ");

        System.out.println("Starting: "+ managerJar);

        String javaCommand = "java";
        if (System.getProperty("os.name").startsWith("Windows"))
            javaCommand = "javaw";

        String startCommand = javaCommand +" -jar "+ managerJar +" "+ sb.toString();
        System.out.println(startCommand);
        
        try {
            Runtime.getRuntime().exec(startCommand);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
