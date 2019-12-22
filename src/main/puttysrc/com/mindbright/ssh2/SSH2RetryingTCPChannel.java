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

import java.io.IOException;
import java.net.Socket;

/**
 * A subclass of <code>SSH2TCPChannel</code> which retries the open if
 * it fails.
 */
public class SSH2RetryingTCPChannel extends SSH2TCPChannel {

    private int  numOfRetries;
    private long retryDelayTime;

    /**
     * Create a new retrying tcp channel of the given type. The channel is
     * associated with an ssh connection. Channel types are
     * defined in <code>SSH2Connection</code> and starts with
     * <code>CH_TYPE</code>.
     *
     * @param channelType Type of channel to create.
     * @param connection The ssh connection to associate the channel with.
     * @param creator The object the channel is created from.
     * @param endpoint Socket the channel is connected to at the local end.
     * @param remoteAddr Remote server to connect to.
     * @param remotePort Remote port to connect to.
     * @param originAddr Originating host of local connection.
     * @param originPort Originating port of local connection.
     */
    public SSH2RetryingTCPChannel(int channelType, SSH2Connection connection,
                                  Object creator,
                                  Socket endpoint,
                                  String remoteAddr, int remotePort,
                                  String originAddr, int originPort)
    throws IOException {
        super(channelType, connection, creator,
              endpoint, remoteAddr, remotePort, originAddr, originPort);
        this.numOfRetries   = 3;
        this.retryDelayTime = 200L;
    }

    /**
     * Set number of retries to do.
     *
     * @param numOfRetries Number of retries.
     */
    protected void setRetries(int numOfRetries) {
        this.numOfRetries = numOfRetries;
    }

    /**
     * Set delay between retries.
     *
     * @param retryDelayTime Delay in seconds.
     */
    public void setRetryDelay(long retryDelayTime) {
        this.retryDelayTime = retryDelayTime;
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
                                      String langTag) {
        boolean retry = true;
        if(numOfRetries > 0) {
            if(getCreator() instanceof SSH2Listener) {
                try {
                    Thread.sleep(retryDelayTime);
                } catch (InterruptedException e) {}
                connection.getLog().notice("SSH2RetryingTCPChannel",
                                           "retry (" + numOfRetries +
                                           ") connection on ch. #" + getChannelId() +
                                           " to " + remoteAddr + ":" + remotePort);
                SSH2Listener listener = (SSH2Listener)getCreator();
                listener.sendChannelOpen(this, endpoint);
            } else {
                connection.getLog().error("SSH2RetryingTCPChannel",
                                          "openFailureImpl",
                                          "unexpected use of this class");
            }
        } else {
            outputClosed();
            retry = false;
        }
        numOfRetries--;

        return retry;
    }

}
