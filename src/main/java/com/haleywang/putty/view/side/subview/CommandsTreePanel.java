package com.haleywang.putty.view.side.subview;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.CollectionUtils;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.PathUtils;
import com.haleywang.putty.view.LeftMenuView;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.SideView;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author haley
 * @date 2020/2/2
 */
public class CommandsTreePanel extends JScrollPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsTreePanel.class);
    private static final FileStorage FILE_STORAGE = FileStorage.INSTANCE;
    private static final long serialVersionUID = 590946503894137269L;

    private final JTree commandsTreeView;

    /**
     * eg: /root/aa/@1
     */
    private String currentPathWithLeafIndex;

    public CommandsTreePanel() {
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        commandsTreeView = createSideCommandTree();
        setViewportView(commandsTreeView);
    }

    private void selectCurrentCommandPathByDefault() {

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) commandsTreeView.getModel().getRoot();
        DefaultMutableTreeNode firstLeaf = getFirstLeafChild(root);
        if (firstLeaf == null) {
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) firstLeaf.getParent();
        TreePath treePath = new TreePath(parent);

        setCurrentPathWithLeafIndex(treePath, parent.getIndex(firstLeaf));
    }

    private DefaultMutableTreeNode getFirstLeafChild(DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) parent.getFirstChild();
        if (first == null) {
            return null;
        }
        if (first.isLeaf()) {
            return first;
        }

        return getFirstLeafChild(first);
    }

    public String getCurrentPathWithLeafIndex() {
        return currentPathWithLeafIndex;
    }

    public void setCurrentPathWithLeafIndex(TreePath treePath, int index) {
        this.currentPathWithLeafIndex = getPath(index, treePath);
    }

    @NotNull
    private String getPath(int index, TreePath treePath) {
        return PathUtils.getPath(treePath) + "/@" + index;
    }

    public String getCurrentPathNoIndex() {
        int lastGroupIndex = currentPathWithLeafIndex.lastIndexOf("/");
        return currentPathWithLeafIndex.substring(0, lastGroupIndex);
    }

    public int getCurrentPathIndex() {
        int lastGroupIndex = currentPathWithLeafIndex.lastIndexOf("/");
        return Integer.parseInt(currentPathWithLeafIndex.substring(lastGroupIndex + 2));
    }

    public DefaultMutableTreeNode getNodeByCurrentPathWithLeafIndex() {
        int index = getCurrentPathIndex();

        TreePath treePath = getTreePath(commandsTreeView, getCurrentPathNoIndex());

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();

        if (node.getChildCount() <= index) {
            //todo insert
        }
        return (DefaultMutableTreeNode) node.getChildAt(index);
    }

    public static String unescapeSpecialCharacters(String name) {
        return name.replaceAll("\\\\/", "/");
    }

    private static TreePath getTreePath(JTree tree, String path) {
        String[] tokens = path.substring(1).split("(?<!\\\\)/");
        TreeModel treeModel = tree.getModel();
        if (treeModel == null) {
            throw new RuntimeException("Could not find model for tree");
        }
        Object rootNode = treeModel.getRoot();
        int start = tree.isRootVisible() ? 1 : 0;
        TreePath treePath = new TreePath(rootNode);
        StringBuilder searchedPath = new StringBuilder();
        if (tree.isRootVisible()) {
            String rootNodeText = unescapeSpecialCharacters(tokens[0]);
            searchedPath.append("/" + rootNodeText);
            Preconditions.checkNotNull(rootNode, "JTree does not have a root node!");
            String errMsg = "JTree root node does not match: Expected </" + getPathText(tree, treePath) + "> Actual: <"
                    + searchedPath.toString() + ">";
            Preconditions.checkArgument(searchedPath.toString().equals("/" + getPathText(tree, treePath)), errMsg);
        }
        for (int i = start; i < tokens.length; i++) {
            String childText = unescapeSpecialCharacters(tokens[i]);
            searchedPath.append("/" + childText);
            boolean matched = false;
            tree.expandPath(treePath);
            for (int j = 0; j < treeModel.getChildCount(treePath.getLastPathComponent()); j++) {
                Object child = treeModel.getChild(treePath.getLastPathComponent(), j);
                TreePath childPath = treePath.pathByAddingChild(child);
                String pt = getPathText(tree, childPath);
                if (childText.equals(pt)) {
                    treePath = childPath;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return null;
            }
        }
        return treePath;
    }

    private static String getPathText(JTree tree, TreePath path) {
        Object lastPathComponent = path.getLastPathComponent();
        if (lastPathComponent == null) {
            return "";
        }
        return getTextForNodeObject(tree, lastPathComponent);
    }

    static Pattern r = Pattern.compile("<span>([^/]*)</span><br/>");

    private static String getTextForNodeObject(JTree tree, Object lastPathComponent) {
        TreeCellRenderer renderer = tree.getCellRenderer();
        if (renderer == null) {
            return null;
        }
        Component c = renderer.getTreeCellRendererComponent(tree, lastPathComponent, false, false, false, 0, false);
        if (c != null && c instanceof JLabel) {
            String text = ((JLabel) c).getText();
            if (text != null && text.startsWith("<html>")) {
                Matcher m = r.matcher(text);
                if (m.find()) {
                    text = m.group(1);
                }
            }
            return text;
        }
        return lastPathComponent.toString();
    }

    public CommandDto getCurrentCommandDto() {
        return (CommandDto) getNodeByCurrentPathWithLeafIndex().getUserObject();
    }

    public void initCurrentCommandPath(SpringRemoteView omegaRemote) {
        if (currentPathWithLeafIndex == null) {
            SettingDto accountSetting = FILE_STORAGE.getSettingDto(omegaRemote.getUserName());
            currentPathWithLeafIndex = accountSetting.getCurrentCommandPath();
            if (currentPathWithLeafIndex == null) {
                selectCurrentCommandPathByDefault();
            } else if (getCurrentCommandDto() == null) {
                selectCurrentCommandPathByDefault();
            }
        }
        if (getCurrentCommandDto() != null) {
            SideView.getInstance().getCommandEditorPanel()
                    .resetUpdateCommandView(getCurrentCommandDto(), getCurrentPathWithLeafIndex());
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
                clickEvent(e);
            }
        }


        private void myPopupEvent(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            JTree tree = (JTree) e.getSource();
            final TreePath path = tree.getPathForLocation(x, y);
            if (path == null) {
                return;
            }

            tree.setSelectionPath(path);

            DefaultMutableTreeNode pnode = (DefaultMutableTreeNode) path.getParentPath().getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            int currentIndex = pnode.getIndex(node);

            CommandDto obj = (CommandDto) node.getUserObject();

            String label = "Edit: " + obj.toString();
            JPopupMenu popup = new JPopupMenu();
            JMenuItem editMenuItem = new JMenuItem(label);
            JMenuItem runMenuItem = new JMenuItem("Run");

            JMenuItem duplicateItem = new JMenuItem("Duplicate: " + obj.toString());
            JMenuItem deleteItem = new JMenuItem("Delete: " + obj.toString());
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

                SideView.getInstance().resetUpdateCommandView(path.getParentPath(), currentIndex);
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

        DefaultMutableTreeNode root = createCommandTreeData(this.getClass(), true);
        final JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addMouseListener(new MyTreeMouseAdapter(treeRoot));

        return treeRoot;
    }

    public static DefaultMutableTreeNode createCommandTreeData(Class cls, boolean showLeaf) {

        CommandDto dto = null;

        String str = FILE_STORAGE.getCommandsData();
        if (str != null) {
            dto = JsonUtils.fromJson(str, CommandDto.class, null);
        }

        if (dto == null || isChildrenEmpty(dto)) {

            try {
                str = IoTool.read(cls, "/myCommandsExample.json");
                dto = new Gson().fromJson(str, CommandDto.class);
                FILE_STORAGE.saveCommandsData(dto, null);
            } catch (Exception e) {
                dto = new CommandDto();
            }

        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);
        List<CommandDto> children = CollectionUtils.notNullList(dto.getChildren());

        paintCommandsTree(children, root, showLeaf);
        return root;
    }

    private static boolean isChildrenEmpty(CommandDto dto) {
        return dto.getChildren() == null || dto.getChildren().isEmpty();
    }

    private static void paintCommandsTree(List<CommandDto> dtos, DefaultMutableTreeNode parent, boolean showLeaf) {

        for (CommandDto dto : dtos) {

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(dto);
            if (!showLeaf) {
                node.setAllowsChildren(true);
            }
            if (dto.getCommand() != null) {
                node.setAllowsChildren(false);
            }
            parent.setAllowsChildren(true);
            parent.add(node);

            if (dto.getChildren() != null) {
                List<CommandDto> children = CollectionUtils.notNullList(dto.getChildren());

                paintCommandsTree(children, node, showLeaf);
            }
        }

    }

    public JTree getCommandsTreeView() {
        return commandsTreeView;
    }

    public void changeCommandsTree() {
        DefaultMutableTreeNode root = createCommandTreeData(this.getClass(), true);

        TreeModel model = new DefaultTreeModel(root, false);
        if (commandsTreeView != null) {
            commandsTreeView.setModel(model);
        }
    }

    public void reloadData() {
        changeCommandsTree();
    }


}
