/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.bean;

import java.util.logging.Logger;

import net.bytemine.manager.db.GroupDAO;

/**
 * Holds all data of a group
 *
 * @author Daniel Rauer
 */
public class Group {

    private static Logger logger = Logger.getLogger(Group.class.getName());

    private int groupid;
    private String name;
    private String description;

    // indicates wether the object is persistent (serverDAO!=null) or not
    private GroupDAO groupDAO = null;


    public Group(int id) {
        this.groupid = id;

        groupDAO = GroupDAO.getInstance();
    }

    public Group(String name, String description) {

        initialize(name, description);
    }


    private void initialize(String name, String description) {

        this.name = name;
        this.description = description;

        groupDAO = GroupDAO.getInstance();
        // write to db
        groupDAO.create(this);
    }


    /**
     * deletes the group from the db
     */
    public void delete() {
        logger.info("deleting group with id " + this.getGroupid());
        groupDAO.delete(this);
    }

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int id) {
        this.groupid = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
