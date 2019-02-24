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

package com.mindbright.ssh;

import java.io.File;
import java.io.IOException;

import com.mindbright.sshcommon.SSHSCP1;

public class SSHSCPClient extends SSHConsoleClient {

    private SSHSCP1 scp1;

    public SSHSCPClient(String sshHost, int port,
                        SSHAuthenticator authenticator,
                        SSHInteractor interactor,
                        File cwd, boolean verbose)
    throws IOException {
        super(sshHost, port, authenticator, interactor,
              SSHSCP1.DEFAULT_COPY_BUFFER_SZ);
        this.scp1 = new SSHSCP1(cwd, this, verbose);
    }

    public SSHSCP1 scp1() {
        return scp1;
    }

}
