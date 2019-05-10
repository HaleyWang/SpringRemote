package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haleywang.putty.common.AESException;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AESUtil;
import com.haleywang.putty.util.CmdUtils;
import com.haleywang.putty.util.IOTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Label;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SideView extends JSplitPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(SideView.class);

    public static SideView getInstance(){
        return SideView.SingletonHolder.sInstance;
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
        super(JSplitPane.VERTICAL_SPLIT);
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
                LOGGER.error("reload CommandsJson error", e);

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
                LOGGER.error("reload ConnectionsJson error", e);

            }
        }
    }

    private void initSidePanel() {
        this.setBackground(Color.WHITE);

        this.setSize(new Dimension(180, 300));
        this.setPreferredSize(new Dimension(180, 300));
        this.setMinimumSize(new Dimension(0, 0));

        this.setResizeWeight(.5d);
        this.setDividerSize(8);
        this.setContinuousLayout(true);

        createTopSidePanelWrap();
        this.setTopComponent(topSidePanelWrap);

        createBottomSidePanelWrap();
        this.setBottomComponent(bottomSidePanelWrap);

        initSideTabPanel();

    }

    private JPanel initSideTabPanel() {

        LeftMenuView leftMenuView = LeftMenuView.getInstance();


        leftMenuView.getConnectionsJsonTabBtn().addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "updateConnectionsJsonPanel"));

        leftMenuView.getCommandsTabBtn().addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "commandsTreePanel"));

        leftMenuView.getPasswordTabBtn().addActionListener(e -> bottomCardLayout.show(bottomSidePanelWrap, "updatePasswordPanel"));

        leftMenuView.getCommandsJsonTabBtn().addActionListener(e -> topCardLayout.show(topSidePanelWrap, "updateCommandsJsonPanel"));

        leftMenuView.getConnectionsTabBtn().addActionListener(e -> topCardLayout.show(topSidePanelWrap, "connectionsTreePanel"));


        return leftMenuView;
    }


    private void createBottomSidePanelWrap() {
        bottomCardLayout = new CardLayout();
        bottomSidePanelWrap = new JPanel();
        bottomSidePanelWrap.setLayout(bottomCardLayout);

        commandsTreeView = createSideCommandTree();
        int v2 = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h2 = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        JScrollPane commandsTreePanel = new JScrollPane(commandsTreeView, v2, h2);

        JPanel updatePasswordPanel = new JPanel();

        accountField = new JTextField(null, null, 20);
        accountField.setSize(200,30);
        passwordField = new JPasswordField(null, null, 20);

        setPasswordToConnectGroupLabel = new JLabel("For group: ");
        setPasswordToConnectGroupLabel.setSize(200, 30);
        updatePasswordPanel.add(setPasswordToConnectGroupLabel);
        JLabel newLine = new JLabel("<html><body><p>&nbsp;</p><br/></body></html>", SwingConstants.CENTER);
        newLine.setPreferredSize(new Dimension(300000, 1));
        updatePasswordPanel.add(setPasswordToConnectGroupLabel);
        updatePasswordPanel.add(newLine);

        updatePasswordPanel.add(new Label("Account:"));
        updatePasswordPanel.add(new JPanel());
        updatePasswordPanel.add(accountField);
        updatePasswordPanel.add(new JPanel());
        updatePasswordPanel.add(new Label("Password:"));
        updatePasswordPanel.add(new JPanel());
        updatePasswordPanel.add(passwordField);
        updatePasswordPanel.add(new JPanel());
        JButton updatePasswordBtn = new JButton("OK");
        updatePasswordPanel.add(updatePasswordBtn);

        updatePasswordBtn.addActionListener(e -> saveConnectionPassword());

        JPanel updateConnectionsJsonPanel = new JPanel();
        updateConnectionsJsonPanel.setLayout(new BorderLayout());
        bottomSidePanelWrap.add("commandsTreePanel", commandsTreePanel);
        bottomSidePanelWrap.add("updateConnectionsJsonPanel", updateConnectionsJsonPanel);
        bottomSidePanelWrap.add("updatePasswordPanel", updatePasswordPanel);
        bottomSidePanelWrap.setBackground(Color.WHITE);


        RSyntaxTextArea textArea = new RSyntaxTextArea(3, 10);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        updateConnectionsJsonTextArea = textArea;

        sp.setVerticalScrollBarPolicy(RTextScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        updateConnectionsJsonTextArea.setLineWrap(false);

        updateConnectionsJsonTextArea.setEditable(true);

        updateConnectionsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> {
            changeConnectionsTree();
            FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
        });

        updateConnectionsJsonPanel.add(sp, BorderLayout.CENTER);

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
            LOGGER.error("saveConnectionPassword error", e1);
            throw new AESException(e1);
        }

        Map<String, Object> hashMap = getConnectionsPasswordsMap();
        hashMap.put(key, pass);
        hashMap.put(key+"_account", accountField.getText());

        FileStorage.INSTANCE.saveConnectionPassword(hashMap);
    }

    private void createTopSidePanelWrap() {
        topCardLayout = new CardLayout();
        topSidePanelWrap = new JPanel();
        topSidePanelWrap.setBackground(Color.WHITE);
        topSidePanelWrap.setLayout(topCardLayout);


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

        RSyntaxTextArea textArea = new RSyntaxTextArea(3, 10);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        updateCommandsJsonTextArea = textArea;
        updateCommandsJsonTextArea.setLineWrap(false);

        updateCommandsJsonTextArea.setEditable(true);
        updateCommandsJsonTextArea.getDocument().addDocumentListener((MyDocumentListener) e -> {
            changeCommandsTree();
            fileStorage.saveCommandsData(updateCommandsJsonTextArea.getText());
        });


        updateCommandsJsonPanel.add(sp, BorderLayout.CENTER);
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
                    if (commandDto.getCommand() == null) {
                        return;
                    }
                    if (commandDto.getCommand().startsWith("cmd>") || commandDto.getCommand().startsWith("term>")) {
                        CmdUtils.run(commandDto);
                    }else {
                        SpringRemoteView.getInstance().onTypedString(commandDto.getCommand());

                        SwingUtilities.invokeLater(() ->
                            treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath())
                        );
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

            if(note == null) {
                return;
            }
            if(setPasswordToConnectGroupLabel != null) {
                setPasswordToConnectGroupLabel.setText("For group: " + StringUtils.ifBlank(note.toString(), ""));
            }

            AccountDto connectionAccount = getConnectionAccount(note);
            if(note.getChildCount() == 0 && connectionAccount == null) {
                JOptionPane.showMessageDialog(SideView.this,
                        "Account and password can not be empty.",
                        "Cannot Connect to " + note.toString()
                        ,
                        JOptionPane.ERROR_MESSAGE);

                LeftMenuView.getInstance().getPasswordTabBtn().doClick();
                return;
            }

            Optional.ofNullable(note).map(DefaultMutableTreeNode::getUserObject).ifPresent(userObject -> {
                if (userObject instanceof ConnectionDto) {
                    ConnectionDto connectionDto = (ConnectionDto) userObject;
                    if (connectionDto.getHost() != null) {
                        SpringRemoteView.getInstance().onCreateConnectionsTab(connectionDto, connectionAccount);

                        SwingUtilities.invokeLater(() ->
                            treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath())
                        );
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
        AccountDto accountDto = getConnectionAccountByNodeName(name);
        if (accountDto != null) {
            return accountDto;
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
        if (dto == null || dto.getChildren() == null || dto.getChildren().isEmpty()) {
            String str = fileStorage.getConnectionsInfoData();
            if (str != null) {
                dto = new Gson().fromJson(str, ConnectionDto.class);
            }
        }

        if (dto == null || dto.getChildren() == null || dto.getChildren().isEmpty()) {
            try {
                String str = IOTool.read(this.getClass().getResourceAsStream("/myConnectionsInfoExample.json"));
                dto = new Gson().fromJson(str, ConnectionDto.class);
            } catch (Exception e) {
                LOGGER.error("createConnectionsTreeData error", e);
            }

        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dto);
        if(dto != null) {
            paintConnectionsTree(dto.getChildren(), root);
        }


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
        if (dto == null || isChildrenEmpty(dto)) {
            String str = fileStorage.getCommandsData();
            if (str != null) {
                dto = JsonUtils.fromJson(str, CommandDto.class, null);
            }

        }

        if (dto == null || isChildrenEmpty(dto)) {

            try (InputStream in = this.getClass().getResourceAsStream("/myCommandsExample.json")) {
                String str = IOTool.read(in);
                dto = new Gson().fromJson(str, CommandDto.class);
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
            throw new AESException(e);
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
