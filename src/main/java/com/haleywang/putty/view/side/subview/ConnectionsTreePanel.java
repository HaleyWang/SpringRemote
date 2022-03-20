package com.haleywang.putty.view.side.subview;

import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.LeftMenuView;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.SideView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author haley
 * @date 2020/2/2
 */
public class ConnectionsTreePanel extends JScrollPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionsTreePanel.class);
    private static final long serialVersionUID = -5948862724658967856L;
    private static final FileStorage FILE_STORAGE = FileStorage.INSTANCE;


    private final JTree connectionsInfoTreeView;

    public ConnectionsTreePanel() {

        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        connectionsInfoTreeView = createShhConnentTree();
        setViewportView(connectionsInfoTreeView);
    }

    private void paintConnectionsTree(List<ConnectionDto> dtos, DefaultMutableTreeNode parent) {

        for (ConnectionDto dto : dtos) {
            if (dto == null) {
                continue;
            }

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(dto);
            parent.add(node);

            if (dto.getChildren() != null) {
                paintConnectionsTree(dto.getChildren(), node);
            }
        }
    }

    private static class MyTreeMouseAdapter extends MouseAdapter {

        private final JTree treeRoot;

        public MyTreeMouseAdapter(JTree treeRoot) {
            this.treeRoot = treeRoot;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                myPopupEvent(e);
            }

            if (e.getClickCount() == Constants.DOUBLE_CLICK_NUM) {
                createConnectionsTab(e);
            }else {
                DefaultMutableTreeNode note =
                        (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

                if (note == null || !(note.getUserObject() instanceof ConnectionDto)) {
                    return;
                }
                changePasswordToConnectGroupLabel(note);
            }
        }

        private void myPopupEvent(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            JTree tree = (JTree) e.getSource();
            TreePath path = tree.getPathForLocation(x, y);
            if (path == null) {
                return;
            }

            tree.setSelectionPath(path);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            ConnectionDto obj = (ConnectionDto) node.getUserObject();

            String label = "Edit config json";
            String openSession = "Open session";
            JPopupMenu popup = new JPopupMenu();
            JMenuItem editMenuItem = new JMenuItem(label);
            JMenuItem duplicateItem = new JMenuItem("Duplicate: "+ obj.toString());
            JMenuItem deleteItem = new JMenuItem("Delete: "+ obj.toString());
            JMenuItem openFileMenuItem = new JMenuItem("Open config file");
            JMenuItem openMenuItem = new JMenuItem(openSession);
            JMenuItem passwordMenuItem = new JMenuItem("Edit Account");
            openMenuItem.setEnabled(node.isLeaf());
            duplicateItem.setVisible(node.isLeaf());
            deleteItem.setVisible(node.isLeaf());

            popup.add(openMenuItem);
            popup.add(openFileMenuItem);
            popup.add(editMenuItem);
            popup.add(duplicateItem);
            popup.add(deleteItem);
            popup.add(passwordMenuItem);

            passwordMenuItem.addActionListener(ev -> {

                treeRoot.getSelectionModel().setSelectionPath(path);

                SideView.getInstance().showUpdatePasswordPanel();
                changePasswordToConnectGroupLabel(node);

            });
            editMenuItem.addActionListener(ev -> {

                LOGGER.info("===== click editMenuItem event");
                SideView.getInstance().showUpdateConnectionsJsonPanel(node);

            });
            addDuplicateItemActionListener(tree, node, obj, duplicateItem);
            addDeleteItemActionListener(tree, node, deleteItem);

            openMenuItem.addActionListener(ev -> {

                LOGGER.info("===== click openMenuItem event");
                createConnectionsTab(e);

            });
            openFileMenuItem.addActionListener(ev -> {

                LOGGER.info("===== click openFileMenuItem event");
                try {
                    Desktop.getDesktop().open(new File(FileStorage.DATA_FOLDER_PATH));
                } catch (IOException e1) {
                    LOGGER.error("open file error" , e1);
                }

            });

            popup.show(tree, x, y);
        }

        private void addDeleteItemActionListener(JTree tree, DefaultMutableTreeNode node, JMenuItem deleteItem) {
            deleteItem.addActionListener(ev -> {

                LOGGER.info("===== click editMenuItem event");

                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

                ConnectionDto parentObj = (ConnectionDto)  parent.getUserObject();

                int deleteIndex = parent.getIndex(node);
                parent.remove(deleteIndex);

                int childCount = parent.getChildCount();
                ArrayList<ConnectionDto> children = new ArrayList<>(childCount);
                for(int  i = 0 ; i < childCount; i++) {
                    if(i == deleteIndex) {
                        continue;
                    }
                    children.add((ConnectionDto)  ((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject());
                }
                parentObj.setChildren(children);

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(parent);

                DefaultMutableTreeNode root = (DefaultMutableTreeNode)  model.getRoot();
                ConnectionDto rootObj = (ConnectionDto) root.getUserObject();
                String json = JsonUtils.toJson(rootObj);

                SideView.getInstance().showUpdateConnectionsJsonPanel(parent, json);

            });
        }

        private void addDuplicateItemActionListener(JTree tree, DefaultMutableTreeNode node, ConnectionDto obj, JMenuItem duplicateItem) {
            duplicateItem.addActionListener(ev -> {

                LOGGER.info("===== click duplicateItem event");


                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

                ConnectionDto newObj = new ConnectionDto();
                newObj.setName(obj.getName() + " copy");
                newObj.setHost(obj.getHost());
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newObj);
                parent.insert(newNode,parent.getIndex(node)+1);

                ConnectionDto parentObj = (ConnectionDto)  parent.getUserObject();

                int childCount = parent.getChildCount();
                ArrayList<ConnectionDto> children = new ArrayList<>(childCount);
                for(int  i = 0 ; i < childCount; i++) {
                    children.add((ConnectionDto)  ((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject());
                }
                parentObj.setChildren(children);

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(parent);

                DefaultMutableTreeNode root = (DefaultMutableTreeNode)  model.getRoot();
                ConnectionDto rootObj = (ConnectionDto) root.getUserObject();
                String json = JsonUtils.toJson(rootObj);

                SideView.getInstance().showUpdateConnectionsJsonPanel(newNode, json);

            });
        }

        private void createConnectionsTab(MouseEvent e) {

            LOGGER.info("createConnectionsTab: {}", e.getComponent().getClass());
            DefaultMutableTreeNode note =
                    (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

            if (note == null || !(note.getUserObject() instanceof ConnectionDto)) {
                return;
            }

            Object userObject = note.getUserObject();
            ConnectionDto connectionDto = (ConnectionDto) userObject;
            if(showAccountEmptyMessageDialog(note, connectionDto)) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                SideView.getInstance().createConnectionsTab(connectionDto);

                treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath());
                SpringRemoteView.getInstance().focusCurrentTerm();

            });

        }

        private void changePasswordToConnectGroupLabel(DefaultMutableTreeNode note) {
            SideView.getInstance().changePasswordToConnectGroupLabel(note);
        }

        private boolean showAccountEmptyMessageDialog(DefaultMutableTreeNode note, ConnectionDto connectionDto) {

            if (!StringUtils.isBlank(connectionDto.getHost())) {

                AccountDto connectionAccount = SideView.getInstance().getConnectionAccount(note);
                if (note.getChildCount() == 0 && connectionAccount == null) {
                    JOptionPane.showMessageDialog(SideView.getInstance(),
                            "Account can not be empty.",
                            "Cannot Connect to " + note.toString(),
                            JOptionPane.ERROR_MESSAGE);

                    LeftMenuView.getInstance().getPasswordTabBtn().doClick();
                    return true;
                }
            }
            return false;
        }
    }

    private JTree createShhConnentTree() {

        DefaultMutableTreeNode root = createConnectionsTreeData();

        JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addMouseListener(new MyTreeMouseAdapter(treeRoot));

        return treeRoot;
    }

    private DefaultMutableTreeNode createConnectionsTreeData() {

        ConnectionDto dto = null;

        String str = FILE_STORAGE.getConnectionsInfoData();
        if (str != null) {
            dto = new Gson().fromJson(str, ConnectionDto.class);
        }


        if (dto == null || dto.getChildren() == null || dto.getChildren().isEmpty()) {
            try {
                str = IoTool.read(this.getClass(), "/myConnectionsInfoExample.json");
                dto = new Gson().fromJson(str, ConnectionDto.class);
                FILE_STORAGE.saveConnectionsInfoData(str);
            } catch (Exception e) {
                LOGGER.error("createConnectionsTreeData error", e);
            }

        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);
        if (dto != null) {
            paintConnectionsTree(dto.getChildren(), root);
        }


        return root;
    }

    public JTree getConnectionsInfoTreeView() {
        return connectionsInfoTreeView;
    }

    public void changeConnectionsTree() {
        DefaultMutableTreeNode root = createConnectionsTreeData();

        TreeModel model = new DefaultTreeModel(root, false);
        if (connectionsInfoTreeView != null) {
            connectionsInfoTreeView.setModel(model);
            //connectionsInfoTreeView
        }

    }
}
