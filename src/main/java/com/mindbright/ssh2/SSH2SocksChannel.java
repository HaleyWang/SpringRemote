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

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class together with <code>SSH2SocksListener</code> implements
 * a simple SOCKS proxy server that open port forwards for each CONNECT
 * packet. Only TCP is supported.
 *
 * @see SSH2SocksListener
 */

public class SSH2SocksChannel implements Runnable {
    private Thread myThread;
    private Socket s;
    private SSH2Connection connection;

    private String localAddr;
    private int localPort;
    private String originAddr;
    private int originPort;
    
    private InputStream is = null;
    private OutputStream os = null;

    /**
     * Create a channel object for the specified socket that 
     * speaks (a subset of) SOCKS.
     *
     * @param s The socket to use.
     * @param connection The connection to use.
     */    
    
    public SSH2SocksChannel(Socket s, SSH2Connection connection) {
        this.s = s;
        this.connection = connection;

	this.localAddr  = s.getLocalAddress().getHostAddress();
	this.localPort  = s.getLocalPort();
	this.originAddr = s.getInetAddress().getHostAddress();
	this.originPort = s.getPort();

	this.myThread = new Thread(this, "SSH2SocksChannel_" + localAddr + ":" + localPort);
	this.myThread.setDaemon(true);
	this.myThread.start();
    }

    public void terminate() {
    }

    public void run() {
        connection.getLog().debug("SSH2SocksChannel", "starting");
        
        try {
            byte buf[] = new byte[256];
            int l, i;
            boolean found;
            int port;
            String host = null;
            byte cmd;

            is = s.getInputStream();
            os = s.getOutputStream();
            
            // Do version handshake
            doRead(is, buf, 2);
            if (buf[0] != 0x05) {
                Debug("Unsupported SOCKS protocol version: " + buf[0]);
                s.close();
                return;
            }

            l = buf[1];
            if (l != 0) {
                doRead(is, buf, l);
            }

            found = false;
            for (i=0; i<l; i++) {
                if (buf[i] == 0x00) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Debug("SOCKS client do not support 'no authentication' method");
                s.close();
                return;
            }

            buf[0] = 0x05;
            buf[1] = 0x00;
            os.write(buf, 0, 2);            

            // Read command
            doRead(is, buf, 4);
            if (buf[0] != 0x05) {
                Debug("Unsupported SOCKS protocol version: " + buf[0]);
                s.close();
                return;
            }
            cmd = buf[1];
            
            switch (buf[3]) {
                case 0x01: // IPv4
                    doRead(is, buf, 6);
                    port = toUInt(buf[4], buf[5]);
                    host = 
                        ((buf[0]) & 0xff) + "." +
                        ((buf[1]) & 0xff) + "." +
                        ((buf[2]) & 0xff) + "." +
                        ((buf[3]) & 0xff); 
                    break;

                case 0x03: // DOMAINNAME
                    doRead(is, buf, 1);
                    l = buf[0];
                    doRead(is, buf, l + 2);
                    port = toUInt(buf[l], buf[l+1]);
		    StringBuilder sb = new StringBuilder();
                    for (i=0; i<l; i++)
			sb.append((char)buf[i]);
		    host = sb.toString();
                    break;

                default:
                    Debug("Unsupported SOCKS address type: " + buf[3]);
                    s.close();
                    return;
            }

            switch (cmd) {
                case 0x01: // CONNECT
                    buf[0] = 0x05;
                    buf[1] = 0x00;
                    buf[2] = 0x00;
                    buf[3] = 0x01;
                    buf[4] = 0x7f;
                    buf[5] = 0x00;
                    buf[6] = 0x00;
                    buf[7] = 0x01;
                    buf[8] = (byte)0xff;
                    buf[9] = (byte)0xff;                    
                    os.write(buf, 0, 10);

                    SSH2TCPChannel channel = 
                        new SSH2TCPChannel(SSH2Connection.CH_TYPE_DIR_TCPIP, connection, this,
                                           s, host, port, originAddr, originPort);

                    connection.getLog().notice("SSH2SocksChannel",
                                               "connect from: " +
                                               originAddr + ":" +
                                               originPort+ " on " +
                                               localAddr + ":" + localPort +
                                               " --> " + host + ":" + port +
                                               ", new ch. #" + channel.getChannelId());

                    connection.setSocketOptions(SSH2Preferences.SOCK_OPT_LOCAL +
                                                localAddr + "." + localPort, s);

                    connection.getEventHandler().channelConnect
                        (this, channel, s);
                    
                    connection.connectLocalChannel
                        (channel, host, port, originAddr, originPort);
                    
                    break;

                default:
                    Debug("Unsupported SOCKS command: " + cmd);
                    s.close();
                    return;
            }            

        } catch (Exception e) {
            e.printStackTrace();
            try { s.close();  } catch (Exception ee) { }
            try { is.close(); } catch (Exception ee) { }
            try { os.close(); } catch (Exception ee) { }
        } finally {
        }

        connection.getLog().debug("SSH2SocksChannel", "stopping");
    }


    private int toUInt(byte hi, byte lo) {
        return ((hi)&0xff) << 8 | ((lo)&0xff);
    }

    private void doRead(InputStream is, byte[] b, int count) 
        throws IOException
    {
        int c = 0, l;
        while (c < count) {
            l = is.read(b, c, count - c);
            if (l < 0) 
                throw new IOException();
            c += l;
        }
    }

    private void Debug(String s) {
        connection.getLog().debug("SSH2SocksChannel", "Debug:: " + s);
    }
}
