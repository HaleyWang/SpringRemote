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

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import java.net.InetAddress;
import java.net.SocketException;


import java.net.InetSocketAddress;

/**
 * Holds a non-blocking internet socket.
 */
public class NetworkConnection {

    private SocketChannel _channel;
    private Switchboard _switchboard;
    private NonBlockingInput _input;
    private NonBlockingOutput _output;
    private int _readtimeout = 0;

    /**
     * Creates a new NetworkConnection based on the provided SocketChannel.
     * The SocketChannel must be connected or at least connecting.
     */
    public NetworkConnection(SocketChannel channel) 
        throws IOException {
        this((Switchboard)null, channel);
    }

    /**
     * Creates a network connection to the specified host and port.
     * This call will return directly and the progress can be monitored via the
     * {@link Switchboard#notifyWhenConnected Switchboard.notifyWhenConnected}
     * method.
     *
     * @param host  the host name
     * @param port  the port to connect to
     *
     * @see Switchboard
     */
    public static NetworkConnection open(String host, int port) 
        throws IOException {
        return open(InetAddress.getByName(host), port, false);
    }

    /**
     * Creates a network connection to the specified host and port.
     * If <code>block> is false then the call will return directly
     * and the progress can be monitored via the
     * {@link Switchboard#notifyWhenConnected Switchboard.notifyWhenConnected}
     * method.
     *
     * @param host  the host name
     * @param port  the port to connect to
     * @param block true if the call should block until the connection
     *              is established
     *
     * @see Switchboard
     */
    public static NetworkConnection open(String host, int port, boolean block) 
        throws IOException {
        return open(InetAddress.getByName(host), port, block);
    }

    /**
     * Creates a network connection to the specified host and port.
     * This call will return directly and the progress can be monitored via the
     * {@link Switchboard#notifyWhenConnected Switchboard.notifyWhenConnected}
     * method.
     *
     * @param address IP-address of remote server
     * @param port    the port to connect to
     *
     * @see Switchboard
     */
    public static NetworkConnection open(InetAddress address, int port) 
        throws IOException {
        return open(address, port, false);
    }

    /**
     * Creates a network connection to the specified address and port.
     * If <code>block</code> is false then the call will return directly
     * and the progress can be monitored via the
     * {@link Switchboard#notifyWhenConnected Switchboard.notifyWhenConnected}
     * method.
     *
     * @param address IP-address of remote server
     * @param port    the port to connect to
     * @param block   true if the call should block until the connection
     *                is established
     *
     * @see Switchboard
     */
    public static NetworkConnection open(InetAddress address, int port,
                                         boolean block) throws IOException {
        return open(null, address, port, block);
    }

    /**
     * Creates a network connection to the specified switchboard,
     * address and port.
     * If <code>block</code> is false then the call will return directly
     * and the progress can be monitored via the
     * {@link Switchboard#notifyWhenConnected Switchboard.notifyWhenConnected}
     * method.
     *
     * @param address IP-address of remote server
     * @param port    the port to connect to
     * @param block   true if the call should block until the connection
     *                is established
     *
     * @see Switchboard
     */
    public static NetworkConnection open(Switchboard s,
                                         InetAddress address, int port,
                                         boolean block) throws IOException {
        InetSocketAddress sock = new InetSocketAddress(address, port);
        SocketChannel channel = null;

        try {
            channel = SocketChannel.open();
            channel.configureBlocking(block);
            channel.connect(sock);
            return new NetworkConnection(s, channel);
        } catch (IOException e) {
            if (channel != null) {
                channel.close();
            }
            throw e;
        }
    }

    public NetworkConnection(Switchboard s, SocketChannel channel) 
        throws IOException {

        // Sanity check
        if (!channel.isConnected() && !channel.isConnectionPending()) {
            throw new IOException("Channel must be connecting");
        }

        if (s == null) {
            s = Switchboard.getSwitchboard();
        }

        _channel     = channel;
        _switchboard = s;
        _input       = new NonBlockingInput(_switchboard, _channel);
        _output      = new NonBlockingOutput(_switchboard, _channel);
    }

    /**
     * Returns the switchboard implementation which regulates this
     * NetworkConnection instance.
     */
    public Switchboard getSwitchboard() {
        return _switchboard;
    }

    /**
     * Close the connection.
     * @see Socket
     */
    public void close() {
        try {
            try {
                _channel.socket().shutdownInput();
            } catch (Throwable t) {}
            _switchboard.close (_channel);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Returns the remote port to which this socket is connected.
     * @see Socket
     */
    public int getPort() {
        return _channel.socket().getPort();
    }

    /**
     * Returns the local port to which this socket is bound.
     * @see Socket
     */
    public int getLocalPort() {
        return _channel.socket().getLocalPort();
    }

    /**
     * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
     * @see Socket
     */
    public void setTcpNoDelay(boolean delay) throws SocketException {
        _channel.socket().setTcpNoDelay(delay);
    }

    /**
     * Returns setting for SO_TIMEOUT.
     * @see Socket
     */
    public int getSoTimeout() {
        return _readtimeout;
    }

    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
     * @see Socket
     */
    public void setSoTimeout(int timeout) {
        _readtimeout = timeout;
    }

    /**
     * Returns the address to which the socket is connected.
     * @see Socket
     */
    public InetAddress getInetAddress() {
        return _channel.socket().getInetAddress();
    }

    /**
     * Gets the local address to which the socket is bound.
     * @see Socket
     */
    public InetAddress getLocalAddress() {
        return _channel.socket().getLocalAddress();
    }

    /**
     * Disables the output stream for this socket.
     * @see Socket
     */
    public void shutdownOutput() throws IOException {
        _channel.socket().shutdownOutput();
    }

    /**
     * Get the input stream from which one can read data which arrives
     * on this socket.
     */
    public NonBlockingInput getInput() {
        return _input;
    }

    /**
     * Get the output stream to which one can write data to the remote
     * end of this socket.
     */
    public NonBlockingOutput getOutput() {
        return _output;
    }

    /**
     * Get the underlying SocketChannel
     */
    public SocketChannel getChannel() {
        return _channel;
    }

    /**
     * Get the underlying socket. Note that you should be careful in
     * operating directly on the socket of a NetworkConnection. Great
     * confusion is bound to happen if you mix direct socket
     * operations with nio operations.
     *
     * As a side effect this will configure the channel to be blocking.
     *
     * @return a socket or null if there is no socket underlying the
     * network connection.
     */
    public Socket getSocket() throws IOException {
        _channel.configureBlocking(true);
        return _channel.socket();
    }

    public String toString() {
        Socket s = _channel.socket();

        return s.getLocalAddress() + ":" + s.getLocalPort() + " -> " + s.getInetAddress() + ":" + s.getPort();
    }
}
