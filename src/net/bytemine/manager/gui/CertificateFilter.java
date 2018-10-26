/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import net.bytemine.manager.Constants;
import net.bytemine.utility.FileUtils;

/**
 * file filter for certificates
 *
 * @author Daniel Rauer
 */
public class CertificateFilter extends FileFilter {


    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        String extension = FileUtils.getExtension(file);
        return extension.equals(Constants.DEFAULT_CERT_EXTENSION);

    }


    public String getDescription() {
        return "Certificates (" + Constants.DEFAULT_CERT_EXTENSION + ")";
    }
}
