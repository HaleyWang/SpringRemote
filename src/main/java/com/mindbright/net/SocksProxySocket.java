/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import com.mindbright.nio.NetworkConnection;

/**
 * Socket that connects through a socks proxy. Note
 * that the proxy setup is done in blocking mdoe so this will block
 * until the socket is connected, or it gives up.
 *
 * This supports both Socks4 and Socks5
 */
public class SocksProxySocket {

    private final static String[] replyErrorV5 = {
                "Success",
                "General SOCKS server failure",
                "Connection not allowed by ruleset",
                "Network unreachable",
                "Host unreachable",
                "Connection refused",
                "TTL expired",
                "Command not supported",
                "Address type not supported"
            };

    private final static String[] replyErrorV4 = {
                "Request rejected or failed",
                "SOCKS server cannot connect to identd on the client",
                "The client program and identd report different user-ids"
            };

    private String  proxyHost;
    private int     proxyPort;
    private String  targetHost;
    private int     targetPort;
    private long    proxyTimeout;
    private boolean localLookup;
    private String  userId;
    private ProxyAuthenticator authenticator;

    private SocketChannel channel = null;
    private Socket        socket = null;
    private InputStream   proxyIn = null;
    private OutputStream  proxyOut = null;
    private InetAddress   hostAddr;

    String     serverDesc;

    public String getServerDesc() {
        return serverDesc;
    }

    private SocksProxySocket(String targetHost, int targetPort,
                             String proxyHost, int proxyPort,
                             long proxyTimeout,
                             String userId,
                             boolean localLookup,
                             ProxyAuthenticator auth) {

        this.proxyHost     = proxyHost;
        this.proxyPort     = proxyPort;
        this.targetHost    = targetHost;
        this.targetPort    = targetPort;
        this.proxyTimeout  = proxyTimeout;
        this.userId        = userId;
        this.localLookup   = localLookup;
        this.authenticator = auth;
    }

    /**
     * Connect through a proxy speaking the socks4 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param userId        User to connect as
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks4Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort, 
                                                   String userId)
        throws IOException, UnknownHostException {
        return getSocks4Proxy(targetHost, targetPort, proxyHost, proxyPort, 
                              0, userId);
    }

    /**
     * Connect through a proxy speaking the socks4 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param proxyTimeout  How many milliseconds to wait before giving
     * @param userId        User to connect as
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks4Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort, 
                                                   long proxyTimeout,
                                                   String userId)
        throws IOException, UnknownHostException {

        SocksProxySocket proxySocket =
            new SocksProxySocket(targetHost, targetPort,
                                 proxyHost, proxyPort, proxyTimeout,
                                 userId, false,null);
        return proxySocket.connect4();
    }

    /**
     * Connect through a proxy speaking the socks5 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param auth          Used to athenticate (if needed)
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks5Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort,
                                                   ProxyAuthenticator auth)
        throws IOException, UnknownHostException {
        return getSocks5Proxy(targetHost, targetPort, proxyHost, proxyPort,
                              0, false, auth);
    }

    /**
     * Connect through a proxy speaking the socks5 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param localLookup   If true then the targetHost is resolved
     *                      locally. If false then the socks server
     *                      will resolve.
     * @param auth          Used to athenticate (if needed)
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks5Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort,
                                                   boolean localLookup,
                                                   ProxyAuthenticator auth)
        throws IOException, UnknownHostException {
        return getSocks5Proxy(targetHost, targetPort, proxyHost, proxyPort,
                              0, localLookup, auth);
    }

    /**
     * Connect through a proxy speaking the socks5 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param proxyTimeout  How many milliseconds to wait before giving
     * @param auth          Used to athenticate (if needed)
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks5Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort,
                                                   long proxyTimeout,
                                                   ProxyAuthenticator auth)
        throws IOException, UnknownHostException {
        return getSocks5Proxy(targetHost, targetPort, proxyHost, proxyPort,
                              proxyTimeout, false, auth);
    }

    /**
     * Connect through a proxy speaking the socks5 protocol
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param targetHost    Host we want to connect to
     * @param targetPort    Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param proxyTimeout  How many milliseconds to wait before giving
     * @param localLookup   If true then the targetHost is resolved
     *                      locally. If false then the socks server
     *                      will resolve.
     * @param auth          Used to athenticate (if needed)
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getSocks5Proxy(String targetHost,
                                                   int targetPort,
                                                   String proxyHost,
                                                   int proxyPort,
                                                   long proxyTimeout,
                                                   boolean localLookup,
                                                   ProxyAuthenticator auth)
        throws IOException, UnknownHostException {
        SocksProxySocket proxySocket =
            new SocksProxySocket(targetHost, targetPort, proxyHost, proxyPort,
                                 proxyTimeout, null, localLookup, auth);
        return proxySocket.connect5();
    }


    private void open() throws IOException {
        if (proxyIn != null)  proxyIn.close();
        if (proxyOut != null) proxyOut.close();
        if (socket != null)   socket.close();
        if (channel != null)  channel.close();

        hostAddr = InetAddress.getByName(targetHost);
        channel = SocketFactory.newSocket(proxyHost, proxyPort, proxyTimeout);
        channel.configureBlocking(true);
        socket = channel.socket();
           
        proxyIn  = socket.getInputStream();
        proxyOut = socket.getOutputStream();
    }

    private NetworkConnection connect4() throws IOException {
        try {
            open();

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(0x04); // V4
            buf.write(0x01); // CONNECT
            buf.write((targetPort >>> 8) & 0xff);
            buf.write(targetPort & 0xff);
            buf.write(hostAddr.getAddress());
            buf.write(userId.getBytes());
            buf.write(0x00); // NUL terminate userid string
            proxyOut.write(buf.toByteArray());
            proxyOut.flush();

            int res = proxyIn.read();
            if(res == -1) {
                throw new IOException("SOCKS4 server " +
                                      proxyHost + ":" + proxyPort +
                                      " disconnected");
            }
            if(res != 0x00) {
                throw new IOException("Invalid response from SOCKS4 server " +
                                      "(" + res + ") " +
                                      proxyHost + ":" + proxyPort);
            }

            int code = proxyIn.read();
            if(code != 90) {
                if(code > 90 && code < 93) {
                    throw new IOException("SOCKS4 server unable to connect, " +
                                          "reason: " + replyErrorV4[code - 91]);
                }
				throw new IOException("SOCKS4 server unable to connect, " +
				                      "reason: " + code);
            }

            byte[] data = new byte[6];

            if (proxyIn.read(data, 0, 6) != 6) {
                throw new IOException(
                    "SOCKS4 error reading destination address/port");
            }

            serverDesc = data[2]  + "." + data[3] +  "." +
		    data[4] + "." + data[5] + ":" + ((data[0] << 8) | (data[1] & 0xff));

        } catch (SocketException e) {
            throw new SocketException("Error communicating with SOCKS4 " +
                                      "server " + proxyHost + ":" + proxyPort + 
                                      ", " + e.getMessage());
        }

        return new NetworkConnection(channel);
    }

    private NetworkConnection connect5() throws IOException {
        try {
            open();

            // Simplest form, only no-auth and cleartext username/password
            //
            byte[] request = {(byte)0x05, (byte)0x02, (byte)0x00, (byte)0x02};

            proxyOut.write(request);
            proxyOut.flush();

            int res = proxyIn.read();
            if(res == -1) {
                throw new IOException("SOCKS5 server " +
                                      proxyHost + ":" + proxyPort +
                                      " disconnected");
            }
            if(res != 0x05) {
                throw new IOException("Invalid response from SOCKS5 server (" +
                                      res + ") " +
                                      proxyHost + ":" + proxyPort);
            }

            int method = proxyIn.read();
            switch(method) {
            case 0x00:
                break;
            case 0x02:
                doAuthentication(proxyIn, proxyOut, authenticator,
                                 proxyHost, proxyPort);
                break;
            default:
                throw new IOException("SOCKS5 server does not support our " +
                                      "authentication methods");
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            if (localLookup) {
                // Request connect to targetHost (as 'ip-number') : targetPort
                //
                InetAddress hostAddr;
                try {
                    hostAddr = InetAddress.getByName(targetHost);
                } catch (UnknownHostException e) {
                    throw new IOException("Can't do local lookup on: " +
                                          targetHost +
                                          ", try socks5 without local lookup");
                }
                request = new byte[] {(byte)0x05, (byte)0x01,
                                      (byte)0x00, (byte)0x01};
                buf.write(request);
                buf.write(hostAddr.getAddress());
            } else {
                // Request connect to targetHost (as 'domain-name') : targetPort
                //
                request = new byte[] {(byte)0x05, (byte)0x01,
                                      (byte)0x00, (byte)0x03 };
                buf.write(request);
                buf.write(targetHost.length());
                buf.write(targetHost.getBytes());
            }
            buf.write((targetPort >>> 8) & 0xff);
            buf.write(targetPort & 0xff);
            proxyOut.write(buf.toByteArray());
            proxyOut.flush();

            res = proxyIn.read();
            if(res != 0x05)
                throw new IOException("Invalid response from SOCKS5 server (" +
                                      res + ") " + proxyHost + ":" + proxyPort);

            int status = proxyIn.read();
            if(status != 0x00) {
                if(status > 0 && status < 9) {
                    throw new IOException("SOCKS5 server unable to connect, " +
                                          "reason: " + replyErrorV5[status]);
                }
				throw new IOException("SOCKS5 server unable to connect, " +
				                      "reason: " + status);
            }

            proxyIn.read(); // 0x00 RSV

            int aType = proxyIn.read();
            byte[] data = new byte[255];
            switch(aType) {
            case 0x01:
                if (proxyIn.read(data, 0, 4) != 4) {
                    throw new IOException("SOCKS5 error reading address");
                }
                serverDesc = data[0]  + "." + data[1] +  "." +
                    data[2] + "." + data[3];
                break;
            case 0x03:
                int n = proxyIn.read();
                if (proxyIn.read(data, 0, n) != n) {
                    throw new IOException("SOCKS5 error reading address");
                }
                serverDesc = new String(data);
                break;
            default:
                throw new IOException("SOCKS5 gave unsupported address type: "+
                                      aType);
            }

            if(proxyIn.read(data, 0, 2) != 2)
                throw new IOException("SOCKS5 error reading port");
            serverDesc += ":" + ((data[0] << 8) | (data[1] & 0xff));

        } catch (SocketException e) {
            throw new SocketException("Error communicating with SOCKS5 " +
                                      "server " +
                                      proxyHost + ":" + proxyPort + ", " +
                                      e.getMessage());
        }

        return new NetworkConnection(channel);
    }

    private static void doAuthentication(InputStream proxyIn,
                                         OutputStream proxyOut,
                                         ProxyAuthenticator authenticator,
                                         String proxyHost, int proxyPort)
        throws IOException {

        String username = authenticator.getProxyUsername("SOCKS5", null);
        String password = authenticator.getProxyPassword("SOCKS5", null);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.reset();
        buf.write(0x01);
        buf.write(username.length());
        buf.write(username.getBytes());
        buf.write(password.length());
        buf.write(password.getBytes());
        proxyOut.write(buf.toByteArray());
        proxyOut.flush();

        int res = proxyIn.read();

        /*
         * We accept both 0x05 and 0x01 as version in the response here. 0x01
         * is the right response but some buggy servers will respond with 0x05
         * (i.e. not complying with rfc1929).
         */
        if (res != 0x01 && res != 0x05) {
            throw new IOException("Invalid response from SOCKS5 server (" +
                                  res + ") " + proxyHost + ":" + proxyPort);
        }

        if (proxyIn.read() != 0x00) {
            throw new IOException(
                "Invalid username/password for SOCKS5 server");
        }
    }

    public String toString() {
        return "SocksProxySocket[addr=" + socket.getInetAddress() +
               ",port=" + socket.getPort() +
               ",localport=" + socket.getLocalPort() + "]";
    }
}
