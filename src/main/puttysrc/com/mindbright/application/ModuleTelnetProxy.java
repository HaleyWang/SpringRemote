/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.mindbright.gui.GUI;

import com.mindbright.ssh2.SSH2TelnetProxyListener;

public class ModuleTelnetProxy implements MindTermModule, ActionListener {

    private MindTermApp mindterm;

    private SSH2TelnetProxyListener telnetProxy;
    private ModuleTelnetProxyDialogControl dialog;

    public void init(MindTermApp mindterm) {
        this.mindterm = mindterm;
    }
    
    public void activate(MindTermApp mindterm) {
        dialog = new ModuleTelnetProxyDialog(
            GUI.getFrame(mindterm.getDialogParent()),
            mindterm.getAppName() + " - Telnet Proxy", false);

        dialog.initDialog(this);
        
        String host = mindterm.getProperty("module.telnetproxy.host");
        String port = mindterm.getProperty("module.telnetproxy.port");
        if (host != null && !host.equals("")) 
            dialog.setHost(host);
        if (port != null && !port.equals("")) 
            dialog.setPort(port);
        
        updateTelnetDialog(false);
            
        dialog.showDialog();        
    }
    
    public boolean isAvailable(MindTermApp mindterm) {
        return (mindterm.isConnected() && (mindterm.getConnection() != null));
    }

    public void connected(MindTermApp mindterm) {
        String proxyHost  = mindterm.getProperty("module.telnetproxy.host");
        String proxyPortS = mindterm.getProperty("module.telnetproxy.port");
        if (proxyHost != null && proxyHost.trim().length() > 0) {
            try {
                int proxyPort = Integer.parseInt(proxyPortS);

                telnetProxy =
                    new SSH2TelnetProxyListener(proxyHost, proxyPort,
                                                mindterm.getConnection());
                mindterm.alert("Starting telnet proxy on " +
                               proxyHost + ":" + proxyPort);
            } catch (Exception e) {
                mindterm.alert("Error starting telnet proxy on " +
                               proxyHost + ":" + proxyPortS + " - " +
                               e.getMessage());
            }
        }
    }

    public void disconnected(MindTermApp mindterm) {
        if (telnetProxy != null) {
            telnetProxy.stop();
            telnetProxy = null;
        }
    }

    public String description(MindTermApp mindterm) {
        return null;
    }

    private void updateTelnetDialog(boolean preserveMsg) {
        if (telnetProxy == null) {
            if (!preserveMsg)
                dialog.setStatus("Proxy disabled");
            dialog.setMode(true);
        } else {
            if (!preserveMsg)
                dialog.setStatus("Proxy running: " +
                                 dialog.getHost() + ":" +
                                 dialog.getPort());
            dialog.setMode(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (telnetProxy != null) {
            disconnected(mindterm);
            updateTelnetDialog(false);
            mindterm.setProperty("module.telnetproxy.host", "");
            mindterm.setProperty("module.telnetproxy.port", "");
        } else {
            boolean err = false;
            dialog.setStatus("Setting up proxy...");
            try {
                String host = dialog.getHost();
                String port = dialog.getPort();
                mindterm.setProperty("module.telnetproxy.host", host);
                mindterm.setProperty("module.telnetproxy.port", port);
                connected(mindterm);
            } catch (Exception ex) {
                err = true;
                disconnected(mindterm);
                dialog.setStatus("Error: " + ex.getMessage());
            }
            updateTelnetDialog(err);
        }
    }
}
