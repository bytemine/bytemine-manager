/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection between a user and a group
 *
 * @author Daniel Rauer
 */
public class GroupToUserEntry {

    private Logger logger = Logger.getLogger(GroupToUserEntry.class.getName());

    public String groupid;
    public String userid;

    public GroupToUserEntry(String groupid, String userid) {
        this.groupid = groupid;
        this.userid = userid;
    }

    /**
     * detects the equality of this and the given obj by comparing the
     * groupid and the userid
     *
     * @param obj The object to compare
     * @return true, if obj is an GroupToUserEntry instance and groupid and userid are
     *         equal to the attributes of this
     */
    public boolean equals(Object obj) {
        try {
            if (obj != null && obj instanceof GroupToUserEntry) {
                GroupToUserEntry entry = (GroupToUserEntry) obj;
                if (entry.groupid.equals(groupid) && entry.userid.equals(userid))
                    return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting equality of this and an obj", e);
        }
        return false;
	}
}
