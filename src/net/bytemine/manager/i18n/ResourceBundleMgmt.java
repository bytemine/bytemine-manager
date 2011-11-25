/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.Constants;


/**
 * Manages the available resource bundles
 *
 * @author Daniel Rauer
 */
public class ResourceBundleMgmt {

    private static ResourceBundleMgmt instance = null;

    private static ResourceBundle userBundle = null;
    private static ResourceBundle englishBundle = ResourceBundle.getBundle(Constants.BUNDLE_PACKAGE, Locale.US);
    private static ResourceBundle germanBundle = ResourceBundle.getBundle(Constants.BUNDLE_PACKAGE, Locale.GERMAN);


    private ResourceBundleMgmt() {
    }


    /**
     * Returns the instance
     *
     * @return the ResourceBundleMgmt instance
     */
    public static ResourceBundleMgmt getInstance() {
        if (instance == null) {
            instance = new ResourceBundleMgmt();
        }

        return instance;
    }


    /**
     * Returns the bundle corresponding to the given languageCode
     *
     * @param languageCode defined in Configuration-LANGUAGE_CODE
     * @return the requested ResourceBundle or the default Bundle
     */
    public ResourceBundle getBundle(String languageCode) {
        if (languageCode != null && Constants.LANGUAGE_CODE_GERMAN.equals(languageCode))
            return germanBundle;
        else if (languageCode != null && Constants.LANGUAGE_CODE_ENGLISH.equals(languageCode))
            return englishBundle;
        else
            return getDefaultBundle();
    }


    /**
     * Returns the bundle the user selected
     *
     * @return the selected ResourceBundle or the default ResourceBundle
     */
    public ResourceBundle getUserBundle() {
        if (userBundle != null)
            return userBundle;
        else {
            String lang = Configuration.getInstance().LANGUAGE;
            if (lang != null) {
                setUserBundle(lang);
                return userBundle;
            } else
                return getDefaultBundle();
        }

    }


    /**
     * Returns the default ResourceBundle
     *
     * @return the default ResourceBundle
     */
    private ResourceBundle getDefaultBundle() {
        ResourceBundle b = ResourceBundle.getBundle(Constants.BUNDLE_PACKAGE, Locale.getDefault());
        setSelectedLanguage(Locale.getDefault().getLanguage());
        return b;
    }


    /**
     * Sets the selected language as current language
     * and stores it into the database.
     *
     * @param languageCode The code of the selected language (Configuration.LANGUAGE_CODE)
     */
    public void setSelectedLanguage(String languageCode) {
        setUserBundle(languageCode);
    }


    /**
     * Sets the user bundle corresponding to the given languageCode
     *
     * @param languageCode Defined in Configuration-LANGUAGE_CODE
     */
    public void setUserBundle(String languageCode) {
        if (languageCode != null && Constants.LANGUAGE_CODE_GERMAN.equals(languageCode))
            userBundle = germanBundle;
        else if (languageCode != null && Constants.LANGUAGE_CODE_ENGLISH.equals(languageCode))
            userBundle = englishBundle;
        else
            userBundle = getDefaultBundle();
    }


    /**
     * Returns true if a language is already selected
     * and the coice is stored in the database
     *
     * @return true if language was selected, false if not
     */
    public static boolean isLanguageSelected() {
        if (Configuration.getInstance().LANGUAGE == null)
            return false;
        else
            return true;
    }


}
