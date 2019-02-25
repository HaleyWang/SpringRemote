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
public class SpringRemoteView extends javax.swing.JFrame implements MyWindowListener,SideViewListener {

    private JTabbedPane connectionsTab;
    private JPanel mainPanel;

    /**
     * Creates new SpringRemoteView
     */
    public SpringRemoteView() {

        setSize(880, 680);
        setVisible(true);

        mainPanel = new JPanel();
        mainPanel.setBackground(Color.darkGray);
        BorderLayout layout = new BorderLayout();
        layout.setHgap(2);
        layout.setVgap(2);
        mainPanel.setLayout(layout);
        setContentPane(mainPanel);
        initMenu();

        LoginDialog loginDlg = new LoginDialog(this);
        loginDlg.setVisible(true);
        addWindowListener(this);
    }

    private void initMenu() {
        JPanel menuPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        menuPanel.add(refreshBtn);
        JButton pasteBtn = new JButton("Paste");
        menuPanel.add(pasteBtn);
        menuPanel.add(new JButton("About"));
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        pasteBtn.addActionListener(e -> {
            try {
                String data = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
                typedString(data);
            } catch (UnsupportedFlavorException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        });

        refreshBtn.addActionListener(e -> {
            System.out.println("do Refresh");

        });
    }

    private void createAndAddPuttyPane(JTabbedPane tab, ConnectionDto connectionDto, String connectionPassword) {
        String port = connectionDto.getPort() == null ? "22" : connectionDto.getPort();
        PuttyPane putty = new PuttyPane(connectionDto.getHost(), connectionDto.getUser(), port, connectionPassword);

        tab.add(connectionDto.toString(), putty);
        tab.setSelectedIndex(tab.getTabCount()-1);

        SwingUtilities.invokeLater(putty::init);
    }

    void afterLogin(String key) {

        SideView sidePanel = new SideView(this);
        sidePanel.setAesKey(key);

        mainPanel.add(sidePanel, BorderLayout.WEST);

        connectionsTab = new JTabbedPaneCloseButton();
        mainPanel.add(connectionsTab, BorderLayout.CENTER);
        mainPanel.revalidate();
    }

    @Override
    public void onTypedString(String command) {

        typedString(command);


    }

    void typedString(String command) {
        Component component = connectionsTab.getSelectedComponent();
        if(component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.typedString(command);

        }
    }

    @Override
    public void onCreateConnectionsTab(ConnectionDto connectionDto, String connectionPassword) {
        createAndAddPuttyPane(connectionsTab, connectionDto, connectionPassword);

    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }


}
