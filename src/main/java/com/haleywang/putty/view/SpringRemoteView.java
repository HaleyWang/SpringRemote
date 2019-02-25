/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.haleywang.putty.view;

import com.haleywang.putty.dto.ConnectionDto;
import line.someonecode.JTabbedPaneCloseButton;
import org.alvin.puttydemo.PuttyPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowEvent;
import java.io.IOException;

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

    private void createAndAddPuttyPane(JTabbedPane tab, ConnectionDto connectionDto, String connectionPassword) {
        String port = connectionDto.getPort() == null ? "22" : connectionDto.getPort();
        PuttyPane putty = new PuttyPane(connectionDto.getHost(), connectionDto.getUser(), port, connectionPassword);

        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount()-1);

        SwingUtilities.invokeLater(putty::init);
    }

    void afterLogin(String key) {

        SideView sidePanel =  SideView.getInstance();
        sidePanel.setAesKey(key);

        mainPanel.add(sidePanel, BorderLayout.WEST);

        connectionsTab = new JTabbedPaneCloseButton();
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

    public void onCreateConnectionsTab(ConnectionDto connectionDto, String connectionPassword) {
        createAndAddPuttyPane(connectionsTab, connectionDto, connectionPassword);

    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }


}
