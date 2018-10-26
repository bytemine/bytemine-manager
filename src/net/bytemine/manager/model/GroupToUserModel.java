/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
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
    private Vector<GroupToUserEntry> originals;
    // the new connections
    public Vector<GroupToUserEntry> toAdd = new Vector<>();
    // the connections to remove
    public Vector<GroupToUserEntry> toRemove = new Vector<>();

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
        return (toAdd.contains(entry) || originals.contains(entry)) && !toRemove.contains(entry);
    }


    /**     * just debug output
     */
    public void printDebug() {
        System.out.println("adding entries: ");
        toAdd.stream().map(element -> "  group: " + element.groupid + ", userid: " + element.userid).forEach(System.out::println);

        System.out.println("removing entries: ");
        toRemove.stream().map(element -> "  group: " + element.groupid + ", userid: " + element.userid).forEach(System.out::println);
	}
	
}
