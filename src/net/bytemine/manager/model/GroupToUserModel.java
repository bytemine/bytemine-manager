/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.util.Iterator;
import java.util.Vector;

import net.bytemine.manager.db.GroupQueries;


/**
 * Holds the connections between users and groups
 *
 * @author Daniel Rauer
 */
public class GroupToUserModel {

    // all user<->group connections
    public Vector<GroupToUserEntry> originals = new Vector<GroupToUserEntry>();
    // the new connections
    public Vector<GroupToUserEntry> toAdd = new Vector<GroupToUserEntry>();
    // the connections to remove
    public Vector<GroupToUserEntry> toRemove = new Vector<GroupToUserEntry>();

    public GroupToUserModel() {
        originals = GroupQueries.getAllUserToGroupConnections();
    }

    public void addEntry(String groupid, String userid) {
        GroupToUserEntry entry = new GroupToUserEntry(groupid, userid);
        if (toRemove.contains(entry))
            toRemove.add(entry);
        if (!toAdd.contains(entry))
            toAdd.add(entry);
    }

    public void removeEntry(String groupid, String userid) {
        GroupToUserEntry entry = new GroupToUserEntry(groupid, userid);
        if (toAdd.contains(entry))
            toAdd.add(entry);
        if (!toRemove.contains(entry))
            toRemove.add(entry);
    }

    /**
     * Detects if a checkbox will be checked
     *
     * @param groupid The groupid
     * @param userid  The userid
     * @return true, if the checkbox will be checked
     */
    public boolean markChecked(String groupid, String userid) {
        GroupToUserEntry entry = new GroupToUserEntry(groupid, userid);
        if ((toAdd.contains(entry) || originals.contains(entry)) && !toRemove.contains(entry))
            return true;
        else
            return false;
    }


    /**     * just debug output
     */
    public void printDebug() {
        System.out.println("adding entries: ");
        for (Iterator<GroupToUserEntry> iter = toAdd.iterator(); iter.hasNext();) {
            GroupToUserEntry element = (GroupToUserEntry) iter.next();
            System.out.println("  group: " + element.groupid + ", userid: " + element.userid);
        }

        System.out.println("removing entries: ");
        for (Iterator<GroupToUserEntry> iter = toRemove.iterator(); iter.hasNext();) {
            GroupToUserEntry element = (GroupToUserEntry) iter.next();
            System.out.println("  group: " + element.groupid + ", userid: " + element.userid);
		}
	}
	
}
