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

import com.mindbright.util.FlushingOutputStream;

/**
 * This is a subclass of <code>SSH2StreamChannel</code> which
 * implements channels which are connected to a local process
 */
public class SSH2ProcChannel extends SSH2StreamChannel {

    protected Process proc;
    protected String  originAddr;
    protected int     originPort;

    /**
     * Create a new process channel of the given type. The channel is
     * associated with an ssh connection. Channel types are
     * defined in <code>SSH2Connection</code> and starts with
     * <code>CH_TYPE</code>.
     *
     * @param channelType Type of channel to create.
     * @param connection The ssh connection to associate the channel with.
     * @param creator The object the channel is created from.
     * @param proc Process the channel communicates with
     * @param originAddr Originating host of remote connection.
     * @param originPort Originating port of remote connection.
     */
    public SSH2ProcChannel(int channelType, SSH2Connection connection,
                           Object creator,
                           Process proc,
                           String originAddr, int originPort) {
        super(channelType, connection, creator, proc.getInputStream(),
              new FlushingOutputStream(proc.getOutputStream()));
        this.proc       = proc;
        this.originAddr = originAddr;
        this.originPort = originPort;
    }

    protected void outputClosed() {
        if(proc != null) {
            proc.destroy();
        }
        proc = null;
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
                                      String langTag) {
        outputClosed();
        return false;
    }

    /**
     * Get the address the connection is comming from
     */
    public String getOriginAddress() {
        return originAddr;
    }

    /**
     * Get the port the connection is comming from
     */
    public int getOriginPort() {
        return originPort;
    }

    /**
     * Create a string representation of this object.
     *
     * @return A string describing this instance.
     */
    public String toString() {
        return "[remote] " + originAddr + ":" + originPort +
            " <--> [sshd] <--ssh2--> " + proc;
    }
}
