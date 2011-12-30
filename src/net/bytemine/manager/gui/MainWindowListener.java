/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.event.WindowEvent;

import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytemine.manager.Configuration;
import net.bytemine.manager.ThreadMgmt;
import net.bytemine.manager.db.DBConnector;
import net.bytemine.manager.model.ServerUserTreeModel;


/**
 * A WindowListener for the main frame
 *
 * @author Daniel Rauer
 */
class MainWindowListener implements WindowListener {

    private static Logger logger = Logger.getLogger(MainWindowListener.class.getName());
    
    /**
     * catches the normal close event
     */
    public void windowClosed(WindowEvent arg0) {
        saveStates();
        cleanupAndExit();
    }

    /**
     * catches ALT+F4 and the click on the X of the frame
     * and asks the user to exit or not, if there are
     * background tasks running
     */
    public void windowClosing(WindowEvent arg0) {
        saveStates();
        
        // show userWarning when having modified a user without syncing to server
        
        if (!Configuration.getInstance().NOT_SYNCED_SERVERS.isEmpty())
        	Dialogs.showWarningUserChangedBox();						// determine whether the user wants to proceed
        
        if (!Configuration.getInstance().NOT_SYNCED_SERVERS.isEmpty()) 	// if user want to exit despite changes
        	return;
        // show exitDialog
        
       	if (ThreadMgmt.getInstance().areThreadsRunning())
       		Dialogs.showExitDialogWithActiveThreads();
       	else
        	Dialogs.showExitDialog();
        	
    }
    
    /**
     * Cleanup database connection and exit the application
     */
    private void cleanupAndExit() {
        try {
            DBConnector.getInstance().getConnection().setAutoCommit(false);
            DBConnector.getInstance().getConnection().commit();
            DBConnector.getInstance().getBaseConnection().setAutoCommit(false);
            DBConnector.getInstance().getBaseConnection().commit();
            DBConnector.resetInstance();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error committing db connection", e);
            e.printStackTrace();
        }

        logger.info("Ending application at " + new Date());
        System.exit(0);
    }
    
    /**
     * Save some states like tree states, GUI sizes and positions
     */
    private void saveStates() {
        // save tree states
        ServerUserTreeModel.getInstance().saveState();
        
        // save the last window size and location
        Configuration.getInstance().setGuiWidth(ManagerGUI.mainFrame.getWidth());
        Configuration.getInstance().setGuiHeight(ManagerGUI.mainFrame.getHeight());
        Configuration.getInstance().setGuiLocationX(ManagerGUI.mainFrame.getLocation().x);
        Configuration.getInstance().setGuiLocationY(ManagerGUI.mainFrame.getLocation().y);
        Configuration.getInstance().setServerUserTreeDividerLocation(ManagerGUI.serverUserSplitPane.getDividerLocation());
    }

    public void windowOpened(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowDeactivated(WindowEvent arg0) {
	}

}
