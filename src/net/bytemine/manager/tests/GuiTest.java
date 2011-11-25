/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.gui.ManagerGUI;
import junit.framework.TestCase;

import net.bytemine.manager.db.DBTasks;

public class GuiTest extends TestCase {
    
    public GuiTest(String text) {
        super (text);
    }
    
    // These tests require a blank database without configurations
    public void testFirstSetup() throws Exception {
    	
    	DBTasks.resetDB(false);
    	
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ManagerGUI();
            }
        });

        // configuration dialog should pop up
        JDialog configurationDialog = null;
        for (int i = 0; configurationDialog == null; ++i) {
            Thread.sleep(300);
            configurationDialog = (JDialog)TestUtils.getChildIndexed(ManagerGUI.mainFrame, "JDialog", 0);
            assertTrue(i < 10);
        }
        
        assertNotNull(configurationDialog);
        
        // Test configuration dialog
        JTextField export_path = (JTextField)TestUtils.getChildNamed(configurationDialog, "export_path");
        export_path.setText("/tmp/export_tmp");
        final JButton saveButton = (JButton)TestUtils.getChildNamed(configurationDialog, "config_save");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                saveButton.doClick();
            }
        });        
        Thread.sleep(300);
        assertEquals("/tmp/export_tmp", Configuration.getInstance().CERT_EXPORT_PATH);
        
        // Test if all tabs were rendered
        JTabbedPane tabs = (JTabbedPane)TestUtils.getChildNamed(ManagerGUI.mainFrame, "main_tabs");
        assertEquals(tabs.getComponentCount(), 4);
        
    }
}
