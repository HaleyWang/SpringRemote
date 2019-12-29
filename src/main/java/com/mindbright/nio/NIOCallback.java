/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone AB. All Rights Reserved.
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

package com.mindbright.nio;

import java.nio.ByteBuffer;

/**
 * Defines callback methods for non blocking io operations.
 *
 * @see Switchboard
 * @see NetworkConnection
 * @see NonBlockingInput
 */
public interface NIOCallback {
    /**
     * Called once the network read operation has been completed
     *
     * @param buf the buffer provided to the read call
     */
    public void completed(ByteBuffer buf);

    /**
     * Called if the read failed
     */
    public void readFailed(Exception e);

    /**
     * Called if the write failed
     */
    public void writeFailed();

    /**
     * Called once the connection has been established (assuming
     * interest for this has been registered by calling the
     * <code>NotifyWhenConnected</code> method of <code>Switchboard</code>).
     *
     * @param timeout true if the connection attempt timed out
     */
    public void connected(boolean timeout);

    /**
     * Called if the connection failed (assuming
     * interest for this has been registered by calling the
     * <code>NotifyWhenConnected</code> method of <code>Switchboard</code>).
     *
     * @param e the exception the connection failed with.
     */
    public void connectionFailed(Exception e);
}
