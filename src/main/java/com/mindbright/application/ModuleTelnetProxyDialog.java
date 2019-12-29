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

package com.mindbright.application;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.mindbright.gui.GUI;

public class ModuleTelnetProxyDialog extends JDialog implements
   ModuleTelnetProxyDialogControl
{
	private static final long serialVersionUID = 1L;
    private JTextField   proxyHost;
    private JTextField   proxyPort;
    private JLabel       lblStatus;
    private JButton      startBut, closeBut;

    public ModuleTelnetProxyDialog(Frame parent, String title,
                                        boolean modal) {
        super(parent, title, modal);
    }
    
    public void initDialog(ActionListener al) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridwidth = 1;
        
        JLabel lbl = new JLabel(LBL_LISTEN_ADDR);
        p.add(lbl, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        lbl = new JLabel(LBL_LISTEN_PORT);
        p.add(lbl, gbc);
        
        gbc.gridwidth = 1;
        proxyHost = new JTextField(DEFAULT_HOST, 20);
        p.add(proxyHost, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        proxyPort = new JTextField(DEFAULT_PORT, 5);
        p.add(proxyPort, gbc);
        
        gbc.anchor = GridBagConstraints.CENTER;
        lblStatus = new JLabel(LBL_PROXY_DISABLED, SwingConstants.CENTER);
        p.add(lblStatus, gbc);
        
        getContentPane().add(p, BorderLayout.CENTER);

        startBut = new JButton(LBL_ENABLE);
        startBut.addActionListener(al);

        closeBut = new JButton(LBL_DISMISS);
        closeBut.addActionListener(new GUI.CloseAction(this));
        
        getContentPane().add(GUI.newButtonPanel(new JComponent[] {startBut, closeBut}),
                             BorderLayout.SOUTH);
        
        setResizable(true);
        pack();

        GUI.placeDialog(this);
        addWindowListener(GUI.getWindowDisposer());
    }

    public void showDialog() {
        setVisible(true);
    }

    public void setMode(boolean enable) {
        proxyPort.setEnabled(enable);
        proxyHost.setEnabled(enable);
        startBut.setText(enable ? LBL_ENABLE : LBL_DISABLE);
    }

    public void disposeDialog() {
        dispose();
    }
    
    public void setHost(String host) {
        proxyHost.setText(host);
    }
    
    public void setPort(String port) {
        proxyPort.setText(port);
    }

    public void setStatus(String status) {
        lblStatus.setText(status);
    }

    public String getHost() {
        return proxyHost.getText();
    }

    public String getPort() {
        return proxyPort.getText();
    }
}
