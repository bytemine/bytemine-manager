/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


/**
 * A simple test for the UserDAO-class and the db actions
 * @author Daniel Rauer
 */
package net.bytemine.manager.db.test;

import net.bytemine.manager.bean.User;


public class UserTest {

    public static void main(String[] args) {
        createAndDeleteUser();
    }


    private static void createAndDeleteUser() {
        // create new user
        User user;
        try {
            user = new User("adam", "test", -1);

            int id = user.getUserid();

            // load this user by id
            user = User.getUserByID(id);
            System.out.println(user != null ? "Success: loaded user with id " + id + ": " + user.getUsername() : "Error: could not load user with id " + id);

            // delete the user
            //user.delete();

            // try to load, should fail
            user = User.getUserByID(id);

            System.out.println(user != null ? "Error: user with id " + id + " could not be deleted" : "Success: user with id " + id + " has been deleted");

        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

}
