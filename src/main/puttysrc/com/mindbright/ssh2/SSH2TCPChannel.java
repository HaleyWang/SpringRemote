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
import java.net.InetAddress;

/**
 * This is a subclass of <code>SSH2StreamChannel</code> which
 * implements channels which are connected to TCP streams at both
 * ends.
 */
public class SSH2TCPChannel extends SSH2StreamChannel {

    protected Socket endpoint;
    protected String originAddr;
    protected int    originPort;
    protected String remoteAddr;
    protected int    remotePort;

    /**
     * Create a new tcp channel of the given type. The channel is
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
    public SSH2TCPChannel(int channelType, SSH2Connection connection,
                          Object creator,
                          Socket endpoint,
                          String remoteAddr, int remotePort,
                          String originAddr, int originPort)
    throws IOException {
        super(channelType, connection, creator, endpoint);

        this.endpoint   = endpoint;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.originAddr = originAddr;
        this.originPort = originPort;
    }

    protected void outputClosed() {
        // No need to close socket here, this will done through the SocketChannels by parent class
        endpoint = null;
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
                                      String langTag) {
        if (endpoint != null) {
            try {
                endpoint.close();
            } catch (IOException e) { /* don't care */
            }
        }
        outputClosed();
        return false;
    }

    /**
     * Gets the address of the enpoint. That is the address to which
     * the local TCP socket is connected.
     */
    public InetAddress getAddress() {
        return endpoint.getInetAddress();
    }

    /**
     * Gets the port of the enpoint. That is the port to which
     * the local TCP socket is connected.
     */
    public int getPort() {
        return endpoint.getPort();
    }

    /**
     * Get the address the server is supposed to be connected to.
     */
    public String getRemoteAddress() {
        return remoteAddr;
    }

    /**
     * Get the port the server is supposed to be connected to.
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * Gets the origin address which was given in the
     * constructor. This should return the same address as returned by
     * <code>getAddress</code>.
     */
    public String getOriginAddress() {
        return originAddr;
    }

    /**
     * Gets the origin port which was given in the
     * constructor. This should return the same port as returned by
     * <code>getPort</code>.
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
        String desc = "<N/A>";
        switch(channelType) {
        case SSH2Connection.CH_TYPE_FWD_TCPIP:
            desc = "[remote] " + originAddr + ":" + originPort + " <--> " +
                   getRemoteAddress() + ":" + getRemotePort() + " <--ssh2--> " +
                   getAddress().getHostAddress() + ":" + getPort();
            break;
        case SSH2Connection.CH_TYPE_DIR_TCPIP:
            SSH2Listener l = (SSH2Listener)creator;
            desc = "[local] " + originAddr + ":" + originPort + " <--> " +
                   l.getListenHost() + ":" + l.getListenPort() + " <--ssh2--> " +
                   getRemoteAddress() + ":" + getRemotePort();
            break;
        default:
            System.out.println("!!! NOT SUPPORTED IN SSH2TCPChannel.toString !!!");
            break;
        }
        return desc;
    }

}
