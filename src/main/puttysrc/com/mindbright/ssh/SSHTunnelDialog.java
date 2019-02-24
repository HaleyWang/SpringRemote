/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.ssh;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.awt.event.*;

import javax.swing.*;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mindbright.gui.GUI;

public final class SSHTunnelDialog {

    private final static String LBL_LOCAL      = "Local";
    private final static String LBL_REMOTE     = "Remote";
    private final static String LBL_LOCALHOST  = "localhost";
    private final static String LBL_ALL_HOSTS  = "all (0.0.0.0)";
    private final static String LBL_IP         = "ip";

    private final static String LBL_TYPE       = "Type";
    private final static String LBL_PLUGIN     = "Plugin";
    private final static String LBL_SRC_PORT   = "Bind port";
    private final static String LBL_BIND_ADDR  = "Bind address";
    private final static String LBL_DEST_ADDR  = "Dest. address";
    private final static String LBL_DEST_PORT  = "Dest. port";

    private final static String LBL_BTN_ADD    = "Add...";
    private final static String LBL_BTN_DEL    = "Delete";
    private final static String LBL_BTN_DISMISS= "Dismiss";
    private final static String LBL_BTN_OK     = "Ok";
    private final static String LBL_BTN_CANCEL = "Cancel";

    private final static String   PLUGIN_NONE  = "none";
    private final static String   PLUGIN_FTP   = "ftp";

    private final static String[] PLUGIN_NAMES = { PLUGIN_NONE, PLUGIN_FTP };
    private static SSHPropertyHandler   propsHandler;
    private static Component            parent;
    private static SSHInteractiveClient client;

    private static JList   tunnelList;
    private static JButton delButton;

    private static void showAddDialog() {
        final JDialog dialog = GUI.newBorderJDialog(
            parent, "Add tunnel", true);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.fill   = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 2, 2, 2);

        final JRadioButton local = new JRadioButton(LBL_LOCAL, true);
        local.setActionCommand("local");
        final JRadioButton remote = new JRadioButton(LBL_REMOTE); 
        remote.setActionCommand("remote");
        ButtonGroup bg = new ButtonGroup();
        bg.add(local);
        bg.add(remote);

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p1.add(local);
        p1.add(remote);
        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel(LBL_TYPE), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        p.add(p1, gbc);

        final JRadioButton localhost = new JRadioButton(LBL_LOCALHOST, true);
        localhost.setActionCommand("localhost");
        final JRadioButton allhosts  = new JRadioButton(LBL_ALL_HOSTS);
        allhosts.setActionCommand("allhosts");
        final JRadioButton spechost  = new JRadioButton(LBL_IP);
        spechost.setActionCommand("spechost");
        bg = new ButtonGroup();
        bg.add(localhost);
        bg.add(allhosts);
        bg.add(spechost);

        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        gbc.insets    = new Insets(2, 2, 0, 2);
        p.add(new JLabel(LBL_BIND_ADDR), gbc);

        gbc.anchor    = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(localhost, gbc);

        gbc.gridx     = 1;
        p.add(allhosts, gbc);

        gbc.insets    = new Insets(2, 2, 2, 2);
        p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p1.add(spechost);
        final JTextField listenip = new JTextField("", 16);
        listenip.setEnabled(false);
        p1.add(listenip);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(p1, gbc);

        gbc.gridx     = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel(LBL_SRC_PORT), gbc);
        final JTextField srcport = new JTextField("", 5);
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(srcport, gbc);

        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel(LBL_DEST_ADDR), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        final JTextField destaddr = new JTextField("", 16);
        p.add(destaddr, gbc);

        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel(LBL_DEST_PORT), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        final JTextField destport = new JTextField("", 5);
        p.add(destport, gbc);

        gbc.gridwidth = 1;
        gbc.anchor    = GridBagConstraints.EAST;
        final JLabel pluginlbl = new JLabel(LBL_PLUGIN);
        p.add(pluginlbl, gbc);
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        final JComboBox plugins = new JComboBox(PLUGIN_NAMES);
        p.add(plugins, gbc);

        gbc.fill      = GridBagConstraints.BOTH;
        gbc.weightx   = 1.0;
        gbc.weighty   = 1.0;
        p.add(new JPanel(), gbc);

        JButton ok = new JButton(LBL_BTN_OK);
        ok.setActionCommand("ok");

        JButton cancel = new JButton(LBL_BTN_CANCEL);
        cancel.addActionListener(new GUI.CloseAction(dialog));

        dialog.getContentPane().add(p, BorderLayout.CENTER);
        dialog.getContentPane().add(GUI.newButtonPanel
                   (new JComponent[] { ok, cancel }), BorderLayout.SOUTH);

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if ("local".equals(cmd)) {
                    pluginlbl.setEnabled(true);
                    plugins.setEnabled(true);
                } else if ("remote".equals(cmd)) {
                    pluginlbl.setEnabled(false);
                    plugins.setEnabled(false);
                    plugins.setSelectedIndex(0);
                } else if ("localhost".equals(cmd)) {
                    listenip.setEnabled(false);
                    listenip.setText("");
                } else if ("allhosts".equals(cmd)) {
                    listenip.setEnabled(false);
                    listenip.setText("");
                } else if ("spechost".equals(cmd)) {
                    listenip.setEnabled(true);
                } else if ("ok".equals(cmd)) {
                    int lp = -1, rp = -1;
                    try {
                        lp = Integer.valueOf(srcport.getText()).intValue();
                        rp = Integer.valueOf(destport.getText()).intValue();
                        if (lp < 1 || lp > 65535) {
                            lp = -1;
                            throw new NumberFormatException();
                        }
                        if (rp < 1 || rp > 65535) {
                            rp = -1;
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException ee) {
                        if (lp == -1) {
                            srcport.setText("");
                            srcport.requestFocus();
                        } else {
                            destport.setText("");
                            destport.requestFocus();
                        }
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }

                    String plug = (String)plugins.getSelectedItem();
                    if (plug.equals(PLUGIN_NONE))
                        plug = "general";
                    String daddr = destaddr.getText().trim();
                    if (daddr.equals("")) {
                        destport.requestFocus();
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }

                    String key;

                    try {
                        if (local.isSelected()) {
                            key = "local" + client.localForwards.size();
                        } else {
                            key = "remote" + client.remoteForwards.size();
                        }
                        String baddr = "";
                        if (localhost.isSelected()) {
                            baddr = "127.0.0.1";
                        } else if (allhosts.isSelected()) {
                            baddr = "0.0.0.0";
                        } else {
                            baddr = listenip.getText().trim();
                            if (baddr.equals("")) {
                                listenip.requestFocus();
                                Toolkit.getDefaultToolkit().beep();
                                return;
                            }
                        }

                        propsHandler.setProperty(key, "/" + plug + "/" +
                                                 baddr + ":" + lp + ":" +
                                                 daddr + ":" +  rp);
                    } catch (Throwable ee) {
                        SSHMiscDialogs.alert("Tunnel Notice",
                                             "Could not open tunnel: " +
                                             ee.getMessage(), parent);
                        return;
                    }
                    dialog.dispose();
                }
            }
        };
        ok.addActionListener(al);
        local.addActionListener(al);
        remote.addActionListener(al);        
        localhost.addActionListener(al);
        allhosts.addActionListener(al);
        spechost.addActionListener(al);        

        srcport.requestFocus();

        dialog.setResizable(false);
        dialog.pack();
        GUI.placeDialog(dialog);
        dialog.addWindowListener(GUI.getWindowDisposer());
        dialog.setVisible(true);
    }
    
    private static class Action implements 
          ActionListener, ItemListener, ListSelectionListener 
    {
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if ("add".equals(cmd)) {
                showAddDialog();
            } else if ("del".equals(cmd)) {
                int i = tunnelList.getSelectedIndex();
                if (i < 0) return;
                int len = client.localForwards.size();
                if (i < len) {
                    propsHandler.removeLocalTunnelAt(i, true);
                } else {
                    propsHandler.removeRemoteTunnelAt(i-len);
                }
            }
            updateTunnelList();
        }

        public void itemStateChanged(ItemEvent e) {}

        public void valueChanged(ListSelectionEvent e) {
            int i = tunnelList.getSelectedIndex();
            delButton.setEnabled(i >= 0);
        }
    }
    
    public static void show(String title, SSHInteractiveClient cli,
                            SSHPropertyHandler props, Component p) {
        propsHandler = props;
        parent       = p;
        client       = cli;

        JDialog dialog = GUI.newBorderJDialog(parent, title,true);

        Action al = new Action();

        tunnelList = new JList();
        tunnelList.setVisibleRowCount(8);
        tunnelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(tunnelList);
        dialog.getContentPane().add(sp, BorderLayout.CENTER);
        tunnelList.addListSelectionListener(al);

        JButton add = new JButton(LBL_BTN_ADD);
        add.setActionCommand("add");
        add.addActionListener(al);

        delButton = new JButton(LBL_BTN_DEL);
        delButton.setActionCommand("del");
        delButton.addActionListener(al);
        delButton.setEnabled(false);

        JButton ok  = new JButton(LBL_BTN_DISMISS);
        ok.addActionListener(new GUI.CloseAction(dialog));

        dialog.getContentPane().add(
            GUI.newButtonPanel(new JComponent[] {
                new JLabel(""), new JLabel(""), add, delButton, ok
            }), BorderLayout.SOUTH);


        updateTunnelList();

        dialog.setResizable(true);
        dialog.pack();
        GUI.placeDialog(dialog);
        dialog.addWindowListener(GUI.getWindowDisposer());
        dialog.setVisible(true);
    }
    
    private static void updateTunnelList() {
        int llen = client.localForwards.size();
        int rlen = client.remoteForwards.size();
        String[] s = new String[llen+rlen];

        for(int i = 0; i < llen; i++) {
            SSHClient.LocalForward fwd = client.localForwards.elementAt(i);
            String plugStr = (fwd.plugin.equals("general")) ? 
                "" : (" (plugin: " + fwd.plugin + ")");
            s[i] = "L: " + fwd.localHost + ":" + fwd.localPort + 
                " --> " + fwd.remoteHost + ":" + fwd.remotePort + plugStr;
        }

        for(int i = 0; i < rlen; i++) {
            SSHClient.RemoteForward fwd = client.remoteForwards.elementAt(i);
            s[i+llen] = "R: " + fwd.localHost + ":" + fwd.localPort + 
                " <-- " + fwd.remoteHost + ":" + fwd.remotePort;
        }

        tunnelList.setListData(s);
    }
}
