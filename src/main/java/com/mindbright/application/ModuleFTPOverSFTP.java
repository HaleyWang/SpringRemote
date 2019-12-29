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

import java.applet.AppletContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.mindbright.gui.GUI;

public class ModuleFTPOverSFTP implements MindTermModule, ActionListener,
    Runnable {

    ServerSocket ftpdListen;
    boolean ftpdIsWindows;
    MindTermApp mindterm;

    Thread ftpd;

    ModuleFTPOverSFTPDialogControl dialog;

    public void init(MindTermApp mindterm) {
        this.mindterm = mindterm;
    }

    public void activate(MindTermApp mindterm) {
        dialog = new ModuleFTPOverSFTPDialog(
            GUI.getFrame(mindterm.getDialogParent()),
            mindterm.getAppName() + " - FTP To SFTP Bridge",
            false);

        dialog.initDialog(this, mindterm.isApplet());

        String host = mindterm.getProperty("sftpbridge-host");
        String port = mindterm.getProperty("sftpbridge-port");
        String hosttype = mindterm.getProperty("sftpbridge-hosttype");
        if (host != null && !host.equals("")) 
            dialog.setHost(host);
        if (port != null && !port.equals(""))
            dialog.setPort(port);
        if (hosttype != null && hosttype.toLowerCase().equals("windows"))
            dialog.setRemoteSystemIsUnix(false);
        updateFtpdDialog(false);

        dialog.showDialog();
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return (mindterm.isConnected() && (mindterm.getConnection() != null));
    }

    public void connected(MindTermApp mindterm) {
        String ftpdHost = mindterm.getProperty("sftpbridge-host");
        String ftpdPort = mindterm.getProperty("sftpbridge-port");
        String hosttype = mindterm.getProperty("sftpbridge-hosttype");
        if (ftpdHost != null && ftpdHost.trim().length() > 0) {
            try {
                startFtpdLoop(ftpdHost, ftpdPort, 
                              hosttype != null && hosttype.toLowerCase().equals("windows"));
                mindterm.alert("Starting ftp to sftp bridge on " +
                               ftpdHost + ":" + ftpdPort);
            } catch (Exception e) {
                mindterm.alert("Error starting ftp to sftp bridge on " +
                               ftpdHost + ":" + ftpdPort + " - " +
                               e.getMessage());
            }
        }
    }

    public void disconnected(MindTermApp mindterm) {
        stopFtpdLoop();
    }

    public String description(MindTermApp mindterm) {
        return null;
    }

    private void updateFtpdDialog(boolean preserveMsg) {
        if (ftpdListen == null) {
            if (!preserveMsg)
                dialog.setStatus("Bridge disabled");
            dialog.setMode(true);
        } else {
            if (!preserveMsg)
                dialog.setStatus("Bridge enabled: " +
                                 dialog.getHost() + ":" +
                                 dialog.getPort());
            dialog.setMode(false);
        }
    }

    public void run() {
        ftpdLoop(ftpdListen);
    }

    public void startFtpdLoop(String host, String portStr, boolean remoteWindows) throws Exception {
        int port = Integer.parseInt(portStr);
        ftpdListen = new ServerSocket(port, 32, InetAddress.getByName(host));
        ftpdIsWindows = remoteWindows;
        ftpd = new Thread(this, "FTPOverSFTP");
        ftpd.start();
    }

    @SuppressWarnings("deprecation")
	public void stopFtpdLoop() {
        if (ftpdListen != null) {
            try {
                ftpdListen.close();
            } catch (IOException e) {
                /* don't care */
            } finally {
                ftpdListen = null;
            }
        }
        if (ftpd != null && ftpd.isAlive()) {
            ftpd.stop();
        }
        ftpd = null;
    }

    public void ftpdLoop(ServerSocket listen) {
        Socket conn = null;
        try {
            for(;;) {
                conn = null;
                conn = listen.accept();
                try {
                    new com.mindbright.ssh2.SSH2FTPOverSFTP(mindterm.getConnection(),
					                                        conn.getInputStream(),
					                                        conn.getOutputStream(),
					                                        ftpdIsWindows,
					                                        mindterm.getAppName() +
					                                        ", FTP To SFTP Bridge");
                } catch (Throwable se) {
                    mindterm.alert("Failed to start FTP over SFTP bridge: " + se.getMessage());
                    se.printStackTrace();
                    try {
                        conn.close();
                    } catch (Throwable t) { }
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            /* ServerSocket closed (or error...) */
        } finally {
            if (conn != null)
                try { conn.close(); } catch (Throwable t) { }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Browser...")) {
            AppletContext ctx = mindterm.getAppletContext();
            String host = dialog.getHost();
            if ("0.0.0.0".equals(host)) {
                host = "127.0.0.1";
            }
            String url  = "ftp://" + host + ":" + dialog.getPort();
            try {
                ctx.showDocument(new java.net.URL(url), "_blank");
            } catch (Exception ex) {
                mindterm.alert("Error running ftp browser: " + ex);
            }
        } else {
            if (ftpd != null) {
                stopFtpdLoop();
                updateFtpdDialog(false);
                mindterm.setProperty("sftpbridge-host", "");
                mindterm.setProperty("sftpbridge-port", "");
                mindterm.setProperty("sftpbridge-hosttype", "");
            } else {
                boolean err = false;
                dialog.setStatus("Starting...");
                try {
                    String host = dialog.getHost();
                    String port = dialog.getPort();
                    boolean isWindows = !dialog.isRemoteSystemUnix();
                    startFtpdLoop(host, port, isWindows);
                    mindterm.setProperty("sftpbridge-host", host);
                    mindterm.setProperty("sftpbridge-port", port);
                    mindterm.setProperty("sftpbridge-hosttype", isWindows ? "windows" : "unix");
                } catch (Exception ex) {
                    err = true;
                    ftpdListen = null;
                    dialog.setStatus("Error: " + ex.getMessage());
                }
                updateFtpdDialog(err);
            }
        }
    }

}
