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
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Implements support for FTP over SSH. This implemntation sits on the
 * client side of the FTP command stream and opens new port forwards as
 * needed for the various data transfers. No server-side proxy is needed.
 */
public class SSH2FTPProxyFilter implements SSH2StreamFilter,
    SSH2StreamFilterFactory {
    class FTPInput extends FilterInputStream {

        Random portRandomizer;
        int    lastBoundPort;

        public FTPInput(InputStream toBeFiltered) {
            super(toBeFiltered);
            portRandomizer = new Random();
            lastBoundPort  = -1;
        }

        public int read(byte b[], int off, int len) throws IOException {
            len = in.read(b, off, len);
            if(len < 0) {
                return len;
            }
            String msg = new String(b, off, len);
            msg = msg.toLowerCase();
            if (msg.startsWith("pasv")) {
                waitingPASVResponse = true;
            } else if (msg.startsWith("port ")) {
                msg = msg.substring(5);
                int[] d = new int[6];
                if(parseHostAndPort(msg, d)) {
                    String toHost = d[0] + "." + d[1] + "." + d[2] + "." + d[3];
                    int    toPort = (d[4] << 8) | d[5];

                    if(lastBoundPort > 0) {
                        connection.deleteRemoteForward(serverLocalAddr.
                                                       getHostAddress(),
                                                       lastBoundPort);
                    }

                    int port = 0;
                    boolean success = false;
                    while(!success) {
                        port = portRandomizer.nextInt();
			if (port < 0)
			    port = -port;
                        port %= (65536 - 1024);
                        port += 1024;
                        success =
                            connection.newRemoteForwardBlocking(serverLocalAddr.
                                                                getHostAddress(),
                                                                port,
                                                                toHost, toPort,
                                                                null);
                    }
                    lastBoundPort = port;

                    int p1 = (port >>> 8) & 0xff;
                    int p2 = port & 0xff;

                    msg = "PORT " + localAddrPORTStr + "," +
                          p1 + "," + p2 + "\n";
                    byte[] newmsg = msg.getBytes();
                    len = newmsg.length;
                    System.arraycopy(newmsg, 0, b, off, len);
                } else {
                    connection.getLog().warning("SSH2FTPProxyFilter",
                                                "error in FTP proxy filter (port) for: " +
                                                msg);
                }
            }
            return len;
        }

    }

    class FTPOutput extends FilterOutputStream {

        public FTPOutput(OutputStream toBeFiltered) {
            super(toBeFiltered);
        }

        public void write(byte b[], int off, int len) throws IOException {
            SSH2Listener listener = null;
            String msg = new String(b, off, len);

            if(waitingPASVResponse && msg.startsWith("227 ")) {
                byte[] newmsg;
                waitingPASVResponse = false;
                msg = msg.substring(27);
                int[] d = new int[6];
                if(parseHostAndPort(msg, d)) {
                    String toHost = d[0] + "." + d[1] + "." + d[2] + "." + d[3];
                    int    toPort = (d[4] << 8) | d[5];
                    int    localPort;
                    try {
                        listener = connection.newLocalForward(
                        		localHost.getHostAddress(),
                                0, toHost, toPort);
                        // !!! TODO should have a blocking listener.ready()
                        Thread.sleep(250);
                    } catch(InterruptedException e) {
                        connection.getLog().warning("SSH2FTPProxyFilter",
                                                    "interrupted in FTP proxy filter: " +
                                                    e.toString());
                        listener = null;
                    } catch(IOException e) {
                        connection.getLog().warning("SSH2FTPProxyFilter",
                                                    "error in FTP proxy filter: " +
                                                    e.toString());
                    }
                    if(listener != null) {
                        listener.setAcceptMax(1); // Go away after next accept...
                        localPort = listener.getListenPort();
                        int p1 = (localPort >>> 8) & 0xff;
                        int p2 = localPort & 0xff;
                        msg = "227 Entering Passive Mode (" +
                              localAddrPASVStr + "," + p1 + "," + p2 + ")\n";
                        newmsg = msg.getBytes();
                        out.write(newmsg, 0, newmsg.length);
                    }
                } else {
                    connection.getLog().warning("SSH2FTPProxyFilter",
                                                "error in FTP proxy filter (pasv) for: " +
                                                msg);
                }
            } else {
                out.write(b, off, len);
            }
        }

    }

    private static boolean parseHostAndPort(String msg, int[] d) {
        boolean ok = true;
        int   cl   = 0;
        int   cn   = 0;
        try {
            for(int i = 0; i < 6; i++) {
                String num;
                if(i == 5) {
                    cn = msg.indexOf(')', cl);
                    if(cn == -1)
                        cn = msg.indexOf('\r', cl);
                    else if(cn == -1)
                        cn = msg.indexOf('\n', cl);
                } else
                    cn = msg.indexOf(',', cl);
                num = msg.substring(cl, cn);
                cl = cn + 1;
                d[i] = Integer.parseInt(num);
            }
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    private String      localAddrPASVStr;
    private String      localAddrPORTStr;
    private InetAddress serverLocalAddr;
    private InetAddress localHost;

    /**
     * Factory instance constructor
     */
    public SSH2FTPProxyFilter(String localHost, String serverLocalAddr)
    throws UnknownHostException {
        this(InetAddress.getByName(localHost),
             InetAddress.getByName(serverLocalAddr));
    }

    /**
     * Factory instance constructor
     */
    public SSH2FTPProxyFilter(InetAddress localHost,
                              InetAddress serverLocalAddr) {
        this.localHost       = localHost;
        this.serverLocalAddr = serverLocalAddr;

        byte[] localAddrArr = localHost.getAddress();

        // !!! TODO: want to be able to get actual connected adress
        // i.e. through call to our stream ("instanceof TCP")
        //
        if((localAddrArr[0] & 0xff) == 0) {
            try {
                localAddrArr = InetAddress.getLocalHost().getAddress();
            } catch (UnknownHostException e) {
                throw new Error("Error in SSH2FTPProxyFilter: " + e);
            }
        }

        localAddrPASVStr = "";
        for(int i = 0; i < 4; i++) {
            localAddrPASVStr += (localAddrArr[i] & 0xff) + (i < 3 ? "," : "");
        }

        localAddrArr = serverLocalAddr.getAddress();
        localAddrPORTStr = "";
        for(int i = 0; i < 4; i++) {
            localAddrPORTStr += (localAddrArr[i] & 0xff) + (i < 3 ? "," : "");
        }
    }

    protected SSH2Connection    connection;
    protected FTPInput          ftpIn;
    protected FTPOutput         ftpOut;

    protected volatile boolean waitingPASVResponse;

    protected SSH2FTPProxyFilter(SSH2Connection connection,
                                 SSH2StreamChannel channel,
                                 SSH2FTPProxyFilter factory) {
        this.connection = connection;

        this.localAddrPASVStr = factory.localAddrPASVStr;
        this.localAddrPORTStr = factory.localAddrPORTStr;
        this.serverLocalAddr  = factory.serverLocalAddr;
        this.localHost        = factory.localHost;

        this.waitingPASVResponse = false;
    }

    public SSH2StreamFilter createFilter(SSH2Connection connection,
                                         SSH2StreamChannel channel) {
        return new SSH2FTPProxyFilter(connection, channel, this);
    }

    public InputStream getInputFilter(InputStream toBeFiltered) {
        this.ftpIn = new FTPInput(toBeFiltered);
        return this.ftpIn;
    }

    public OutputStream getOutputFilter(OutputStream toBeFiltered) {
        this.ftpOut = new FTPOutput(toBeFiltered);
        return this.ftpOut;
    }

}
