/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.ssh2;

import java.io.File;

import com.mindbright.application.MindTermApp;

import com.mindbright.sshcommon.SSHFileTransfer;
import com.mindbright.sshcommon.SSHFileTransferFactory;

/**
 * Factory which creates new <code>SSH2SFTPTransfer</code> instances.
 */
public class SSH2SFTPFactory implements SSHFileTransferFactory {
    public SSHFileTransfer create(MindTermApp client, File cwd)
    throws Exception {
        return new SSH2SFTPTransfer(cwd, client.getConnection());
    }
}
