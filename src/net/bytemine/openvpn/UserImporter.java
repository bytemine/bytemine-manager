/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;


/**
 * Decides which user import class will be used
 *
 * @author Daniel Rauer
 */
public class UserImporter {

    public static UserImport getUserImportImplementation(boolean createCertificatesForUsers) {
        return Configuration.getInstance().USER_IMPORT_TYPE == Constants.USER_IMPORT_TYPE_LDAP ? UserImportLdap.getInstance(createCertificatesForUsers) : UserImportFile.getInstance(createCertificatesForUsers);
    }

}
