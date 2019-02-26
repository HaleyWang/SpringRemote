package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haleywang.putty.common.AESDecryptException;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AESUtil;
import com.haleywang.putty.util.IOTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import line.someonecode.VerticalButton;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SideView extends JPanel {


    public static SideView getInstance(){
        return SingletonHolder.sInstance;
    }



    private static class SingletonHolder {
        private static final SideView sInstance = new SideView();
    }

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

    private FileStorage fileStorage = FileStorage.INSTANCE;
    private JTextField accountField;
    private JPasswordField passwordField;
    private JLabel setPasswordToConnectGroupLabel;


    private SideView() {
        super();
        initSidePanel();

        reloadData();
    }

    public void reloadData() {

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

        accountField = new JTextField(null, null, 20);
        accountField.setSize(300,30);
        passwordField = new JPasswordField(null, null, 20);

        setPasswordToConnectGroupLabel = new JLabel("For group: ");
        setPasswordToConnectGroupLabel.setSize(300, 30);
        updatePasswordPanel.add(setPasswordToConnectGroupLabel);
        JLabel newLine = new JLabel("<html><body><p>&nbsp;</p><br/></body></html>", SwingConstants.CENTER);
        newLine.setPreferredSize(new Dimension(300000, 1));
        updatePasswordPanel.add(setPasswordToConnectGroupLabel);
        updatePasswordPanel.add(newLine);

        updatePasswordPanel.add(new Label("Account:"));
        updatePasswordPanel.add(accountField);
        updatePasswordPanel.add(new Label("Password:"));
        updatePasswordPanel.add(passwordField);
        JButton updatePasswordBtn = new JButton("OK");
        updatePasswordPanel.add(updatePasswordBtn);
        //updatePasswordPanel.add(Box.createGlue());

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

        updateConnectionsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> {
            changeConnectionsTree();
            FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
        });


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
        hashMap.put(key+"_account", accountField.getText());

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
        updateCommandsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> {
            changeCommandsTree();
            fileStorage.saveCommandsData(updateCommandsJsonTextArea.getText());
        });


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

            Optional.ofNullable(note).map(DefaultMutableTreeNode::getUserObject).ifPresent(userObject -> {
                if (userObject instanceof CommandDto) {
                    CommandDto commandDto = (CommandDto) userObject;
                    if (commandDto.getCommand() != null) {
                        SpringRemoteView.getInstance().onTypedString(commandDto.getCommand());

                        SwingUtilities.invokeLater(() -> {
                            treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath());
                        });
                    }
                }
            });
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

            if(setPasswordToConnectGroupLabel != null && note != null) {
                setPasswordToConnectGroupLabel.setText("For group: " + StringUtils.ifBlank(note.toString(), ""));
            }

            AccountDto connectionAccount = getConnectionAccount(note);

            Optional.ofNullable(note).map(DefaultMutableTreeNode::getUserObject).ifPresent(userObject -> {
                if (userObject instanceof ConnectionDto) {
                    ConnectionDto connectionDto = (ConnectionDto) userObject;
                    if (connectionDto.getHost() != null) {
                        SpringRemoteView.getInstance().onCreateConnectionsTab(connectionDto, connectionAccount);

                        SwingUtilities.invokeLater(() -> {
                            treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath());
                        });
                    }
                }
            });

        });

        return treeRoot;
    }

    private AccountDto getConnectionAccount(DefaultMutableTreeNode note) {
        if(note == null) {
            return null;
        }

        return getConnectionAccountExtend(note);
    }

    private AccountDto getConnectionAccountExtend(TreeNode note) {
        if (note == null) {
            return null;
        }
        String name = note.toString();
        try {
            AccountDto accountDto = getConnectionAccountByNodeName(name);
            if (accountDto != null) {
                return accountDto;
            }
        }catch (AESDecryptException e) {
            System.out.println("try to get password from parent node");
        }

        return getConnectionAccountExtend(note.getParent());
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
                    dto = JsonUtils.fromJson(commandsJson, CommandDto.class, null);
                }
            }
        }
        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {
            String str = fileStorage.getCommandsData();
            if (str != null) {
                dto = JsonUtils.fromJson(str, CommandDto.class, null);
            }

        }

        if (dto == null || dto.getChildren() == null || dto.getChildren().size() == 0) {

            try (InputStream in = this.getClass().getResourceAsStream("/myCommandsExample.json")) {
                String str = IOTool.read(in);
                dto = new Gson().fromJson(str, CommandDto.class);
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

    }

    private void changeCommandsTree() {
        DefaultMutableTreeNode root = createCommandTreeData();

        TreeModel model = new DefaultTreeModel(root, false);
        if (commandsTreeView != null) {
            commandsTreeView.setModel(model);
        }
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

    private AccountDto getConnectionAccountByNodeName(String nodeName) {
        Map map = getConnectionsPasswordsMap();
        if (!map.containsKey(nodeName)) {
            return null;
        }
        AccountDto dto = new AccountDto();
        String pass = (String) map.get(nodeName);
        try {
            dto.setPassword( AESUtil.decrypt(pass, aesKey));
        } catch (Exception e) {
            throw new AESDecryptException(e);
        }
        dto.setName((String) map.get(nodeName+"_account"));
        if(dto.getName() != null) {
            dto.setName(dto.getName().replace("\\\\", "\\"));
        }
        return dto;
    }


    void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
