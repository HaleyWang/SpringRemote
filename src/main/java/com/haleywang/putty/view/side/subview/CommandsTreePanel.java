package com.haleywang.putty.view.side.subview;

import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.view.LeftMenuView;
import com.haleywang.putty.view.side.SideView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;
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
public class CommandsTreePanel extends JScrollPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsTreePanel.class);


    private final JTree commandsTreeView;
    private FileStorage fileStorage = FileStorage.INSTANCE;


    public CommandsTreePanel() {

        int v2 = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h2 = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        setVerticalScrollBarPolicy(v2);
        setHorizontalScrollBarPolicy(h2);

        commandsTreeView = createSideCommandTree();
        setViewportView(commandsTreeView);

    }

    private class MyTreeMouseAdapter extends MouseAdapter {

        private JTree treeRoot;
        public MyTreeMouseAdapter(JTree treeRoot) {
            this.treeRoot = treeRoot;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                myPopupEvent(e);
            }

            if (e.getClickCount() == Constants.DOUBLE_CLICK_NUM) {
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

            CommandDto obj = (CommandDto) node.getUserObject();

            String label = "Edit: " + obj.toString();
            JPopupMenu popup = new JPopupMenu();
            JMenuItem editMenuItem = new JMenuItem(label);
            JMenuItem runMenuItem = new JMenuItem("Run");

            JMenuItem duplicateItem = new JMenuItem("Duplicate: "+ obj.toString());
            JMenuItem deleteItem = new JMenuItem("Delete: "+ obj.toString());
            JMenuItem openFileMenuItem = new JMenuItem("Open config file");
            JMenuItem editJsonMenuItem = new JMenuItem("Edit config file");

            duplicateItem.setVisible(node.isLeaf());
            deleteItem.setVisible(node.isLeaf());

            popup.add(runMenuItem);
            popup.add(editMenuItem);
            popup.add(duplicateItem);
            popup.add(deleteItem);
            popup.add(editMenuItem);
            popup.add(openFileMenuItem);
            popup.add(editJsonMenuItem);

            editJsonMenuItem.addActionListener(ev ->
                    SideView.getInstance().showUpdateCommandsJsonPanel(node)
            );
            runMenuItem.addActionListener(ev ->
                    SideView.getInstance().sendCommand(treeRoot)
            );

            openFileMenuItem.addActionListener(ev -> {
                try {
                    Desktop.getDesktop().open(new File(FileStorage.DATA_FOLDER_PATH));
                } catch (IOException e1) {
                    LOGGER.error("open file error", e1);
                }
            });

            addDuplicateItemActionListener(tree, node, obj, duplicateItem);
            addDeleteItemActionListener(tree, node, deleteItem);

            editMenuItem.addActionListener(ev -> {

                LOGGER.info("===== click tree editMenuItem event");
                if (!node.isLeaf()) {
                    LeftMenuView.getInstance().getTopButtonGroup().setSelected(LeftMenuView.getInstance().getCommandsJsonTabBtn().getModel(), true);
                    SideView.getInstance().showUpdateCommandsJsonPanel();
                    return;
                }

                SideView.getInstance().resetUpdateCommandView(obj);
                LeftMenuView.getInstance().getTopButtonGroup().setSelected(LeftMenuView.getInstance().getCommandTabBtn().getModel(), true);
                SideView.getInstance().showUpdateCommandPanel();
            });

            popup.show(tree, x, y);
        }

        private void addDeleteItemActionListener(JTree tree, DefaultMutableTreeNode node, JMenuItem deleteItem) {
            deleteItem.addActionListener(ev -> {

                LOGGER.info("===== click editMenuItem event");

                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

                CommandDto parentObj = (CommandDto)  parent.getUserObject();

                int deleteIndex = parent.getIndex(node);
                parent.remove(deleteIndex);

                int childCount = parent.getChildCount();
                ArrayList<CommandDto> children = new ArrayList<>(childCount);
                for(int  i = 0 ; i < childCount; i++) {
                    if(i == deleteIndex) {
                        continue;
                    }
                    children.add((CommandDto)  ((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject());
                }
                parentObj.setChildren(children);

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(parent);

                DefaultMutableTreeNode root = (DefaultMutableTreeNode)  model.getRoot();
                CommandDto rootObj = (CommandDto) root.getUserObject();
                String json = JsonUtils.toJson(rootObj);

                SideView.getInstance().showUpdateCommandsJsonPanel(parent, json);

            });
        }

        private void addDuplicateItemActionListener(JTree tree, DefaultMutableTreeNode node, CommandDto obj, JMenuItem duplicateItem) {
            duplicateItem.addActionListener(ev -> {

                LOGGER.info("===== click editMenuItem event");
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

                CommandDto newObj = new CommandDto();
                newObj.setName(obj.getName() + " copy");
                newObj.setCommand(obj.getCommand());
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newObj);
                parent.insert(newNode,parent.getIndex(node)+1);

                CommandDto parentObj = (CommandDto)  parent.getUserObject();

                int childCount = parent.getChildCount();
                ArrayList<CommandDto> children = new ArrayList<>(childCount);
                for(int  i = 0 ; i < childCount; i++) {
                    children.add((CommandDto)  ((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject());
                }
                parentObj.setChildren(children);

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.reload(parent);

                DefaultMutableTreeNode root = (DefaultMutableTreeNode)  model.getRoot();
                CommandDto rootObj = (CommandDto) root.getUserObject();
                String json = JsonUtils.toJson(rootObj);

                SideView.getInstance().showUpdateCommandsJsonPanel(newNode, json);

            });
        }

        private void clickEvent(MouseEvent e) {

            LOGGER.info("clickEvent: {}", e.getComponent().getClass());

            SideView.getInstance().sendCommand(treeRoot);
        }
    }

    private JTree createSideCommandTree() {

        DefaultMutableTreeNode root = createCommandTreeData();
        final JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addMouseListener(new MyTreeMouseAdapter(treeRoot));

        return treeRoot;
    }

    private DefaultMutableTreeNode createCommandTreeData() {

        CommandDto dto = null;

        String str = fileStorage.getCommandsData();
        if (str != null) {
            dto = JsonUtils.fromJson(str, CommandDto.class, null);
        }

        if (dto == null || isChildrenEmpty(dto)) {

            try {
                str = IoTool.read(this.getClass(), "/myCommandsExample.json");
                dto = new Gson().fromJson(str, CommandDto.class);
                fileStorage.saveCommandsData(str);
            } catch (Exception e) {
                dto = new CommandDto();
            }

        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);
        paintCommandsTree(dto.getChildren(), root);
        return root;
    }

    private boolean isChildrenEmpty(CommandDto dto) {
        return dto.getChildren() == null || dto.getChildren().isEmpty();
    }

    private void paintCommandsTree(List<CommandDto> dtos, DefaultMutableTreeNode parent) {

        for (CommandDto dto : dtos) {

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(dto);

            parent.add(node);

            if (dto != null && dto.getChildren() != null) {
                paintCommandsTree(dto.getChildren(), node);
            }
        }

    }

    public JTree getCommandsTreeView() {
        return commandsTreeView;
    }

    public void changeCommandsTree() {
        DefaultMutableTreeNode root = createCommandTreeData();

        TreeModel model = new DefaultTreeModel(root, false);
        if (commandsTreeView != null) {
            commandsTreeView.setModel(model);
        }
    }


}
