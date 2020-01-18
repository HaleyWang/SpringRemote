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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.mindbright.gui.GUI;

import com.mindbright.ssh2.SSH2SocksListener;

public class ModuleSocksProxy implements MindTermModule, ActionListener {

    MindTermApp mindterm;

    SSH2SocksListener listener;
    
    ModuleSocksProxyDialogControl dialog;

    public void init(MindTermApp mindterm) {
        this.mindterm = mindterm;
    }

    public void activate(MindTermApp mindterm) {
        dialog = new ModuleSocksProxyDialog(
            GUI.getFrame(mindterm.getDialogParent()),
            mindterm.getAppName() + " - SOCKS Proxy", false);

        dialog.initDialog(this);

        String host = mindterm.getProperty("socksproxy-host");
        String port = mindterm.getProperty("socksproxy-port");
        if (host != null && !host.equals("")) 
            dialog.setHost(host);
        if (port != null && !port.equals(""))
            dialog.setPort(port);

        updateSocksDialog(false);

        dialog.showDialog();
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return (mindterm.isConnected() && (mindterm.getConnection() != null));
    }

    public void connected(MindTermApp mindterm) {
        String socksHost = mindterm.getProperty("socksproxy-host");
        String socksPort = mindterm.getProperty("socksproxy-port");
        if (socksHost != null && socksHost.trim().length() > 0) {
            try {
                startSocksListener(socksHost, socksPort);
                mindterm.alert("Starting SOCKS proxy on " +
                               socksHost + ":" + socksPort);
            } catch (Exception e) {
                mindterm.alert("Error starting SOCKS proxy on " +
                               socksHost + ":" + socksPort + " - " +
                               e.getMessage());
            }
        }
    }

    public void disconnected(MindTermApp mindterm) {
        stopSocksLoop();
    }

    public String description(MindTermApp mindterm) {
        return null;
    }

    private void updateSocksDialog(boolean preserveMsg) {
        if (listener == null) {
            if (!preserveMsg)
                dialog.setStatus("SOCKS proxy disabled");
            dialog.setMode(true);
        } else {
            if (!preserveMsg)
                dialog.setStatus("SOCKS proxy enabled: " +
                                 dialog.getHost() + ":" +
                                 dialog.getPort());
            dialog.setMode(false);
        }
    }

    public void startSocksListener(String host, String portStr) throws Exception {
        listener = new SSH2SocksListener(host, Integer.parseInt(portStr),
                                         mindterm.getConnection());
    }

    public void stopSocksLoop() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            stopSocksLoop();
            updateSocksDialog(false);
            mindterm.setProperty("socksproxy-host", "");
            mindterm.setProperty("socksproxy-port", "");
        } else {
            boolean err = false;
            dialog.setStatus("Starting...");
            try {
                String host = dialog.getHost();
                String port = dialog.getPort();
                startSocksListener(host, port);
                mindterm.setProperty("socksproxy-host", host);
                mindterm.setProperty("socksproxy-port", port);
            } catch (Exception ex) {
                err = true;
                dialog.setStatus("Error: " + ex.getMessage());
            }
            updateSocksDialog(err);
        }
    }
}
