/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.model;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.bytemine.manager.Constants;
import net.bytemine.manager.bean.X509;
import net.bytemine.manager.db.TreeStateQueries;
import net.bytemine.manager.db.X509DAO;
import net.bytemine.manager.i18n.ResourceBundleMgmt;
import net.bytemine.manager.utility.X509Utils;
import net.bytemine.utility.ImageUtils;
import net.bytemine.utility.StringUtils;
import net.bytemine.utility.TreeUtils;


/**
 * Tree for servers and users
 * @author Daniel Rauer
 *
 * Just a prototype for now...
 */
public class X509TreeModel {
    
    private ResourceBundle rb = ResourceBundleMgmt.getInstance().getUserBundle();
    private static X509TreeModel instance = null;
    
    private JTree tree;
    private X509 x509;
    private DefaultMutableTreeNode topNode = null;
    private String topNodeName = null;
    
    //all expanded tree nodes
    private LinkedList<String> expandedTreeObjects = new LinkedList<String>();
    // has to be suppressed for the expansion
    private boolean supressExpansionEvent = false;
    
    private final String TREENAME = "x509Tree";
    
    
    private X509TreeModel(int x509Id) { 
        x509 = new X509(x509Id);
        x509 = X509DAO.getInstance().read(x509);
        
        restoreState();
        
        initialize();
        createEventListeners();
    }
    
    public static X509TreeModel getInstance(int x509Id) {
        if (instance == null)
            instance = new X509TreeModel(x509Id);
        return instance;
    }

    private void initialize() {
        topNodeName = X509Utils.getCnFromSubject(x509.getSubject());
        
        topNode = new DefaultMutableTreeNode(topNodeName);
        createNodes();
        
        DefaultTreeModel model = new DefaultTreeModel(topNode);
        
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setCellRenderer(new ServerUserRenderer());
        
        MouseListener ml = new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                //Point position = new Point(e.getX(), e.getY());
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                //TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 1) {
                        // left click
                        
                    }
                }
            }
        };
        tree.addMouseListener(ml);
        
        restoreTree(topNode);
    }
    
    
    private void createNodes() {
        DefaultMutableTreeNode idNode = new DefaultMutableTreeNode(
                new AttributeNode(
                        "id", rb.getString("detailsFrame.id"), x509.getX509id()+""));
        DefaultMutableTreeNode versionNode = new DefaultMutableTreeNode(
                new AttributeNode(
                        "version", rb.getString("detailsFrame.version"), x509.getVersion()));
        DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(
                new AttributeNode(
                        "type", rb.getString("detailsFrame.type"), 
                        X509.transformTypeToString(x509.getType())));
        topNode.add(idNode);
        topNode.add(versionNode);
        topNode.add(typeNode);
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
        
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode topNode = (DefaultMutableTreeNode)model.getRoot();
        topNode.removeAllChildren();
        createNodes();
        topNode = TreeUtils.sortTree(topNode);
        
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
        if (supressExpansionEvent == false) {
            TreePath p = (TreePath) e.getPath();
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
        TreePath p = (TreePath) e.getPath();
        Object[] Objs = p.getPath();
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) Objs[Objs.length - 1];
        String myString = TreeUtils.getUserObjectPath(dmtn.getUserObjectPath());
        expandedTreeObjects.remove(myString);
    }
    
    
    /**
     * Save the actual tree state to DB
     */
    public void saveState() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<String> iterator = expandedTreeObjects.iterator(); iterator.hasNext();) {
            String nodePath = (String) iterator.next();
            sb.append(nodePath);
            if (iterator.hasNext())
                sb.append("#");
        }
        TreeStateQueries.saveTreeState(TREENAME, sb.toString());
    }
    
    
    /**
     * Tries to restore the tree state
     */
    public void restoreState() {
        String expandedStr = TreeStateQueries.getTreestate(TREENAME);
        if (expandedStr == null)
            return;
        
        String[] nodeStrings = StringUtils.tokenize(expandedStr, "#");
        if (nodeStrings == null)
            return;
        for (int i = 0; i < nodeStrings.length; i++) {
            String node = nodeStrings[i];
            expandedTreeObjects.add(node);
        }
    }
    
    
    public JTree getX509Tree() {
        return tree;
    }
}


class AttributeNode {
    private String name;
    private String title;
    private String value;
    
    public AttributeNode(String name, String title, String value) {
        this.name = name;
        this.title = title;
        this.value = value;
    }
    
    public String toString() {
        return title;
    }
    
    public String getName() {
        return name;
    }
    public String getTitle() {
        return title;
    }
    public String getValue() {
        return value;
    }
}


class X509Renderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 3214945233939488408L;

    ImageIcon usersIcon = ImageUtils.createImageIcon(Constants.ICON_USERS,
        "users");
    ImageIcon userIcon = ImageUtils.createImageIcon(Constants.ICON_USER,
        "user");
    ImageIcon serverIcon = ImageUtils.createImageIcon(Constants.ICON_SERVER,
        "server");
    ImageIcon serversIcon = ImageUtils.createImageIcon(Constants.ICON_SERVERS,
        "servers");

    public X509Renderer() {}

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
        if (node.getPath().length == 1) {
            
        } else {
            
        }

        return this;
    }
}
