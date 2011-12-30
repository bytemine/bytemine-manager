/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.action;

import java.util.logging.Logger;

import net.bytemine.manager.bean.Group;
import net.bytemine.manager.db.GroupDAO;


/**
 * Group actions
 *
 * @author Daniel Rauer
 */
public class GroupAction {

    private static Logger logger = Logger.getLogger(GroupAction.class.getName());



    /**
     * Create new group
     *
     * @param name
     * @param description
     * @return the new groupid
     * @throws java.lang.Exception
     */
    public static int createGroup(String name, String description)
            throws Exception {

        // create new group
        Group newGroup = new Group(name, description);
        return newGroup.getGroupid();
    }


    /**
     * delete the group with the given id
     *
     * @param groupId
     * @throws java.lang.Exception
     */
    public static void deleteGroup(String groupId) throws Exception {
        GroupDAO.getInstance().deleteById(groupId);
    }


    /**
     * Updates attributes of the group
     *
     * @param groupId
     * @param name
     * @param description
     * @throws java.lang.Exception
     */
    public static void updateGroup(
            String groupId, String name, String description)
            throws Exception {
        logger.info("Update Group with id: " + groupId);

        Group group = new Group(Integer.parseInt(groupId));
        group = GroupDAO.getInstance().read(group);

        group.setName(name);
        group.setDescription(description);

        GroupDAO.getInstance().update(group);
    }

}
