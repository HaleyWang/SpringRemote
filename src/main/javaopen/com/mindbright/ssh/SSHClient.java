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
import java.math.BigInteger;
import java.util.Vector;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

import java.nio.ByteBuffer;
import com.mindbright.nio.NIOCallback;
import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.nio.TimerCallback;

import com.mindbright.util.SecureRandomAndPad;

import com.mindbright.terminal.*;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.nio.Switchboard;

/**
 * This class contains the main functionality for setting up a connection to a
 * ssh-server. It can be used both to implement a "full" ssh-client, or it can
 * be used to fire off a single command on the server (both in a background
 * thread and in the current-/foreground-thread). A set of properties can be
 * used to control different aspects of the connection. These are fetched from
 * an object implementing the <code>SSHClientUser</code>-interface.  The
 * authentication can be done in different ways, all which is handled through an
 * object implementing the <code>SSHAuthenticator</code>-interface. The
 * console-output of the <code>SSHClient</code> is (optionally) handled through
 * an object implementing the <code>SSHConsole</code>-interface.  <p>
 *
 * A class realizing a full interactive ssh-client is
 * <code>SSHInteractiveClient</code>. 
 *
 * @see     SSHAuthenticator
 * @see     SSHClientUser
 * @see     SSHConsole 
 */
public class SSHClient extends SSH implements NIOCallback {

    static public class AuthFailException extends IOException {
    	private static final long serialVersionUID = 1L;
        public AuthFailException(String msg) {
            super(msg);
        }
        public AuthFailException() {
            this("permission denied");
        }
    }

    static public class ExitMonitor implements Runnable {
        SSHClient client;
        long      msTimeout;
        public ExitMonitor(SSHClient client, long msTimeout) {
            this.msTimeout  = msTimeout;
            this.client     = client;
        }
        public ExitMonitor(SSHClient client) {
            this(client, 0);
        }
        public void run() {
            client.waitForExit(msTimeout);
            // If we have already exited gracefully don't report...
            //
            if(!client.gracefulExit)
                client.disconnect(false);
        }
    }

    private class KeepAliveThread implements TimerCallback {
        private volatile int     interval;
        private volatile boolean keepRunning;
        private volatile Object  handler;
        private int count = 0;

        protected KeepAliveThread(int interval) {
            this.interval    = interval;
            this.keepRunning = true;
            this.handler     = switchboard.registerTimer(1000, this);
        }

        protected synchronized void setInterval(int interval) {
            if (interval < 1) {
                switchboard.unregisterTimer(this.handler);
            } else {
                this.interval = interval;
            }
        }

        public void timerTrig() {
            if (++count < interval) {
                return;
            }

            count = 0;
            if (SSHClient.this.controller != null) {
                try {
                    SSHPduOutputStream ignmsg;
                    ignmsg = new SSHPduOutputStream(MSG_DEBUG,
                                                    controller.sndCipher,
                                                    controller.sndComp,
                                                    rand);
                    ignmsg.writeString("heartbeat");
                    controller.transmit(ignmsg);
                } catch (IOException e) {}
            }
        }

        public boolean isRunning() {
            return keepRunning;
        }

    }

    // Local port forwarding
    //
    public static class LocalForward {
        protected String localHost;
        protected int    localPort;
        protected String remoteHost;
        protected int    remotePort;
        protected String plugin;
        public LocalForward(String localHost, int localPort, String remoteHost, int remotePort, String plugin) {
            this.localHost  = localHost;
            this.localPort  = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.plugin     = plugin;
        }
    }

    // Remote port forwarding
    //
    public static class RemoteForward {
        protected String remoteHost;
        protected int    remotePort;
        protected String localHost;
        protected int    localPort;
        protected String plugin;
        public RemoteForward(String remoteHost, int remotePort, String localHost, int localPort, String plugin) {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.localHost  = localHost;
            this.localPort  = localPort;
            this.plugin     = plugin;
        }
    }

    protected KeepAliveThread    heartbeat;
    protected SecureRandomAndPad rand;

    protected InetAddress serverAddr;
    protected InetAddress serverRealAddr = null;
    protected InetAddress localAddr;
    protected String      srvVersionStr;
    protected int         srvVersionMajor;
    protected int         srvVersionMinor;

    protected Vector<LocalForward>  localForwards;
    protected Vector<RemoteForward> remoteForwards;
    protected String commandLine;

    protected Object               controllerMonitor;
    protected SSHChannelController controller;
    protected SSHConsole           console;
    protected SSHAuthenticator     authenticator;
    protected SSHClientUser        user;
    protected SSHInteractor        interactor;

    protected NetworkConnection    sshSocket;
    protected NonBlockingInput     sshIn;
    protected NonBlockingOutput    sshOut;

    protected boolean gracefulExit;
    protected boolean isConnected;
    protected boolean isOpened;

    boolean usedOTP;

    protected int refCount;

    // !!! KLUDGE
    protected boolean havePORTFtp     = false;
    protected int     firstFTPPort    = 0;
    protected boolean activateTunnels = true;
    // !!! KLUDGE

    private Object openMonitor;
    private boolean haveCnxWatch;
    private Switchboard switchboard;

    // State when receiving data from network
    private final static int READ_STATE_IDSTRING = 0;
    private final static int READ_STATE_PDU      = 1;
    private int readState = -1;

    // State when handling PDUs
    private final static int PDU_STATE_PUBKEY     =  0;
    private final static int PDU_STATE_SENT_KEYS  =  1;
    private final static int PDU_STATE_AUTH1      =  2;
    private final static int PDU_STATE_AUTH_SENT  =  3;
    private final static int PDU_STATE_AUTH_RSA   =  4;
    private final static int PDU_STATE_AUTH_TIS   =  5;
    private final static int PDU_STATE_AUTH_SDI1  =  6;
    private final static int PDU_STATE_AUTH_SDI2  =  7;
    private final static int PDU_STATE_COMPR_REQ  =  8;
    private final static int PDU_STATE_COMPR_DONE =  9;
    private final static int PDU_STATE_PTY_REQ    = 10;
    private final static int PDU_STATE_MAXPKT_REQ = 11;
    private final static int PDU_STATE_X11_REQ    = 12;
    private final static int PDU_STATE_TUNNEL_REQ = 13;
    private final static int PDU_STATE_CONTROLLER = 14;
    private int pduState = -1;

    private Exception storedException;

    /**
     * SSHClient will set this variable to true when it is reading
     * from the input connection. So subclasses which implement
     * NIOCallback should test this in completed() and call the
     * completed() in SSHClient when it is set to true.
     */
    public boolean isReading = false;

    /**
     * Exit code.
     */
    protected Integer exitStatus = null;

    public SSHClient(SSHAuthenticator authenticator, SSHClientUser user) {
        this.user           = user;
        this.authenticator  = authenticator;
        this.interactor     = user.getInteractor();
        this.srvVersionStr  = null;
        this.refCount       = 0;
        this.usedOTP        = false;
        this.openMonitor    = new Object();
        this.controllerMonitor = new Object();

        try {
            this.localAddr = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            if(interactor != null)
                interactor.alert("FATAL: Could not create local InetAddress: " + e.getMessage());
        }
        clearAllForwards();
    }

    public void setConsole(SSHConsole console) {
        this.console = console;
        if(controller != null)
            controller.console = console;
    }

    public SSHConsole getConsole() {
        return console;
    }

    public InetAddress getServerAddr() {
        return serverAddr;
    }

    public InetAddress getServerRealAddr() {
        if(serverRealAddr == null)
            return serverAddr;
        return serverRealAddr;
    }

    public void setServerRealAddr(InetAddress realAddr) {
        serverRealAddr = realAddr;
    }

    public InetAddress getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(String addr) throws UnknownHostException {
        localAddr = InetAddress.getByName(addr);
    }

    public String getServerVersion() {
        return srvVersionStr;
    }

    public void addLocalPortForward(int localPort, String remoteHost,
                                    int remotePort, String plugin)
        throws IOException {
        addLocalPortForward(localAddr.getHostAddress(), localPort,
                            remoteHost, remotePort, plugin);
    }

    public void addLocalPortForward(String localHost, int localPort,
                                    String remoteHost, int remotePort,
                                    String plugin)
        throws IOException {
        delLocalPortForward(localHost, localPort);
        localForwards.addElement(
            new LocalForward(localHost, localPort,
                             remoteHost, remotePort, plugin));
        if(isOpened) {
            try {
                requestLocalPortForward(localHost, localPort,
                                        remoteHost, remotePort, plugin);
            } catch(IOException e) {
                delLocalPortForward(localHost, localPort);
                throw e;
            }
        }
    }

    public void delLocalPortForward(String localHost, int port) {
        if(port == -1) {
            if(isOpened)
                controller.killListenChannels();
            localForwards = new Vector<LocalForward>();
        } else {
            for(int i = 0; i < localForwards.size(); i++) {
                LocalForward fwd = localForwards.elementAt(i);
                if(fwd.localPort == port && fwd.localHost.equals(localHost)) {
                    localForwards.removeElementAt(i);
                    if(isOpened)
                        controller.killListenChannel(fwd.localHost,
                                                     fwd.localPort);
                    break;
                }
            }
        }
    }

    public void addRemotePortForward(String remoteHost, int remotePort, 
                                     String localHost, int localPort,
                                     String plugin) {
        delRemotePortForward(remoteHost, remotePort);
        remoteForwards.addElement(
            new RemoteForward(remoteHost, remotePort,
                              localHost, localPort, plugin));
    }

    public void delRemotePortForward(String remoteHost, int port) {
        if(port == -1) {
            remoteForwards = new Vector<RemoteForward>();
        } else {
            for(int i = 0; i < remoteForwards.size(); i++) {
                RemoteForward fwd = remoteForwards.elementAt(i);
                if(fwd.remotePort==port && fwd.remoteHost.equals(remoteHost)){
                    remoteForwards.removeElementAt(i);
                    break;
                }
            }
        }
    }

    public void delRemotePortForward(String plugin) {
        for(int i = 0; i < remoteForwards.size(); i++) {
            RemoteForward fwd = remoteForwards.elementAt(i);
            if(fwd.plugin.equals(plugin)) {
                remoteForwards.removeElementAt(i);
                i--;
            }
        }
    }

    public void clearAllForwards() {
        this.localForwards  = new Vector<LocalForward>();
        this.remoteForwards = new Vector<RemoteForward>();
    }

    public void startExitMonitor() {
        startExitMonitor(0);
    }

    public void startExitMonitor(long msTimeout) {
        Thread t = new Thread(new ExitMonitor(this, msTimeout));
        t.setName("ExitMonitor");
        t.start();
    }

    public synchronized int addRef() {
        return ++refCount;
    }

    public void forcedDisconnect() {
        if(controller != null) {
            controller.sendDisconnect("exit");
            controller.killAll();
        } else if (interactor != null) {
            interactor.disconnected(this, false);
        }
    }

    public synchronized int delRef() {
        if(--refCount <= 0) {
            forcedDisconnect();
            waitForExit(2000);
        }
        return refCount;
    }

    public void waitForExit() {
        waitForExit(0);
    }

    public void waitForExit(long msTimeout) {
        synchronized(controllerMonitor) {
            try {
                controllerMonitor.wait(msTimeout);
            } catch (InterruptedException e) {}
        }
        
        if (controller != null) {
            controller.waitForExit(msTimeout);
        }

        if(sshSocket != null)
            sshSocket.close();
    }

    public void doSingleCommand(String commandLine, boolean background,
                                long msTimeout)
    throws IOException {
        this.commandLine = commandLine;

        this.exitStatus = null;

        bootSSH(false);
        if (!background) {
            waitForExit(msTimeout);
        } else if (msTimeout > 0) {
            startExitMonitor(msTimeout);
        }
    }

    public long getConnectTimeout() {
        return 60*1000;
    }
    public long getHelloTimeout() {
        return 10*1000;
    }

    public void bootSSH(boolean haveCnxWatch) throws IOException {
        bootSSH(haveCnxWatch, false);
    }

    public void bootSSH(boolean haveCnxWatch, boolean releaseConnection)
        throws IOException {
        
        isReading = false;
        storedException = null;
        try {
            rand = secureRandom();

            // Give the interactor a chance to hold us until the user wants to
            // "connect" (e.g. with a dialog with server, username, password,
            // proxy-info)
            //
            if(interactor != null)
                interactor.startNewSession(this);

            // We first ask for the ssh server address since this might
            // typically be a prompt in the SSHClientUser
            //
            String serverAddrStr = user.getSrvHost();

            // When the SSHClientUser has reported which host to
            // connect to we report this to the interactor as
            // sessionStarted
            //
            if(interactor != null)
                interactor.sessionStarted(this);

            // It's the responsibility of the SSHClientUser to
            // establish a proxied connection if that is needed, the
            // SSHClient does not want to know about proxies. If a
            // proxy is not needed getProxyConnection() just returns
            // null.
            //
            sshSocket = user.getProxyConnection();

            if(sshSocket == null) {
                InetAddress serverAddresses[] =
                    InetAddress.getAllByName(serverAddrStr);
		switchboard = Switchboard.getSwitchboard();
                for (int i=0; sshSocket == null && i<serverAddresses.length; i++) {
                    serverAddr = serverAddresses[i];
                    if (SSH.DEBUG) {
                        System.out.println("Connecting to " +
                                           serverAddr.getHostAddress() +
                                           ":" + user.getSrvPort());
                    }
                    sshSocket = switchboard.connect(serverAddr,
                                                    user.getSrvPort(), false);
                }
            } else {
                serverAddr = sshSocket.getInetAddress();
		switchboard = sshSocket.getSwitchboard();
                if(interactor != null)
                    interactor.report("Connecting through proxy at "
                                      + serverAddr.getHostAddress() +
                                      ":" + sshSocket.getPort());
            }

            if(releaseConnection) {
                return;
            }

            this.haveCnxWatch = haveCnxWatch;
	    
            sshSocket.getSwitchboard().notifyWhenConnected(
                sshSocket, user.getConnectTimeout()*1000, this);
            
            // wait until connected and authenticated
            synchronized (openMonitor) {
                long timeout = (user.getHelloTimeout() + user.getKexTimeout()) *1000;		
                if (!isOpened) {
                    try {
                        openMonitor.wait(timeout);
                    } catch (InterruptedException e) {       
                    }
                    if (storedException == null && readState == READ_STATE_IDSTRING || !isOpened)
                        throw new IOException("Timeout");
                }
            }

            if (storedException != null) {
                if (SSH.DEBUGMORE)
                    storedException.printStackTrace();
                String msg = storedException.getMessage();
                storedException = null;
                throw new IOException(msg);
            }

        } catch (IOException e) {
            shutdown(e);
            throw e;
        }
    }

    protected void boot(boolean haveCnxWatch, NetworkConnection tpSocket) {
        this.sshSocket = tpSocket;
        boot();
    }

    private HelloTimeoutCallback timeoutcallback = null;

    private class HelloTimeoutCallback implements TimerCallback {
        private volatile Object handler;
        private volatile boolean running;
        protected HelloTimeoutCallback(int interval) {
            handler = switchboard.registerTimer(interval, this);
            running = true;
        }
        public void timerTrig() {
            if (running) {
                running = false;
                (new Thread( new Runnable() {
                    public void run() {
                        switchboard.unregisterTimer(handler);
                        shutdown(new IOException("Timeout"));
                    }   
                })).start();
            }
        }
        protected void stop() {
            if (running) {
                running = false;
                switchboard.unregisterTimer(handler);
            }
        }        
    }
    
    private void boot() {
        try {
            sshIn  = sshSocket.getInput();
            sshOut = sshSocket.getOutput();

            int hellotimeout = user.getHelloTimeout()*1000;    
            if (hellotimeout > 0) 
                timeoutcallback = new HelloTimeoutCallback(hellotimeout);
            readState = READ_STATE_IDSTRING;
            ByteBuffer buf = sshIn.createBuffer(new byte[1]);
            isReading = true;
            sshIn.read(buf, this);
        } catch (IOException e) {
            shutdown(e);
	}
    }

    private byte[] idBuf = new byte[256];
    private int idBufUsed = 0;
    private SSHPduInputStream pduIn;

    private ByteBuffer handleReadData(ByteBuffer buf) throws IOException {
        switch (readState) {
        case READ_STATE_IDSTRING:
            byte c = buf.get(0);
            // Are we done?
            if (c != '\n' && idBufUsed < idBuf.length) {
                if (c != '\r') {
                    idBuf[idBufUsed++] = c;
                }
                buf.clear();
                return buf;
            }
            negotiateVersion(new String(idBuf, 0, idBufUsed));

            if (timeoutcallback != null) {
                timeoutcallback.stop();
                timeoutcallback = null;
            }
            
            // We now have a physical connection to a sshd, report
            // this to the SSHClientUser
            isConnected = true;
            if(interactor != null)
                interactor.connected(this);

            pduState = PDU_STATE_PUBKEY;
            pduIn = new SSHPduInputStream(SMSG_PUBLIC_KEY, null, null);
            readState = READ_STATE_PDU;
            return pduIn.initReceive(sshIn);

        case READ_STATE_PDU:
            try {
                ByteBuffer more = pduIn.processData(sshIn, buf);
                if (more != null) {
                    return more;
                }

                pduIn = handlePDU(pduIn);
            } catch (IOException e) {
                shutdown(e);
                return null;
            }

            if (pduIn == null) {
                return null;
            }

            return pduIn.initReceive(sshIn);
        }

        // Should not get here
        return null;
    }

    private SSHPduInputStream handlePDU(SSHPduInputStream pdu)
        throws IOException {

        switch (pduState) {
        case PDU_STATE_PUBKEY:
            receiveServerData(pdu);
            initiatePlugins();

            // Check that selected cipher is supported by server
            //
            cipherType = getSupportedCipher(authenticator.getCipher(user));
            if(cipherType == CIPHER_INVALID) {
                int cipher = authenticator.getCipher(user);
                throw new IOException("Sorry, server does not support the '" +
                                      getCipherName(cipher) + "' cipher.");
            }
            SSH.log("Using cipher: " + getCipherName(cipherType));

            generateSessionId();
            generateSessionKey();
            initClientCipher();
            return sendSessionKey(cipherType);

        case PDU_STATE_SENT_KEYS:
            if(!isSuccess(pdu))
                throw new IOException("Error while sending session key!");
            return authenticateUser1();

        case PDU_STATE_AUTH1:
            return authenticateUser2(pdu);

        case PDU_STATE_AUTH_SENT:
            return authLoop(pdu);

        case PDU_STATE_AUTH_RSA:
            return doRSAAuth2(pdu);

        case PDU_STATE_AUTH_TIS:
            return doTISAuth2(pdu);

        case PDU_STATE_AUTH_SDI1:
            return doSDIAuth2(pdu);

        case PDU_STATE_COMPR_REQ:
            return requestCompression2(pdu);

        case PDU_STATE_COMPR_DONE:
            controller = new SSHChannelController(this, switchboard, sshOut,
                                                  sndCipher, sndComp,
                                                  console, haveCnxWatch);

            synchronized(controllerMonitor) {
                controllerMonitor.notify();
            }

            if (user.wantPTY()) {
                return requestPTY();
            }
            pdu = null;
            /* Fallthrough */

        case PDU_STATE_PTY_REQ:
            if (pdu != null && !isSuccess(pdu) && interactor != null) {
                interactor.report("Error requesting PTY");
            }

            int maxPktSz = user.getMaxPacketSz();
            if(maxPktSz > 0) {
                return requestMaxPacketSz(maxPktSz);
            }
            pdu = null;
            /* Fallthrough */

        case PDU_STATE_MAXPKT_REQ:
            if (pdu != null && !isSuccess(pdu) && interactor != null) {
                interactor.report("Error requesting max packet size: " +
                    user.getMaxPacketSz());
            }

            if (user.wantX11Forward()) {
                return requestX11Forward();
            }
            pdu = null;
            /* Fallthrough */

        case PDU_STATE_X11_REQ:
            if (pdu != null && !isSuccess(pdu) && interactor != null) {
                interactor.report("Error requesting X11 forward");
            }
            pdu = null;
            /* Fallthrough */
            

        case PDU_STATE_TUNNEL_REQ:
            if (pdu != null && !isSuccess(pdu) && interactor != null) {
                RemoteForward fwd = remoteForwards.elementAt(remoteTunnelIndex-1);
                interactor.report("Error requesting remote port forward: " + 
                                  fwd.plugin + "/" + fwd.remotePort + ":" +
                                  fwd.localHost + ":" + fwd.localPort);

            }

            if (activateTunnels) {
                SSHPduInputStream out = initiateTunnels();
                if (out != null) {
                    return out;
                }
            }

            if (commandLine != null) {
                requestCommand(commandLine);
            } else {
                requestShell();
            }
            
            if (console != null) {
                console.serverConnect(controller, sndCipher);
            }

            isOpened = true;
            synchronized(openMonitor) {
                openMonitor.notify();
            }
            if (interactor != null)
                interactor.open(this);

            // Start "heartbeat" if needed
            setAliveInterval(user.getAliveInterval());

            pduState = PDU_STATE_CONTROLLER;
            return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);

        case PDU_STATE_CONTROLLER:
            controller.receive(pdu);
            return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
        }
        return null;
    }

    private SSHPduInputStream authDone(boolean success) throws IOException {
        if (!success) {
            shutdown(new AuthFailException("No more authentication methods"));
        }

        return requestCompression(user.getCompressionLevel());
    }

    /**
     * Called from SSHChannelController to set the exit status.
     */
    protected void setExitStatus(int status) {
        exitStatus = Integer.valueOf(status);
    }
    
    /**
     * Returns the exit status if it was set or null otherwise.
     */
    public Integer getExitStatus() {
        return exitStatus;
    }
    
    protected void disconnect(boolean graceful) {
        disconnect(graceful, null);
    }

    protected void disconnect(boolean graceful, Exception e) {
        if(!isConnected)
            return;
        isConnected   = false;
        isOpened      = false;
        gracefulExit  = graceful;
        srvVersionStr = null;
        setAliveInterval(0); // Stop "heartbeat"...
        if (controller != null) {
            controller.exit();
        }
        if(interactor != null) {
            interactor.disconnected(this, graceful);
            StringBuilder sb = new StringBuilder();
            if(sndComp != null || rcvComp != null) {
                for(int i = 0; i < 2; i++) {
                    SSHCompressor comp = (i == 0 ? sndComp : rcvComp);
                    if(comp != null) {
                        long compressed, uncompressed;
                        compressed   = comp.numOfCompressedBytes();
                        uncompressed = comp.numOfUncompressedBytes();
                        sb.append( (i == 0 ? "outgoing" : "\r\nincoming"))
			    .append(" raw data (bytes) = ").append(uncompressed)
			    .append(", compressed = ").append(compressed)
			    .append(" (")
			    .append(((compressed * 100) / uncompressed)).append("%)");
                    }
                }
                interactor.report(sb.toString());
            }
        }
        rcvComp = sndComp = null;
    }

    void negotiateVersion(String version) throws IOException {
        byte buf[];
        String verStr;

        // Strip white-space
        srvVersionStr = version.trim();

        try {
            int l = version.indexOf('-');
            int r = version.indexOf('.');
            srvVersionMajor = Integer.parseInt(version.substring(l+1,r));
            l = r;
            r = version.indexOf('-', l);
            if(r == -1) {
                srvVersionMinor=Integer.parseInt(version.substring(l+1));
            } else {
                srvVersionMinor =
                    Integer.parseInt(version.substring(l + 1, r));
            }
        } catch (Throwable t) {
            throw new IOException("Server version string invalid: " +
                                  version);
        }

        if (srvVersionMajor > 1) {
            throw new IOException("This server doesn't support ssh1, connect" +
                                  " with ssh2 enabled");
        } else if(srvVersionMajor < 1 || srvVersionMinor < 5) {
            throw new IOException("Server's protocol version (" +
                                  srvVersionMajor + "-" + srvVersionMinor +
                                  ") is too old, please upgrade");
        }

        verStr = getVersionId(true);
        verStr += "\n";
        buf    = verStr.getBytes();

        sshOut.write(buf);
        sshOut.flush();
    }

    void receiveServerData(SSHPduInputStream pdu) throws IOException {
        //pdu.readFrom(sshIn);

        BigInteger e, n;

        srvCookie = new byte[8];
        pdu.readFully(srvCookie, 0, 8);

        pdu.readInt();
        e = pdu.readBigInteger();
        n = pdu.readBigInteger();
        try {
            KeyFactory       rsaKeyFact = com.mindbright.util.Crypto.getKeyFactory("RSA");
            RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);
            srvServerKey = (RSAPublicKey)rsaKeyFact.generatePublic(rsaPubSpec);
        } catch (Exception ee) {
            throw new IOException("Failed to generate RSA public server key");
        }

        pdu.readInt();
        e = pdu.readBigInteger();
        n = pdu.readBigInteger();
        try {
            KeyFactory       rsaKeyFact = com.mindbright.util.Crypto.getKeyFactory("RSA");
            RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);
            srvHostKey = (RSAPublicKey)rsaKeyFact.generatePublic(rsaPubSpec);
        } catch (Exception ee) {
            throw new IOException("Failed to generate RSA public host key");
        }

        int keyLenDiff = Math.abs(srvServerKey.getModulus().bitLength() -
                                  srvHostKey.getModulus().bitLength());

        if(keyLenDiff < 24) {
            throw new IOException("Invalid server keys, difference in sizes must be at least 24 bits");
        }

        if(!authenticator.verifyKnownHosts(srvHostKey)) {
            throw new IOException("Verification of known hosts failed");
        }

        protocolFlags      = pdu.readInt();
        supportedCiphers   = pdu.readInt();
        supportedAuthTypes = pdu.readInt();

        // OUCH: Support SDI patch from ftp://ftp.parc.xerox.com://pub/jean/sshsdi/
        // (we want the types to be in sequence for simplicity, kludge but simple)
        //
        if((supportedAuthTypes & (1 << 16)) != 0) {
            supportedAuthTypes = ((supportedAuthTypes & 0xffff) | (1 << AUTH_SDI));
        }

        if (SSH.DEBUGMORE) {
            SSH.logDebug("SSH1 server data: ");
	    StringBuilder csb = new StringBuilder();
            for (int i=0; i<cipherClasses.length; i++) {
                if ((supportedCiphers & (1<<i)) != 0) {
                    if (csb.length() > 0) 
                        csb.append(", ");
                    csb.append(cipherClasses[i][1]);
                }
            }
            StringBuilder asb = new StringBuilder();
            for (int i=0; i<authTypeDesc.length; i++) {
                if ((supportedAuthTypes & (1<<i)) != 0) {
                    if (asb.length() > 0)
                        asb.append(", ");
                    asb.append(authTypeDesc[i]);
                }
            }
            SSH.logDebug(" flags:      " +
                         Integer.toHexString(protocolFlags));
            SSH.logDebug(" ciphers:    " + csb.toString());
            SSH.logDebug(" auth types: " + asb.toString());
        }
    }

    void generateSessionKey() {
        sessionKey = new byte[SESSION_KEY_LENGTH / 8];
        rand.nextBytes(sessionKey);
    }

    private final static BigInteger one = BigInteger.valueOf(1L);

    private static BigInteger doPublic(BigInteger input, BigInteger modulus,
				       BigInteger publicExponent) {
        return input.modPow(publicExponent, modulus);
    }

    private static BigInteger doPrivate(BigInteger input, BigInteger modulus,
					BigInteger privateExponent) {
        return doPublic(input, modulus, privateExponent);
    }

    // private static BigInteger doPrivateCrt(BigInteger input,
    // 					   BigInteger privateExponent,
    // 					   BigInteger primeP, BigInteger primeQ,
    // 					   BigInteger crtCoefficient) {
    //     return doPrivateCrt(input,
    //                         primeP, primeQ,
    //                         getPrimeExponent(privateExponent, primeP),
    //                         getPrimeExponent(privateExponent, primeQ),
    //                         crtCoefficient);
    // }

    private static BigInteger doPrivateCrt(BigInteger input,
                                          BigInteger primeP, BigInteger primeQ,
                                          BigInteger primeExponentP,
                                          BigInteger primeExponentQ,
                                          BigInteger crtCoefficient) {
        if(!crtCoefficient.equals(primeQ.modInverse(primeP))) {
            BigInteger t = primeP;
            primeP = primeQ;
            primeQ = primeP;
            t = primeExponentP;
            primeExponentP = primeExponentQ;
            primeExponentQ = t;
        }
        BigInteger s_1 = input.modPow(primeExponentP, primeP);
        BigInteger s_2 = input.modPow(primeExponentQ, primeQ);
        BigInteger h   = crtCoefficient.multiply(s_1.subtract(s_2)).mod(primeP);
        return s_2.add(h.multiply(primeQ));
    }

    private static BigInteger getPrimeExponent(BigInteger privateExponent,
            BigInteger prime) {
        BigInteger pe = prime.subtract(one);
        return privateExponent.mod(pe);
    }

    private static BigInteger addPKCS1Pad(BigInteger input, int type,
                                         int padLen, SecureRandomAndPad rand)
    throws SignatureException {
        BigInteger result;
        BigInteger rndInt;
        int inByteLen  = (input.bitLength() + 7) / 8;

        if(inByteLen > padLen - 11) {
            throw new SignatureException("PKCS1Pad: Input too long to pad");
        }

        byte[] padBytes = new byte[(padLen - inByteLen - 3) + 1];
        padBytes[0] = 0;

        for(int i = 1; i < (padLen - inByteLen - 3 + 1); i++) {
            if(type == 0x01) {
                padBytes[i] = (byte)0xff;
            } else {
                byte[] b = new byte[1];
                do {
                    rand.nextBytes(b);
                } while(b[0] == 0);
                padBytes[i] = b[0];
            }
        }

        rndInt = new BigInteger(1, padBytes);
        rndInt = rndInt.shiftLeft((inByteLen + 1) * 8);
        result = BigInteger.valueOf(type);
        result = result.shiftLeft((padLen - 2) * 8);
        result = result.or(rndInt);
        result = result.or(input);

        return result;
    }

    private static BigInteger stripPKCS1Pad(BigInteger input, int type)
    throws SignatureException {
        byte[] strip = input.toByteArray();
        byte[] val;
        int    i;

        if(strip[0] != type) {
            throw new SignatureException("Invalid PKCS1 padding, type != " +
                                         type);
        }

        for(i = 1; i < strip.length; i++) {
            if(strip[i] == 0) {
                break;
            }
            if(type == 0x01 && strip[i] != (byte)0xff) {
                throw new SignatureException("Invalid PKCS1 padding, " +
                                             "corrupt data");
            }
        }

        if(i == strip.length) {
            throw new SignatureException("Invalid PKCS1 padding, corrupt data");
        }

        val = new byte[strip.length - i];
        System.arraycopy(strip, i, val, 0, val.length);
        return new BigInteger(1, val);
    }

    private SSHPduInputStream sendSessionKey(int cipherType)
        throws IOException {
        byte[]             key = new byte[sessionKey.length + 1];
        BigInteger         encKey;
        SSHPduOutputStream pdu;

        key[0] = 0;
        System.arraycopy(sessionKey, 0, key, 1, sessionKey.length);

        for(int i = 0; i < sessionId.length; i++)
            key[i + 1] ^= sessionId[i];

        encKey = new BigInteger(key);

        int serverKeyByteLen = (srvServerKey.getModulus().bitLength() + 7) / 8;
        int hostKeyByteLen   = (srvHostKey.getModulus().bitLength() + 7) / 8;

        try {
            if (serverKeyByteLen < hostKeyByteLen) {
                BigInteger padded;
                padded = addPKCS1Pad(encKey, 2, serverKeyByteLen, rand);
                encKey = doPublic(padded,
				  srvServerKey.getModulus(),
				  srvServerKey.getPublicExponent());
                padded = addPKCS1Pad(encKey, 2, hostKeyByteLen,
				     rand);
                encKey = doPublic(padded,
				  srvHostKey.getModulus(),
				  srvHostKey.getPublicExponent());
            } else {
                BigInteger padded;
                padded = addPKCS1Pad(encKey, 2, hostKeyByteLen,
				     rand);
                encKey = doPublic(padded,
				  srvHostKey.getModulus(),
				  srvHostKey.getPublicExponent());
                padded = addPKCS1Pad(encKey, 2, serverKeyByteLen,
				     rand);
                encKey = doPublic(padded,
				  srvServerKey.getModulus(),
				  srvServerKey.getPublicExponent());
            }
        } catch (SignatureException e) {
            throw new IOException(e.getMessage());
        }

        pdu = new SSHPduOutputStream(CMSG_SESSION_KEY, null, null, rand);
        pdu.writeByte((byte)cipherType);
        pdu.write(srvCookie, 0, srvCookie.length);
        pdu.writeBigInteger(encKey);
        // !!! TODO: check this pdu.writeInt(PROTOFLAG_SCREEN_NUMBER | PROTOFLAG_HOST_IN_FWD_OPEN);
        pdu.writeInt(protocolFlags);
        pdu.writeTo(sshOut);

        // !!!
        // At this stage the communication is encrypted
        // !!!

        pduState = PDU_STATE_SENT_KEYS;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream authenticateUser1()
        throws IOException {
        SSHPduOutputStream outpdu;

        usedOTP = false;

        outpdu = new SSHPduOutputStream(CMSG_USER, sndCipher, sndComp, rand);
        outpdu.writeString(authenticator.getUsername(user));
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH1;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private int[] authTypes;
    private int authTypeIndex;

    private SSHPduInputStream authenticateUser2(SSHPduInputStream pdu)
        throws IOException {
        if(isSuccess(pdu)) {
            if(interactor != null)
                interactor.report("Authenticated directly by server, " +
                                  "no other authentication required");
            return authDone(true);
        }

        authTypes = authenticator.getAuthTypes(user);
        authTypeIndex = 0;
        return authLoop(null);
    }
    
    private SSHPduInputStream authLoop(SSHPduInputStream pdu)
        throws IOException {
        if (pdu != null) {
            if (isSuccess(pdu)) {
                return authDone(true);
            } else if (interactor != null) {
                interactor.report("Authenticating with " +
                                  authTypeDesc[authTypes[authTypeIndex-1]] +
                                  " failed");
            }
        }

        while (authTypeIndex < authTypes.length
            && !isAuthTypeSupported(authTypes[authTypeIndex])) {
            if(interactor != null) {
                interactor.report("Authenticating with " +
                                  authTypeDesc[authTypes[authTypeIndex]] +
                                  " failed, not supported by server");
            }
            authTypeIndex++;
        }

        if (authTypeIndex >= authTypes.length) {
            return authDone(false);
        }

        switch (authTypes[authTypeIndex++]) {
        case AUTH_PUBLICKEY:
            return doRSAAuth(false);
        case AUTH_PASSWORD:
            return doPasswdAuth();
        case AUTH_RHOSTS_RSA:
            return doRSAAuth(true);
        case AUTH_TIS:
            return doTISAuth();
        case AUTH_RHOSTS:
            return doRhostsAuth();
        case AUTH_SDI:
            usedOTP = true;
            return doSDIAuth();
        case AUTH_KERBEROS:
        case PASS_KERBEROS_TGT:
        default:
            throw new IOException("We do not support the selected " +
                                  "authentication type " +
                                  authTypeDesc[authTypes[authTypeIndex-1]]);
        }
    }

    private SSHPduInputStream doPasswdAuth() throws IOException{
        SSHPduOutputStream outpdu;
        String password;

        password = authenticator.getPassword(user);

        outpdu = new SSHPduOutputStream(CMSG_AUTH_PASSWORD, sndCipher, sndComp,
                                        rand);
        outpdu.writeString(password);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_SENT;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream doRhostsAuth() throws IOException {
        SSHPduOutputStream outpdu;

        outpdu = new SSHPduOutputStream(CMSG_AUTH_RHOSTS, sndCipher,
                                        sndComp, rand);
        outpdu.writeString(authenticator.getUsername(user));
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_SENT;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream doTISAuth() throws IOException {
        SSHPduOutputStream outpdu;
        outpdu = new SSHPduOutputStream(CMSG_AUTH_TIS, sndCipher,sndComp,rand);
        outpdu.writeTo(sshOut);
        
        pduState = PDU_STATE_AUTH_TIS;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream doTISAuth2(SSHPduInputStream pdu) 
        throws IOException {
        SSHPduOutputStream outpdu;

        if (pdu.type == SMSG_FAILURE) {
            throw new AuthFailException("TIS authentication server not " +
                                        "reachable or user unknown");
        } else if( pdu.type != SMSG_AUTH_TIS_CHALLENGE) {
            throw new IOException("Protocol error, expected TIS challenge " +
                                  "but got " + pdu.type);
        }
        String prompt = pdu.readString();

        String response = authenticator.getChallengeResponse(user, prompt);

        outpdu = new SSHPduOutputStream(CMSG_AUTH_TIS_RESPONSE, sndCipher,
                                        sndComp, rand);
        outpdu.writeString(response);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_SENT;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private boolean isRhosts;
    private SSHRSAKeyFile keyFile;
    private RSAPublicKey  pubKey;

    private SSHPduInputStream doRSAAuth(boolean rhosts)
        throws IOException {

        SSHPduOutputStream outpdu;
        keyFile = authenticator.getIdentityFile(user);
        pubKey  = keyFile.getPublic();
        isRhosts = rhosts;

        if(rhosts) {
            outpdu = new SSHPduOutputStream(CMSG_AUTH_RHOSTS_RSA, sndCipher,
                                            sndComp, rand);
            outpdu.writeString(authenticator.getUsername(user));
            outpdu.writeInt(pubKey.getModulus().bitLength());
            outpdu.writeBigInteger(pubKey.getPublicExponent());
            outpdu.writeBigInteger(pubKey.getModulus());
        } else {
            outpdu = new SSHPduOutputStream(CMSG_AUTH_RSA, sndCipher, sndComp,
                                            rand);
            outpdu.writeBigInteger(pubKey.getModulus());
        }
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_RSA;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream doRSAAuth2(SSHPduInputStream pdu)
        throws IOException {

        if (pdu.type == SMSG_FAILURE) {
            throw new  AuthFailException("server refused our key" +
                                         (isRhosts ? " or rhosts" : ""));
        } else if (pdu.type != SMSG_AUTH_RSA_CHALLENGE) {
            throw new IOException("Protocol error, expected RSA-challenge " +
                                  "but got " + pdu.type);
        }

        BigInteger challenge = pdu.readBigInteger();

        // First try with an empty passphrase...
        RSAPrivateCrtKey privKey;
        privKey = keyFile.getPrivate("");
		if (privKey == null) {
		    privKey = keyFile.getPrivate(
		        authenticator.getIdentityPassword(user));
		} else if (interactor != null) {
		    interactor.report("Authenticated with password-less rsa-key '"+
		                      keyFile.getComment() + "'");
		}
        if (privKey == null) {
            if (interactor != null) {
                interactor.report("invalid password for key-file '" +
                                  keyFile.getComment() + "'");
            }
            return authLoop(null);
        }
        return rsaChallengeResponse(privKey, challenge);
    }

    private final static int CANNOT_CHOOSE_PIN = 0;
    private final static int USER_SELECTABLE   = 1;
    private final static int MUST_CHOOSE_PIN   = 2;

    private SSHPduInputStream  doSDIAuth() throws IOException {
        SSHPduOutputStream outpdu;
        String password;

        password = authenticator.getChallengeResponse(
            user, authenticator.getUsername(user) + "'s SDI token passcode: ");

        outpdu = new SSHPduOutputStream(CMSG_AUTH_SDI, sndCipher, sndComp,
                                        rand);
        outpdu.writeString(password);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_SDI1;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream doSDIAuth2(SSHPduInputStream pdu)
        throws IOException {
        SSHPduOutputStream outpdu;

        switch(pdu.type) {
        case SMSG_SUCCESS:
            interactor.report("SDI authentication accepted.");
            return authDone(true);

        case SMSG_FAILURE:
            return authLoop(null);

        case CMSG_ACM_NEXT_CODE_REQUIRED:
            String password =
                interactor.promptPassword("Next token required: ");
            outpdu = new SSHPduOutputStream(CMSG_ACM_NEXT_CODE,
                                            sndCipher, sndComp, rand);
            outpdu.writeString(password);
            outpdu.writeTo(sshOut);
            pduState = PDU_STATE_AUTH_SENT;
            return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);

        case CMSG_ACM_NEW_PIN_REQUIRED:
            if (!interactor.askConfirmation(
                    "New PIN required, do you want to continue?", false)) {
                throw new IOException("new PIN not wanted");
            }

            String type       = pdu.readString();
            String size       = pdu.readString();
            int    userSelect = pdu.readInt();

            switch(userSelect) {
            case CANNOT_CHOOSE_PIN:
                throw new IOException("Failed to choose pin");

            case USER_SELECTABLE:
            case MUST_CHOOSE_PIN:
                String pwdChk;
                do {
                    password =
                        interactor.promptPassword("Please enter new PIN" +
                                                  " containing " + size +
                                                  " " + type);
                    pwdChk = interactor.promptPassword(
                        "Please enter new PIN again");
                } while (!password.equals(pwdChk));

                outpdu = new SSHPduOutputStream(CMSG_ACM_NEW_PIN, sndCipher,
                                                sndComp, rand);
                outpdu.writeString(password);
                outpdu.writeTo(sshOut);

                pduState = PDU_STATE_AUTH_SDI2;
                return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);

            default:
                throw new IOException("invalid response from server");
            }

        case CMSG_ACM_ACCESS_DENIED:
            // Fall through
        default:
            throw new AuthFailException();
        }
    }

    private SSHPduInputStream rsaChallengeResponse(RSAPrivateCrtKey privKey,
                                                   BigInteger challenge)
        throws IOException {
        MessageDigest md5;

        try {
            challenge = doPrivateCrt(challenge,
				     privKey.getPrimeP(),
				     privKey.getPrimeQ(),
				     privKey.getPrimeExponentP(),
				     privKey.getPrimeExponentQ(),
				     privKey.getCrtCoefficient());
            challenge = stripPKCS1Pad(challenge, 2);
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }

        byte[] response = challenge.toByteArray();

        try {
            md5 = com.mindbright.util.Crypto.getMessageDigest("MD5");
            if(response[0] == 0)
                md5.update(response, 1, 32);
            else
                md5.update(response, 0, 32);
            md5.update(sessionId);
            response = md5.digest();
        } catch(Exception e) {
            throw new IOException(
                "MD5 not implemented, can't generate session-id");
        }

        SSHPduOutputStream outpdu = 
            new SSHPduOutputStream(CMSG_AUTH_RSA_RESPONSE, sndCipher, sndComp,
                                   rand);
        outpdu.write(response, 0, response.length);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_AUTH_SENT;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    void initiatePlugins() {
        SSHProtocolPlugin.initiateAll(this);
    }

    private boolean requestedLocalTunnels = false;
    private int remoteTunnelIndex = 0;

    private SSHPduInputStream initiateTunnels() throws IOException {
        int i;
        if (!requestedLocalTunnels) {
            for (i = 0; i < localForwards.size(); i++) {
                LocalForward fwd = localForwards.elementAt(i);
                requestLocalPortForward(fwd.localHost, fwd.localPort,
                                        fwd.remoteHost, fwd.remotePort,
                                        fwd.plugin);
            }
            requestedLocalTunnels = true;
        }

        if (remoteTunnelIndex < remoteForwards.size()) {
            RemoteForward fwd = remoteForwards.elementAt(remoteTunnelIndex++);
            return requestRemotePortForward(fwd.remotePort, fwd.localHost,
                                            fwd.localPort, fwd.plugin);
        }
        return null;
    }

    private SSHPduInputStream requestCompression(int level) throws IOException{
        if (level == 0) {
            pduState = PDU_STATE_COMPR_DONE;
            return handlePDU(null);
        }

        SSHPduOutputStream outpdu = new SSHPduOutputStream(
            CMSG_REQUEST_COMPRESSION, sndCipher, sndComp, rand);

        outpdu.writeInt(level);
        outpdu.writeTo(sshOut);
        pduState = PDU_STATE_COMPR_REQ;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream requestCompression2(SSHPduInputStream pdu)
        throws IOException{

        int level = user.getCompressionLevel();
        if (!isSuccess(pdu) && interactor != null)
            interactor.report("Error requesting compression level: " + level);

        sndComp = SSHCompressor.getInstance(
            "zlib",SSHCompressor.COMPRESS_MODE, level);
        rcvComp = SSHCompressor.getInstance(
            "zlib", SSHCompressor.UNCOMPRESS_MODE, level);

        pduState = PDU_STATE_COMPR_DONE;
        return handlePDU(null);
    }

    private SSHPduInputStream requestMaxPacketSz(int sz) throws IOException {
        SSHPduOutputStream outpdu = new SSHPduOutputStream(
            CMSG_MAX_PACKET_SIZE, sndCipher, sndComp, rand);
        outpdu.writeInt(sz);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_MAXPKT_REQ;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream requestX11Forward() throws IOException {
        SSHPduOutputStream outpdu = new SSHPduOutputStream(
            CMSG_X11_REQUEST_FORWARDING, sndCipher, sndComp, rand);

        // !!!
        outpdu.writeString("MIT-MAGIC-COOKIE-1");
        outpdu.writeString("112233445566778899aabbccddeeff00");
        outpdu.writeInt(0);
        // !!!

        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_X11_REQ;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private SSHPduInputStream requestPTY() throws IOException {
        SSHPduOutputStream outpdu =
            new SSHPduOutputStream(CMSG_REQUEST_PTY, sndCipher, sndComp, rand);
        TerminalWindow myTerminal = null;
        if(console != null)
            myTerminal = console.getTerminal();
        if(myTerminal != null) {
            outpdu.writeString(myTerminal.terminalType());
            outpdu.writeInt(myTerminal.rows());
            outpdu.writeInt(myTerminal.cols());
            outpdu.writeInt(myTerminal.vpixels());
            outpdu.writeInt(myTerminal.hpixels());
        } else {
            outpdu.writeString("");
            outpdu.writeInt(0);
            outpdu.writeInt(0);
            outpdu.writeInt(0);
            outpdu.writeInt(0);
        }
        outpdu.writeByte((byte)TTY_OP_END);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_PTY_REQ;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    public void requestLocalPortForward(String localHost, int localPort,
                                         String remoteHost, int remotePort,
                                         String plugin)
    throws IOException {
        controller.newListenChannel(localHost, localPort, remoteHost,
                                    remotePort, plugin);
    }

    private SSHPduInputStream requestRemotePortForward(
        int remotePort, String localHost, int localPort, String plugin)
        throws IOException {
        try {
            SSHProtocolPlugin.getPlugin(plugin).remoteListener(
                remotePort, localHost, localPort, controller);
        } catch (NoClassDefFoundError e) {
            throw new IOException("Plugins not available");
        }

        SSHPduOutputStream outpdu = new SSHPduOutputStream(
            CMSG_PORT_FORWARD_REQUEST, sndCipher, sndComp, rand);
        outpdu.writeInt(remotePort);
        outpdu.writeString(localHost);
        outpdu.writeInt(localPort);
        outpdu.writeTo(sshOut);

        pduState = PDU_STATE_TUNNEL_REQ;
        return new SSHPduInputStream(MSG_ANY, rcvCipher, rcvComp);
    }

    private void requestCommand(String command) throws IOException {
        SSHPduOutputStream outpdu =
            new SSHPduOutputStream(CMSG_EXEC_CMD, sndCipher, sndComp, rand);
        outpdu.writeString(command);
        outpdu.writeTo(sshOut);
    }

    private void requestShell() throws IOException {
        SSHPduOutputStream outpdu =
            new SSHPduOutputStream(CMSG_EXEC_SHELL, sndCipher, sndComp, rand);
        outpdu.writeTo(sshOut);
    }

    private boolean isSuccess(SSHPduInputStream pdu) throws IOException {
        if (pdu.type == SMSG_SUCCESS) {
            return true;
        } else if(pdu.type == SMSG_FAILURE) {
            return false;
        } else if(pdu.type == MSG_DISCONNECT) {
            throw new IOException("Server disconnected: " +
                                  pdu.readString());
        } else {
            throw new IOException("Protocol error: got " + pdu.type +
                                  " when expecting success/failure");
        }
    }

    void setInteractive() {
        try {
            sshSocket.setTcpNoDelay(true);
        } catch (SocketException e) {
            if (interactor != null)
                interactor.report("Error setting interactive mode: " +
                                  e.getMessage());
        }
    }

    void setAliveInterval(int intervalSeconds) {
        if (heartbeat != null && heartbeat.isRunning()) {
            heartbeat.setInterval(intervalSeconds);
        } else if (intervalSeconds > 0) {
            heartbeat = new KeepAliveThread(intervalSeconds);
        }
    }

    public boolean isOpened() {
        return isOpened;
    }

    public boolean isConnected() {
        return isConnected;
    }

    void stdinWriteChar(char c) throws IOException {
        stdinWriteString(String.valueOf(c));
    }

    void stdinWriteString(String str) throws IOException {
        stdinWriteString(str.getBytes(), 0, str.length());
    }

    void stdinWriteString(byte[] str) throws IOException {
        stdinWriteString(str, 0, str.length);
    }

    void stdinWriteString(byte[] str, int off, int len) throws IOException {
        SSHPduOutputStream stdinPdu;
        if(isOpened && controller != null) {
            stdinPdu = new SSHPduOutputStream(SSH.CMSG_STDIN_DATA, sndCipher,
                                              sndComp, rand);
            stdinPdu.writeInt(len);
            stdinPdu.write(str, off, len);
            controller.transmit(stdinPdu);
        }
    }

    void signalWindowChanged(int rows, int cols, int vpixels, int hpixels) {
        if(isOpened && controller != null) {
            try {
                SSHPduOutputStream pdu;
                pdu = new SSHPduOutputStream(SSH.CMSG_WINDOW_SIZE, sndCipher,
                                             sndComp, rand);
                pdu.writeInt(rows);
                pdu.writeInt(cols);
                pdu.writeInt(vpixels);
                pdu.writeInt(hpixels);
                controller.transmit(pdu);
            } catch (Exception ex) {
                if(interactor != null)
                    interactor.alert("Error when sending sigWinch: " +
                                     ex.toString());
            }
        }
    }

    /**
     * Shut down the connection
     */
    protected void shutdown(Exception e) {
        if (timeoutcallback != null) {
            timeoutcallback.stop();
            timeoutcallback = null;
        }
        storedException = e;
        if (sshSocket != null) {
            sshSocket.close();
        }
        disconnect(false, e);
        if (controller != null) {
            controller.killListenChannels();
            controller = null;
        }
        synchronized(openMonitor) {
            openMonitor.notify();
        }
        synchronized(controllerMonitor) {
            controllerMonitor.notify();
        }
    }

    /*
     * NIOCallback
     */
    public void completed(ByteBuffer buf) {
        try {
            isReading = true;
            do {
                buf = handleReadData(buf);
            } while (buf != null && sshIn.read(buf, this, true, false));
            if (buf == null) {
                isReading = false;
            }
        } catch (IOException e) {
            shutdown(e);
        }
    }

    public void readFailed(Exception e) {
        shutdown(new IOException("Read failed"));
    }

    public void writeFailed() {
        shutdown(new IOException("Write failed"));
    }

    public void connected(boolean timeout) {
        if (timeout) {
            shutdown(new IOException("Timeout"));
        } else {
            boot();
        }
    }

    public void connectionFailed(Exception e) {
        shutdown(e);
    }
}
