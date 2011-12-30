/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import javax.swing.JMenu;


public class TestUtils {
    
    /**
     * Returns the component identified by the given name.
     * Component has to be a child of the parent component.
     * @param parent The parent component.
     * @param name The name of the component to find.
     * @return The found component or null.
     */
    public static Component getChildNamed(Component parent, String name) {
        // Debug line
        //System.out.println("Class: " + parent.getClass() +
        //    " Name: " + parent.getName());
        
        if (name.equals(parent.getName())) { 
            return parent;
        }
        
        if (parent instanceof Container) {
            Component[] children = ((Container)parent).getComponents();
            
            for (int i = 0; i < children.length; ++i) {
                Component child = getChildNamed(children[i], name);
                if (child != null) { 
                    return child; 
                }
            }
        }
        
        if (parent instanceof Container) {
            // This detection of a menu is not needed at the moment
            //Component[] children = (parent instanceof JMenu) ?
            //        ((JMenu)parent).getMenuComponents() :
            //        ((Container)parent).getComponents();
            
        }
        
        return null;
    }
        
    static int counter;
    /**
     * Returns the component identified by the given class name and index.
     * Component has to be a child of the parent component.
     * @param parent The parent component.
     * @param classname The class name of the component to find.
     * @param index The index of the component. Starts with 0.
     * @return The found component or null.
     */
    public static Component getChildIndexed(Component parent, String classname, int index) {
        counter = 0;

        // Step in only owned windows and ignore its components in JFrame
        if (parent instanceof Window) {
            Component[] children = ((Window)parent).getOwnedWindows();

            for (int i = 0; i < children.length; ++i) {
                // Take only active windows
                if (children[i] instanceof Window &&
                        !((Window)children[i]).isActive()) {
                    continue;
                }

                Component child = getChildIndexedInternal(
                        children[i], classname, index);
                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Returns the component identified by the given class name and index.
     * Component has to be a child of the parent component.
     * @param parent The parent component.
     * @param classname The class name of the component to find.
     * @param index The index of the component. Starts with 0.
     * @return The found component or null.
     */
    private static Component getChildIndexedInternal(
            Component parent, String classname, int index) {

        // Debug line
        //System.out.println("Class: " + parent.getClass() +
        //    " Name: " + parent.getName());

        if (parent.getClass().toString().endsWith(classname)) {
            if (counter == index) {
                return parent;
            }
            ++counter;
        }

        if (parent instanceof Container) {
            Component[] children = (parent instanceof JMenu) ?
                    ((JMenu)parent).getMenuComponents() :
                        ((Container)parent).getComponents();

                    for (int i = 0; i < children.length; ++i) {
                        Component child = getChildIndexedInternal(
                                children[i], classname, index);
                        if (child != null) { 
                            return child;
                        }
                    }
        }

        return null;
    }
}
