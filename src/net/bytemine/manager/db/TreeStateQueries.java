/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.db;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SQL-Queries for the treestates table
 *
 * @author Daniel Rauer
 */
public class TreeStateQueries {

    private static Logger logger = Logger.getLogger(TreeStateQueries.class.getName());

    
    /**
     * Saves the state for the given tree
     * @param treeName The name of the tree
     * @param expandedNodes The node states
     */
    public static void saveTreeState(String treeName, String expandedNodes) {
        try {
            int treestateid = isTreeStateExisting(treeName); 
            if (treestateid != -1) {
                PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                        "UPDATE treestates SET treename=?, expandednodes=? " +
                                "WHERE stateid=?"
                );
                pst.setInt(3, treestateid);
                pst.setString(1, treeName);
                pst.setString(2, expandedNodes);
                pst.executeUpdate();
                pst.close();
            } else {
                int nextTreeStateId = getNextTreestateid();
        
                PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                        "INSERT INTO treestates VALUES(?,?,?)"
                );
                pst.setInt(1, nextTreeStateId);
                pst.setString(2, treeName);
                pst.setString(3, expandedNodes);
                pst.executeUpdate();
                pst.close();
           }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error saving treestate", e);
        }
    }
    
    
    /**
     * Detects if the tree state of this tree has already been persisted
     * @param treeName The name of the tree
     * @return The treeState id or -1
     */
    private static int isTreeStateExisting(String treeName) {
        int id = -1;
        try {
            PreparedStatement pst = DBConnector.getInstance().getConnection().prepareStatement(
                    "SELECT stateid FROM treestates WHERE treename=?"
            );
            pst.setString(1, treeName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                id = rs.getInt("stateid");
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error detecting if treestate is persited", e);
        }
        return id;
    }
    
    /**
     * retrieves the next treestateid from the db
     *
     * @return int the next treestate id
     */
    private static int getNextTreestateid() throws Exception {
        int treestateid = 0;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT max(stateid) AS maxId FROM treestates");
            if (rs.next())
                treestateid = rs.getInt("maxId") + 1;

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting next treestateid", e);
            throw e;
        }

        return treestateid;
    }
    
    
    /**
     * Reads a saved state of this tree
     *
     * @param treeName The name of the tree to load
     * @return A String containing the expanded nodes
     */
    public static String getTreestate(String treeName) {
        String expandedNodes = null;
        try {
            Statement st = DBConnector.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT expandednodes FROM treestates " +
            		"WHERE treename='" + treeName + "'");
            if (rs.next())
                expandedNodes = rs.getString("expandednodes");

            rs.close();
            st.close();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "error getting the tree state", e);
        }

        return expandedNodes;
    }
}
