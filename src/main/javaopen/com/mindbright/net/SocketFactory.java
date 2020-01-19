/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;

/**
 * Opens and connects a new SocketChannel with an optional timeout.
 */
public class SocketFactory {

    /**
     * Connects to the given server.
     *
     * @param ia        Address of server to connect to
     * @param port      Port to connect to
     *
     * @return A SocketChannel connected to the given address
     *
     * @throws IOException If there are any errors during the
     *                     connection.
     */
    public static SocketChannel newSocket(InetAddress ia, int port)
        throws IOException {
        return newSocket(ia, port, 0);
    }

    /**
     * Connects to the given server.
     *
     * @param host      Name of server to connect to
     * @param port      Port to connect to
     *
     * @return A SocketChannel connected to the given address
     *
     * @throws IOException If there are any errors during the
     *                     connection.
     */
    public static SocketChannel newSocket(String host, int port) 
        throws UnknownHostException, IOException {
        return newSocket(host, port, 0);
    }

    /**
     * Connects to the given server.
     *
     * @param host      Name of server to connect to
     * @param port      Port to connect to
     * @param mstimeout How many millisecondse to wait before giving up 
     *
     * @return A SocketChannel connected to the given address
     *
     * @throws IOException If there are any errors during the
     *                     connection or if the connection has not
     *                     been established before mstimeout
     *                     milliseconds have passwd.
     */
    public static SocketChannel newSocket(String host, int port,long mstimeout) 
        throws UnknownHostException, IOException {
        return newSocket(InetAddress.getByName(host), port, 0);
    }

    /**
     * Connects to the given server.
     *
     * @param ia        Address of server to connect to
     * @param port      Port to connect to
     * @param mstimeout How many millisecondse to wait before giving up 
     *
     * @return A SocketChannel connected to the given address
     *
     * @throws IOException If there are any errors during the
     *                     connection or if the connection has not
     *                     been established before mstimeout
     *                     milliseconds have passwd.
     */
    @SuppressWarnings("deprecation")
	public static SocketChannel newSocket(InetAddress ia, int port,
                                          long mstimeout) throws IOException {
        final InetSocketAddress sa = new InetSocketAddress(ia, port);
        if (mstimeout <= 0) return SocketChannel.open(sa);
        
        final Vector<Object> v = new Vector<Object>();
        Thread connector = new Thread(new Runnable() {
            public void run() {
                SocketChannel s;
                try {
                    s = SocketChannel.open(sa);
                    v.addElement(s);
                } catch (IOException e) {
                    v.addElement(e);
                }
            }
        }, "SocketFactory.connector[" + ia + ", " + port + "]");
        connector.start();

        for (long t=0; t<mstimeout/100; t++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            
            if (v.size() > 0)
                break;
        }
        if (v.size() > 0) {
            Object o = v.firstElement();
            if (o instanceof SocketChannel)
                return (SocketChannel)o;
            throw (IOException)o;
        }
 
        try { 
            connector.stop();
        } catch (Throwable t){
        }
        
        throw new IOException("timeout when connecting");
    }    
}
