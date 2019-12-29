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

import java.io.IOException;

import com.mindbright.nio.NetworkConnection;

/**
 * This interface is used when explicitly connection with the version 1
 * protocol. No new clients should be built based on this.
 */
public interface SSHClientUser {
    /**
     * Get host to connect to
     */
    public String  getSrvHost() throws IOException;

    /**
     * Get port number to connect to
     */
    public int     getSrvPort();

    /**
     * Return a connection to the server. This can be used to connect
     * through proxies etc.
     */
    public NetworkConnection getProxyConnection() throws IOException;

    /**
     * Get the display for X11 forwardings
     */
    public String  getDisplay();

    /**
     * get maximum packet size (0 = no limit)
     */
    public int     getMaxPacketSz();

    /**
     * Get alive interval (0 = do not send keepalive packets)
     */
    public int     getAliveInterval();

    /**
     * Get desired level of compression
     */
    public int     getCompressionLevel();

    /**
     * Timeout when connecting to server (in seconds)
     */
    public int     getConnectTimeout();

    /**
     * Timeout when waiting for initial greeting from server (in seconds)
     */
    public int     getHelloTimeout();

    /**
     * Timeout of key exchange (in seconds)
     */
    public int     getKexTimeout();

    /**
     * Return true if X11 forwarding is desired
     */
    public boolean wantX11Forward();

    /**
     * Return true if we need a PTY on the server
     */
    public boolean wantPTY();

    /**
     * Get interactor which should handle the authentication phase
     */
    public SSHInteractor getInteractor();
}
