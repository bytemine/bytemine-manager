/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Florian Reichel                E-Mail:    reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import net.bytemine.manager.Configuration;


/**
 * A Winodw Listener for the X509 Window
 * 
 * @author Florian Reichel
 *
 */

class X509WindowListener implements WindowListener {
    
    /**
     * catches the normal close event
     */
    public void windowClosed(WindowEvent arg0) {
        saveStates();
    }

    /**
     * catches ALT+F4 and the click on the X of the frame
     */
    public void windowClosing(WindowEvent arg0) {
        saveStates();
    }
    
    /**
     * Save GUI sizes and positions
     */
    private void saveStates() {

        // save the last window size and location
    	
        Configuration.getInstance().setX509GuiWidth(X509Details.detailsFrame.getWidth());
        Configuration.getInstance().setX509GuiHeight(X509Details.detailsFrame.getHeight());
        Configuration.getInstance().setX509GuiLocationX(X509Details.detailsFrame.getLocation().x);
        Configuration.getInstance().setX509GuiLocationY(X509Details.detailsFrame.getLocation().y);
        
        Configuration.getInstance().X509_GUI_LOCATION_X = X509Details.detailsFrame.getLocation().x;
        Configuration.getInstance().X509_GUI_LOCATION_Y = X509Details.detailsFrame.getLocation().y;
        Configuration.getInstance().X509_GUI_WIDTH = X509Details.detailsFrame.getWidth();
        Configuration.getInstance().X509_GUI_HEIGHT = X509Details.detailsFrame.getHeight();
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

