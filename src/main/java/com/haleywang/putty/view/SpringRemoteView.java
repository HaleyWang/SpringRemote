package com.haleywang.putty.view;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.dto.EventDto;
import com.haleywang.putty.dto.RemoteSystemInfo;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.service.ActionExecuteService;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.util.UiTool;
import com.haleywang.putty.view.puttypanel.IdeaPuttyPanel;
import com.haleywang.putty.view.side.SideView;
import com.intellij.util.ArrayUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jediterm.terminal.TerminalDisplay;
import com.jediterm.terminal.ui.TerminalPanel;
import com.mindbright.terminal.DisplayView;
import com.mindbright.terminal.DisplayWindow;
import org.alvin.puttydemo.PuttyPane;
import org.alvin.puttydemo.PuttyPaneImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unknown.tab.close.DnDCloseButtonTabbedPane;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * @author haley wang
 */
public class SpringRemoteView extends JFrame implements MyWindowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);
    private static final int DIVIDER_SIZE = 8;
    private static final String FLAT_DARCULA_LAF = "FlatDarculaLaf";
    public static final int VERTICAL_SPLIT_NATURE_INDEX = 3;
    public static final int HORIZONTAL_SPLIT_NATURE_INDEX = 2;
    private static final long serialVersionUID = -3296737234823450798L;
    private DnDCloseButtonTabbedPane currentTabPanel;
    private JSplitPane mainSplitPane;
    private String userName;
    private JLabel notificationLabel;
    private final boolean useNewTerminal = true;

    public static SpringRemoteView getInstance() {
        return SpringRemoteView.SingletonHolder.S_INSTANCE;
    }

    public void changeAndSaveTermIndex(String layoutAction) {

        int natureIndex = ActionsData.getIndex(ActionsData.getLayoutActionsData(), layoutAction) + 1;

        SwingUtilities.invokeLater(() -> {
            SettingDto settingDto = FileStorage.INSTANCE.getSettingDto(getUserName());
            settingDto.setTabLayout(natureIndex);
            FileStorage.INSTANCE.saveSettingDto(getUserName(), settingDto);
        });

        changeTermIndex(natureIndex);
    }

    public void initCurrentCommandPath(SpringRemoteView omegaRemote) {
        SideView.getInstance().getCommandsTreePanel().initCurrentCommandPath(omegaRemote);
    }


    private static class SingletonHolder {
        private static final SpringRemoteView S_INSTANCE = new SpringRemoteView();
    }

    private int orientation;
    private int termCount = 2;
    private static final int MAX_TERM_COUNT = 4;

    private final List<DnDCloseButtonTabbedPane> tabPanels = new ArrayList<>();
    private final JPanel mainPanel;

    void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    private void changeTermIndex(int natureIndex) {

        termCount = natureIndex;
        if (natureIndex == VERTICAL_SPLIT_NATURE_INDEX) {
            termCount = Constants.TERM_TWO;
            setOrientation(JSplitPane.VERTICAL_SPLIT);

        } else if (natureIndex == HORIZONTAL_SPLIT_NATURE_INDEX) {
            termCount = Constants.TERM_TWO;
            setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        }

        if (termCount > MAX_TERM_COUNT) {
            this.termCount = MAX_TERM_COUNT;
        } else if (termCount <= 0) {
            this.termCount = 1;
        }

        changeLayout();
    }

    public static LookAndFeel getLookAndFeel() {
        String theme = FileStorage.INSTANCE.getTheme();
        if (FLAT_DARCULA_LAF.equalsIgnoreCase(theme)) {
            return new FlatDarculaLaf();
        }

        return new FlatIntelliJLaf();
    }

    public void changeTheme(String theme) {
        try {
            UIManager.setLookAndFeel(getLookAndFeel());
        } catch (Exception ex) {
            LOGGER.error("setLookAndFeel", ex);
        }
        try {
            FileStorage.INSTANCE.saveTheme(theme);
            SwingUtilities.updateComponentTreeUI(this);
            SideView.getInstance().changeTheme();
        } catch (Exception ex) {
            LOGGER.error("change_theme_error", ex);
        }
    }

    /**
     * Creates new SpringRemoteView
     */
    private SpringRemoteView() {

        SettingDto setting = FileStorage.INSTANCE.getSetting();

        setSize(setting.getFrameWidth(), setting.getFrameHeight());
        setVisible(true);
        this.setTitle("SpringRemote");

        mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        BorderLayout layout = new BorderLayout();
        layout.setHgap(0);
        layout.setVgap(0);
        mainPanel.setLayout(layout);
        setContentPane(mainPanel);
        LOGGER.info("SpringRemote start");

        LoginDialog loginDlg = new LoginDialog(this);
        loginDlg.setVisible(true);
        addWindowListener(this);
        initMenu();
        initGlobalKeyListener();
    }

    private void initGlobalKeyListener() {
        // Java Global JFrame Key Listener
        KeyboardFocusManager manager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(e -> {
            AWTKeyStroke ak = AWTKeyStroke.getAWTKeyStrokeForEvent(e);

            if (ak.equals(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))) {

                ActionsDialog actionsDialog = new ActionsDialog(SpringRemoteView.this, null);
                actionsDialog.setVisible(true);

            } else if (ak.equals(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))) {

                ActionsDialog actionsDialog = new ActionsDialog(SpringRemoteView.this, ActionCategoryEnum.SSH);
                actionsDialog.setVisible(true);
            }
            return false;
        });
    }

    private void initMenu() {
        mainPanel.add(MenuView.getInstance(), BorderLayout.NORTH);
    }

    private void tabPopupEvent(JPanel tabComp) {
        JLabel tabLb = getTabLabel(tabComp);
        JComponent target = tabLb == null ? tabComp : tabLb;
        int x = target.getX();
        int y = target.getY() + 12;

        String label = "Rename Session";
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(ev -> {

            LOGGER.info("===== click tree item event");
            new TerminalTabReNameDialog(this, tabComp).setVisible(true);

        });
        popup.add(item);
        popup.show(tabComp, x, y);
    }

    JLabel getTabLabel(JPanel tabNamePanel) {
        Component[] comps = tabNamePanel.getComponents();
        if (!ArrayUtil.isEmpty(comps)) {
            for (Component comp : comps) {
                if (comp instanceof JLabel) {
                    return ((JLabel) comp);

                }
            }
        }
        return null;
    }

    private DnDCloseButtonTabbedPane createTabPanel() {

        DnDCloseButtonTabbedPane.TabListener tabListener = tab -> {
            if (tab instanceof PuttyPane) {
                ((PuttyPane) tab).close();
            }
        };

        DnDCloseButtonTabbedPane tabPanel = new DnDCloseButtonTabbedPane(tabListener);

        tabPanel.setFocusable(true);
        tabPanel.addChangeListener(e -> {
            LOGGER.info("tab panel change ");
            try {
                showRemoteSystemInfo(false);
            } catch (Exception e1) {
                LOGGER.error("showRemoteSystemInfo errors", e1);
            }

        });
        tabPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                int tabIndex = tabPanel.indexAtLocation(e.getX(), e.getY());
                if (tabIndex != -1 && tabIndex < tabPanel.getTabCount()) {
                    JPanel tabComp = ((JPanel) tabPanel.getTabComponentAt(tabIndex));
                    if (SwingUtilities.isRightMouseButton(e)) {
                        tabPopupEvent(tabComp);
                        return;

                    } else if (e.getClickCount() == Constants.DOUBLE_CLICK_NUM) {
                        LOGGER.info("doubleClickTabEvent");
                    }
                }

                LOGGER.info("click tab panel");
                DnDCloseButtonTabbedPane nextTabPanel = findTabPanel(e.getComponent());
                currentTabPanel = nextTabPanel;
                activeTabPanel();
            }
        });

        return tabPanel;
    }


    private void createAndAddPuttyPane(JTabbedPane tab, ConnectionDto connectionDto, AccountDto connectionAccountIn) {
        String port = StringUtils.ifBlank(connectionDto.getPort(), "22");
        AccountDto connectionAccount = Optional.ofNullable(connectionAccountIn).orElse(new AccountDto());

        String connectionPassword = null;
        String connectionUser = null;
        if (!StringUtils.isBlank(connectionDto.getHost())) {
            if (connectionDto.getUser() == null || Objects.equals(connectionDto.getUser(), connectionAccount.getName())) {
                connectionPassword = connectionAccount.getPassword();
            }
            connectionUser = connectionDto.getUser() != null ? connectionDto.getUser() : connectionAccount.getName();
        }

        String pem = StringUtils.ifBlank(connectionDto.getPem(), connectionAccount.getPem());
        IdeaPuttyPanel putty = new IdeaPuttyPanel(connectionDto.getHost(), connectionUser, port, connectionPassword, pem);

        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount() - 1);

        SwingUtilities.invokeLater(() -> {
            putty.init();
            TerminalDisplay displayObj = putty.getSession().getTerminalDisplay();
            if (displayObj instanceof TerminalPanel) {
                TerminalPanel displayWindow = (TerminalPanel) displayObj;
                displayWindow.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        LOGGER.info("click displayWindow");

                        currentTabPanel = findTabPanel(e.getComponent());
                        activeTabPanel();

                    }
                });
            }
        });

    }

    private void createAndAddPuttyPaneOld(JTabbedPane tab, ConnectionDto connectionDto, AccountDto connectionAccount) {
        String port = StringUtils.ifBlank(connectionDto.getPort(), "22");

        String connectionPassword = null;
        if (connectionDto.getUser() == null || Objects.equals(connectionDto.getUser(), connectionAccount.getName())) {
            connectionPassword = connectionAccount.getPassword();
        }
        String connectionUser = connectionDto.getUser() != null ? connectionDto.getUser() : connectionAccount.getName();

        PuttyPaneImpl putty = new PuttyPaneImpl(connectionDto.getHost(), connectionUser, port, connectionPassword);


        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount() - 1);

        SwingUtilities.invokeLater(() -> {
            putty.init();
            DisplayView displayObj = putty.getTerm().getDisplay();
            if (displayObj instanceof DisplayWindow) {
                DisplayWindow displayWindow = (DisplayWindow) displayObj;
                displayWindow.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        LOGGER.info("click displayWindow");

                        currentTabPanel = findTabPanel(e.getComponent());
                        activeTabPanel();

                    }
                });
            }
        });

    }


    private DnDCloseButtonTabbedPane findTabPanel(Component c) {
        if (c == null) {
            return null;
        }
        if (c instanceof DnDCloseButtonTabbedPane) {
            return (DnDCloseButtonTabbedPane) c;
        }
        int loop = 20;
        Container parent = c.getParent();
        for (int i = 0; i < loop && parent != null; i++) {

            if (parent instanceof DnDCloseButtonTabbedPane) {
                return (DnDCloseButtonTabbedPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void activeTabPanel() {

        tabPanels.forEach(o -> {
            if (o != null && o.getParent() != null) {
                changePanelBorder(o.getParent(), BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            }
        });

        Container parent = getCurrentTabPanel().getParent();
        changePanelBorder(parent, BorderFactory.createLineBorder(UiTool.toColorFromString("4db8f8"), 2));

    }

    private void changePanelBorder(Container parent, Border b) {
        if (parent instanceof JPanel) {
            JPanel panel = (JPanel) parent;
            panel.setBorder(b);
        }
    }

    void changeLayout() {


        for (int i = tabPanels.size(); i < termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        List<JPanel> termMyPanels = new ArrayList<>();
        for (int i = 0; i < termCount; i++) {

            JPanel termMyPanel = new JPanel();
            termMyPanel.setLayout(new BorderLayout());
            termMyPanel.add(tabPanels.get(i), BorderLayout.CENTER);
            termMyPanels.add(termMyPanel);
        }

        JPanel rightComponent = new JPanel(new BorderLayout());
        rightComponent.setBorder(new EmptyBorder(2, 2, 2, 0));
        mainSplitPane.setRightComponent(rightComponent);
        for (int i = 0; i < termCount; i++) {

            if (termCount == 1) {
                rightComponent.add(termMyPanels.get(i));
            }

            if (termCount == 2) {

                JSplitPane termSplitPane = new JSplitPane();
                termSplitPane.setOrientation(orientation);

                termSplitPane.setLeftComponent(termMyPanels.get(0));
                termSplitPane.setRightComponent(termMyPanels.get(1));
                termSplitPane.setResizeWeight(.5d);
                termSplitPane.setDividerSize(DIVIDER_SIZE + 2);

                termSplitPane.setContinuousLayout(true);
                rightComponent.add(termSplitPane);
            }

            if (termCount == 4) {

                JSplitPane termSplitPane = new JSplitPane();

                termSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

                JSplitPane subTermSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                subTermSplitPane1.setResizeWeight(.5d);

                JSplitPane subTermSplitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                subTermSplitPane2.setResizeWeight(.5d);

                subTermSplitPane1.setLeftComponent(termMyPanels.get(0));
                subTermSplitPane1.setRightComponent(termMyPanels.get(1));
                subTermSplitPane2.setLeftComponent(termMyPanels.get(2));
                subTermSplitPane2.setRightComponent(termMyPanels.get(3));

                termSplitPane.setLeftComponent(subTermSplitPane1);
                termSplitPane.setRightComponent(subTermSplitPane2);
                termSplitPane.setResizeWeight(.5d);
                termSplitPane.setDividerSize(DIVIDER_SIZE);
                termSplitPane.setContinuousLayout(true);
                rightComponent.add(termSplitPane);
            }
        }

        currentTabPanel = tabPanels.get(0);
        activeTabPanel();
        MenuView.getInstance().changeLayoutButtonsStatus(termCount, orientation);

    }

    void afterLogin(String userName, String key) {
        LOGGER.info("afterLogin");
        this.userName = userName;

        mainPanel.add(LeftMenuView.getInstance(), BorderLayout.WEST);

        mainSplitPane = new JSplitPane();
        mainSplitPane.setResizeWeight(.18d);

        SideView sidePanel = getSideView(key);
        mainSplitPane.setLeftComponent(sidePanel);

        mainSplitPane.setDividerSize(6);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        for (int i = 0; i < termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        JPanel notificationPanel = new JPanel(new BorderLayout());
        notificationLabel = new JLabel(" ");
        notificationLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        notificationPanel.add(notificationLabel, BorderLayout.CENTER);
        JLabel eventLogLabel = new JLabel(" Event Log ");
        eventLogLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                new EventsDialog(SpringRemoteView.this).setVisible(true);
            }
        });
        notificationPanel.add(eventLogLabel, BorderLayout.EAST);
        mainPanel.add(notificationPanel, BorderLayout.SOUTH);
        mainPanel.revalidate();


        SwingUtilities.invokeLater(() ->
                {
                    MenuView.getInstance().setLayoutButtonsStatus();

                    ConnectionDto localTerm = new ConnectionDto();
                    localTerm.setName("my term");
                    ActionExecuteService.getInstance().execute(localTerm);
                }
        );


        NotificationsService.getInstance().info("Welcome back.");


    }

    public void fillNotificationLabel(EventDto eventDto) {
        this.notificationLabel.setText(" " + eventDto.getMessage() + " ");
    }

    private SideView getSideView(String key) {
        SideView sidePanel = SideView.getInstance();
        sidePanel.setAesKey(key);
        sidePanel.setDividerSize(6);
        return sidePanel;
    }


    public void onTypedString(final String command) {
        if (command.trim().contains("\n")) {
            new RunCmdDialog("Run command dialog", "Are you sure you want to run the multiline commands?") {
                private static final long serialVersionUID = 1725469319855455739L;

                @Override
                public void ok() {
                    typedString(command);
                }
            }.setVisible(true);
            return;
        }
        typedString(command);
    }


    static abstract class RunCmdDialog extends JDialog implements ActionListener {
        private static final long serialVersionUID = -4150879304928630217L;
        String content;
        String ok = "OK";
        String cancel = "CANCEL";

        public RunCmdDialog(String title, String content) {
            this.content = content;

            JLabel jLabel = new JLabel("<html><body>" + content + "</body></html>");
            jLabel.setFont(new Font("", Font.PLAIN, 14));
            jLabel.setForeground(Color.black);
            jLabel.setBounds(75, 43, 220, 85);
            JButton okBut = new JButton(ok);
            JButton cancelBut = new JButton(cancel);
            okBut.setBackground(Color.LIGHT_GRAY);
            okBut.setBorderPainted(false);
            okBut.setBounds(65, 126, 98, 31);
            cancelBut.setBounds(175, 126, 98, 31);
            cancelBut.setBackground(Color.LIGHT_GRAY);
            cancelBut.setBorderPainted(false);
            okBut.addActionListener(this);
            cancelBut.addActionListener(this);

            add(jLabel);
            add(okBut);
            add(cancelBut);
            setLayout(null);
            setTitle(title);
            setModal(true);
            setSize(350, 210);
            setLocationRelativeTo(null);
            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }

        public abstract void ok();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (ok.equals(e.getActionCommand())) {
                this.setVisible(false);
                ok();
            }
            if (cancel.equals(e.getActionCommand())) {
                this.setVisible(false);
                this.dispose();
            }
        }
    }

    public void focusCurrentTerm() {
        Component component = getCurrentTabPanel().getSelectedComponent();
        if (component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.setTermFocus();
        }
    }

    void typedString(String command) {
        Component component = getCurrentTabPanel().getSelectedComponent();
        if (component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.typedString(command);
            puttyPane.setTermFocus();

        }
    }

    public void showRemoteSystemInfo(boolean reload) {

        Component component = getCurrentTabPanel().getSelectedComponent();
        if (component instanceof IdeaPuttyPanel) {
            IdeaPuttyPanel puttyPane = (IdeaPuttyPanel) component;
            if (puttyPane.isLocal() || !puttyPane.isConnected()) {
                return;
            }
            try {
                RemoteSystemInfo info = puttyPane.getRemoteSystemInfo(reload);
                String diskUsageString = info.getDiskUsageString();
                LOGGER.info("showRemoteSystemInfo:{}", diskUsageString);
            } catch (IOException | JSchException e) {
                LOGGER.error("showRemoteSystemInfoError", e);
            }
        }

    }

    ChannelSftp openSftpChannel() throws JSchException {

        Component component = getCurrentTabPanel().getSelectedComponent();
        if (component instanceof IdeaPuttyPanel) {
            IdeaPuttyPanel puttyPane = (IdeaPuttyPanel) component;
            return puttyPane.openSftpChannel();

        } else {
            throw new NullPointerException();
        }

    }

    public void onCreateConnectionsTab(ConnectionDto connectionDto, AccountDto connectionAccount) {
        if (useNewTerminal) {
            createAndAddPuttyPane(getCurrentTabPanel(), connectionDto, connectionAccount);

        } else {
            createAndAddPuttyPaneOld(getCurrentTabPanel(), connectionDto, connectionAccount);
        }
    }

    public void changeCurrentTabPanel(int index) {
        if (tabPanels.size() <= index) {
            return;
        }
        currentTabPanel = tabPanels.get(index);
        activeTabPanel();
    }

    public JTabbedPane getCurrentTabPanel() {
        if (currentTabPanel == null) {
            currentTabPanel = tabPanels.get(0);
        }

        boolean inUsedList = false;
        for (int i = 0; i < termCount; i++) {
            DnDCloseButtonTabbedPane tp = tabPanels.get(i);
            if (currentTabPanel == tp) {
                inUsedList = true;
                break;
            }
        }
        if (!inUsedList) {
            currentTabPanel = tabPanels.get(0);
        }
        return currentTabPanel;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        try {
            saveFrameSetting();
        } catch (Exception ex) {
            LOGGER.info("save_setting_when_close", ex);
        }

        LOGGER.info("window_closing");
        System.exit(0);
    }

    private void saveFrameSetting() {
        Dimension frameSize = this.getSize();
        SettingDto setting = FileStorage.INSTANCE.getSetting();

        setting.setFrameHeight((int) frameSize.getHeight());
        setting.setFrameWidth((int) frameSize.getWidth());
        FileStorage.INSTANCE.saveSetting(setting);
    }

    public String getUserName() {
        return userName;
    }
}
