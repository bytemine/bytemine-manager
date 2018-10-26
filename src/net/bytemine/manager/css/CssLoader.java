/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.css;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.bytemine.manager.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Loads an xml file with style rules
 * Mainly taken from http://today.java.net/pub/a/today/2003/10/14/swingcss.html,
 * published under BSD license
 *
 * @author Daniel Rauer
 */
public class CssLoader {

    /**
     * Loads an xml file from the given location and adds the
     * contained rules to the CssRuleManager
     *
     * @param xmlStream The xml file as stream
     * @param crm       The CssRuleManager
     * @throws java.lang.Exception
     */
    public static void load(InputStream xmlStream, CssRuleManager crm) throws Exception {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().
                newDocumentBuilder();

        Document doc = builder.parse(xmlStream);
        Element css = doc.getDocumentElement();
        NodeList list = css.getElementsByTagName("rule");
        for (int i = 0; i < list.getLength(); i++) {
            Element rule = (Element) list.item(i);

            String clss = rule.getAttribute("class");
            String propertyName = rule.getAttribute("property");
            String value = rule.getAttribute("value");
            if (!clss.contains("."))
                clss = "javax.swing." + clss;
            
            if (clss.indexOf("CustomTableCellRenderer") > 0) {
                if (propertyName.equals("background1"))
                    Constants.setColorRow1(value);
                else if (propertyName.equals("background2"))
                    Constants.setColorRow2(value);
            } else {
                Class<?> real_class = Class.forName(clss);
                crm.addCssRule(real_class, propertyName, value);
            }
        }
    }
}
