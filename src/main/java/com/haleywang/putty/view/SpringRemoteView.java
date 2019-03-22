package com.haleywang.putty.view;

import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.dto.ConnectionDto;
import other.JTabbedPaneCloseButton;
import puttydemo.PuttyPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Objects;

/**
 *
 * @author haley wang
 */
public class SpringRemoteView extends javax.swing.JFrame implements MyWindowListener {


    public static SpringRemoteView getInstance(){
        return SpringRemoteView.SingletonHolder.sInstance;
    }

    private static class SingletonHolder {
        private static final SpringRemoteView sInstance = new SpringRemoteView();
    }

    private JTabbedPane connectionsTab;
    private JPanel mainPanel;

    /**
     * Creates new SpringRemoteView
     */
    private SpringRemoteView() {

        setSize(880, 680);
        setVisible(true);
        this.setTitle("SpringRemote");

        mainPanel = new JPanel();
        mainPanel.setBackground(Color.darkGray);
        BorderLayout layout = new BorderLayout();
        layout.setHgap(2);
        layout.setVgap(2);
        mainPanel.setLayout(layout);
        setContentPane(mainPanel);

        LoginDialog loginDlg = new LoginDialog(this);
        loginDlg.setVisible(true);
        addWindowListener(this);
        initMenu();

    }

    private void initMenu() {
        mainPanel.add(MenuView.getInstance(), BorderLayout.NORTH);
    }

    private void createAndAddPuttyPane(JTabbedPane tab, ConnectionDto connectionDto, AccountDto connectionAccount) {
        String port = connectionDto.getPort() == null ? "22" : connectionDto.getPort();

        String connectionPassword = null;
        if(connectionDto.getUser() == null || Objects.equals(connectionDto.getUser(), connectionAccount.getName())) {
            connectionPassword = connectionAccount.getPassword();
        }
        String connectionUser = connectionDto.getUser() != null ? connectionDto.getUser() : connectionAccount.getName();

        PuttyPane putty = new PuttyPane(connectionDto.getHost(), connectionUser, port, connectionPassword);
        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount()-1);

        SwingUtilities.invokeLater(putty::init);
    }

    void afterLogin(String key) {

        SideView sidePanel =  SideView.getInstance();
        sidePanel.setAesKey(key);

        mainPanel.add(sidePanel, BorderLayout.WEST);

        connectionsTab = new JTabbedPaneCloseButton(tab -> {
            if(tab instanceof PuttyPane) {
                ((PuttyPane)tab).close();
            }
        });
        mainPanel.add(connectionsTab, BorderLayout.CENTER);
        mainPanel.revalidate();
    }

    public void onTypedString(String command) {

        typedString(command);
    }

    void typedString(String command) {
        Component component = connectionsTab.getSelectedComponent();
        if(component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.typedString(command);
            puttyPane.setTermFocus();

        }
    }

    public void onCreateConnectionsTab(ConnectionDto connectionDto, AccountDto connectionAccount) {
        createAndAddPuttyPane(connectionsTab, connectionDto, connectionAccount);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }


}
