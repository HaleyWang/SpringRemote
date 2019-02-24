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
import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.application.MindTermApp;

import com.mindbright.sshcommon.SSHFileTransfer;
import com.mindbright.sshcommon.SSHFileTransferFactory;

/**
 * Factory which creates <code>SSHFileTransfer</code> objects which
 * use the scp1 protocol.
 */
public class SSH2SCP1Factory implements SSHFileTransferFactory {
    public SSHFileTransfer create(final MindTermApp client, File cwd)
    throws Exception {
        OutputStream alertOutput = new OutputStream() {
            public void write(int bb) throws IOException {
                byte[] buf = new byte[] { (byte)bb };
                write(buf);
            }
            public void write(byte bb[], int off, int len)
                throws IOException {
                client.alert("Remote warning/error: " +
                             new String(bb, off, len));
            }
        };
        return (new SSH2SCP1Client(cwd, client.getConnection(),
                                   alertOutput, false)).scp1();
    }
}
