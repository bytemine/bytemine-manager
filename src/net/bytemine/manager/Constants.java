/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.bytemine.manager.i18n.ResourceBundleMgmt;

/**
 * Constants for the manager application
 *
 * @author Daniel Rauer
 */
public class Constants {

    // language code for english
    public static final String LANGUAGE_CODE_ENGLISH = "en";
    // language code for german
    public static final String LANGUAGE_CODE_GERMAN = "de";

    public static final String KEYSTORE_PKCS12 = "PKCS12";

    // the resource bundle package name
    public static final String BUNDLE_PACKAGE = "net.bytemine.manager.i18n.manager";
    public static final String ROOT_BUNDLE_NAME = "root_cert";
    public static final String SERVER_BUNDLE_NAME = "server_cert";
    public static final String INTERMEDIATE_BUNDLE_NAME = "inter_cert";
    public static final String CLIENT_BUNDLE_NAME = "client_cert";

    // SimpleDateFormat for displaying dates in the gui
    private static final SimpleDateFormat SHOW_FORMAT_DE = new SimpleDateFormat(
            "dd.MM.yyyy");
    private static final SimpleDateFormat SHOW_FORMAT_EN = new SimpleDateFormat(
            "yyyy/MM/dd");
    // SimpleDateFormat more detailed
    public static final SimpleDateFormat DETAILED_FORMAT_DE = new SimpleDateFormat(
            "dd. MMMMM yyyy HH:mm:ss", Locale.GERMAN);
    public static final SimpleDateFormat DETAILED_FORMAT_EN = new SimpleDateFormat(
            "dd. MMMMM yyyy HH:mm:ss", Locale.ENGLISH);
    public static final SimpleDateFormat PROPERTIES_DATE_FORMAT = new SimpleDateFormat(
            "dd.MM.yyyy");

    public static final String DEFAULT_KEY_EXTENSION = "key";
    public static final String DEFAULT_CERT_EXTENSION = "crt";
    public static final String DEFAULT_PKCS12_EXTENSION = "p12";
    public static final String DEFAULT_CRL_FILENAME = "crl.pem";

    public static final int DEFAULT_X509_VERSION = 3;
    public static final int DEFAULT_CRL_VERSION = 2;

    static final int DEFAULT_SERVER_CERT_VALIDITY = 730;
    static final int DEFAULT_CLIENT_CERT_VALIDITY = 365;
    
    public static final String[] AVAILABLE_KEYSTRENGTH = new String[] {"1024", "2048", "4096"};

    // import types for users
    public static final int USER_IMPORT_TYPE_FILE = 0;
    public static final int USER_IMPORT_TYPE_LDAP = 1;
    
    public static final String DEFAULT_EXPORT_PATH = "export" + File.separator;
    public static final String EXPORT_UNASSIGNED_DIRECTORY = "__unassigned" + File.separator; 

    // certificate types
    public static final int CERTIFICATE_TYPE_BASE64 = 0;
    public static final int CERTIFICATE_TYPE_PKCS12 = 1;

    // pkcs12 password types
    public static final int PKCS12_NO_PASSWORD = 0;
    public static final int PKCS12_SINGLE_PASSWORD = 1;

    public static final String PKCS12_CHARSET = "iso-8859-15";


    public static final String ICON_OPEN_PATH = 
            "net/bytemine/manager/gui/resources/open.png";
    public static final String ICON_UK_PATH = 
            "net/bytemine/manager/gui/resources/uk.png";
    public static final String ICON_GERMANY_PATH = 
            "net/bytemine/manager/gui/resources/germany.png";
    public static final String ICON_CLOSE_PATH =
            "net/bytemine/manager/gui/resources/info.png";
    public static final String ICON_ERROR_PATH = 
            "net/bytemine/manager/gui/resources/error.png";
    public static final String ICON_INFO_PATH = 
            "net/bytemine/manager/gui/resources/info.png";
    public static final String ICON_WAITING_PATH = 
            "net/bytemine/manager/gui/resources/waiting.gif";
    public static final String ICON_EXPAND = 
            "net/bytemine/manager/gui/resources/expand.png";
    public static final String ICON_COLLAPSE = 
            "net/bytemine/manager/gui/resources/collapse.png";
    public static final String ICON_USER =
            "net/bytemine/manager/gui/resources/user.png";
    public static final String ICON_USERS =
            "net/bytemine/manager/gui/resources/users.png";
    public static final String ICON_GROUP =
            "net/bytemine/manager/gui/resources/group.png";
    public static final String ICON_SERVER =
            "net/bytemine/manager/gui/resources/server.png";
    public static final String ICON_SERVERS =
            "net/bytemine/manager/gui/resources/servers.png";
    public static final String ICON_PRINT =
            "net/bytemine/manager/gui/resources/print.png";

    // certificate management
    public static final int MODE_CA = 1;
    // control center
    public static final int MODE_CC = 2;
    // management and control center
    public static final int MODE_COMBINED = 3;

    
    public static final int KNOWN_HOST_STATUS_OK = 0;
    public static final int KNOWN_HOST_STATUS_NEW = 1;
    public static final int KNOWN_HOST_STATUS_CHANGED = 2;
    public static final int KNOWN_HOST_STATUS_MISTRUSTED = 3;


    public static final String UPDATE_HTTPS_SERVER = 
            "applianceupdate.bytemine.net";
    public static final int UPDATE_HTTPS_PORT = 443;
    public static final String UPDATE_REPOSITORY = "/repo-manager/";
    public static final String UPDATE_PAGE = "repo.yml";
    public static final String UPDATE_PATH = "";
    public static final String UPDATE_JAR_PATH = "lib";

    public static final String UPDATE_SCHEMA_FILE = 
            "net/bytemine/manager/db/schema/schema.xml";

    public static final String DB_OUTDATED = "0";
    public static final String DB_UP_TO_DATE = "1";
    
    public static final Font FONT_PLAIN = new Font("Sans-Serif", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Sans-Serif", Font.BOLD, 12);
    
    public static final Color COLOR_WHITE = new Color(255, 255, 255);
    public static final Color COLOR_ERROR = new Color(255, 83, 83);
    public static Color COLOR_ROW1 = new Color(189, 188, 140); //BDBC8C
    public static Color COLOR_ROW2 = new Color(218, 217, 166); //DAD9A6

    public static final String SUPPORT_EMAIL = "support@bytemine.net";
    public static final String SUPPORT_SERVER = "mail.bytemine.net";
    
    public static final String DEFAULT_SSH_KEYFILE_PATH = System.getProperty("user.home") + "/.ssh/id_rsa";
    
    // FIXME: currently the path to the server-config file is not configurable
    // /etc/openvpn/ is currently used as a hard value
    public static final String DEFAULT_OPENVPN_START_COMMAND_BOA = "/usr/local/sbin/openvpn\n  " +
            "--config /etc/openvpn/server.conf\n   " +
            "--script-security 2\n   " +
            "--daemon\n   " +
            "--writepid /var/run/openvpnd.pid";
    public static final String DEFAUlT_OPENVPN_START_COMMAND_NON_BOA = "openvpn\n " +
            "--config /etc/openvpn/server.conf\n " +
            "--script-security 2\n " +
            "--daemon";
    
    public static final String DEFAULT_SSH_PORT = "22";
    public static final String DEFAULT_OPENVPN_PORT = "1194";
    public static final String DEFAULT_OPENVPN_USER = "_openvpn";
    public static final String DEFAULT_OPENVPN_GROUP = "_openvpn";
    public static final String DEFAULT_OPENVPN_KEEPALIVE_PING = "10";
    public static final String DEFAULT_OPENVPN_KEEPALIVE_ASSUME = "120";
    
    
    /**
     * returns a SimpleDateFormat depending on the current locale
     * @return SimpleDateFormat
     */
    public static SimpleDateFormat getShowFormatForCurrentLocale() {
        String currentLocale = 
            ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage();
        if(LANGUAGE_CODE_ENGLISH.equals(currentLocale))
            return SHOW_FORMAT_EN;
        else
            return SHOW_FORMAT_DE;
    }
    
    /**
     * returns a SimpleDateFormat depending on the current locale
     * @return SimpleDateFormat
     */
    public static SimpleDateFormat getDetailedFormatForCurrentLocale() {
        String currentLocale = 
            ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage();
        if(LANGUAGE_CODE_ENGLISH.equals(currentLocale))
            return DETAILED_FORMAT_EN;
        else
            return DETAILED_FORMAT_EN;
    }
    
    /**
     * Parses a given String by detailed date formats
     * considering the german and english formats
     * @param date The date String to parse
     * @return The date, or null
     */
    public static Date parseDetailedFormat(String date) {
        Date dt = null;
        try {
            dt = DETAILED_FORMAT_DE.parse(date);
        } catch (ParseException e) {
            try {
                dt = DETAILED_FORMAT_EN.parse(date);
            } catch (ParseException e2) {
                e.printStackTrace();
            }
        }
        return dt;
    }
    
    
    /**
     * Formats a given Date by detailed date formats
     * considering the german and english formats
     * @param date The Date to parse
     * @return The date String, or null
     */
    public static String formatDetailedFormat(Date date) {
        String dt;
        String currentLocale = 
            ResourceBundleMgmt.getInstance().getUserBundle().getLocale().getLanguage();
        if(LANGUAGE_CODE_ENGLISH.equals(currentLocale))
            dt = DETAILED_FORMAT_EN.format(date);
        else
            dt = DETAILED_FORMAT_DE.format(date);
        return dt;
    }
    
    public static void setColorRow1(String hexvalue) {
        COLOR_ROW1 = Color.decode(hexvalue);
    }
    
    public static void setColorRow2(String hexvalue) {
        COLOR_ROW2 = Color.decode(hexvalue);
    }
    
    public static void main(String[] args) {
        String date = "10. Juli 2009 12:12:12";
        Date dt = null;
        try {
            dt = DETAILED_FORMAT_DE.parse(date);
        } catch (ParseException e) {
            try {
                dt = DETAILED_FORMAT_EN.parse(date);
            } catch (ParseException ignored) {}
        }
        assert dt != null;
        System.out.println(dt.getTime());
    }
}
