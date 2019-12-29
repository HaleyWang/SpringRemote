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

import com.mindbright.sshcommon.SSHSCP1;

/**
 * Creates a window in which gives us a command-line interface to scp1.
 */
public final class SSH2SCP1Client extends SSH2ConsoleRemote {

    private SSHSCP1 scp1;

    /**
     * @param cwd Name of current working directory.
     * @param connection Connection object to run over.
     * @param stderr Stream to output error messages on
     * @param verbose Verbose flag
     */
    public SSH2SCP1Client(File cwd, SSH2Connection connection,
                          OutputStream stderr, boolean verbose) {
        super(connection, null, stderr);
        this.scp1 = new SSHSCP1(cwd, this, verbose);
    }

    /**
     * Get the underlying <code>SSHSCP1</code> object.
     */
    public SSHSCP1 scp1() {
        return scp1;
    }

}
