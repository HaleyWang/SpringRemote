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

import java.net.*;
import java.io.*;

public class SSHListenChannel extends SSHChannel {
    static boolean allowRemoteConnect = false;

    static final int LISTEN_QUEUE_SIZE = 16;

    SSHChannelController controller;

    ServerSocket listenSocket;
    String       remoteHost;
    int          remotePort;
    InetAddress  localHost1;
    InetAddress  localHost2;

    boolean temporaryListener;

    public SSHListenChannel(String localHost, int localPort,
                            String remoteHost, int remotePort,
                            SSHChannelController controller)
    throws IOException {
        super(SSH.LISTEN_CHAN_NUM);
        this.controller         = controller;
        try {
            this.listenSocket = new ServerSocket(localPort, LISTEN_QUEUE_SIZE,
                                                 InetAddress.getByName(localHost));
        } catch (IOException e) {
            throw new IOException("Error setting up local forward on port " +
                                  localPort + ", " + e.getMessage());
        }
        this.remoteHost         = remoteHost;
        this.remotePort         = remotePort;

        this.localHost1 = InetAddress.getLocalHost();
        this.localHost2 = InetAddress.getByName("127.0.0.1");
    }

    public int getListenPort() {
        return listenSocket.getLocalPort();
    }

    public String getListenHost() {
        return listenSocket.getInetAddress().getHostAddress();
    }

    public static synchronized void setAllowRemoteConnect(boolean val) {
        allowRemoteConnect = val;
    }

    static synchronized boolean getAllowRemoteConnect() {
        return allowRemoteConnect;
    }

    public SSHTunnel newTunnel(Socket ioSocket, int channelId, int remoteChannelId,
                               SSHChannelController controller) throws IOException {
        return new SSHTunnel(ioSocket, channelId, remoteChannelId, controller);
    }

    public void setTemporaryListener(boolean val) {
        temporaryListener = val;
    }

    public void serviceLoop() throws IOException {

        SSH.log("Starting listen-chan: " + listenSocket.getLocalPort());

        try {
            for(;;) {
                Socket  fwdSocket = listenSocket.accept();

                if(!getAllowRemoteConnect() &&
                        !(fwdSocket.getInetAddress().equals(localHost1) ||
                          fwdSocket.getInetAddress().equals(localHost2))) {
                    controller.alert("Remote connect to local tunnel rejected: " + fwdSocket.getInetAddress());
                    fwdSocket.close();
                    continue;
                }

                SSHPduOutputStream respPdu =
                    new SSHPduOutputStream(SSH.MSG_PORT_OPEN,
                                           controller.sndCipher,
                                           controller.sndComp,
                                           controller.secureRandom());

                int newChan      = controller.newChannelId();
                SSHTunnel tunnel = newTunnel(fwdSocket,
                                             newChan, SSH.UNKNOWN_CHAN_NUM,
                                             controller);
                controller.addTunnel(tunnel);
                tunnel.setRemoteDesc(remoteHost + ":" + remotePort);

                respPdu.writeInt(newChan);
                respPdu.writeString(remoteHost);
                respPdu.writeInt(remotePort);

                SSH.log("got connect for: " + remoteHost + " : " + remotePort + ", " + newChan);

                //
                // !!! TODO: check this !!! if(controller.haveHostInFwdOpen())
                //
                respPdu.writeString(fwdSocket.getInetAddress().getHostAddress());

                controller.transmit(respPdu);

                if(temporaryListener)
                    break;
            }
        } finally {
            listenSocket.close();
        }
    }

    @SuppressWarnings("deprecation")
	public void forceClose() {
        if(isAlive()) {
            stop();
        }
        try {
            listenSocket.close();
        } catch (IOException e) {}
    }

}
