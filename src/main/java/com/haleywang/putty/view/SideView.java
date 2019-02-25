package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haleywang.putty.common.AESDecryptException;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AESUtil;
import com.haleywang.putty.util.IOTool;
import com.haleywang.putty.util.StringUtils;
import line.someonecode.VerticalButton;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SideView extends JPanel {


    private CardLayout bottomCardLayout;
    private JPanel bottomSidePanelWrap;
    private CardLayout topCardLayout;
    private JPanel topSidePanelWrap;
    private JTextArea updateCommandsJsonTextArea;
    private JTextArea updateConnectionsJsonTextArea;
    private JTree connectionsInfoTreeView;
    private JTree commandsTreeView;
    private String aesKey;
    private String connectionsJson;
    private String commandsJson;

    private SideViewListener sideViewListener;
    private FileStorage fileStorage = FileStorage.INSTANCE;
    private JPasswordField passwordField;

    public SideView(SideViewListener sideViewListener) {
        super();
        this.sideViewListener = sideViewListener;
        initSidePanel();
    }

    private void initSidePanel() {
        SideView sidePanelWrap = this;
        sidePanelWrap.setLayout(new BorderLayout());
        JPanel sidePanel = new JPanel();

        createTopSidePanelWrap(sidePanel);

        sidePanel.add(topSidePanelWrap);

        createBottomSidePanelWrap();

        JPanel sideTabPanel = initSideTabPanel();

        sidePanel.add(bottomSidePanelWrap);

        sidePanelWrap.add(sideTabPanel, BorderLayout.WEST);
        sidePanelWrap.add(sidePanel, BorderLayout.CENTER);
    }

    private JPanel initSideTabPanel() {
        JPanel sideTabPanel = new JPanel();
        sideTabPanel.setPreferredSize(new Dimension(36, 200));

        sideTabPanel.setLayout(new BoxLayout(sideTabPanel, BoxLayout.Y_AXIS));

        VerticalButton connectionsTabBtn = VerticalButton.rotateLeftBtn("Connections");
        connectionsTabBtn.setSelected(true);

        sideTabPanel.add(connectionsTabBtn);

        VerticalButton commandsJsonTabBtn = VerticalButton.rotateLeftBtn("Commands json");

        sideTabPanel.add(commandsJsonTabBtn);

        ButtonGroup topButtonGroup = new ButtonGroup();
        topButtonGroup.add(connectionsTabBtn);
        topButtonGroup.add(commandsJsonTabBtn);

        sideTabPanel.add(Box.createVerticalGlue());

        VerticalButton connectionsJsonTabBtn = VerticalButton.rotateLeftBtn("Connections json");
        sideTabPanel.add(connectionsJsonTabBtn);

        VerticalButton commandsTabBtn = VerticalButton.rotateLeftBtn("Commands");
        commandsTabBtn.setSelected(true);
        sideTabPanel.add(commandsTabBtn);

        VerticalButton passwordTabBtn = VerticalButton.rotateLeftBtn("Password");
        sideTabPanel.add(passwordTabBtn);

        connectionsJsonTabBtn.addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "updateConnectionsJsonPanel"));

        commandsTabBtn.addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "commandsTreePanel"));

        passwordTabBtn.addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "updatePasswordPanel"));

        commandsJsonTabBtn.addActionListener(e -> topCardLayout.show(topSidePanelWrap, "updateCommandsJsonPanel"));

        connectionsTabBtn.addActionListener(e -> topCardLayout.show(topSidePanelWrap, "connectionsTreePanel"));

        ButtonGroup bottomButtonGroup = new ButtonGroup();

        bottomButtonGroup.add(connectionsJsonTabBtn);
        bottomButtonGroup.add(commandsTabBtn);
        bottomButtonGroup.add(passwordTabBtn);
        return sideTabPanel;
    }


    private void createBottomSidePanelWrap() {
        bottomCardLayout = new CardLayout();
        bottomSidePanelWrap = new JPanel();
        bottomSidePanelWrap.setLayout(bottomCardLayout);


        commandsTreeView = createSideCommandTree();
        //treeRoot.addMouseListener();
        int v2 = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h2 = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        JScrollPane commandsTreePanel = new JScrollPane(commandsTreeView, v2, h2);


        //updatePasswordPanel
        JPanel updatePasswordPanel = new JPanel();

        passwordField = new JPasswordField(null, null, 20);

        updatePasswordPanel.add(new Label("Password:"));
        updatePasswordPanel.add(passwordField);
        JButton updatePasswordBtn = new JButton();
        updatePasswordBtn.setText("OK");
        updatePasswordPanel.add(updatePasswordBtn);
        updatePasswordBtn.addActionListener(e -> saveConnectionPassword());


        //updateConnectionsJsonPanel
        JPanel updateConnectionsJsonPanel = new JPanel();
        updateConnectionsJsonPanel.setLayout(new BorderLayout());
        bottomSidePanelWrap.add("commandsTreePanel", commandsTreePanel);
        bottomSidePanelWrap.add("updateConnectionsJsonPanel", updateConnectionsJsonPanel);
        bottomSidePanelWrap.add("updatePasswordPanel", updatePasswordPanel);

        updateConnectionsJsonTextArea = new JTextArea(3, 10);
        updateConnectionsJsonTextArea.setLineWrap(true);

        updateConnectionsJsonTextArea.setEditable(true);

        updateConnectionsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> changeConnectionsTree());

        String connectionsInfoData = fileStorage.getConnectionsInfoData();
        if (connectionsInfoData != null) {
            updateConnectionsJsonTextArea.setText(connectionsInfoData);
        } else {
            try (InputStream in = this.getClass().getResourceAsStream("/myConnectionsInfoExample.json")) {
                String str1 = IOTool.read(in);
                updateConnectionsJsonTextArea.setText(str1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JScrollPane scrollPane = new JScrollPane(updateConnectionsJsonTextArea);
        updateConnectionsJsonPanel.add(scrollPane, BorderLayout.CENTER);

    }

    private void saveConnectionPassword() {

        DefaultMutableTreeNode note =
                (DefaultMutableTreeNode) connectionsInfoTreeView.getLastSelectedPathComponent();

        String key = note.toString();
        String password = String.valueOf(passwordField.getPassword());

        if (StringUtils.isBlank(password)) {
            return;
        }
        String pass = password;
        try {
            pass = AESUtil.encrypt(pass, aesKey);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        Map<String, Object> hashMap = getConnectionsPasswordsMap();
        hashMap.put(key, pass);

        FileStorage.INSTANCE.saveConnectionPassword(hashMap);
    }

    private void createTopSidePanelWrap(JPanel sidePanel) {
        topCardLayout = new CardLayout();
        topSidePanelWrap = new JPanel();
        topSidePanelWrap.setLayout(topCardLayout);

        sidePanel.setBackground(Color.darkGray);
        sidePanel.setSize(300, 300);
        GridLayout gl = new GridLayout(2, 1);
        //sidePanel.setMinimumSize(d);
        sidePanel.setPreferredSize(new Dimension(260, 300));

        sidePanel.setLayout(gl);

        connectionsInfoTreeView = createShhConnentTree();

        int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        JScrollPane connectionsTreePanel = new JScrollPane(connectionsInfoTreeView, v, h);

        JPanel updateCommandsJsonPanel = createUpdateCommandsJsonPanel();

        topSidePanelWrap.add("connectionsTreePanel", connectionsTreePanel);
        topSidePanelWrap.add("updateCommandsJsonPanel", updateCommandsJsonPanel);
    }

    private JPanel createUpdateCommandsJsonPanel() {
        JPanel updateCommandsJsonPanel = new JPanel();
        updateCommandsJsonPanel.setLayout(new BorderLayout());
        updateCommandsJsonTextArea = new JTextArea(3, 10);
        updateCommandsJsonTextArea.setLineWrap(true);

        updateCommandsJsonTextArea.setEditable(true);
        updateCommandsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> changeCommandsTree());

        String str = fileStorage.getCommandsData();
        if (str != null) {
            updateCommandsJsonTextArea.setText(str);
        } else {
            try (InputStream in = this.getClass().getResourceAsStream("/myCommandsExample.json")) {
                str = IOTool.read(in);
                updateCommandsJsonTextArea.setText(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        JScrollPane scrollPane1 = new JScrollPane(updateCommandsJsonTextArea);
        updateCommandsJsonPanel.add(scrollPane1, BorderLayout.CENTER);
        return updateCommandsJsonPanel;
    }


    private JTree createSideCommandTree() {

        DefaultMutableTreeNode root = createCommandTreeData();
        final JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);

        treeRoot.addTreeSelectionListener(e -> {

            DefaultMutableTreeNode note =
                    (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

            Object userObject = note.getUserObject();
            if (userObject instanceof CommandDto) {
                CommandDto commandDto = (CommandDto) userObject;
                if (commandDto.getCommand() != null) {
                    sideViewListener.onTypedString(commandDto.getCommand());
                }
            }
        });

        return treeRoot;
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

        treeRoot.addTreeSelectionListener(e -> {

            DefaultMutableTreeNode note =
                    (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();
            String connectionPassword = getConnectionPassword(note);

            Object userObject = note.getUserObject();
            if (userObject instanceof ConnectionDto) {
                ConnectionDto connectionDto = (ConnectionDto) userObject;
                if (connectionDto.getHost() != null) {
                    sideViewListener.onCreateConnectionsTab(connectionDto, connectionPassword);
                }
            }
        });

        return treeRoot;
    }

    private String getConnectionPassword(DefaultMutableTreeNode note) {
        return getConnectionPasswordExtend(note);
    }

    private String getConnectionPasswordExtend(TreeNode note) {
        if (note == null) {
            return null;
        }
        String name = note.toString();
        try {
            String pass = getConnectionPasswordByName(name);
            if (!StringUtils.isBlank(pass)) {
                return pass;
            }
        }catch (AESDecryptException e) {
            System.out.println("try to get password from parent node");
        }

        return getConnectionPasswordExtend(note.getParent());
    }

    private DefaultMutableTreeNode createConnectionsTreeData() {

        ConnectionDto dto = null;
        if (updateConnectionsJsonTextArea != null) {
            try {
                dto = new Gson().fromJson(updateConnectionsJsonTextArea.getText(), ConnectionDto.class);
                connectionsJson = updateConnectionsJsonTextArea.getText();
            } catch (Exception e) {
                if (connectionsJson != null) {
                    dto = new Gson().fromJson(connectionsJson, ConnectionDto.class);
                }
            }

        }
        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {
            String str = fileStorage.getConnectionsInfoData();
            if (str != null) {
                dto = new Gson().fromJson(str, ConnectionDto.class);
            }
        }

        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {
            try (InputStream in = this.getClass().getResourceAsStream("/myConnectionsInfoExample.json")) {
                String str = IOTool.read(in);
                dto = new Gson().fromJson(str, ConnectionDto.class);
                System.out.println(str);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);
        paintConnectionsTree(dto.getChildren(), root);

        return root;
    }

    private DefaultMutableTreeNode createCommandTreeData() {

        CommandDto dto = null;
        if (updateCommandsJsonTextArea != null) {
            try {
                dto = new Gson().fromJson(updateCommandsJsonTextArea.getText(), CommandDto.class);
                commandsJson = updateCommandsJsonTextArea.getText();
            } catch (Exception e) {
                if (commandsJson != null) {
                    dto = new Gson().fromJson(commandsJson, CommandDto.class);
                }
            }
        }
        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {
            String str = fileStorage.getCommandsData();
            if (str != null) {
                dto = new Gson().fromJson(str, CommandDto.class);
            }

        }

        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {

            try (InputStream in = this.getClass().getResourceAsStream("/myCommandsExample.json")) {
                String str = IOTool.read(in);
                dto = new Gson().fromJson(str, CommandDto.class);
                System.out.println(str);
            } catch (Exception e) {
                e.printStackTrace();
                dto = new CommandDto();
            }

        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);//创建根节点
        paintCommandsTree(dto.getChildren(), root);
        return root;
    }


    private void changeConnectionsTree() {
        DefaultMutableTreeNode root = createConnectionsTreeData();

        TreeModel model = new DefaultTreeModel(root, false);
        if (connectionsInfoTreeView != null) {
            connectionsInfoTreeView.setModel(model);
            //connectionsInfoTreeView
        }
        FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());

    }

    private void changeCommandsTree() {
        DefaultMutableTreeNode root = createCommandTreeData();

        TreeModel model = new DefaultTreeModel(root, false);
        if (commandsTreeView != null) {
            commandsTreeView.setModel(model);
        }
        fileStorage.saveCommandsData(updateCommandsJsonTextArea.getText());
    }

    private Map<String, Object> getConnectionsPasswordsMap() {
        String text = fileStorage.getConnectionsPasswords();
        if (text == null) {
            return new HashMap<>();
        }
        Map<String, Object> map = new Gson().fromJson(text, new TypeToken<HashMap<String, Object>>() {
        }.getType());
        if (map == null) {
            return new HashMap<>();
        }
        return map;
    }

    private String getConnectionPasswordByName(String nodeName) {
        Map map = getConnectionsPasswordsMap();
        if (!map.containsKey(nodeName)) {
            return null;
        }
        String pass = (String) map.get(nodeName);
        try {
            return AESUtil.decrypt(pass, aesKey);
        } catch (Exception e) {
            throw new AESDecryptException(e);
        }
    }


    void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
