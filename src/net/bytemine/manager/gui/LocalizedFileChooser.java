/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.util.ResourceBundle;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

import net.bytemine.manager.i18n.ResourceBundleMgmt;


/**
 * Localized FileChooser
 *
 * @author Daniel Rauer
 */
public class LocalizedFileChooser {

    public static JFileChooser getLocalizedFileChooser() {

        ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();

        UIManager.put("FileChooser.fileNameLabelText", rb.getString("fc.fileNameLabelText"));
        UIManager.put("FileChooser.lookInLabelText", rb.getString("fc.lookInLabelText"));
        UIManager.put("FileChooser.filesOfTypeLabelText", rb.getString("fc.filesOfTypeLabelText"));
        UIManager.put("FileChooser.upFolderToolTipText", rb.getString("fc.upFolderToolTipText"));
        UIManager.put("FileChooser.homeFolderToolTipText", rb.getString("fc.homeFolderToolTipText"));
        UIManager.put("FileChooser.newFolderToolTipText", rb.getString("fc.newFolderToolTipText"));
        UIManager.put("FileChooser.listViewButtonToolTipTextlist", rb.getString("fc.listViewButtonToolTipTextlist"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText", rb.getString("fc.detailsViewButtonToolTipText"));
        UIManager.put("FileChooser.saveButtonText", rb.getString("fc.saveButtonText"));
        UIManager.put("FileChooser.openButtonText", rb.getString("fc.openButtonText"));
        UIManager.put("FileChooser.updateButtonText", rb.getString("fc.updateButtonText"));
        UIManager.put("FileChooser.helpButtonText", rb.getString("fc.helpButtonText"));
        UIManager.put("FileChooser.saveButtonToolTipText", rb.getString("fc.saveButtonToolTipText"));
        UIManager.put("FileChooser.openButtonToolTipText", rb.getString("fc.openButtonToolTipText"));
        UIManager.put("FileChooser.cancelButtonText", rb.getString("fc.cancelButtonText"));
        UIManager.put("FileChooser.cancelButtonToolTipText", rb.getString("fc.cancelButtonToolTipText"));
        UIManager.put("FileChooser.updateButtonToolTipText", rb.getString("fc.updateButtonToolTipText"));
        UIManager.put("FileChooser.helpButtonToolTipText", rb.getString("fc.helpButtonToolTipText"));

        return new JFileChooser();
    }

}
