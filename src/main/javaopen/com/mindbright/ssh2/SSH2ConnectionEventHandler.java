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

import java.net.Socket;

/**
 * This interface is an event callback interface used to monitor the connection
 * layer of an ssh2 connection. It is used with the class
 * <code>SSH2Connection</code> to get info on the progress and status of all
 * forwards and resulting channels through the connection.
 * <p>
 * All callback methods which indicates channel open confirmation or channel
 * open failure uses the naming convention that when it begins with "local"
 * (e.g. <code>localDirectConnect</code>) it is coupled to a channel originating
 * locally and conversly when it begins with "remote"
 * (e.g. <code>remoteForwardedConnect</code>) it is coupled to a channel
 * originating remotely. The naming of the channel types in the connection
 * protocol specification is used to identify whether a channel is a local
 * forward or a remote forward. This means that local forwards are called
 * "direct" and remote forwards are called "forwarded". This naming can be
 * somewhat confusing for example the method <code>localForwardedConnect</code>
 * might be the one might expect to be called when a local forward channel is
 * confirmed to be open, instead the call is
 * <code>localDirectConnect</code>. The reason for this is to have a symmetrical
 * naming for all callbacks which are valid on both client and server side,
 * hence check each callback and see if it applies to youe need (i.e. client or
 * server).
 *
 * @see SSH2Connection
 * @see SSH2ConnectionEventAdapter
 * @see SSH2Listener
 * @see SSH2Channel
 */
public interface SSH2ConnectionEventHandler {
    // !!! TODO add globalRequest<type> calls... ???
    // public void globalRequest(SSH2Connection conn, String type);

    /**
     * Called when a new channel is added. That is when a new channel
     * has been opened through a port forward.
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was added
     *
     */
    public void channelAdded(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a channel is deleted. That is when a channel has
     * been finally removed.
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was deleted
     */
    public void channelDeleted(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a channel is connected to a <code>Socket</code>.
     *
     * @param originator the responsible listener/connector
     * @param channel    the created channel
     * @param fwdSocket  the socket which is connected to
     */
    public void channelConnect(Object originator, SSH2Channel channel,
                               Socket fwdSocket);

    /**
     * Called when a channel is closed. That is when the channel has
     * been closed and will be flushed and then removed.
     *
     * @param connection the connection layer responsible
     * @param channel the channel which was deleted
     */
    public void channelClosed(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a listener accepts a new connection. That is when a
     * local forward is opened. This callback indicates if a connection
     * should be handled or not by returning <code>true</code> or
     * <code>false</code>.
     *
     * @param  listener  the responsible listener
     * @param  fwdSocket the socket which resulted
     * @return <code>boolean</code> indicating wether to process connection or
     *         not.
     */
    public boolean listenerAccept(SSH2Listener listener, Socket fwdSocket);

    /**
     * Called on the server side when a remote forward channel is confirmed to
     * be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localForwardedConnect(SSH2Connection connection,
                                      SSH2Listener listener,
                                      SSH2Channel channel);
    /**
     * Called on the client side when a local forward channel is confirmed to
     * be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localDirectConnect(SSH2Connection connection,
                                   SSH2Listener listener,
                                   SSH2Channel channel);

    /**
     * Called on the client side when a session channel is confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param channel the channel which was opened
     */
    public void localSessionConnect(SSH2Connection connection,
                                    SSH2Channel channel);

    /**
     * Called on the server side when an X11 channel is confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localX11Connect(SSH2Connection connection,
                                SSH2Listener listener,
                                SSH2Channel channel);

    /**
     * Called on either side when a locally originating channel gets a channel
     * open failure indication from peer. See the class <code>SSH2</code> for
     * reason codes.
     *
     * @param connection  the connection layer responsible
     * @param channel     the channel which was opened
     * @param reasonCode  the reason code 
     * @param reasonText
     * @param languageTag
     *
     * @see SSH2
     */
    public void localChannelOpenFailure(SSH2Connection connection,
                                        SSH2Channel channel,
                                        int reasonCode, String reasonText,
                                        String languageTag);

    /**
     * Called on the client side when a remote forward channel has been
     * confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param remoteAddr
     * @param remotePort
     * @param channel    the channel which was opened
     */
    public void remoteForwardedConnect(SSH2Connection connection,
                                       String remoteAddr, int remotePort,
                                       SSH2Channel channel);

    /**
     * Called on the client side when a remote direct channel has been
     * confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was opened
     */
    public void remoteDirectConnect(SSH2Connection connection,
                                    SSH2Channel channel);

    /**
     * Called on the client side when a remote session channel has been
     * confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param remoteAddr
     * @param remotePort
     * @param channel    the channel which was opened
     */
    public void remoteSessionConnect(SSH2Connection connection,
                                     String remoteAddr, int remotePort,
                                     SSH2Channel channel);

    /**
     * Called on the client side when a remote X11 channel has been
     * confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was opened
     */
    public void remoteX11Connect(SSH2Connection connection,
                                 SSH2Channel channel);

    /**
     * Called on either side when there is a problem opening a remotely
     * originating channel resulting in a channel open failure indication beeing
     * sent back to peer. The exception which was the cause of the problem is
     * provided aswell as the type of channel and relevant addresses and ports.
     *
     * @param connection  the connection layer responsible
     * @param channelType the type of channel
     * @param targetAddr  the address which should have been connected to
     * @param targetPort  the port which should have been connected to
     * @param originAddr  the address where the channel originated (depends on type)
     * @param originPort  the port where the channel originated (depends on type)
     * @param cause       the exception which was the cause of the problem
     */
    public void remoteChannelOpenFailure(SSH2Connection connection,
                                         String channelType,
                                         String targetAddr, int targetPort,
                                         String originAddr, int originPort,
                                         SSH2Exception cause);

    /**
     * Called to set socket options on newly connected port forward channels
     *
     * @param channelType the type of the channel
     * @param s           socket to manipulate
     */
    public void setSocketOptions(int channelType, Socket s);

}
