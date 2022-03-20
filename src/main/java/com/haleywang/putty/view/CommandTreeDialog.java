package com.haleywang.putty.view;

import com.google.common.base.Preconditions;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.view.side.SideView;
import com.haleywang.putty.view.side.subview.CommandsTreePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CommandTreeDialog extends JDialog
{

    private static final long serialVersionUID = 2887490079763355645L;
    JTree tree;
    DefaultTreeModel model;
    private static final FileStorage FILE_STORAGE = FileStorage.INSTANCE;

    TreePath movePath;

    JButton addSiblingButton = new JButton("Add sibling nodes");
    JButton addChildButton = new JButton("Add child node");
    JButton deleteButton = new JButton("Delete node");
    JButton editButton = new JButton("Edit node");

    SpringRemoteView owner;
    private JLabel pathLabel;

    public CommandTreeDialog(SpringRemoteView instance) {
        super(instance, "Save as", true);
        this.setSize(600, 500);
        this.owner = instance;
        init();
    }

    public CommandTreeDialog init()
    {

        DefaultMutableTreeNode root = CommandsTreePanel.createCommandTreeData(this.getClass(), false);

        tree = new JTree(root);
        model = (DefaultTreeModel)tree.getModel();

        model.setAsksAllowsChildren(true);
        tree.setEditable(true);
        MouseListener ml = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
                if (tp != null)
                {
                    movePath = tp;
                }
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
                TreePath tp = tree.getPathForLocation(e.getX(), e.getY());

                if (tp != null && movePath != null)
                {
                    //阻止向子节点拖动
                    if (movePath.isDescendant(tp) && movePath != tp)
                    {
                        JOptionPane.showMessageDialog(owner, "目标节点是被移动节点的子节点，无法移动！",
                                "非法操作", JOptionPane.ERROR_MESSAGE );
                        return;
                    }
                    //既不是向子节点移动，而且鼠标按下、松开的不是同一个节点
                    else if (movePath != tp)
                    {
                        System.out.println(tp.getLastPathComponent());
                        //add方法可以先将原节点从原父节点删除，再添加到新父节点中
                        DefaultMutableTreeNode currentNode = ((DefaultMutableTreeNode)tp.getLastPathComponent());
                        if(currentNode.getAllowsChildren()) {
                            currentNode.add(
                                    (DefaultMutableTreeNode)movePath.getLastPathComponent());
                            tree.updateUI();
                        }
                        movePath = null;
                    }
                }
            }
        };
        tree.addMouseListener(ml);
        final JButton okButton = new JButton("Ok");

        tree.addTreeSelectionListener((e) -> {

            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath.getPathCount() <= 1) {
                okButton.setEnabled(false);
            }else{
                okButton.setEnabled(true);
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
            if(!node.getAllowsChildren()) {
                selectedPath = selectedPath.getParentPath();
            }
            pathLabel.setText(selectedPath.toString());
        });

        JPanel buttonsPanel = new JPanel();

        JPanel menuPanel = new JPanel();
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        JLabel pathTileLabel = new JLabel();
        pathLabel = new JLabel();
        topPanel.add(pathTileLabel, BorderLayout.WEST);
        pathTileLabel.setText("Selected: ");
        topPanel.add(pathLabel, BorderLayout.CENTER);

        topPanel.add(menuPanel, BorderLayout.NORTH);

        addSiblingButton.addActionListener(event -> {
            //获取选中节点
            DefaultMutableTreeNode selectedNode
                    = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            //如果节点为空，直接返回
            if (selectedNode == null) {
                return;
            }
            //获取该选中节点的父节点
            DefaultMutableTreeNode parent
                    = (DefaultMutableTreeNode)selectedNode.getParent();
            //如果父节点为空，直接返回
            if (parent == null) {
                return;
            }


            //创建一个新节点
            DefaultMutableTreeNode newNode = createNewNode();
            //获取选中节点的选中索引
            int selectedIndex = parent.getIndex(selectedNode);
            //在选中位置插入新节点
            model.insertNodeInto(newNode, parent, selectedIndex + 1);
            //--------下面代码实现显示新节点（自动展开父节点）-------
            //获取从根节点到新节点的所有节点
            TreeNode[] nodes = model.getPathToRoot(newNode);
            //使用指定的节点数组来创建TreePath
            TreePath path = new TreePath(nodes);
            //显示指定TreePath
            tree.scrollPathToVisible(path);
        });
        menuPanel.add(addSiblingButton);

        addChildButton.addActionListener(event -> {
            //获取选中节点
            DefaultMutableTreeNode selectedNode
                    = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            //如果节点为空，直接返回
            if (selectedNode == null) {
                return;
            }
            //创建一个新节点
            DefaultMutableTreeNode newNode = createNewNode();
            //直接通过model来添加新节点，则无需通过调用JTree的updateUI方法
            //model.insertNodeInto(newNode, selectedNode, selectedNode.getChildCount());
            //直接通过节点添加新节点，则需要调用tree的updateUI方法
            selectedNode.add(newNode);
            //--------下面代码实现显示新节点（自动展开父节点）-------
            TreeNode[] nodes = model.getPathToRoot(newNode);
            TreePath path = new TreePath(nodes);
            tree.scrollPathToVisible(path);
            tree.updateUI();
        });
        menuPanel.add(addChildButton);

        deleteButton.addActionListener(event -> {
            DefaultMutableTreeNode selectedNode
                    = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getParent() != null)
            {
                //删除指定节点
                model.removeNodeFromParent(selectedNode);
            }
        });
        menuPanel.add(deleteButton);

        editButton.addActionListener(event -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath != null)
            {
                //编辑选中节点
                tree.startEditingAtPath(selectedPath);
            }
        });
        menuPanel.add(editButton);



        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(e -> {
            System.out.println(e);
            DefaultMutableTreeNode selectedNode
                    = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            //如果节点为空，直接返回
            if (selectedNode == null) {
                return;
            }

            String currentName = SideView.getInstance().getCommandEditorPanel().getCommandNameTextField().getText();

            currentName = getNewName(selectedNode, currentName);

            SideView.getInstance().getCommandEditorPanel().getCommandNameTextField().setText(currentName);

            //创建一个新节点
            CommandDto dto = new CommandDto();

            dto.setName(currentName);
            dto.setCommand(SideView.getInstance().getCommandEditorPanel().getUpdateCommandTextArea().getText());

            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(dto);
            //直接通过model来添加新节点，则无需通过调用JTree的updateUI方法
            int count = selectedNode.getChildCount();
            int index = count;
            model.insertNodeInto(newNode, selectedNode, index);
            //直接通过节点添加新节点，则需要调用tree的updateUI方法
            //selectedNode.add(newNode);
            //tree.updateUI();

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            //model.reload(selectedNode.getParent());

            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)  model.getRoot();
            CommandDto rootObj = toCommandDto(rootNode);


            TreePath treePath = tree.getSelectionPath();
            Preconditions.checkNotNull(treePath);
            SideView.getInstance().getCommandsTreePanel().setCurrentPathWithLeafIndex(treePath, index);

            FILE_STORAGE.saveCommandsData(rootObj, SideView.getInstance().getCommandsTreePanel().getCurrentPathWithLeafIndex());

            SideView.getInstance().reloadData();
            //SideView.getInstance().showUpdateCommandsJsonPanel(selectedNode, commandsJson);
            SideView.getInstance().changeCommandsTree();
            SideView.getInstance().resetUpdateCommandView(treePath, index);

            CommandTreeDialog.this.dispose();
        });
        cancelButton.addActionListener(e -> {
            CommandTreeDialog.this.setVisible(false);
            CommandTreeDialog.this.dispose();
        });

        buttonsPanel.add(cancelButton);
        okButton.setEnabled(false);
        buttonsPanel.add(okButton);

        this.add(new JScrollPane(tree));
        this.add(topPanel , BorderLayout.NORTH);
        //buttonsPanel
        this.add(buttonsPanel , BorderLayout.SOUTH);

        //this.pack()
        this.setResizable(true);
        return this;
    }

    private String getNewName(DefaultMutableTreeNode selectedNode, String currentName) {
        Set<String> names = new HashSet<>();
        int childCount = selectedNode.getChildCount();
        for (int i =0; i < childCount;i ++) {
            TreeNode cNode = selectedNode.getChildAt(i);
            if(cNode instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode aNode = (DefaultMutableTreeNode)cNode;
                CommandDto commandDto =  (CommandDto)aNode.getUserObject();
                names.add(commandDto.getName());
            }
        }

        currentName = FileStorage.getNewName(currentName, names);

        return currentName;
    }



    private CommandDto toCommandDto(DefaultMutableTreeNode rootNode) {
        CommandDto rootDto = (CommandDto)rootNode.getUserObject();
        CommandDto newCommandDto = new CommandDto();
        newCommandDto.setName(rootDto.getName());
        handleSubNode(rootNode, newCommandDto);
        return newCommandDto;
    }

    void handleSubNode(DefaultMutableTreeNode parentNode, CommandDto parentDto){
        parentDto.setChildren(new ArrayList<>());
        for(int i = 0 ; i < parentNode.getChildCount(); i ++) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)parentNode.getChildAt(i);
            CommandDto subDto = (CommandDto)subNode.getUserObject();
            parentDto.addChild(subDto);
            handleSubNode(subNode, subDto);
        }
    }

    @NotNull
    private DefaultMutableTreeNode createNewNode() {
        DateTimeFormatter dateTimeFormatterPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        CommandDto group = new CommandDto();
        group.setName("New node " + LocalDateTime.now().format(dateTimeFormatterPattern));
        return new DefaultMutableTreeNode(group);
    }


}