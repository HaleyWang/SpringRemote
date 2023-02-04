package com.haleywang.putty.view.side;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.haleywang.putty.common.AesException;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AesUtil;
import com.haleywang.putty.util.CmdUtils;
import com.haleywang.putty.util.Debouncer;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
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
    private static final String CMD = "cmd>";
    private static final String TERM = "term>";
    private static final long serialVersionUID = -9204510196631762177L;

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

    RTextScrollPane updateCommandsJsonTextAreaScroll;

    private String aesKey;

    private static final FileStorage FILE_STORAGE = FileStorage.INSTANCE;

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

        String commandsData = FILE_STORAGE.getCommandsData();
        if (commandsData == null) {
            try {
                commandsData = IoTool.read(this.getClass(), "/myCommandsExample.json");
            } catch (Exception e) {
                LOGGER.error("reload CommandsJson error", e);
            }
        }

        updateCommandsJsonTextArea.setText(commandsData);

        String connectionsInfoData = FILE_STORAGE.getConnectionsInfoData();
        if (connectionsInfoData == null) {
            try {
                connectionsInfoData = IoTool.read(this.getClass(), "/myConnectionsInfoExample.json");
            } catch (Exception e) {
                LOGGER.error("reload ConnectionsJson error", e);
            }
        }
        updateConnectionsJsonTextArea.setText(connectionsInfoData);

        commandsTreePanel.reloadData();
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
        updateConnectionsJsonTextArea.setAfterFormatAction(this::saveConnectionsInfoData);
        changeStyleViaThemeXml(updateConnectionsJsonTextArea);

        updateConnectionsJsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        updateConnectionsJsonTextArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(updateConnectionsJsonTextArea);

        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        updateConnectionsJsonTextArea.setLineWrap(false);
        updateConnectionsJsonTextArea.setEditable(true);

        updateConnectionsJsonTextArea.addKeyListener(new KeyAdapter() {
            private void run() {
                saveConnectionsInfoDataAndChangeConnectionsTree();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateConnectionsJsonTextArea.getClass(), this::run);
            }
        });

        updateConnectionsJsonPanel.add(sp, BorderLayout.CENTER);

    }

    public void saveConnectionsInfoDataAndChangeConnectionsTree() {
        FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
        changeConnectionsTree();
    }

    public void saveConnectionsInfoData() {
        FileStorage.INSTANCE.saveConnectionsInfoData(updateConnectionsJsonTextArea.getText());
    }

    private String getAccountKey(String nodeName) {
        return nodeName + "_account";
    }

    private String getAccountPwdKey(String nodeName) {
        return nodeName;
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
            LOGGER.error("changeStyleViaThemeXml error", ioe);
        }
    }


    private JPanel createUpdateCommandsJsonPanel() {
        JPanel updateCommandsJsonPanel = new JPanel();
        updateCommandsJsonPanel.setLayout(new BorderLayout());

        updateCommandsJsonTextArea = new MyJsonTextArea(3, 10);
        changeStyleViaThemeXml(updateCommandsJsonTextArea);
        updateCommandsJsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        updateCommandsJsonTextArea.setCodeFoldingEnabled(true);
        updateCommandsJsonTextAreaScroll = new RTextScrollPane(updateCommandsJsonTextArea);
        updateCommandsJsonTextArea.setLineWrap(false);
        updateCommandsJsonTextArea.setEditable(true);
        updateCommandsJsonTextArea.setAfterFormatAction(this::saveCommandsData);

        updateCommandsJsonTextArea.addKeyListener(new KeyAdapter() {
            private void run() {
                saveCommandsDataAndChangeCommandsTree();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                debouncer.debounce(updateCommandsJsonTextArea.getClass(), this::run);
            }
        });

        updateCommandsJsonPanel.add(updateCommandsJsonTextAreaScroll, BorderLayout.CENTER);
        return updateCommandsJsonPanel;
    }

    public void saveCommandsDataAndChangeCommandsTree() {
        saveCommandsData();
        changeCommandsTree();
    }

    public void saveCommandsData() {
        FILE_STORAGE.saveCommandsData(updateCommandsJsonTextArea.getText(), commandsTreePanel.getCurrentPathWithLeafIndex());
    }


    public void saveCommand() {

        SwingUtilities.invokeLater(() -> {
            String command = commandEditorPanel.getUpdateCommandTextArea().getText();
            String commandName = commandEditorPanel.getCommandNameTextField().getText();

            boolean isTmp = false;
            String jsonPath = commandEditorPanel.getPathLb().getText();
            String tmpPath = "Commands root/_tmp/@0";
            System.out.println(" ======= jsonPath === " + jsonPath);
            if (StringUtils.isBlank(jsonPath) || tmpPath.equals(jsonPath)) {
                commandEditorPanel.getPathLb().setText(tmpPath);
                isTmp = true;
            } else {
                commandEditorPanel.syncCommandsTree();
            }

            Object userDataObject = ((DefaultMutableTreeNode) commandsTreePanel.getCommandsTreeView().getModel().getRoot()).getUserObject();

            if (isTmp && userDataObject instanceof CommandDto) {
                CommandDto rootCommandDto = (CommandDto) userDataObject;

                CommandDto tmpGroup = rootCommandDto.getChildren().stream().filter(c -> "_tmp".equals(c.getName()))
                        .findFirst().orElseGet(() -> {
                            CommandDto tmpGroup1 = new CommandDto();
                            tmpGroup1.setName("_tmp");
                            tmpGroup1.addChild(new CommandDto());
                            rootCommandDto.addChild(tmpGroup1);
                            return tmpGroup1;
                        });

                CommandDto tmpCmd = tmpGroup.getChildren().stream().findFirst().orElseGet(() -> {
                    CommandDto cmd = new CommandDto();
                    tmpGroup.addChild(cmd);
                    return cmd;
                });

                tmpCmd.setName(commandName);
                tmpCmd.setCommand(command);


            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String commandsJson = gson.toJson(userDataObject);
            FILE_STORAGE.saveCommandsData(commandsJson, commandsTreePanel.getCurrentPathWithLeafIndex());

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
            if (commandDto.getCommand().startsWith(CMD) || commandDto.getCommand().startsWith(TERM)) {
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
            SpringRemoteView.getInstance().onCreateConnectionsTab(connectionDto, null);
        } else {
            AccountDto connectionAccount = getConnectionAccount(nodes.get(0));
            SpringRemoteView.getInstance().onCreateConnectionsTab(connectionDto, connectionAccount);
        }
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

    public void changeCommandsTree() {
        commandsTreePanel.changeCommandsTree();
    }

    private Map<String, Object> getConnectionsPasswordsMap() {
        String text = FILE_STORAGE.getConnectionsPasswords();
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
        Map<String, Object> map = getConnectionsPasswordsMap();
        String accountKey =  getAccountKey(nodeName);
        String pwdKey = getAccountPwdKey(nodeName);

        AccountDto dto = new AccountDto();
        String pass = (String) map.get(pwdKey);
        if(!StringUtils.isBlank(pass)) {
            try {
                dto.setPassword(AesUtil.decrypt(pass, aesKey));
            } catch (Exception e) {
                LOGGER.error("decrypt error", e);
            }
        }

        String name = (String) map.getOrDefault(accountKey, "");
        if (!StringUtils.isBlank(name)) {
            dto.setName(name.replace("\\\\", "\\"));
        } else {
            return null;
        }
        return dto;
    }

    public void saveConnectionPassword() {

        String nodeName = accountPasswordPanel.getNodeName();

        String password = String.valueOf(accountPasswordPanel.getPasswordField().getPassword());

        String pass = null;

        if (!StringUtils.isBlank(password)) {
            try {
                pass = AesUtil.encrypt(password, aesKey);
            } catch (Exception e1) {
                throw new AesException("saveConnectionPassword error", e1);
            }
        }

        Map<String, Object> hashMap = getConnectionsPasswordsMap();
        hashMap.put(getAccountPwdKey(nodeName), pass);
        String name = accountPasswordPanel.getAccountField().getText();
        hashMap.put(getAccountKey(nodeName), name);

        FileStorage.INSTANCE.saveConnectionPassword(hashMap);
        NotificationsService.getInstance().info("Save account: " + name);
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public void showUpdateCommandsJsonPanel() {
        topCardLayout.show(topSidePanelWrap, "updateCommandsJsonPanel");
    }
    public void showUpdateCommandsJsonPanel(DefaultMutableTreeNode node) {
        showUpdateCommandsJsonPanel(node, null);
    }

    public void showUpdateCommandsJsonPanel(DefaultMutableTreeNode node, String json) {
        LeftMenuView.getInstance().getCommandsJsonTabBtn().doClick();

        if(!StringUtils.isBlank(json)) {
            updateCommandsJsonTextArea.setText(JsonUtils.getFormatJsonString(json));
            saveCommandsData();
        }

        try {
            CommandDto obj = (CommandDto) node.getUserObject();

            String parentText = node.getParent().toString();
            String name = obj.getName();
            String cmd = obj.getCommand();

            String text = updateCommandsJsonTextArea.getText();

            int parentIndex = text.indexOf(parentText);
            int nameIndex = name == null ? 0 : text.indexOf(name);
            int cmdIndex = cmd == null ? 0 : text.indexOf(cmd);

            int index = Math.max(parentIndex, nameIndex);
            index = Math.max(index, cmdIndex);
            updateCommandsJsonTextArea.setCaretPosition(index);
        } catch (Exception e) {
            LOGGER.error("showUpdateCommandsJsonPanel error", e);
        }

    }

    public void showUpdateCommandPanel() {
        topCardLayout.show(topSidePanelWrap, UPDATE_COMMAND);
    }


    public void resetUpdateCommandView(TreePath treePath, int index) {

        getCommandsTreePanel().setCurrentPathWithLeafIndex(treePath, index);
        CommandDto obj = getCommandsTreePanel().getCurrentCommandDto();
        if (obj == null) {
            return;
        }

        commandEditorPanel.resetUpdateCommandView(obj, getCommandsTreePanel().getCurrentPathWithLeafIndex());
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
    public void showUpdateConnectionsJsonPanel(DefaultMutableTreeNode node) {
        showUpdateConnectionsJsonPanel(node, null);
    }
    public void showUpdateConnectionsJsonPanel(DefaultMutableTreeNode node, String text) {
        LeftMenuView.getInstance().getConnectionsJsonTabBtn().doClick();
        if(!StringUtils.isBlank(text)) {
            updateConnectionsJsonTextArea.setText(JsonUtils.getFormatJsonString(text));
            saveConnectionsInfoData();
        }

        try {
            ConnectionDto obj = (ConnectionDto) node.getUserObject();

            String parentText = node.getParent().toString();
            String name = obj.toString();

            String currentText = updateConnectionsJsonTextArea.getText();

            int parentIndex = currentText.indexOf(parentText);
            int nameIndex = currentText.indexOf(name);

            int index = Math.max(parentIndex, nameIndex);
            updateConnectionsJsonTextArea.setCaretPosition(index);
        }catch (Exception e) {
            LOGGER.error("showUpdateConnectionsJsonPanel error", e);
        }

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

    public CommandEditorPanel getCommandEditorPanel() {
        return commandEditorPanel;
    }

    public CommandsTreePanel getCommandsTreePanel() {
        return commandsTreePanel;
    }

    public JTree getConnectionsInfoTreeView() {
        return connectionsTreePanel.getConnectionsInfoTreeView();
    }
}

