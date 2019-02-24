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

import java.io.*;

import com.mindbright.util.Queue;

public class SSHTxChannel extends SSHChannel {

    protected OutputStream out;
    protected Queue        queue;

    boolean closePending;

    public SSHTxChannel(OutputStream out, int channelId) {
        super(channelId);
        this.out          = out;
        this.closePending = false;
        queue = new Queue();
    }

    public Queue getQueue() {
        return queue;
    }

    public void setClosePending() {
        closePending = true;
        queue.setBlocking(false);
    }

    public synchronized boolean isClosePending() {
        return closePending;
    }

    public void serviceLoop() throws Exception {
        SSH.logExtra("Starting tx-chan: " + channelId);
        for(;;) {
            SSHPdu pdu;
            // !!! the thread is (hopefully) suspended when we set closePending
            // so we don't have to access a lock each loop
            if(closePending && queue.isEmpty()) {
                throw new Exception("CLOSE");
            }
            pdu = (SSHPdu)queue.getFirst();
            //      pdu = pdu.preProcess();
            pdu.writeTo(out);
            //      pdu = pdu.postProcess();
        }
    }

}
