package com.haleywang.putty.view.side.subview;

import com.google.gson.Gson;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.IoTool;
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
import java.util.List;

public class ConnectionsTreePanel extends JScrollPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionsTreePanel.class);


    private final JTree connectionsInfoTreeView;
    private FileStorage fileStorage = FileStorage.INSTANCE;


    public ConnectionsTreePanel() {


        int v2 = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h2 = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        setVerticalScrollBarPolicy(v2);
        setHorizontalScrollBarPolicy(h2);


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

    private JTree createShhConnentTree() {

        DefaultMutableTreeNode root = createConnectionsTreeData();

        JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    myPopupEvent(e);
                }

                if (e.getClickCount() == 2) {
                    clickEvent(e);
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

                String label = "Edit config json";
                String openSession = "Open session";
                JPopupMenu popup = new JPopupMenu();
                JMenuItem editMenuItem = new JMenuItem(label);
                JMenuItem openFileMenuItem = new JMenuItem("Open config file");
                JMenuItem openMenuItem = new JMenuItem(openSession);
                JMenuItem passwordMenuItem = new JMenuItem("Edit Account");
                openMenuItem.setEnabled(node.isLeaf());
                passwordMenuItem.addActionListener(ev -> {

                    treeRoot.getSelectionModel().setSelectionPath(path);

                    SideView.getInstance().showUpdatePasswordPanel();
                    SideView.getInstance().changePasswordToConnectGroupLabel(node);

                });
                editMenuItem.addActionListener(ev -> {

                    LOGGER.info("===== click editMenuItem event");
                    SideView.getInstance().showUpdateConnectionsJsonPanel();

                });
                openMenuItem.addActionListener(ev -> {

                    LOGGER.info("===== click openMenuItem event");
                    clickEvent(e);

                });
                openFileMenuItem.addActionListener(ev -> {

                    LOGGER.info("===== click openFileMenuItem event");
                    try {
                        Desktop.getDesktop().open(new File(FileStorage.DATA_FOLDER_PATH));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                });
                popup.add(openMenuItem);
                popup.add(openFileMenuItem);
                popup.add(editMenuItem);
                popup.add(passwordMenuItem);
                popup.show(tree, x, y);
            }

            private void clickEvent(MouseEvent e) {

                LOGGER.info("clickEvent: {}", e.getComponent().getClass());
                DefaultMutableTreeNode note =
                        (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

                if (note == null || !(note.getUserObject() instanceof ConnectionDto)) {
                    return;
                }
                SideView.getInstance().changePasswordToConnectGroupLabel(note);

                Object userObject = note.getUserObject();
                ConnectionDto connectionDto = (ConnectionDto) userObject;

                if (!StringUtils.isBlank(connectionDto.getHost())) {

                    AccountDto connectionAccount = SideView.getInstance().getConnectionAccount(note);
                    if (note.getChildCount() == 0 && connectionAccount == null) {
                        JOptionPane.showMessageDialog(SideView.getInstance(),
                                "Account can not be empty.",
                                "Cannot Connect to " + note.toString(),
                                JOptionPane.ERROR_MESSAGE);

                        LeftMenuView.getInstance().getPasswordTabBtn().doClick();
                        return;
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    SideView.getInstance().createConnectionsTab(connectionDto);

                    treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath());
                    SpringRemoteView.getInstance().focusCurrentTerm();

                });

            }
        });


        return treeRoot;
    }

    private DefaultMutableTreeNode createConnectionsTreeData() {

        ConnectionDto dto = null;

        String str = fileStorage.getConnectionsInfoData();
        if (str != null) {
            dto = new Gson().fromJson(str, ConnectionDto.class);
        }


        if (dto == null || dto.getChildren() == null || dto.getChildren().isEmpty()) {
            try {
                str = IoTool.read(this.getClass(), "/myConnectionsInfoExample.json");
                dto = new Gson().fromJson(str, ConnectionDto.class);
                fileStorage.saveConnectionsInfoData(str);
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
