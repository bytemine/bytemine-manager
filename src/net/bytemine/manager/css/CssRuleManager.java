/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.css;

import java.awt.Component;
import java.awt.Container;
import java.util.Iterator;
import java.util.Vector;

/**
 * Manages 'css' rules for the application
 * Mainly taken from http://today.java.net/pub/a/today/2003/10/14/swingcss.html,
 * published under BSD license
 *
 * @author Daniel Rauer
 */
public class CssRuleManager {

    private static CssRuleManager instance;
    private Vector<CssRule> cssRules;

    private CssRuleManager() {
        cssRules = new Vector<CssRule>();
    }

    public static CssRuleManager getInstance() {
        if (instance == null)
            instance = new CssRuleManager();
        return instance;
    }

    public void addCssRule(Class<?> c, String propertyName, String value) {
        CssRule rule = new CssRule(c, propertyName, value);
        cssRules.add(rule);
    }

    /**
     * applies all existing styles on this component
     *
     * @param component The component to style
     */
    public void format(Component component) {
        Iterator<CssRule> it = cssRules.iterator();
        while (it.hasNext()) {
            CssRule rule = (CssRule) it.next();
            if (rule.matches(component)) {
                rule.apply(component);
            }
        }

        if (!(component instanceof Container)) {
            return;
        }

        Component[] components = ((Container) component).getComponents();
        for (int i = 0; i < components.length; i++) {
            format(components[i]);
        }
    }
}
