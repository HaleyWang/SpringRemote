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

import com.mindbright.ssh2.SSH2SessionChannel;
import com.mindbright.ssh2.SSH2SFTPFactory;
import com.mindbright.ssh2.SSH2SFTPFileBrowser;

import com.mindbright.sshcommon.SSHChdirEventHandler;
import com.mindbright.sshcommon.SSHFileTransferDialog;
import com.mindbright.sshcommon.SSHFileTransferDialogControl;

public class ModuleSFTP implements MindTermModule, SSHChdirEventHandler {

    private MindTermApp mindtermapp;
	private SSH2SFTPFileBrowser filebrowser;

    public void init(MindTermApp mindterm) {}

    public void activate(MindTermApp mindterm) {
        mindtermapp = mindterm;

        String title = mindterm.getAppName() +
                       " - SFTP (" + mindterm.getHost() + ")";

        String lcwd = mindterm.getProperty("module.sftp.cwd-local");
        String rcwd = mindterm.getProperty("module.sftp.cwd-remote");
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

        SSHFileTransferDialogControl dialog = 
            new SSHFileTransferDialog(title, mindterm, new SSH2SFTPFactory());

        dialog.setLocalFileBrowser
            (new LocalFileBrowser(dialog.getLocalFileDisplay(), lcwd));

        dialog.setRemoteFileBrowser
            (filebrowser = new SSH2SFTPFileBrowser
			 (mindterm.getConnection(), dialog.getRemoteFileDisplay(), rcwd));

        dialog.setLocalChdirCallback(this);

        dialog.doShow();
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return (mindterm.isConnected() && (mindterm.getConnection() != null));
    }

    public void connected(MindTermApp mindterm) {}

    public void disconnected(MindTermApp mindterm) {}

    public String description(MindTermApp mindterm) {
        return null;
    }

    public void chdir(String newdir) {
        mindtermapp.setProperty("module.sftp.cwd-local", newdir);
    }

	public SSH2SessionChannel getSessionChannel() {
		return (filebrowser != null) ? filebrowser.getSFTPClient().getSessionChannel() : null;
	}
}
