package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.haleywang.putty.common.AesException;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AesUtil;
import com.haleywang.putty.util.CmdUtils;
import com.haleywang.putty.util.Debouncer;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author haley
 */
public class SideView extends JSplitPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(SideView.class);
    public static final String FOR_GROUP = "For group: ";
    public static final String UPDATE_COMMAND = "updateCommand";

    private final transient Debouncer debouncer = new Debouncer(TimeUnit.SECONDS, 3);
    private JTextArea updateCommandTextArea;
    private JTextField commandNameTextField;
    private CommandDto currentEditCommand;

    public static SideView getInstance() {
        return SideView.SingletonHolder.S_INSTANCE;
    }

    private static class SingletonHolder {
        private static final SideView S_INSTANCE = new SideView();
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
                str = IoTool.read(in);
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
                String str1 = IoTool.read(in);
                updateConnectionsJsonTextArea.setText(str1);
            } catch (Exception e) {
                LOGGER.error("reload ConnectionsJson error", e);

            }
        }

        commandsTreeView.updateUI();
        connectionsInfoTreeView.updateUI();
    }

    private void initSidePanel() {
        //this.setBackground(Color.WHITE);

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
        leftMenuView.getCommandTabBtn().addActionListener(e -> topCardLayout.show(topSidePanelWrap, UPDATE_COMMAND));

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

        JPanel updatePasswordPanel = new JPanel(new GridBagLayout());
        addAccountAndPwComponents(updatePasswordPanel);

        JPanel updateConnectionsJsonPanel = new JPanel();
        updateConnectionsJsonPanel.setLayout(new BorderLayout());
        bottomSidePanelWrap.add("commandsTreePanel", commandsTreePanel);
        bottomSidePanelWrap.add("updateConnectionsJsonPanel", updateConnectionsJsonPanel);
        bottomSidePanelWrap.add("updatePasswordPanel", updatePasswordPanel);
        //bottomSidePanelWrap.setBackground(Color.WHITE);


        RSyntaxTextArea textArea = new RSyntaxTextArea(3, 10);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        updateConnectionsJsonTextArea = textArea;

        sp.setVerticalScrollBarPolicy(RTextScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        updateConnectionsJsonTextArea.setLineWrap(false);

        updateConnectionsJsonTextArea.setEditable(true);

        updateConnectionsJsonTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateConnectionsJsonTextArea.getClass(), () -> {
                    changeConnectionsTree();
                    FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
                });
            }
        });

        updateConnectionsJsonPanel.add(sp, BorderLayout.CENTER);

    }

    private void addAccountAndPwComponents(JPanel updatePasswordPanel) {
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        accountField = new JTextField(null, null, 20);
        passwordField = new JPasswordField(null, null, 20);

        setPasswordToConnectGroupLabel = new JLabel(FOR_GROUP);
        setPasswordToConnectGroupLabel.setSize(200, 30);

        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;

        updatePasswordPanel.add(setPasswordToConnectGroupLabel, cs);

        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        updatePasswordPanel.add(setPasswordToConnectGroupLabel, cs);

        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        updatePasswordPanel.add(new JLabel("Account:"), cs);

        cs.gridx = 0;
        cs.gridy = 3;
        cs.gridwidth = 1;
        updatePasswordPanel.add(accountField, cs);

        cs.gridx = 0;
        cs.gridy = 4;
        cs.gridwidth = 1;
        updatePasswordPanel.add(new JLabel("Password:"), cs);

        cs.gridx = 0;
        cs.gridy = 5;
        cs.gridwidth = 1;
        updatePasswordPanel.add(passwordField, cs);

        cs.gridx = 0;
        cs.gridy = 6;
        cs.gridwidth = 1;
        JButton updatePasswordBtn = new JButton("OK");
        cs.gridx = 0;
        cs.gridy = 7;
        cs.gridwidth = 1;
        updatePasswordPanel.add(updatePasswordBtn, cs);
        updatePasswordBtn.addActionListener(e -> saveConnectionPassword());
    }

    private void saveConnectionPassword() {

        TreeNode note =
                (DefaultMutableTreeNode) connectionsInfoTreeView.getLastSelectedPathComponent();

        if (note == null) {
            return;
        }

        if (note.isLeaf()) {
            note = note.getParent();
        }

        String key = note.toString();
        String password = String.valueOf(passwordField.getPassword());

        if (StringUtils.isBlank(password)) {
            return;
        }
        String pass = password;
        try {
            pass = AesUtil.encrypt(pass, aesKey);
        } catch (Exception e1) {
            LOGGER.error("saveConnectionPassword error", e1);
            throw new AesException(e1);
        }

        Map<String, Object> hashMap = getConnectionsPasswordsMap();
        hashMap.put(key, pass);
        hashMap.put(key + "_account", accountField.getText());

        FileStorage.INSTANCE.saveConnectionPassword(hashMap);
    }

    private void createTopSidePanelWrap() {
        topCardLayout = new CardLayout();
        topSidePanelWrap = new JPanel();
        //topSidePanelWrap.setBackground(Color.WHITE);
        topSidePanelWrap.setLayout(topCardLayout);


        connectionsInfoTreeView = createShhConnentTree();

        int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        JScrollPane connectionsTreePanel = new JScrollPane(connectionsInfoTreeView, v, h);

        JPanel updateCommandsJsonPanel = createUpdateCommandsJsonPanel();
        JPanel updateCommandPanel = createUpdateCommandPanel();


        topSidePanelWrap.add("connectionsTreePanel", connectionsTreePanel);
        topSidePanelWrap.add("updateCommandsJsonPanel", updateCommandsJsonPanel);
        topSidePanelWrap.add(UPDATE_COMMAND, updateCommandPanel);
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


        updateCommandsJsonTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateCommandsJsonTextArea.getClass(), () -> {
                    changeCommandsTree();
                    fileStorage.saveCommandsData(updateCommandsJsonTextArea.getText());
                });
            }
        });


        updateCommandsJsonPanel.add(sp, BorderLayout.CENTER);
        return updateCommandsJsonPanel;
    }

    private JPanel createUpdateCommandPanel() {
        JPanel updateCommandPanel = new JPanel();
        updateCommandPanel.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea(3, 10);

        JScrollPane sp = new JScrollPane(textArea);
        updateCommandTextArea = textArea;
        updateCommandTextArea.setLineWrap(true);

        updateCommandTextArea.setEditable(true);

        updateCommandTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                if ((e.getKeyCode() == KeyEvent.VK_E)
                        && (e.isControlDown())) {

                    SwingUtilities.invokeLater(() ->
                            SpringRemoteView.getInstance().onTypedString(updateCommandTextArea.getText())
                    );

                } else if ((e.getKeyCode() == KeyEvent.VK_S)
                        && (e.isControlDown())) {

                    saveCommand();

                }


            }
        });

        JPanel btnsPanel = new JPanel();
        JButton execBtn = new JButton("Run");
        JButton saveBtn = new JButton("Save");
        btnsPanel.add(saveBtn);
        btnsPanel.add(execBtn);

        execBtn.addActionListener(e ->
                SwingUtilities.invokeLater(() ->
                        SpringRemoteView.getInstance().onTypedString(updateCommandTextArea.getText())
                )
        );
        saveBtn.addActionListener(e -> saveCommand());

        updateCommandPanel.add(sp, BorderLayout.CENTER);
        updateCommandPanel.add(btnsPanel, BorderLayout.SOUTH);

        commandNameTextField = new JTextField();
        updateCommandPanel.add(commandNameTextField, BorderLayout.NORTH);

        return updateCommandPanel;
    }

    private void saveCommand() {

        SwingUtilities.invokeLater(() -> {
            currentEditCommand.setCommand(updateCommandTextArea.getText());
            currentEditCommand.setName(commandNameTextField.getText());


            Object userDataObject = ((DefaultMutableTreeNode) commandsTreeView.getModel().getRoot()).getUserObject();


            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            commandsJson = gson.toJson(userDataObject);
            fileStorage.saveCommandsData(commandsJson);

            reloadData();
        });

    }


    private JTree createSideCommandTree() {

        DefaultMutableTreeNode root = createCommandTreeData();
        final JTree treeRoot = new JTree(root);
        treeRoot.setEditable(false);
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    myPopupEvent(e);

                } else {
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

                CommandDto obj = (CommandDto) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();

                String label = "Edit: " + obj.toString();
                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem(label);
                item.addActionListener(ev -> {

                    LOGGER.info("===== click tree item event");

                    currentEditCommand = obj;
                    if (updateCommandTextArea != null) {
                        updateCommandTextArea.setText(obj.getCommand());
                        commandNameTextField.setText(obj.getName());
                    }

                    LeftMenuView.getInstance().getTopButtonGroup().setSelected(LeftMenuView.getInstance().getCommandTabBtn().getModel(), true);
                    topCardLayout.show(topSidePanelWrap, UPDATE_COMMAND);
                });
                popup.add(item);
                popup.show(tree, x, y);
            }

            private void clickEvent(MouseEvent e) {

                LOGGER.info("clickEvent: {}", e.getComponent().getClass());

                sendCommand(treeRoot);
            }
        });

        return treeRoot;
    }

    private void sendCommand(JTree treeRoot) {
        DefaultMutableTreeNode note =
                (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

        Optional.ofNullable(note).map(DefaultMutableTreeNode::getUserObject).ifPresent(userObject -> {
            if (!(userObject instanceof CommandDto)) {
                return;
            }

            CommandDto commandDto = (CommandDto) userObject;
            if (commandDto.getCommand() == null) {
                return;
            }
            if (commandDto.getCommand().startsWith("cmd>") || commandDto.getCommand().startsWith("term>")) {
                CmdUtils.run(commandDto);
            } else {
                SpringRemoteView.getInstance().onTypedString(commandDto.getCommand());

                SwingUtilities.invokeLater(() ->
                        treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath())
                );
            }

        });
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
        treeRoot.setCellRenderer(new MyTreeCellRenderer());

        treeRoot.addTreeSelectionListener(e -> {

            DefaultMutableTreeNode note =
                    (DefaultMutableTreeNode) treeRoot.getLastSelectedPathComponent();

            if (note == null) {
                return;
            }
            changePasswordToConnectGroupLabel(note);


            Optional.ofNullable(note).map(DefaultMutableTreeNode::getUserObject).ifPresent(userObject -> {
                if (userObject instanceof ConnectionDto) {
                    ConnectionDto connectionDto = (ConnectionDto) userObject;

                    if (StringUtils.isBlank(connectionDto.getHost())) {

                        createConnectionsTab(connectionDto);

                        SwingUtilities.invokeLater(() ->
                                treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath())
                        );
                    } else {

                        AccountDto connectionAccount = getConnectionAccount(note);
                        if (note.getChildCount() == 0 && connectionAccount == null) {
                            JOptionPane.showMessageDialog(SideView.this,
                                    "Account and password can not be empty.",
                                    "Cannot Connect to " + note.toString()
                                    ,
                                    JOptionPane.ERROR_MESSAGE);

                            LeftMenuView.getInstance().getPasswordTabBtn().doClick();
                            return;
                        }

                        createConnectionsTab(connectionDto);

                        SwingUtilities.invokeLater(() ->
                                treeRoot.getSelectionModel().removeSelectionPath(treeRoot.getSelectionPath())
                        );
                    }
                }
            });

        });

        return treeRoot;
    }

    private void changePasswordToConnectGroupLabel(DefaultMutableTreeNode note) {
        if (setPasswordToConnectGroupLabel != null) {

            if (note.isLeaf()) {
                setPasswordToConnectGroupLabel.setText(FOR_GROUP + StringUtils.ifBlank(note.getParent().toString(), ""));

            } else {
                setPasswordToConnectGroupLabel.setText(FOR_GROUP + StringUtils.ifBlank(note.toString(), ""));

            }
        }
    }

    public void createConnectionsTab(ConnectionDto connectionDto) {


        DefaultMutableTreeNode connectionsTreeNode = (DefaultMutableTreeNode) getConnectionsInfoTreeView().getModel().getRoot();

        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        findNode(nodes, connectionsTreeNode, connectionDto);
        if (nodes.isEmpty()) {
            return;
        }

        AccountDto connectionAccount = getConnectionAccount(nodes.get(0));


        SpringRemoteView.getInstance().onCreateConnectionsTab(connectionDto, connectionAccount);
    }

    private void findNode(List<DefaultMutableTreeNode> treeNodes, DefaultMutableTreeNode connectionsTreeNode, ConnectionDto connectionDto) {

        Action action = (Action) connectionsTreeNode.getUserObject();

        if (connectionsTreeNode.isLeaf() && action.searchText().equals(connectionDto.searchText())) {
            treeNodes.add(connectionsTreeNode);
        } else {
            int count = connectionsTreeNode.getChildCount();
            for (int i = 0; i < count; i++) {
                TreeNode childNode = connectionsTreeNode.getChildAt(i);
                if (childNode instanceof DefaultMutableTreeNode) {
                    findNode(treeNodes, (DefaultMutableTreeNode) childNode, connectionDto);
                }
            }
        }

    }

    private AccountDto getConnectionAccount(DefaultMutableTreeNode note) {
        if (note == null) {
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
        if (note.isLeaf()) {
            return getConnectionAccountExtend(note.getParent());
        }
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
                String str = IoTool.read(this.getClass().getResourceAsStream("/myConnectionsInfoExample.json"));
                dto = new Gson().fromJson(str, ConnectionDto.class);
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
                String str = IoTool.read(in);
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
            dto.setPassword(AesUtil.decrypt(pass, aesKey));
        } catch (Exception e) {
            throw new AesException(e);
        }
        dto.setName((String) map.get(nodeName + "_account"));
        if (dto.getName() != null) {
            dto.setName(dto.getName().replace("\\\\", "\\"));
        }
        return dto;
    }


    void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }


    public JTree getCommandsTreeView() {
        return commandsTreeView;
    }

    public JTree getConnectionsInfoTreeView() {
        return connectionsInfoTreeView;
    }
}

class MyTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userValue = node.getUserObject();

            if (userValue instanceof CommandDto) {
                CommandDto commandDto = (CommandDto) userValue;
                if (commandDto.getChildrenCount() == 0) {
                    setText(commandDto.getName(), commandDto.getCommand());

                } else {
                    setText(value.toString());

                }
            } else if (userValue instanceof ConnectionDto) {
                ConnectionDto connectionDto = (ConnectionDto) userValue;
                if (connectionDto.getChildrenCount() == 0) {
                    setText(connectionDto.getName(), connectionDto.getHost());

                } else {
                    setText(value.toString());

                }
            } else {
                setText(value.toString());

            }


        }
        return this;
    }

    private void setText(String name, String host) {
        if (StringUtils.isAnyBlank(name, host)) {
            setText("<html><div>" + StringUtils.ifBlank(name, host) + "</div></html>");

        } else {
            setText("<html><div><span>" + name + "</span><br/> <span style=\"color:#888888;\">" + host + "</span></div></html>");

        }
    }
}
