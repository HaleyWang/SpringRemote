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

public abstract class SSHChannel extends Thread {

    protected int                channelId;
    protected SSHChannelListener listener;

    public SSHChannel(int channelId) {
        super();
        setName(this.getClass().getName() + "[" + channelId + "]");
        this.channelId = channelId;
        this.listener  = null;
    }

    public void setSSHChannelListener(SSHChannelListener listener) {
        this.listener = listener;
    }

    public int getChannelId() {
        return channelId;
    }

    public abstract void serviceLoop() throws Exception;

    public void close() {}

    public void run() {
        try {
            serviceLoop();
        } catch (Exception e) {

            if(SSH.DEBUGMORE) {
                System.out.println("--- channel exit (exception is not an error):");
                e.printStackTrace();
                System.out.println("---");
            }

            close();
            if(listener != null)
                listener.close(this);

        } catch (ThreadDeath death) {
            SSH.logExtra("Channel killed " + channelId + " " + this);
            throw death;
        }
    }

}
