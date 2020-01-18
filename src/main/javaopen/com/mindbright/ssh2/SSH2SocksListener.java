/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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
import java.net.Socket;
import java.net.ServerSocket;

/**
 * This class together with <code>SSH2SocksChannel</code> implements
 * a simple SOCKS proxy server that open port forwards for each CONNECT
 * packet. Only TCP is supported.
 *
 * @see SSH2SocksChannel
 */

public class SSH2SocksListener implements Runnable {
    private final static int LISTEN_QUEUE_SIZE = 32;

    private SSH2Connection connection;
    private ServerSocket listenSocket;
    private String listenAddr;
    private int listenPort;
    
    private Thread myListener;
    private boolean keepListening = true;

    /**
     * Creates a SOCKS proxy server listening on the specified port and
     * address.
     *
     * @param listenAddr The address to open the listener to. should
     * normally be "127.0.0.1".
     * @param listenPort The local port to listen at.
     * @param connection The connection to use.
     */    
    public SSH2SocksListener(String listenAddr, int listenPort,
                             SSH2Connection connection) 
        throws IOException {
        
        this.connection = connection;

        this.listenPort = listenPort;
        this.listenAddr = listenAddr;

        connection.getLog().debug("SSH2SocksListener",
                                  "creating listener on " +
                                  listenAddr + ":" + listenPort);

 	this.listenSocket = new ServerSocket(listenPort, LISTEN_QUEUE_SIZE,
					     InetAddress.getByName(listenAddr));
        
	this.myListener = new Thread(this, "SSH2SocksListener_" + listenAddr + ":" +
                                     listenPort);
	this.myListener.setDaemon(true);
	this.myListener.start();
    }

    
    public void stop() {
	if (listenSocket != null && keepListening) {
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
		s = new Socket(addr, listenSocket.getLocalPort());
            } catch (Exception e) {
            } finally {
                try { s.close(); } catch (Exception e) { }
            }
	}
    }


    public void run() {
	try {
	    connection.getLog().debug("SSH2SocksListener",
				      "starting listener on " +
				      listenAddr + ":" + listenPort);
	    Socket fwdSocket = null;
	    try {
		while (keepListening) {    
		    try {
			fwdSocket = listenSocket.accept();
		    } catch (InterruptedIOException e) {
			if (keepListening)
			    continue;
		    }
		    
		    if (keepListening) 
			doConnect(fwdSocket);
		}
	    } finally {
		try { 
		    if (fwdSocket != null)
			fwdSocket.close(); 
		} catch (IOException e) { }
	    }
	} catch (IOException e) {
	    if (keepListening) {
		connection.getLog().error("SSH2SocksListener", "run",
					  "Error in accept for listener " +
					  listenAddr + ":" + listenPort + " : " +
					  e.getMessage());
	    }
	} finally {
	    try {
		listenSocket.close();
		keepListening = false;
	    } catch (IOException e) { /* don't care */ }
	    listenSocket = null;
            
	    connection.getLog().debug("SSH2SocksListener",
				      "stopping listener on " +
				      listenAddr + ":" + listenPort);
	}
    }


    private void doConnect(Socket s) {
        new SSH2SocksChannel(s, connection);
    }    
}
