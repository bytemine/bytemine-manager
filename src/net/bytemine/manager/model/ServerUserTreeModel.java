/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.awt.Component;

import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JTabbedPane;

import net.bytemine.manager.Constants;
import net.bytemine.manager.TreeConfiguration;
import net.bytemine.manager.css.CssRuleManager;
import net.bytemine.manager.db.ServerQueries;
import net.bytemine.manager.db.TreeStateQueries;
import net.bytemine.manager.db.UserQueries;
import net.bytemine.manager.gui.Dialogs;
import net.bytemine.manager.gui.ManagerGUI;
import net.bytemine.manager.gui.ServerDetails;
import net.bytemine.manager.gui.UserDetails;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.StringUtils;
import net.bytemine.utility.TreeUtils;


/**
 * Tree for servers and users
 * @author Daniel Rauer
 *
 */
public class ServerUserTreeModel {
    
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    private static ServerUserTreeModel instance = null;
    
    private JTree tree;
    private DefaultMutableTreeNode topNode = null;
    private String topNodeName = null;
    private String topServerNodeName = null;
    private String topUserNodeName = null;
    private ServerUserTreeModel treeModel;
    private String filterString = "";
    
    //all expanded tree nodes
    private LinkedList<String> expandedTreeObjects = new LinkedList<>();
    // has to be suppressed for the expansion
    private boolean supressExpansionEvent = false;
    
    // flag for remembering unsaved data
    private boolean unsavedData = false;

    
    private final String TREENAME = "serverUserTree";
    
    
    private ServerUserTreeModel() { 
        restoreState();
        
        initialize();
        createEventListeners();
        treeModel = this;
    }
    
    public static ServerUserTreeModel getInstance() {
        if (instance == null)
            instance = new ServerUserTreeModel();
        return instance;
    }

    private void initialize() {
        topNodeName = rb.getString("serverUserTree.topNode.name");
        topUserNodeName = rb.getString("serverUserTree.userTopNode.name");
        topServerNodeName = rb.getString("serverUserTree.serverTopNode.name");

        topNode = new DefaultMutableTreeNode(topNodeName);
        topNode.add(createUserNodes());
        topNode.add(createServerNodes());
        
        DefaultTreeModel model = new DefaultTreeModel(topNode);
        
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setCellRenderer(new ServerUserRenderer());
        
        MouseListener ml = new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                Point position = new Point(e.getX(), e.getY());
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        // right click
                        String[] pathStrings;
                        
                        TreePath[] paths = tree.getSelectionPaths();
                        if (paths!=null && paths.length > 1) {
                            pathStrings = new String[paths.length];
                            // multiple selects
                            if(!TreeUtils.isSelectionAllowed(paths))
                                return;

                            DefaultMutableTreeNode parentNode = null;
                            int level = -1;
                            boolean areServerNodes = false;
                            Vector<String> ids = new Vector<>();
                            for (int i = 0; i < paths.length; i++) {
                                TreePath path = paths[i];
                                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)path.getLastPathComponent();
                                if (isServerNode(selectedNode)) {
                                    areServerNodes = true;
                                    ServerNode sNode = (ServerNode)selectedNode.getUserObject();
                                    ids.add(sNode.getId());
                                } else if (isUserNode(selectedNode)) {
                                    UserNode uNode = (UserNode)selectedNode.getUserObject();
                                    ids.add(uNode.getId());
                                }
                                pathStrings[i] = path.toString();
                                level = path.getPathCount();
                                parentNode = (DefaultMutableTreeNode)selectedNode.getParent();
                            }
                            
                            if (level == 4) {
                                // subnode context menu for deleting assignments of users and servers
                                if (areServerNodes) {
                                    UserNode uNode = (UserNode)(parentNode).getUserObject();
                                    ManagerGUI.showMultipleConnectionsContextFromTree(position, ids, uNode.getId());
                                } else {
                                    ServerNode sNode = (ServerNode)(parentNode).getUserObject();
                                    ManagerGUI.showMultipleConnectionsContextFromTree(position, sNode.getId(), ids);
                                }
                            } else if (areServerNodes)
                                ManagerGUI.showMultipleServersContextFromTree(position, ids);
                            else
                                ManagerGUI.showMultipleUsersContextFromTree(position, ids);
                        } else
                            // single select

                            // is there unsaved data the user wants to store?
                            if (handleUnsavedData(selPath)) {

                                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                                // select this node
                                tree.setSelectionPath(selPath);

                                if (selPath.getPathCount() == 4) {
                                    // connected objects, only manage the assignments
                                    if (isServerNode(selectedNode)) {
                                        ServerNode sNode = (ServerNode) selectedNode.getUserObject();
                                        UserNode uNode = (UserNode) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
                                        ManagerGUI.showConnectionContextFromTree(position, sNode.getId(), uNode.getId());
                                    } else if (isUserNode(selectedNode)) {
                                        UserNode uNode = (UserNode) selectedNode.getUserObject();
                                        ServerNode sNode = (ServerNode) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
                                        ManagerGUI.showConnectionContextFromTree(position, sNode.getId(), uNode.getId());
                                    }
                                } else if (selPath.getPathCount() == 2) {
                                    // user and server top nodes
                                    if (isTopServerNode(selectedNode)) {
                                        ManagerGUI.showTopServerContextFromTree(position);
                                    } else if (isTopUserNode(selectedNode)) {
                                        ManagerGUI.showTopUserContextFromTree(position);
                                    }
                                } else
                                    // manage the object itself
                                    if (isServerNode(selectedNode)) {
                                        ServerNode sNode = (ServerNode) selectedNode.getUserObject();
                                        ManagerGUI.showServerContextFromTree(position, sNode.getId());
                                    } else if (isUserNode(selectedNode)) {
                                        UserNode uNode = (UserNode) selectedNode.getUserObject();
                                        ManagerGUI.showUserContextFromTree(position, uNode.getId());
                                    }
                            }
                    } else if (e.getClickCount() == 1) {
                        // left click
                        
                        // is there unsaved data the user wants to store?
                        if (handleUnsavedData(selPath)) {
                            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                            if (isServerNode(selectedNode)) {
                                ServerNode sNode = (ServerNode)selectedNode.getUserObject();
                                String serverId = sNode.getId();
                                
                                ServerDetails sDetails = new ServerDetails(null, serverId);
                                JTabbedPane detailsPanel = sDetails.createServerDetailsPanel();
                                
                                ManagerGUI.updateServerUserDetails(detailsPanel);
                            } else if (isUserNode(selectedNode)) {
                                UserNode uNode = (UserNode)selectedNode.getUserObject();
                                String userId = uNode.getId();
                                UserDetails uDetails = new UserDetails(null, userId);
                                JPanel detailsPanel = uDetails.createUserDetailsPanel();
                                ManagerGUI.updateServerUserDetails(detailsPanel);
                            } else if (isTopServerNode(selectedNode) || isTopUserNode(selectedNode)) {
                                // add an empty panel, so the tree does not explode to fullscreen
                                JPanel panel = new JPanel();
                                CssRuleManager.getInstance().format(panel);
                                ManagerGUI.updateServerUserDetails(panel);
                            }
                        }
                    } else {
                        // double click
                        
                        // well, that is very weird, but it works...
                        // seems to be a bug in the JTree
                        if (tree.isCollapsed(selPath))
                            tree.collapsePath(selPath);
                        else
                            tree.expandPath(selPath);
                    }
                }
            }
            
            private boolean isServerNode(DefaultMutableTreeNode node) {
                if (node.getPath().length <= 2)
                    return false;
                return node.getUserObject() instanceof ServerNode;
            }
            
            private boolean isUserNode(DefaultMutableTreeNode node) {
                if (node.getPath().length <= 2)
                    return false;
                return node.getUserObject() instanceof UserNode;
            }
            
            private boolean isTopServerNode(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof ServerNode) {
                    ServerNode sNode = (ServerNode)node.getUserObject();
                    return sNode.getId() == null;
                }
                return false;
            }
            
            private boolean isTopUserNode(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof UserNode) {
                    UserNode uNode = (UserNode)node.getUserObject();
                    return uNode.getId() == null;
                }
                return false;
            }
            
            /**
             * Asks the user whether he wants to store his unsaved data
             * @param selPath The selected path
             * @return true, if the user wants to ignore the data
             */
            private boolean handleUnsavedData(TreePath selPath) {
                if (unsavedData) {
                    boolean saveData = Dialogs.showUnsavedDataDialog(ManagerGUI.mainFrame);
                    if (!saveData) {
                        unsavedData = false;
                        return true;
                    }
                    return false;
                }
                return true;
            }
            
        };
        tree.addMouseListener(ml);
        
        
        tree.setDropTarget(new DropTarget(tree, TransferHandler.MOVE,
                new DropTargetAdapter() {
                    public void drop(DropTargetDropEvent dtde) {
                        TreeSelectionModel model = tree.getSelectionModel();
                        TreePath[] selectionPaths = model.getSelectionPaths();

                        Point dropLocation = dtde.getLocation();
                        TreePath targetPath = tree.getClosestPathForLocation(
                                dropLocation.x, dropLocation.y);
                        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath
                                .getLastPathComponent();

                        if (selectionPaths != null) {
                            for (TreePath path : selectionPaths) {
                                DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode) path
                                        .getLastPathComponent();

                                if (isDropAllowed(sourceNode, targetNode)) {

                                    if (TreeConfiguration.DRAG_MEANS_MOVE)
                                        sourceNode.remove(sourceNode);

                                    String serverid;
                                    String userid;
                                    if (sourceNode.getUserObject() instanceof ServerNode) {
                                        serverid = ((ServerNode) sourceNode.getUserObject()).getId();
                                        userid = ((UserNode) targetNode.getUserObject()).getId();
                                    } else {
                                        userid = ((UserNode) sourceNode.getUserObject()).getId();
                                        serverid = ((ServerNode) targetNode.getUserObject()).getId();
                                    }

                                    // save this relationship
                                    ServerQueries.addUserToServer(serverid, userid);

                                    // expand the target node
                                    tree.expandPath(targetPath);

                                    // reload the tree
                                    treeModel.reload(filterString);

                                    dtde.dropComplete(true);
                                    tree.updateUI();
                                } else {
                                    dtde.rejectDrop();
                                    dtde.dropComplete(false);
                                }
                            }
                        }
                    }

                    /**
                     * Decides whether this DnD action is possible
                     * @param sourceNode The node to be dragged
                     * @param targetNode The node that is the drop target
                     * @return true, if the DnD action is possible
                     */
                    private boolean isDropAllowed(DefaultMutableTreeNode sourceNode, 
                            DefaultMutableTreeNode targetNode) {
                        return sourceNode.getUserObject() instanceof ServerNode &&
                                targetNode.getUserObject() instanceof UserNode ? ((ServerNode) sourceNode.getUserObject()).getId() != null &&
                                ((UserNode) targetNode.getUserObject()).getId() != null : sourceNode.getUserObject() instanceof UserNode &&
                                targetNode.getUserObject() instanceof ServerNode && ((UserNode) sourceNode.getUserObject()).getId() != null &&
                                ((ServerNode) targetNode.getUserObject()).getId() != null;
                    }

                }));
        
        restoreTree(topNode);
    }
    
    
    /**
     * Create the user nodes of the tree
     * @return The users top node
     */
    private DefaultMutableTreeNode createUserNodes() {
        DefaultMutableTreeNode userTopNode = new DefaultMutableTreeNode(new UserNode(null, topUserNodeName));
        DefaultMutableTreeNode userNode;
        DefaultMutableTreeNode serverNode;

        Vector<String[]> users = UserQueries.getAllUsersFilteredByUsername(filterString);
        for (String[] user : users) {
            UserNode uNode = new UserNode(user[0], user[1]);
            userNode = new DefaultMutableTreeNode(uNode);

            Vector<String> serverIds = ServerQueries.getServersForUser(user[0]);
            for (String serverId : serverIds) {
                String serverName = ServerQueries.getServerDetails(serverId)[1];
                if (serverName != null) {
                    serverNode = new DefaultMutableTreeNode(new ServerNode(serverId, serverName));
                    userNode.add(serverNode);
                }
            }

            userTopNode.add(userNode);
        }
        userTopNode = TreeUtils.sortTree(userTopNode);
        return userTopNode;
    }
    
    /**
     * Create the server nodes of the tree
     * @return The servers top node
     */
    private DefaultMutableTreeNode createServerNodes() {
        DefaultMutableTreeNode serverTopNode = new DefaultMutableTreeNode(new ServerNode(null, topServerNodeName));
        DefaultMutableTreeNode serverNode;
        DefaultMutableTreeNode userNode;

        Vector<String[]> servers = ServerQueries.getServersFilteredByName(filterString);
        for (String[] server : servers) {
            ServerNode sNode = new ServerNode(server[0], server[1]);
            serverNode = new DefaultMutableTreeNode(sNode);

            Vector<String> userIds = UserQueries.getUsersForServer(server[0]);
            for (String userId : userIds) {
                String userName = UserQueries.getUserDetails(userId)[1];
                if (userName != null) {
                    userNode = new DefaultMutableTreeNode(new UserNode(userId, userName));
                    serverNode.add(userNode);
                }
            }

            serverTopNode.add(serverNode);
        }
        serverTopNode = TreeUtils.sortTree(serverTopNode);
        return serverTopNode;
    }
    
    
    /**
     * Reload the whole tree
     */
    public void reload() {
        reload("");
    }
    
    /**
     * Reload the whole tree
     *
     * @param filterString A String to filter the nodes
     */
    public void reload(String filterString) {
        this.filterString = filterString;
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode topNode = (DefaultMutableTreeNode)model.getRoot();
        topNode.removeAllChildren();
        
        topNode.add(createUserNodes());
        topNode.add(createServerNodes());
        
        model.reload(topNode);
        
        restoreTree(topNode);

        supressExpansionEvent = false;
    }
    
    /**
     * Creates expand and collapse EventListeners
     *
     */
    private void createEventListeners(){
        // Add an expansion listener to the tree
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
                processTreeExpansion(event);
            }
            public void treeCollapsed(TreeExpansionEvent event) {
                processTreeCollapse(event);
            }
        });
    }
    
    /**
     * Restores the tree. Needed after a DnD action, because the reload would
     * collapse all nodes
     * @param topNode The root node
     */
    private void restoreTree(DefaultMutableTreeNode topNode) {
        // Process tree nodes from root node
        restoreTreeNode(tree, new TreePath(topNode), null);
    }
    
    
    /**
     * Restores every node down from the parent node recursively
     * @param tree The whole tree
     * @param parent The node from where to restore downwards
     * @param treeNode The node to restore
     */
    private void restoreTreeNode(JTree tree, TreePath parent, DefaultMutableTreeNode treeNode) {
        // Traverse down through the children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            // Create a child numerator over the node
            Enumeration<?> en = (Enumeration<?>)node.children();
            while (en.hasMoreElements()) {
                DefaultMutableTreeNode dmTreeNode = (DefaultMutableTreeNode)en.nextElement();
                TreePath path = parent.pathByAddingChild(dmTreeNode);
                // recursive call
                restoreTreeNode(tree, path, dmTreeNode);
            }
        }
        
        // Nodes need to be expand from last branch node up
        if (treeNode != null) {
            String myString = TreeUtils.getUserObjectPath(treeNode.getUserObjectPath());
            // compare the current nodes with the stored expanded nodes
            if (expandedTreeObjects.contains(myString)) {
                tree.expandPath(parent);
            }
        }
    }
    
    /**
     * Store the current node as Expanded
     * @param e The ExpansionEvent
     */
    private void processTreeExpansion(TreeExpansionEvent e){
        if (!supressExpansionEvent) {
            TreePath p = e.getPath();
            Object[] Objs = p.getPath();
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) Objs[Objs.length - 1];
            String myString = TreeUtils.getUserObjectPath(dmtn.getUserObjectPath());
            expandedTreeObjects.add(myString);
        }
    }
    
    /**
     * Remove the current node from the expanded nodes
     * @param e The ExpansionEvent
     */
    private void processTreeCollapse(TreeExpansionEvent e){
        TreePath p = e.getPath();
        Object[] Objs = p.getPath();
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) Objs[Objs.length - 1];
        String myString = TreeUtils.getUserObjectPath(dmtn.getUserObjectPath());
        expandedTreeObjects.remove(myString);
    }
    
    
    /**
     * Save the actual tree state to DB
     */
    public void saveState() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> iterator = expandedTreeObjects.iterator(); iterator.hasNext();) {
            String nodePath = iterator.next();
            sb.append(nodePath);
            if (iterator.hasNext())
                sb.append("#");
        }
        TreeStateQueries.saveTreeState(TREENAME, sb.toString());
    }
    
    
    /**
     * Tries to restore the tree state
     */
    private void restoreState() {
        restareTreeState(TREENAME, expandedTreeObjects);
    }

    static void restareTreeState(String treename, LinkedList<String> expandedTreeObjects) {
        String expandedStr = TreeStateQueries.getTreestate(treename);
        if (expandedStr == null)
            return;

        String[] nodeStrings = StringUtils.tokenize(expandedStr, "#");
        Collections.addAll(expandedTreeObjects, nodeStrings);
    }


    public JTree getServerUserTree() {
        return tree;
    }

    public void setUnsavedData(boolean unsavedData) {
        this.unsavedData = unsavedData;
    }
}


class UserNode {
    private String id;
    private String name;
    
    public UserNode(String name) {
        this.id = null;
        this.name = name;
    }
    
    UserNode(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}


class ServerNode {
    private String id;
    private String name;
    
    ServerNode(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String toString() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}


class ServerUserRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 3214945233939488408L;

    private ImageIcon usersIcon = ImageUtils.createImageIcon(Constants.ICON_USERS,
        "users");
    private ImageIcon userIcon = ImageUtils.createImageIcon(Constants.ICON_USER,
        "user");
    private ImageIcon serverIcon = ImageUtils.createImageIcon(Constants.ICON_SERVER,
        "server");
    private ImageIcon serversIcon = ImageUtils.createImageIcon(Constants.ICON_SERVERS,
        "servers");

    ServerUserRenderer() {}

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {
        
        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        if (node.getPath().length != 2) {
            setIcon(isServerNode(value) ? serverIcon : userIcon);
        } else if (isServerNode(value))
            setIcon(node.getChildCount() > 0 ? serversIcon : serverIcon);
        else if (node.getChildCount() > 0)
            setIcon(usersIcon);
        else
            setIcon(userIcon);

        return this;
    }

    private boolean isServerNode(Object value) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        return node.getUserObject() instanceof ServerNode;
    }
}
