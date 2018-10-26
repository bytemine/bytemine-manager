/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.gui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.MenuComponent;
import java.awt.PopupMenu;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;


/**
 * Enhanced JPanel for getting the index of a component
 *
 * @author Daniel Rauer
 */
public class CustomJPanel extends JPanel {

    private static final long serialVersionUID = 3554755396033064206L;

    private List<Object> components = new ArrayList<Object>();


    public CustomJPanel(LayoutManager m) {
        super(m);
    }


    public Component add(Component comp) {
        components.add(comp);
        super.add(comp);
        return comp;
    }

    public void add(PopupMenu m) {
        components.add(m);
        super.add(m);
    }

    public void add(Component comp, Object obj) {
        components.add(comp);
        super.add(comp, obj);
    }

    public Component add(Component comp, int index) {
        components.add(index, comp);
        super.add(comp, index);
        return comp;
    }

    public Component add(String str, Component comp) {
        components.add(comp);
        super.add(str, comp);
        return comp;
    }

    public void add(Component comp, Object obj, int index) {
        components.add(index, comp);
        super.add(comp, obj, index);
    }


    public void remove(Component comp) {
        components.remove(comp);
        super.remove(comp);
    }

    public void remove(int index) {
        components.remove(index);
        super.remove(index);
    }

    public void removeAll() {
        components = new ArrayList<>();
        super.removeAll();
    }

    public void remove(MenuComponent m) {
        components.remove(m);
        super.remove(m);
    }


    /**
     * Returns the index of the component
     *
     * @param comp The component to get the index for
     * @return index
     */
    public int getIndexOf(Component comp) {
        return components.indexOf(comp);
	}

}
