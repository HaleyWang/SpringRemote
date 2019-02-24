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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

/**
 * This class accepts connections to a single address/port pair for creating
 * channels through port forwards. It contains a thread which basically contains
 * an accept loop in which new connections are accepted and new channels are
 * created along with CHANNEL_OPEN messages to peer. There is one
 * <code>SSH2Listener</code> instance for each local forward.
 *
 * @see SSH2Connection
 */
public final class SSH2Listener implements Runnable {
    private final static int LISTEN_QUEUE_SIZE = 32;

    SSH2Connection          connection;
    SSH2StreamFilterFactory filterFactory;

    private int     acceptTimeout;
    private boolean isLocalForward;
    private int     channelType;

    private String localAddr;
    private int    localPort;
    private String remoteAddr;
    private int    remotePort;

    private ServerSocket listenSocket;

    private int acceptCount;
    private int acceptMax;

    private volatile int numOfRetries;
    private long         retryDelayTime;

    private volatile boolean keepListening;

    private Thread myThread;

    /**
     * Creates a listener for filtered connections.
     *
     * @param localAddr The address to open the listener to. should
     * normally be "127.0.0.1".
     * @param localPort The local port to listen at.
     * @param remoteAddr The remote address to connect to. Note that
     * this name is resolved on the server.
     * @param remotePort The remote port to connect to.
     * @param connection The connection to use.
     * @param filterFactory Factory which creates filter
     * instances. There will be one filter created per connection.
     * @param acceptTimeout Timeout for accept call.
     */
    public SSH2Listener(String localAddr, int localPort,
                        String remoteAddr, int remotePort,
                        SSH2Connection connection,
                        SSH2StreamFilterFactory filterFactory,
                        int acceptTimeout)
    throws IOException {

        this.localAddr     = localAddr;
        this.localPort     = localPort;
        this.remoteAddr    = remoteAddr;
        this.remotePort    = remotePort;
        this.connection    = connection;
        this.filterFactory = filterFactory;
        this.keepListening = true;
        this.acceptCount   = 0;
        this.acceptMax     = 0;
        this.acceptTimeout = acceptTimeout;
        this.numOfRetries  = 0;

        this.listenSocket = ServerSocketChannel.open().socket();
        this.listenSocket.bind
            (new InetSocketAddress(InetAddress.getByName(localAddr), localPort), LISTEN_QUEUE_SIZE);

        if(this.acceptTimeout != 0) {
            this.listenSocket.setSoTimeout(this.acceptTimeout);
        }

        if (localPort == 0) {
            this.localPort = listenSocket.getLocalPort();
            connection.getLog().debug("SSH2Listener",
                                      "we got assigned port " + this.localPort);
        }
        
        this.isLocalForward = (remoteAddr != null);

        if(this.isLocalForward) {
            this.channelType = SSH2Connection.CH_TYPE_DIR_TCPIP;
        } else {
            this.channelType = SSH2Connection.CH_TYPE_FWD_TCPIP;
        }

        this.myThread = new Thread(this, "SSH2Listener_" + localAddr + ":" +
                                   this.localPort);
        this.myThread.setDaemon(true);
        this.myThread.start();
    }

    /**
     * Creates a listener for filtered connections.
     *
     * @param localAddr The address to open the listener to. should
     * normally be "127.0.0.1".
     * @param localPort The local port to listen at.
     * @param remoteAddr The remote address to connect to. Note that
     * this name is resolved on the server.
     * @param remotePort The remote port to connect to.
     * @param connection The connection to use.
     * @param filterFactory Factory which creates filter
     * instances. There will be one filter created per connection.
     */
    public SSH2Listener(String localAddr, int localPort,
                        String remoteAddr, int remotePort,
                        SSH2Connection connection,
                        SSH2StreamFilterFactory filterFactory)
    throws IOException {
        this(localAddr, localPort, remoteAddr, remotePort,
             connection, filterFactory, 0);
    }

    /**
     * Creates the remote listener for remote connections.
     *
     * @param localAddr The address to open the listener to. should
     * normally be "127.0.0.1".
     * @param localPort The local port to listen at.
     * @param connection The connection to use.
     */
    public SSH2Listener(String localAddr, int localPort,
                        SSH2Connection connection) throws IOException {
        this(localAddr, localPort, null, -1, connection, null, 0);
    }

    /**
     * Run the listener and listen for connections.
     */
    public void run() {
        try {
            connection.getLog().debug("SSH2Listener",
                                      "starting listener on " +
                                      localAddr + ":" + localPort);

            while(keepListening) {
                Socket fwdSocket = null;

                try {
                    fwdSocket = listenSocket.accept();
                } catch (InterruptedIOException e) {
                    if (!connection.getTransport().isConnected())
                        keepListening = false;
                    if (keepListening)
                        continue;
                }

                if (keepListening == false) {
                    try {
                        if (fwdSocket != null)
                            fwdSocket.close();
                    } catch (IOException e) { }
                    break;
                }

                if(connection.getEventHandler().listenerAccept(this, fwdSocket))
                    doConnect(fwdSocket);

                acceptCount++;
                synchronized (this) {
                    if(acceptCount == acceptMax) {
                        keepListening = false;
                    }
                }
            }

        } catch(IOException e) {
            if(keepListening) {
                connection.getLog().error("SSH2Listener", "run",
                                          "Error in accept for listener " +
                                          localAddr + ":" + localPort + " : " +
                                          e.getMessage());
            }
        } finally {
            try {
                listenSocket.close();
                keepListening = false;
            } catch (IOException e) { /* don't care */
            }
            listenSocket = null;

            connection.getLog().debug("SSH2Listener",
                                      "stopping listener on " +
                                      localAddr + ":" + localPort);

        }
    }

    /**
     * Handle a connect from the given socket. This is used to sneak
     * in connections from other parts of the application.
     *
     * @param fwdSocket A connected socket.
     */
    public void doConnect(Socket fwdSocket) {
        InetAddress originAddr = fwdSocket.getInetAddress();
        int         originPort = fwdSocket.getPort();

        try {
            SSH2TCPChannel channel = null;
            if(numOfRetries > 0) {
                SSH2RetryingTCPChannel retryChan =
                    new SSH2RetryingTCPChannel(channelType, connection, this,
                                               fwdSocket,
                                               remoteAddr, remotePort,
                                               originAddr.getHostName(),
                                               originPort);
                retryChan.setRetries(numOfRetries);
                if(retryDelayTime > 0) {
                    retryChan.setRetryDelay(retryDelayTime);
                }
                channel = retryChan;
            } else {
                channel =
                    new SSH2TCPChannel(channelType, connection, this,
                                       fwdSocket,
                                       remoteAddr, remotePort,
                                       originAddr.getHostName(), originPort);
            }

            connection.getLog().notice("SSH2Listener",
                                       "connect from: " +
                                       originAddr.getHostAddress() + ":" +
                                       originPort + " on " +
                                       localAddr + ":" + localPort +
                                       ", new ch. #" + channel.getChannelId());

            connection.setSocketOptions(SSH2Preferences.SOCK_OPT_LOCAL +
                                        localAddr + "." + localPort, fwdSocket);

            connection.getEventHandler().channelConnect(this, channel,
                    fwdSocket);

            if(filterFactory != null) {
                channel.applyFilter(filterFactory.createFilter(connection,
                                    channel));
            }

            sendChannelOpen(channel, fwdSocket);

        } catch(IOException e) {
            connection.getLog().error("SSH2Listener", "doConnect",
                                      "Error in  " +
                                      localAddr + ":" + localPort + " : " +
                                      e.getMessage());
        }
    }

    /**
     * Send a channel open for the given socket. This is useful when
     * one wants to retry to open a channel when previous attempts
     * have failed. One must call <code>doConnect</code> once first
     * before calling this function.
     *
     * @param channel The channel to retry to open on.
     * @param fwdSocket Socket identifying the local end.
     */
    public void sendChannelOpen(SSH2TCPChannel channel, Socket fwdSocket) {
        String      originAddr = fwdSocket.getInetAddress().getHostAddress();
        int         originPort = fwdSocket.getPort();
        String      remoteAddr;
        int         remotePort;

        if(isLocalForward) {
            remoteAddr = this.remoteAddr;
            remotePort = this.remotePort;
        } else {
            remoteAddr = localAddr;
            remotePort = localPort;
        }

        connection.connectLocalChannel(channel,
                                       remoteAddr, remotePort,
                                       originAddr, originPort);
    }

    public SSH2Connection getConnection() {
        return connection;
    }

    public synchronized void setAcceptMax(int acceptMax) {
        this.acceptMax = acceptMax;
    }

    public void setRetries(int numOfRetries) {
        this.numOfRetries = numOfRetries;
    }

    public void setRetryDelay(long retryDelayTime) {
        this.retryDelayTime = retryDelayTime;
    }

    public void setThreadPriority(int prio) {
        myThread.setPriority(prio);
    }

    public int getListenPort() {
        return localPort;
    }

    public String getListenHost() {
        return listenSocket.getInetAddress().getHostAddress();
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getRemoteHost() {
        return remoteAddr;
    }

    /**
     * Stop the listener. That is do not accept any more
     * connections. Existing connections will not be affected.
     */
    public void stop() {
        if(listenSocket != null && keepListening) {
            keepListening = false;
            /*
             * Ouch! Kludge to be sure the listenSocket.accept() don't hang
             * which it does on some buggy JVM's
             */

            Socket s = null;
            try {
                String addr = listenSocket.getInetAddress().getHostAddress();
                if(addr.equals("0.0.0.0")) {
                    addr = InetAddress.getLocalHost().getHostAddress();
                }
                s = new Socket(addr, localPort);
            } catch (Exception e) {}
            finally {
                try {
                    s.close();
                } catch (Exception e) { }
            }
        }
    }

}
