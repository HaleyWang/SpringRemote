package com.haleywang.putty.view;

import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.dto.EventDto;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.tab.close.DnDCloseButtonTabbedPane;
import com.mindbright.terminal.DisplayView;
import com.mindbright.terminal.DisplayWindow;
import org.alvin.puttydemo.PuttyPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author haley wang
 */
public class SpringRemoteView extends JFrame implements MyWindowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);
    private DnDCloseButtonTabbedPane currentTabPanel;
    private JSplitPane mainSplitPane;
    private String userName;
    private JLabel notificationLabel;
    private JLabel eventLogLabel;

    public static SpringRemoteView getInstance() {
        return SpringRemoteView.SingletonHolder.sInstance;
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



    private static class SingletonHolder {
        private static final SpringRemoteView sInstance = new SpringRemoteView();
    }

    private int orientation;
    private int termCount = 2;
    private static final int MAX_TERM_COUNT = 4;
    //DnDCloseButtonTabbedPane
    private List<DnDCloseButtonTabbedPane> tabPanels = new ArrayList<>();
    private JPanel mainPanel;

    void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    private void changeTermIndex(int natureIndex) {

        termCount = natureIndex;
        if (natureIndex == 3) {
            termCount = 2;
            setOrientation(JSplitPane.VERTICAL_SPLIT);

        } else if (natureIndex == 2) {
            termCount = 2;
            setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        }

        if (termCount > MAX_TERM_COUNT) {
            this.termCount = MAX_TERM_COUNT;
        } else if (termCount <= 0) {
            this.termCount = 1;
        }

        changeLayout();
    }

    /**
     * Creates new SpringRemoteView
     */
    private SpringRemoteView() {

        setSize(880, 680);
        setVisible(true);
        this.setTitle("SpringRemote");

        mainPanel = new JPanel();
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
        manager.addKeyEventDispatcher((e) ->{
            AWTKeyStroke ak = AWTKeyStroke.getAWTKeyStrokeForEvent(e);

            if (ak.equals(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))) {

                ActionsDialog actionsDialog = new ActionsDialog(SpringRemoteView.this);
                actionsDialog.setVisible(true);

            }
            return false;
        });
    }



    private void initMenu() {
        mainPanel.add(MenuView.getInstance(), BorderLayout.NORTH);
    }

    private DnDCloseButtonTabbedPane createTabPanel() {
        DnDCloseButtonTabbedPane tabPanel = new DnDCloseButtonTabbedPane(tab -> {
            if (tab instanceof PuttyPane) {
                ((PuttyPane) tab).close();
            }
        });
        tabPanel.setFocusable(true);
        tabPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                LOGGER.info("click tab panel");
                currentTabPanel = findTabPanel(e.getComponent());
                activeTabPanel();
            }
        });

        return tabPanel;
    }

    private void createAndAddPuttyPane(JTabbedPane tab, ConnectionDto connectionDto, AccountDto connectionAccount) {
        String port = StringUtils.ifBlank(connectionDto.getPort(), "22");

        String connectionPassword = null;
        if (connectionDto.getUser() == null || Objects.equals(connectionDto.getUser(), connectionAccount.getName())) {
            connectionPassword = connectionAccount.getPassword();
        }
        String connectionUser = connectionDto.getUser() != null ? connectionDto.getUser() : connectionAccount.getName();

        PuttyPane putty = new PuttyPane(connectionDto.getHost(), connectionUser, port, connectionPassword);


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
                o.getParent().setBackground(Color.LIGHT_GRAY);

            }
        });

        getCurrentTabPanel().getParent().setBackground(Color.GRAY);

    }

    void changeLayout() {

        for (int i = tabPanels.size(); i < termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        List<MyPanel> termMyPanels = new ArrayList<>();
        for (int i = 0; i < termCount; i++) {

            MyPanel termMyPanel = new MyPanel();
            termMyPanel.setLayout(new BorderLayout());
            termMyPanel.add(tabPanels.get(i), BorderLayout.CENTER);
            termMyPanels.add(termMyPanel);
        }

        for (int i = 0; i < termCount; i++) {

            if (termCount == 1) {
                mainSplitPane.setRightComponent(termMyPanels.get(i));
            }

            if (termCount == 2) {

                JSplitPane termSplitPane = new JSplitPane();
                termSplitPane.setOrientation(orientation);

                termSplitPane.setLeftComponent(termMyPanels.get(0));
                termSplitPane.setRightComponent(termMyPanels.get(1));
                termSplitPane.setResizeWeight(.5d);
                termSplitPane.setDividerSize(8);
                termSplitPane.setContinuousLayout(true);
                mainSplitPane.setRightComponent(termSplitPane);
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
                termSplitPane.setDividerSize(8);
                termSplitPane.setContinuousLayout(true);
                mainSplitPane.setRightComponent(termSplitPane);
            }
        }
        currentTabPanel = tabPanels.get(0);
        activeTabPanel();
    }

    void afterLogin(String userName, String key) {
        LOGGER.info("afterLogin");
        this.userName = userName;

        mainPanel.add(LeftMenuView.getInstance(), BorderLayout.WEST);

        mainSplitPane = new JSplitPane();
        mainSplitPane.setResizeWeight(.18d);

        SideView sidePanel = getSideView(key);
        mainSplitPane.setLeftComponent(sidePanel);
        //hide left component
        //mainSplitPane.setDividerLocation(0)
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        for (int i = 0; i < termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        JPanel notificationPanel = new JPanel(new BorderLayout());
        notificationLabel = new JLabel(" ");
        notificationLabel.setHorizontalTextPosition( JLabel.RIGHT);
        notificationPanel.add(notificationLabel, BorderLayout.CENTER);
        eventLogLabel = new JLabel(" Event Log ");
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

            MenuView.getInstance().setLayoutButtonsStatus()

        );

        NotificationsService.getInstance().info("Welcome back.");


    }

    public void fillNotificationLabel(EventDto eventDto) {
        this.notificationLabel.setText(" " + eventDto.getMessage() + " ");
    }

    private SideView getSideView(String key) {
        SideView sidePanel = SideView.getInstance();
        sidePanel.setAesKey(key);

        return sidePanel;
    }


    public void onTypedString(String command) {
        typedString(command);
    }

    void typedString(String command) {
        Component component = getCurrentTabPanel().getSelectedComponent();
        if (component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.typedString(command);
            puttyPane.setTermFocus();

        }
    }

    void onCreateConnectionsTab(ConnectionDto connectionDto, AccountDto connectionAccount) {
        createAndAddPuttyPane(getCurrentTabPanel(), connectionDto, connectionAccount);
    }

    public void changeCurrentTabPanel(int index) {
        if(tabPanels.size() <= index) {
            return;
        }
        currentTabPanel = tabPanels.get(index);
        activeTabPanel();
    }

    private JTabbedPane getCurrentTabPanel() {
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
        LOGGER.info("windowClosing");
        System.exit(0);
    }

    public String getUserName() {
        return userName;
    }
}
