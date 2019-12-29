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

import com.isnetworks.ssh.LocalFileBrowser;

import com.mindbright.ssh.SSHRemoteFileBrowsingConsole;

import com.mindbright.ssh2.SSH2SCP1Factory;

import com.mindbright.sshcommon.SSHChdirEventHandler;
import com.mindbright.sshcommon.SSHConsoleRemote;
import com.mindbright.sshcommon.SSHFileTransferDialog;
import com.mindbright.sshcommon.SSHFileTransferDialogControl;
import com.mindbright.sshcommon.SSHFileTransferFactory;

public class ModuleSCP implements MindTermModule, SSHChdirEventHandler {

    private MindTermApp mindtermapp;

    public void init(MindTermApp mindterm) {}

    public void activate(MindTermApp mindterm) {
        mindtermapp = mindterm;

        String title = mindterm.getAppName() +
                       " - SCP (" + mindterm.getHost() + ")";

        SSHConsoleRemote       remote  = null;
        SSHFileTransferFactory factory = null;

        if(mindterm.getConnection() == null) {
            try {
                factory = (SSHFileTransferFactory)Class.
                          forName("com.mindbright.ssh.SSHSCPFactory").newInstance();
            } catch (Throwable t) {
                mindterm.alert("Error activating ModuleSCP: " + t);
            }
        } else {
            factory = new SSH2SCP1Factory();
        }

        remote = mindterm.getConsoleRemote();

        SSHFileTransferDialogControl dialog = 
            new SSHFileTransferDialog(title, mindterm, factory);
        
        String lcwd = mindterm.getProperty("module.scp.cwd-local");
        String rcwd = mindterm.getProperty("module.scp.cwd-remote");
        if(rcwd == null) {
            rcwd = ".";
        }
        try {
            if(lcwd == null) {
                lcwd = System.getProperty("user.home");
                if(lcwd == null)
                    lcwd = System.getProperty("user.dir");
                if(lcwd == null)
                    lcwd = System.getProperty("java.home");
            }
        } catch (Throwable t) {
            // !!!
        }

        dialog.setLocalFileBrowser
            (new LocalFileBrowser(dialog.getLocalFileDisplay(), lcwd));

        String cmd = mindterm.getProperty("filelist-remote-command");
        if(cmd == null) {
            cmd = "ls -A -L -F -1\n";
        }

        dialog.setRemoteFileBrowser
            (new SSHRemoteFileBrowsingConsole(dialog.getRemoteFileDisplay(),
                                              cmd, remote, rcwd));

        dialog.setLocalChdirCallback(this);

        dialog.doShow();
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return mindterm.isConnected();
    }

    public void connected(MindTermApp mindterm) {}

    public void disconnected(MindTermApp mindterm) {}

    public String description(MindTermApp mindterm) {
        return null;
    }

    public void chdir(String newdir) {
        mindtermapp.setProperty("module.scp.cwd-local", newdir);
    }
}

