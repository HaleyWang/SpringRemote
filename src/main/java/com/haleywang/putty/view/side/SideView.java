package com.haleywang.putty.view.side;

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
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.LeftMenuView;
import com.haleywang.putty.view.MyJsonTextArea;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.subview.AccountPasswordPanel;
import com.haleywang.putty.view.side.subview.CommandEditorPanel;
import com.haleywang.putty.view.side.subview.CommandsTreePanel;
import com.haleywang.putty.view.side.subview.ConnectionsTreePanel;
import com.intellij.util.ui.DrawUtil;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
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
    private static final String UPDATE_COMMAND = "updateCommand";

    public static SideView getInstance() {
        return SideView.SingletonHolder.S_INSTANCE;
    }


    private static class SingletonHolder {
        private static final SideView S_INSTANCE = new SideView();
    }

    private final transient Debouncer debouncer = new Debouncer(TimeUnit.SECONDS, 2);


    private CardLayout bottomCardLayout;
    private JPanel bottomSidePanelWrap;
    private CardLayout topCardLayout;
    private JPanel topSidePanelWrap;

    private CommandsTreePanel commandsTreePanel;
    private ConnectionsTreePanel connectionsTreePanel;
    private AccountPasswordPanel accountPasswordPanel;
    private CommandEditorPanel commandEditorPanel;

    private MyJsonTextArea updateCommandsJsonTextArea;
    private MyJsonTextArea updateConnectionsJsonTextArea;

    private String aesKey;

    private FileStorage fileStorage = FileStorage.INSTANCE;

    private SideView() {
        super(JSplitPane.VERTICAL_SPLIT);
        initSidePanel();

        reloadData();
    }

    public void changeTheme() {

        changeStyleViaThemeXml(updateCommandsJsonTextArea);
        changeStyleViaThemeXml(updateConnectionsJsonTextArea);
    }

    public void reloadData() {

        String str = fileStorage.getCommandsData();
        if (str != null) {
            updateCommandsJsonTextArea.setText(str);
        } else {
            try {
                str = IoTool.read(this.getClass(), "/myCommandsExample.json");
                updateCommandsJsonTextArea.setText(str);
            } catch (Exception e) {
                LOGGER.error("reload CommandsJson error", e);
            }
        }

        String connectionsInfoData = fileStorage.getConnectionsInfoData();
        if (connectionsInfoData != null) {
            updateConnectionsJsonTextArea.setText(connectionsInfoData);
        } else {
            try {
                String str1 = IoTool.read(this.getClass(), "/myConnectionsInfoExample.json");
                updateConnectionsJsonTextArea.setText(str1);
            } catch (Exception e) {
                LOGGER.error("reload ConnectionsJson error", e);

            }
        }

        commandsTreePanel.getCommandsTreeView().updateUI();
        connectionsTreePanel.getConnectionsInfoTreeView().updateUI();
    }

    private void initSidePanel() {

        setSize(new Dimension(180, 300));
        setPreferredSize(new Dimension(180, 300));
        setMinimumSize(new Dimension(0, 0));

        setResizeWeight(.5d);
        setDividerSize(8);
        setContinuousLayout(true);

        createTopSidePanelWrap();
        setTopComponent(topSidePanelWrap);

        createBottomSidePanelWrap();
        setBottomComponent(bottomSidePanelWrap);

        initSideTabPanel();
    }

    private void initSideTabPanel() {

        LeftMenuView leftMenuView = LeftMenuView.getInstance();

        leftMenuView.getConnectionsJsonTabBtn().addActionListener(e -> showUpdateConnectionsJsonPanel());
        leftMenuView.getCommandsTabBtn().addActionListener(e -> showCommandsTreePanel());
        leftMenuView.getPasswordTabBtn().addActionListener(e -> showUpdatePasswordPanel());
        leftMenuView.getCommandsJsonTabBtn().addActionListener(e -> showUpdateCommandsJsonPanel());
        leftMenuView.getCommandTabBtn().addActionListener(e -> showUpdateCommandPanel());
        leftMenuView.getConnectionsTabBtn().addActionListener(e -> showConnectionsTreePanel());
    }

    private void createBottomSidePanelWrap() {
        bottomCardLayout = new CardLayout();
        bottomSidePanelWrap = new JPanel();
        bottomSidePanelWrap.setLayout(bottomCardLayout);

        commandsTreePanel = new CommandsTreePanel();
        accountPasswordPanel = new AccountPasswordPanel();

        JPanel updateConnectionsJsonPanel = new JPanel();
        updateConnectionsJsonPanel.setLayout(new BorderLayout());
        bottomSidePanelWrap.add("commandsTreePanel", commandsTreePanel);
        bottomSidePanelWrap.add("updateConnectionsJsonPanel", updateConnectionsJsonPanel);
        bottomSidePanelWrap.add("updatePasswordPanel", accountPasswordPanel);

        updateConnectionsJsonTextArea = new MyJsonTextArea(3, 10);
        updateConnectionsJsonTextArea.setAfterFormatAction(() -> saveConnectionsInfoData());
        changeStyleViaThemeXml(updateConnectionsJsonTextArea);

        updateConnectionsJsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        updateConnectionsJsonTextArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(updateConnectionsJsonTextArea);

        sp.setVerticalScrollBarPolicy(RTextScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        updateConnectionsJsonTextArea.setLineWrap(false);
        updateConnectionsJsonTextArea.setEditable(true);

        updateConnectionsJsonTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateConnectionsJsonTextArea.getClass(), () -> {

                    saveConnectionsInfoData();
                });
            }
        });

        updateConnectionsJsonPanel.add(sp, BorderLayout.CENTER);

    }

    public void saveConnectionsInfoData() {
        FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
        changeConnectionsTree();
    }


    private String getAccountKey(TreeNode node) {
        return getAccountKey(node.toString());
    }

    private String getAccountKey(String nodeName) {
        return nodeName + "_account";
    }

    private String getAccountPwdKey(TreeNode node) {
        return node.toString();
    }

    private void createTopSidePanelWrap() {
        topCardLayout = new CardLayout();
        topSidePanelWrap = new JPanel();
        topSidePanelWrap.setLayout(topCardLayout);

        connectionsTreePanel = new ConnectionsTreePanel();

        JPanel updateCommandsJsonPanel = createUpdateCommandsJsonPanel();
        commandEditorPanel = new CommandEditorPanel();

        topSidePanelWrap.add("connectionsTreePanel", connectionsTreePanel);
        topSidePanelWrap.add("updateCommandsJsonPanel", updateCommandsJsonPanel);
        topSidePanelWrap.add(UPDATE_COMMAND, commandEditorPanel);
    }

    private void changeStyleViaThemeXml(MyJsonTextArea textArea) {
        boolean isDark = DrawUtil.isUnderDarcula();

        try {
            String themeStr = isDark ? "dark.xml" : "idea.xml";
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/" + themeStr));
            theme.apply(textArea);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }
    }


    private JPanel createUpdateCommandsJsonPanel() {
        JPanel updateCommandsJsonPanel = new JPanel();
        updateCommandsJsonPanel.setLayout(new BorderLayout());

        updateCommandsJsonTextArea = new MyJsonTextArea(3, 10);
        changeStyleViaThemeXml(updateCommandsJsonTextArea);
        updateCommandsJsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        updateCommandsJsonTextArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(updateCommandsJsonTextArea);
        updateCommandsJsonTextArea.setLineWrap(false);
        updateCommandsJsonTextArea.setEditable(true);
        updateCommandsJsonTextArea.setAfterFormatAction(() -> saveCommandsData());

        updateCommandsJsonTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateCommandsJsonTextArea.getClass(), () -> {
                    saveCommandsData();

                });
            }
        });

        updateCommandsJsonPanel.add(sp, BorderLayout.CENTER);
        return updateCommandsJsonPanel;
    }

    public void saveCommandsData() {
        fileStorage.saveCommandsData(updateCommandsJsonTextArea.getText());
        changeCommandsTree();
    }


    public void saveCommand() {

        SwingUtilities.invokeLater(() -> {
            commandEditorPanel.getCurrentEditCommand().setCommand(commandEditorPanel.getUpdateCommandTextArea().getText());
            commandEditorPanel.getCurrentEditCommand().setName(commandEditorPanel.getCommandNameTextField().getText());

            Object userDataObject = ((DefaultMutableTreeNode) commandsTreePanel.getCommandsTreeView().getModel().getRoot()).getUserObject();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String commandsJson = gson.toJson(userDataObject);
            fileStorage.saveCommandsData(commandsJson);

            reloadData();
        });

    }


    public void sendCommand(JTree treeRoot) {
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


    public void changePasswordToConnectGroupLabel(DefaultMutableTreeNode node) {
        accountPasswordPanel.changePasswordToConnectGroupLabel(node);
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

    public AccountDto getConnectionAccount(DefaultMutableTreeNode note) {
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

    private void changeConnectionsTree() {
        connectionsTreePanel.changeConnectionsTree();

    }

    private void changeCommandsTree() {
        commandsTreePanel.changeCommandsTree();
    }

    private Map<String, Object> getConnectionsPasswordsMap() {
        String text = fileStorage.getConnectionsPasswords();
        if (text == null) {
            return new HashMap<>(6);
        }
        Map<String, Object> map = new Gson().fromJson(text, new TypeToken<HashMap<String, Object>>() {
        }.getType());
        if (map == null) {
            return new HashMap<>(6);
        }
        return map;
    }

    public AccountDto getConnectionAccountByNodeName(String nodeName) {
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
        dto.setName((String) map.get(getAccountKey(nodeName)));
        if (dto.getName() != null) {
            dto.setName(dto.getName().replace("\\\\", "\\"));
        }
        return dto;
    }

    public void saveConnectionPassword() {

        TreeNode node =
                (DefaultMutableTreeNode) connectionsTreePanel.getConnectionsInfoTreeView().getLastSelectedPathComponent();

        if (node == null) {
            Object rootNodeObj = connectionsTreePanel.getConnectionsInfoTreeView().getModel().getRoot();
            if (rootNodeObj instanceof TreeNode) {
                node = (TreeNode) rootNodeObj;
            }
        }
        if (node == null) {
            return;
        }

        if (node.isLeaf()) {
            node = node.getParent();
        }

        String password = String.valueOf(accountPasswordPanel.getPasswordField().getPassword());

        String pass = null;

        if (!StringUtils.isBlank(password)) {
            try {
                pass = AesUtil.encrypt(password, aesKey);
            } catch (Exception e1) {
                LOGGER.error("saveConnectionPassword error", e1);
                throw new AesException(e1);
            }
        }

        Map<String, Object> hashMap = getConnectionsPasswordsMap();
        hashMap.put(getAccountPwdKey(node), pass);
        hashMap.put(getAccountKey(node), accountPasswordPanel.getAccountField().getText());

        FileStorage.INSTANCE.saveConnectionPassword(hashMap);
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public void showUpdateCommandsJsonPanel() {
        topCardLayout.show(topSidePanelWrap, "updateCommandsJsonPanel");
    }

    public void showUpdateCommandPanel() {
        topCardLayout.show(topSidePanelWrap, UPDATE_COMMAND);
    }

    public void resetUpdateCommandView(CommandDto obj) {
        commandEditorPanel.resetUpdateCommandView(obj);

    }

    private void showConnectionsTreePanel() {
        topCardLayout.show(topSidePanelWrap, "connectionsTreePanel");
    }

    public void showUpdatePasswordPanel() {
        bottomCardLayout.show(bottomSidePanelWrap, "updatePasswordPanel");
    }

    public void showUpdateConnectionsJsonPanel() {
        bottomCardLayout.show(bottomSidePanelWrap, "updateConnectionsJsonPanel");
    }

    private void showCommandsTreePanel() {
        bottomCardLayout.show(bottomSidePanelWrap, "commandsTreePanel");
    }

    public void runWithSelectedText(String selectedText) {
        SwingUtilities.invokeLater(() ->
                SpringRemoteView.getInstance().onTypedString(selectedText)
        );

    }

    public JTree getCommandsTreeView() {
        return commandsTreePanel.getCommandsTreeView();
    }

    public JTree getConnectionsInfoTreeView() {
        return connectionsTreePanel.getConnectionsInfoTreeView();
    }
}

