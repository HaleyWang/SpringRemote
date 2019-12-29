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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

import com.mindbright.util.Util;

/**
 * Implements a simple telnet proxy. Listens to a local port and when
 * somebody connects it presents him with a prompt. The enters a
 * hostname, and optionally a port, and the proxy opens a connecting
 * to there through the ssh tunnel.
 */
public final class SSH2TelnetProxyListener implements Runnable {

    private final static int LISTEN_QUEUE_SIZE = 32;

    private SSH2Connection connection;
    private ServerSocket   listenSocket;
    private String         localAddr;
    private int            localPort;
    private String         prompt;

    private volatile boolean keepListening;

    /**
     * @param localAddr Local address to bind listener to.
     * @param localPort Port to listen at.
     * @param connection The ssh connection to use.
     */
    public SSH2TelnetProxyListener(String localAddr, int localPort,
                                   SSH2Connection connection)
    throws IOException {
        this(localAddr, localPort, connection, "\r\nremote host[:port] : ");
    }

    /**
     * @param localAddr Local address to bind listener to.
     * @param localPort Port to listen at.
     * @param connection The ssh connection to use.
     * @param prompt The prompt to use.
     */
    public SSH2TelnetProxyListener(String localAddr, int localPort,
                                   SSH2Connection connection, String prompt)
    throws IOException {
        this.localAddr     = localAddr;
        this.localPort     = localPort;
        this.connection    = connection;
        this.prompt        = prompt;
        this.listenSocket  = new ServerSocket(localPort, LISTEN_QUEUE_SIZE,
                                              InetAddress.getByName(localAddr));
        this.keepListening = true;

        Thread myThread = new Thread(this, "SSHTelnetPrompt2Listener_" +
                                     localAddr + ":" + localPort);
        myThread.setDaemon(true);
        myThread.start();
    }

    /**
     * The thread running this gets created in the constructor. So
     * there is no need to call this function explicitely.
     */
    public void run() {
	Socket fwdSocket = null;
	try {
            while(keepListening) {

                try {
                    fwdSocket = listenSocket.accept();
                } catch (InterruptedIOException e) {
                    if(keepListening)
                        continue;
		}

                if(keepListening == false)
                    break;

                InputStream  in  = fwdSocket.getInputStream();
                OutputStream out = fwdSocket.getOutputStream();

                out.write(prompt.getBytes());

                String remoteHost = readLine(in).trim();
                int    remotePort;

                remotePort = Util.getPort(remoteHost, 23);
                remoteHost = Util.getHost(remoteHost);

                InetAddress originAddr = fwdSocket.getInetAddress();
                int         originPort = fwdSocket.getPort();

                SSH2TCPChannel channel =
                    new SSH2TCPChannel(SSH2Connection.CH_TYPE_DIR_TCPIP,
                                       connection, this,
                                       fwdSocket,
                                       remoteHost, remotePort,
                                       originAddr.getHostName(), originPort);

                connection.connectLocalChannel(channel,
                                               remoteHost, remotePort,
                                               originAddr.getHostAddress(),
                                               originPort);
            }
        } catch(IOException e) {
            if(keepListening) {
                connection.getLog().error("SSH2TelnetProxyListener", "run",
                                          "Error in accept for listener " +
                                          localAddr + ":" + localPort + " : " +
                                          e.getMessage());
            }
        } finally {
	    try {
		if (fwdSocket != null)
		    fwdSocket.close();
	    } catch (IOException e) { }
            try {
                listenSocket.close();
            } catch (IOException e) { /* don't care */
            }
            listenSocket = null;

            connection.getLog().debug("SSH2TelnetProxyListener",
                                      "stopping listener on " +
                                      localAddr + ":" + localPort);
        }

    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder lineBuf = new StringBuilder();
        int c;
        while(true) {
            c = in.read();
            if(c == -1)
                throw new IOException("Client closed connection");
            if(c != '\n') {
                lineBuf.append((char)c);
            } else {
                break;
            }
        }
        return new String(lineBuf);
    }

    public void stop() {
        keepListening = false;
        if(listenSocket != null) {
            /*
             * Ouch! Kludge to be sure the listenSocket.accept() don't hang
             * which it does on some buggy JVM's
             */
            Socket s = null;
            try {
                s = new Socket(listenSocket.getInetAddress(),
                               listenSocket.getLocalPort());
            } catch (Exception e) {}
            finally {
                try {
                    s.close();
                } catch (Exception e) { }
            }
        }
    }

}
