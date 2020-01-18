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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionListener;

import javax.swing.*;

import com.mindbright.gui.GUI;

public class ModuleFTPOverSFTPDialog extends JDialog 
    implements ModuleFTPOverSFTPDialogControl {
	private static final long serialVersionUID = 1L;

    private JTextField ftpHost;
    private JTextField ftpPort;
    private JCheckBox  unixCB, windowsCB;
    private JLabel     lblStatus;
    private JButton    startBut, closeBut, browseBut;

    public ModuleFTPOverSFTPDialog(Frame parent, String title, boolean modal) {
        super(parent, title, modal);
    }
    
    public void initDialog(ActionListener al, boolean showBrowse) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridwidth = 1;
        
        JLabel lbl = new JLabel(LBL_LISTEN_ADDR);
        p.add(lbl, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        lbl = new JLabel(LBL_LISTEN_ADDR);
        p.add(lbl, gbc);
        
        gbc.gridwidth = 1;
        ftpHost = new JTextField(DEFAULT_HOST, 20);
        p.add(ftpHost, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        ftpPort = new JTextField(DEFAULT_PORT, 5);
        p.add(ftpPort, gbc);

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel(LBL_REMOTE_SYSTEM));
        p1.add(unixCB = new JCheckBox(LBL_REMOTE_IS_UNIX, true));
        p1.add(windowsCB = new JCheckBox(LBL_REMOTE_IS_WINDOWS, false));
        ButtonGroup bg = new ButtonGroup();
        bg.add(unixCB);
        bg.add(windowsCB);
        p.add(p1, gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        lblStatus = new JLabel(LBL_BRIDGE_DISABLED, SwingConstants.CENTER);
        p.add(lblStatus, gbc);
        
        getContentPane().add(p, BorderLayout.CENTER);

        JComponent[] buttons;
        startBut = new JButton(LBL_ENABLE);
        startBut.addActionListener(al);

        closeBut = new JButton(LBL_DISMISS);
        closeBut.addActionListener(new GUI.CloseAction(this));
        
        if (showBrowse) {
            browseBut = new JButton(LBL_BROWSER);
            browseBut.addActionListener(al);
            buttons = new JComponent[] {startBut, browseBut, closeBut};
        } else {
            buttons = new JComponent[] {startBut, closeBut};
        }
        
        getContentPane().add(GUI.newButtonPanel(buttons), BorderLayout.SOUTH);

        setResizable(true);
        pack();

        GUI.placeDialog(this);        
        addWindowListener(GUI.getWindowDisposer());  
    }

    public void showDialog() {
        setVisible(true);
    }
    
    public void disposeDialog() {
        dispose();
    }
    
    public void setHost(String host) {
        ftpHost.setText(host);
    }
    
    public void setPort(String port) {
        ftpPort.setText(port);
    }

    public void setRemoteSystemIsUnix(boolean yes) {
        if (yes) {
            unixCB.setSelected(true);
        } else {
            windowsCB.setSelected(true);
        }
    }

    public void setStatus(String status) {
        lblStatus.setText(status);
    }

    public void setMode(boolean enable) {
        ftpPort.setEnabled(enable);
        ftpHost.setEnabled(enable);
        if (browseBut != null)
            browseBut.setEnabled(!enable);
        startBut.setText(enable ? LBL_ENABLE : LBL_DISABLE);
     }

    public String getHost() {
        return ftpHost.getText();
    }
    
    public String getPort() {
        return ftpPort.getText();
    }

    public boolean isRemoteSystemUnix() {
        return unixCB.isSelected();
    }
}
