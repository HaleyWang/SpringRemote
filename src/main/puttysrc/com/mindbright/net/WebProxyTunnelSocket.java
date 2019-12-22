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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.nio.channels.SocketChannel;

import java.security.MessageDigest;

import java.util.Random;

import javax.crypto.Cipher;

import javax.crypto.spec.SecretKeySpec;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.util.Base64;

/**
 * Socket that implements web proxy tunnelling (using CONNECT). Note
 * that the proxy setup is done in blocking mdoe so this will block
 * until the socket is connected, or it gives up.
 *
 * Proxy-authentication in general, and Basic authentication, is described in 
 * RFC2616 and RFC2617.
 *
 * NTLM Authentication is described here:
 *   http://davenport.sourceforge.net/ntlm.html
 *
 */
public class WebProxyTunnelSocket {

    private String targetHost;
    private int    targetPort;
    private String proxyHost;
    private int    proxyPort;
    private long   proxyTimeout;
    private String protoStr;
    private String userAgent;
    private ProxyAuthenticator authenticator;

    private SocketChannel channel = null;
    private Socket        socket = null;
    private InputStream   proxyIn = null;
    private OutputStream  proxyOut = null;

    private HttpHeader responseHeader;
    private WebProxyTunnelSocket(String targetHost, int targetPort,
                                 String proxyHost, int proxyPort,
                                 long proxyTimeout,
                                 String protoStr,
                                 ProxyAuthenticator authenticator,
                                 String userAgent) {

        this.targetHost    = targetHost;
        this.targetPort    = targetPort;
        this.proxyHost     = proxyHost;
        this.proxyPort     = proxyPort;
        this.proxyTimeout  = proxyTimeout;
        this.protoStr      = protoStr;
        this.userAgent     = userAgent;
        this.authenticator = authenticator;
    }

    /**
     * Connect through an HTTP proxy. This method creates a
     * <code>NetworkConnection</code> which is connected through the
     * specified proxy. The connection may be authenticated.
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param host          Host we want to connect to
     * @param port          Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param authenticator Used to authenticate (if needed)
     * @param userAgent     Which user agent we should present
     *                      ourselves as to the proxy
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getProxy(String host, int port,
                                             String proxyHost, int proxyPort,
                                             ProxyAuthenticator authenticator,
                                             String userAgent)
        throws IOException, UnknownHostException {
        return getProxy(host, port, proxyHost, proxyPort, 0, 
                        null, authenticator, userAgent);
    }

    /**
     * Connect through an HTTP proxy. This method creates a
     * <code>NetworkConnection</code> which is connected through the
     * specified proxy. The connection may be authenticated.
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param host          Host we want to connect to
     * @param port          Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param proxyTimeout  How many milliseconds to wait before giving
     *                      up when connecting.
     * @param authenticator Used to authenticate (if needed)
     * @param userAgent     Which user agent we should present
     *                      ourselves as to the proxy
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getProxy(String host, int port,
                                             String proxyHost, int proxyPort,
                                             long proxyTimeout,
                                             ProxyAuthenticator authenticator,
                                             String userAgent)
        throws IOException, UnknownHostException {
        return getProxy(host, port, proxyHost, proxyPort, proxyTimeout,
                        null, authenticator, userAgent);
    }

    /**
     * Connect through an HTTP proxy. This method creates a
     * <code>NetworkConnection</code> which is connected through the
     * specified proxy. The connection may be authenticated.
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param host          Host we want to connect to
     * @param port          Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param protoStr      Extra string to preprend to host when
     *                      issuing the CONNECT call
     * @param authenticator Used to authenticate (if needed)
     * @param userAgent     Which user agent we should present
     *                      ourselves as to the proxy
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getProxy(String host, int port,
                                             String proxyHost, int proxyPort,
                                             String protoStr,
                                             ProxyAuthenticator authenticator,
                                             String userAgent)
        throws IOException, UnknownHostException {
        return getProxy(host, port, proxyHost, proxyPort, 0,
                        protoStr, authenticator, userAgent);
    }

    /**
     * Connect through an HTTP proxy. This method creates a
     * <code>NetworkConnection</code> which is connected through the
     * specified proxy. The connection may be authenticated.
     *
     * Note that the connection is done synchronously so this call
     * will block until the connection is established.
     *
     * @param host          Host we want to connect to
     * @param port          Port on host we want to connect to
     * @param proxyHost     Address or proxy server
     * @param proxyPort     Port on proxy server
     * @param proxyTimeout  How many milliseconds to wait before giving
     *                      up when connecting.
     * @param protoStr      Extra string to preprend to host when
     *                      issuing the CONNECT call
     * @param authenticator Used to authenticate (if needed)
     * @param userAgent     Which user agent we should present
     *                      ourselves as to the proxy
     *
     * @return A NetworkConnection which is connected to the
     *         destination hos through the proxy.
     */
    public static NetworkConnection getProxy(String host, int port,
                                             String proxyHost, int proxyPort,
                                             long proxyTimeout,
                                             String protoStr,
                                             ProxyAuthenticator authenticator,
                                             String userAgent)
        throws IOException, UnknownHostException {

        WebProxyTunnelSocket proxySocket =
            new WebProxyTunnelSocket(host, port, proxyHost, proxyPort,
                                     proxyTimeout, protoStr, authenticator,
                                     userAgent);
        return proxySocket.connect();
    }

    // NTLM message flags
    private static int NTLM_NEGOTIATE_UNICODE   = 0x00000001;
    private static int NTLM_NEGOTIATE_OEM       = 0x00000002;
    private static int NTLM_NEGOTIATE_NTLM      = 0x00000200;
    private static int NTLM_NEGOTIATE_SIGN      = 0x00008000;
    private static int NTLM_NEGOTIATE_NTLM2_KEY = 0x00080000;
    
    private static int NTLM_CHALLENGE_SIZE   = 8;
    private static int NTLM_PADDED_HASH_SIZE = 21;
    private static int NTLM_RESPONSE_SIZE    = 24;
    private static String NTLMSSP_SIGNATURE = "NTLMSSP";

    private static byte[] NTLMType1Msg = {
        'N', 'T', 'L', 'M', 'S', 'S', 'P', 0x00,
        0x01, 0x00, 0x00, 0x00, // type 1
        0x07, (byte)0x82, 0x08, 0x00,// flags=NTLM2_KEY|SIGN|NTLM|OEM|REQ_TARGET|UNICODE=0x88207
        0x00, 0x00, 0x00, 0x00, // no domain
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, // no host
        0x00, 0x00, 0x00, 0x00
    };

    private static class LMAndNTLMResponse {
        byte[] lm, ntlm;
        int lmsz, ntlmsz;

        LMAndNTLMResponse() {
            lmsz = ntlmsz = NTLM_RESPONSE_SIZE;
            lm = new byte[NTLM_RESPONSE_SIZE];
            ntlm = new byte[NTLM_RESPONSE_SIZE];
        }
    };

    private void clearbytes(byte[] a) {
        for (int i=0; i<a.length; i++)
            a[i] = 0x00;
    }

    private byte[] getBytes(String s, String enc) {
        try {
            return s.getBytes(enc);
        } catch (Throwable t) {
        }
        return s.getBytes();
    }

    private void oddParity(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            boolean needsParity = (((b >>> 7) ^ (b >>> 6) ^ (b >>> 5) ^
                                    (b >>> 4) ^ (b >>> 3) ^ (b >>> 2) ^
                                    (b >>> 1)) & 0x01) == 0;
            if (needsParity) {
                bytes[i] |= (byte) 0x01;
            } else {
                bytes[i] &= (byte) 0xfe;
            }
        }
    }
    
    private void des_encrypt(byte[] key7, int keyoff, byte[] plaintext, 
                             byte[] res, int resoff) {
        try {
            Cipher des = com.mindbright.util.Crypto.getCipher("DES/ECB");
            
            byte[] key8 = new byte[8];
            
            key8[0] = key7[keyoff];
            key8[1] = (byte) (key7[keyoff+0] << 7 | (key7[keyoff+1] & 0xff) >>> 1);
            key8[2] = (byte) (key7[keyoff+1] << 6 | (key7[keyoff+2] & 0xff) >>> 2);
            key8[3] = (byte) (key7[keyoff+2] << 5 | (key7[keyoff+3] & 0xff) >>> 3);
            key8[4] = (byte) (key7[keyoff+3] << 4 | (key7[keyoff+4] & 0xff) >>> 4);
            key8[5] = (byte) (key7[keyoff+4] << 3 | (key7[keyoff+5] & 0xff) >>> 5);
            key8[6] = (byte) (key7[keyoff+5] << 2 | (key7[keyoff+6] & 0xff) >>> 6);
            key8[7] = (byte) (key7[keyoff+6] << 1);
            
            oddParity(key8);

            des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key8, "DES"));
            des.doFinal(plaintext, 0, NTLM_CHALLENGE_SIZE, res, resoff);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private boolean do_ntlm_hash(String password, byte[] ntlmhash) {
        // the "ntlm hash" is the md4 digest of mixed case unicode password
        try {
            clearbytes(ntlmhash);
            byte[] pass = password.getBytes("UnicodeLittleUnmarked");
            byte[] digest = com.mindbright.util.Crypto.getMessageDigest("MD4").digest(pass);
            System.arraycopy(digest, 0, ntlmhash, 0, digest.length);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
    }
    
    private LMAndNTLMResponse do_ntlm2(String passwd, byte[] challenge) {
        byte[] ntlmhash = new byte[NTLM_PADDED_HASH_SIZE];
        LMAndNTLMResponse resp = new LMAndNTLMResponse();
        
        if (passwd == null || !do_ntlm_hash(passwd, ntlmhash))
            return null;

        clearbytes(resp.lm);
        
        try {
            byte[] t = new byte[NTLM_CHALLENGE_SIZE];
            Random rand = new Random();
            rand.nextBytes(t);
            System.arraycopy(t, 0, resp.lm, 0, t.length);

            MessageDigest md5 = com.mindbright.util.Crypto.getMessageDigest("MD5");
            md5.update(challenge);
            md5.update(resp.lm, 0, NTLM_CHALLENGE_SIZE);
            byte[] digest = md5.digest();

            for (int i=0; i<3; i++)
                des_encrypt(ntlmhash, i*7, digest, resp.ntlm, i*8);

        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
        
        return resp;
    }

    private LMAndNTLMResponse do_lm_and_ntlm(String passwd, byte[] challenge) {
        byte[] lmhash = new byte[NTLM_PADDED_HASH_SIZE];
        LMAndNTLMResponse resp = new LMAndNTLMResponse();
        int i;
        
        if (passwd == null)
            return null;
        
        // do LM
        clearbytes(lmhash);

        int len = passwd.length();
        if (len <= 14) {
            byte[] pass = new byte[14];
            String lmsecret = "KGS!@#$%";

            clearbytes(pass);

            String passwdupper = passwd.toUpperCase();
            for (i=0; i<len; i++)
                pass[i] = (byte)passwdupper.charAt(i);

            for (i=0; i<2; i++)
                des_encrypt(pass, i*7, getBytes(lmsecret, "US-ASCII"), lmhash, i*8);

            clearbytes(pass);
        }

        for (i=0; i<3; i++)
            des_encrypt(lmhash, i*7, challenge, resp.lm, i*8); 

        // do NTLM

        if (!do_ntlm_hash(passwd, lmhash))
            return null;

        for (i=0; i<3; i++)
            des_encrypt(lmhash, i*7, challenge, resp.ntlm, i*8); 

        clearbytes(lmhash);

        return resp;
    }


    private static void writeShort(OutputStream os, int x) throws IOException {
        os.write( x & 0xff );
        os.write( (x >> 8) & 0xff );
    }

    private static void writeInt(OutputStream os, int x) throws IOException {
        os.write( x & 0xff );
        os.write( (x >> 8) & 0xff );
        os.write( (x >> 16) & 0xff );
        os.write( (x >> 24) & 0xff );
    }

    private static void writeBytes(OutputStream os, byte[] b, int off, int n) 
       throws IOException {
        for (int i=0; i<n; i++)
            os.write(b[off+i]);
    }

    private void open() throws IOException {
        if (proxyIn != null)  proxyIn.close();
        if (proxyOut != null) proxyOut.close();
        if (socket != null)   socket.close();
        if (channel != null)  channel.close();

        channel = SocketFactory.newSocket(proxyHost, proxyPort, proxyTimeout);
        channel.configureBlocking(true);
        socket = channel.socket();
           
        proxyIn  = socket.getInputStream();
        proxyOut = socket.getOutputStream();
    }

    private NetworkConnection connect()
        throws IOException, UnknownHostException {

        int status = -1;
        try { 
            open();
            HttpHeader   requestHeader = new HttpHeader();

            if(protoStr == null)
                protoStr = "";

            requestHeader.setStartLine("CONNECT " + protoStr +
                                       targetHost + ":" +
                                       targetPort + " HTTP/1.0");
            requestHeader.setHeaderField("User-Agent", userAgent);
            requestHeader.setHeaderField("Pragma", "No-Cache");
            requestHeader.setHeaderField("Proxy-Connection", "Keep-Alive");

            requestHeader.writeTo(proxyOut);
            responseHeader = new HttpHeader(proxyIn);

            responseHeader.getHeaderField("server");

            // If proxy requires authentication
            //
            if (responseHeader.getStatus() == 407
                    && authenticator != null) {
                String method = responseHeader.getProxyAuthMethod();

                if ("basic".equalsIgnoreCase(method)) {
                    String realm = responseHeader.getProxyAuthRealm();
                    
                    if (realm == null)
                        realm = "";
                    
                    open();
                    
                    String username =
                        authenticator.getProxyUsername("HTTP Proxy", realm);
                    String password =
                        authenticator.getProxyPassword("HTTP Proxy", realm);
                    
                    requestHeader.setBasicProxyAuth(username, password);
                    requestHeader.writeTo(proxyOut);
                    responseHeader = new HttpHeader(proxyIn);

                } else if ("digest".equalsIgnoreCase(method)) {
                    throw new IOException("We don't support 'Digest' HTTP " +
                                          "Authentication");

                } else if ("ntlm".equalsIgnoreCase(method) ||
                           "negotiate".equalsIgnoreCase(method)) {
                    
                    String keepalive = responseHeader.getHeaderField("proxy-connection");
                    
                    if (keepalive == null || !keepalive.equalsIgnoreCase("keep-alive")) {
                        open();
                    } else { // skip rest of body
                        String tmp = responseHeader.getHeaderField("content-length");
                        int contentlength = 0;
                        if (tmp != null)
                            contentlength = Integer.parseInt(tmp);
                        while (contentlength > 0) {
                            int skipped = (int)proxyIn.skip(contentlength);
                            contentlength -= skipped;
                        }
                    }
                    
                    // send type 1 message
                    String type1 = "NTLM " + new String(Base64.encode(NTLMType1Msg));
                    requestHeader.setHeaderField
                        ("Proxy-Authorization", type1);
                    
                    requestHeader.writeTo(proxyOut);
                    responseHeader = new HttpHeader(proxyIn);

                    if (responseHeader.getStatus() != 407)
                        throw new WebProxyException("Proxy authentication with NTLM failed");

                    // skip rest of body
                    {
                        String tmp = responseHeader.getHeaderField("content-length");
                        int contentlength = 0;
                        if (tmp != null)
                            contentlength = Integer.parseInt(tmp);
                        while (contentlength > 0) {
                            int skipped = (int)proxyIn.skip(contentlength);
                            contentlength -= skipped;
                        }
                    }
                    
                    // check type 2 message 
                    String authline = responseHeader.getHeaderField("Proxy-Authenticate");
                    if (authline == null)
                        throw new WebProxyException("Proxy authentication with NTLM failed");
                    authline = authline.trim();
                    int pos = authline.indexOf(' ');
                    if (pos == -1)
                        throw new WebProxyException("Proxy authentication with NTLM failed");
                    String m = authline.substring(0, pos);
                    if (!m.equalsIgnoreCase("NTLM"))
                        throw new WebProxyException("Proxy authentication with NTLM failed");
                    authline = authline.substring(pos).trim();
                    pos = authline.indexOf(' ');
                    if (pos > 0)
                        authline = authline.substring(0, pos);

                    byte[] type2msg = Base64.decode(getBytes(authline, "US-ASCII"));
                    if (type2msg.length < 32 || type2msg[8] != 0x02) // type 2 msg?
                        throw new WebProxyException("Proxy authentication with NTLM failed");

                    int targetlen = 
                        ((type2msg[12])&0xff) + 
                        (((type2msg[13])&0xff) << 8);

                    int targetpos = 
                        ((type2msg[16])&0xff) + 
                        (((type2msg[17])&0xff) << 8) +
                        (((type2msg[18])&0xff) << 16) +
                        (((type2msg[19])&0xff) << 24);

                    int flags = 
                        ((type2msg[20])&0xff) + 
                        (((type2msg[21])&0xff) << 8) +
                        (((type2msg[22])&0xff) << 16) +
                        (((type2msg[23])&0xff) << 24);

                    byte[] challenge = new byte[NTLM_CHALLENGE_SIZE];
                    System.arraycopy(type2msg, 24, challenge, 0, NTLM_CHALLENGE_SIZE);

                    String username =
                        authenticator.getProxyUsername("HTTP Proxy", null);
                    String password =
                        authenticator.getProxyPassword("HTTP Proxy", null);  
                    
                    LMAndNTLMResponse resp;
                    if ( (flags & NTLM_NEGOTIATE_NTLM2_KEY) != 0) {
                        resp = do_ntlm2(password, challenge);
                    } else { // do NTLM/LM
                        resp = do_lm_and_ntlm(password, challenge);
                    }
                    if (resp == null)
                        throw new WebProxyException("Proxy authentication with NTLM failed");

                    // send type 3 message
                    pos = 64;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bos.write(getBytes(NTLMSSP_SIGNATURE, "US-ASCII"));
                    bos.write(0x00);
                    writeInt(bos, 3);

                    writeShort(bos, resp.lmsz);
                    writeShort(bos, resp.lmsz);
                    writeInt(bos, pos);
                    pos += resp.lmsz;

                    writeShort(bos, resp.ntlmsz);
                    writeShort(bos, resp.ntlmsz);
                    writeInt(bos, pos);
                    pos += resp.ntlmsz;

                    String os = System.getProperty("os.name", "unknown").toLowerCase();

                    byte[] user, ws, target = type2msg;
                    
                    int cpos = username.indexOf('\\');

                    if (cpos != -1) {
                        /* use domain/target from username */
                        String t = username.substring(0, cpos);
                        username = username.substring(cpos+1);
                        if ((flags & NTLM_NEGOTIATE_UNICODE) != 0) {
                            target = getBytes(t, "UnicodeLittleUnmarked");
                        } else {
                            target = getBytes(t, "US-ASCII");
                        }
                        targetlen = target.length;
                        writeShort(bos, targetlen);
                        writeShort(bos, targetlen);
                        writeInt(bos, pos);
                        pos += targetlen;
                        targetpos = 0;
                    } else {
                        if (targetlen == 0) {
                            // Try to pick up domain from environment
                            String domain = System.getProperty("http.auth.ntlm.domain");
                            if (domain == null && os.startsWith("win")) {
                                domain = System.getenv("USERDOMAIN");
                            }
                            if (domain != null) {
                                if ((flags & NTLM_NEGOTIATE_UNICODE) != 0) {
                                    target = getBytes(domain, "UnicodeLittleUnmarked");
                                } else {
                                    target = getBytes(domain, "US-ASCII");
                                }
                                targetlen = target.length;
                                targetpos = 0;
                            }
                        }
                        
                        if (targetlen > 0) {
                            writeShort(bos, targetlen);
                            writeShort(bos, targetlen);
                            writeInt(bos, pos);
                            pos += targetlen;
                        } else {
                            writeShort(bos, 0);
                            writeShort(bos, 0);
                            writeInt(bos, 0);
                        }
                    }
                    
                    if ((flags & NTLM_NEGOTIATE_UNICODE) != 0) {
                        user = getBytes(username, "UnicodeLittleUnmarked");
                    } else {
                        user = getBytes(username, "US-ASCII");
                    }
                    writeShort(bos, user.length);
                    writeShort(bos, user.length);
                    writeInt(bos, pos);
                    pos += user.length;

                    String workstation = null;
                    try {
                        String envstr = "HOST";
                        if (os.startsWith("win"))
                            envstr = "COMPUTERNAME";
                        workstation = System.getenv(envstr);
                    } catch (Throwable t) {
                    }
                    
                    if (workstation == null || workstation.equals("")) {
                        try { 
                            workstation = java.net.InetAddress.getLocalHost().getHostName();
                        } catch (Throwable t) {
                        }
                    }
                    
                    if (workstation == null || workstation.equals(""))
                        workstation = "ag_server";
                    
                    workstation = workstation.toUpperCase();
                    
                    if ((flags & NTLM_NEGOTIATE_UNICODE) != 0) {
                        ws = getBytes(workstation, "UnicodeLittleUnmarked");
                    } else {
                        ws = getBytes(workstation, "US-ASCII");
                    }
                    
                    writeShort(bos, ws.length);
                    writeShort(bos, ws.length);
                    writeInt(bos, pos);
                    pos += ws.length;

                    writeInt(bos, 0); // session key 
                    writeInt(bos, 0); // session key 
                    writeInt(bos, flags & 
                             (NTLM_NEGOTIATE_UNICODE | NTLM_NEGOTIATE_OEM |
                              NTLM_NEGOTIATE_NTLM | NTLM_NEGOTIATE_SIGN | 
                              NTLM_NEGOTIATE_NTLM2_KEY));
                    writeBytes(bos, resp.lm, 0, resp.lmsz);
                    writeBytes(bos, resp.ntlm, 0, resp.ntlmsz);
                    if (targetlen > 0)
                        writeBytes(bos, target, targetpos, targetlen);
                    writeBytes(bos, user, 0, user.length);
                    writeBytes(bos, ws, 0, ws.length);

                    String type3 = "NTLM " + new String(Base64.encode(bos.toByteArray()));
                    requestHeader.setHeaderField
                        ("Proxy-Authorization", type3);

                    requestHeader.writeTo(proxyOut);

                    responseHeader = new HttpHeader(proxyIn);
                } else {
                    throw new IOException("Unknown HTTP Authentication " +
                                          "method '" + method + "'");
                }
            }

            status = responseHeader.getStatus();

        } catch (SocketException e) {
            e.printStackTrace();
            throw new SocketException("Error communicating with proxy server "+
                                      proxyHost + ":" + proxyPort + " (" +
                                      e.getMessage()  + ")");

        }

        if ((status < 200) || (status > 299))
            throw new WebProxyException("Proxy tunnel setup failed: " +
                                        responseHeader.getStartLine());

        channel.configureBlocking(false);
        return new NetworkConnection(channel);
    }

    public String toString() {
        return "WebProxyTunnelSocket[addr=" + targetHost +
               ",port=" + targetPort + "]";
    }
}
