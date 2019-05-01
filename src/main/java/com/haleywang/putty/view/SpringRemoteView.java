package com.haleywang.putty.view;

import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.util.StringUtils;
import com.mindbright.terminal.DisplayView;
import com.mindbright.terminal.DisplayWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import other.JTabbedPaneCloseButton;
import puttydemo.PuttyPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author haley wang
 */
public class SpringRemoteView extends JFrame implements MyWindowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);
    private JTabbedPaneCloseButton currentTabPanel;
    private JSplitPane mainSplitPane;

    public static SpringRemoteView getInstance(){
        return SpringRemoteView.SingletonHolder.sInstance;
    }

    private static class SingletonHolder {
        private static final SpringRemoteView sInstance = new SpringRemoteView();
    }

    private int orientation;
    private int termCount = 2;
    private static  final int MAX_TERM_COUNT = 4;
    private List<JTabbedPaneCloseButton> tabPanels = new ArrayList<>();
    private JPanel mainPanel;

    void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    void setTermCount(int termCount) {
        if(termCount > MAX_TERM_COUNT) {
            this.termCount = MAX_TERM_COUNT;
        }else {
            this.termCount = termCount;
        }
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
    }

    private void initMenu() {
        mainPanel.add(MenuView.getInstance(), BorderLayout.NORTH);
    }

    private JTabbedPaneCloseButton createTabPanel() {
        JTabbedPaneCloseButton tabPanel = new JTabbedPaneCloseButton(tab -> {
            if(tab instanceof PuttyPane) {
                ((PuttyPane)tab).close();
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
        if(connectionDto.getUser() == null || Objects.equals(connectionDto.getUser(), connectionAccount.getName())) {
            connectionPassword = connectionAccount.getPassword();
        }
        String connectionUser = connectionDto.getUser() != null ? connectionDto.getUser() : connectionAccount.getName();

        PuttyPane putty = new PuttyPane(connectionDto.getHost(), connectionUser, port, connectionPassword);


        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount()-1);

        SwingUtilities.invokeLater(() -> {
            putty.init();
            DisplayView displayObj = putty.getTerm().getDisplay();
            if(displayObj instanceof DisplayWindow) {
                DisplayWindow displayWindow = (DisplayWindow)displayObj;
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

    private JTabbedPaneCloseButton findTabPanel(Component c) {
        if(c == null) {
            return null;
        }
        if(c instanceof JTabbedPaneCloseButton){
            return (JTabbedPaneCloseButton)c;
        }
        int loop = 20;
        Container parent = c.getParent();
        for (int i = 0; i < loop && parent != null; i++) {

            if(parent instanceof JTabbedPaneCloseButton) {
                return (JTabbedPaneCloseButton) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void activeTabPanel() {

        tabPanels.forEach(o -> {
            if(o != null && o.getParent() != null) {
                o.getParent().setBackground(Color.LIGHT_GRAY);

            }
        });

        getCurrentTabPanel().getParent().setBackground(Color.GRAY);

    }

    void changeLayout() {

        for(int i= tabPanels.size(); i< termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        List<MyPanel> termMyPanels = new ArrayList<>();
        for(int i= 0; i< termCount; i++) {

            MyPanel termMyPanel = new MyPanel();
            termMyPanel.setLayout(new BorderLayout());
            termMyPanel.add(tabPanels.get(i), BorderLayout.CENTER);
            termMyPanels.add(termMyPanel);
        }

        for(int i= 0; i< termCount; i++) {

            if(termCount == 1) {
                mainSplitPane.setRightComponent(termMyPanels.get(i));
            }

            if(termCount == 2) {

                JSplitPane termSplitPane = new JSplitPane();
                termSplitPane.setOrientation(orientation);

                termSplitPane.setLeftComponent(termMyPanels.get(0));
                termSplitPane.setRightComponent(termMyPanels.get(1));
                termSplitPane.setResizeWeight(.5d);
                termSplitPane.setDividerSize(8);
                termSplitPane.setContinuousLayout(true);
                mainSplitPane.setRightComponent(termSplitPane);
            }

            if(termCount == 4) {

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

    void afterLogin(String key) {
        LOGGER.info("afterLogin");

        mainPanel.add(LeftMenuView.getInstance(), BorderLayout.WEST);

        mainSplitPane = new JSplitPane();
        mainSplitPane.setResizeWeight(.18d);

        SideView sidePanel = getSideView(key);
        mainSplitPane.setLeftComponent(sidePanel);
        //hide left component
        //mainSplitPane.setDividerLocation(0)
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        for(int i= 0; i< termCount; i++) {
            tabPanels.add(createTabPanel());
        }

        changeLayout();

        mainPanel.revalidate();
    }

    private SideView getSideView(String key) {
        SideView sidePanel =  SideView.getInstance();
        sidePanel.setAesKey(key);

        return sidePanel;
    }


    void onTypedString(String command) {
        typedString(command);
    }

    void typedString(String command) {
        Component component = getCurrentTabPanel().getSelectedComponent();
        if(component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.typedString(command);
            puttyPane.setTermFocus();

        }
    }

    void onCreateConnectionsTab(ConnectionDto connectionDto, AccountDto connectionAccount) {
        createAndAddPuttyPane(getCurrentTabPanel(), connectionDto, connectionAccount);
    }

    private JTabbedPane getCurrentTabPanel() {
        if(currentTabPanel == null) {
            currentTabPanel = tabPanels.get(0);
        }

        boolean inUsedList = false;
        for(int i = 0; i< termCount; i++) {
            JTabbedPaneCloseButton tp = tabPanels.get(i);
            if(currentTabPanel == tp) {
                inUsedList = true;
                break;
            }
        }
        if(!inUsedList) {
            currentTabPanel = tabPanels.get(0);
        }
        return currentTabPanel;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        LOGGER.info("windowClosing");
        System.exit(0);
    }

}
