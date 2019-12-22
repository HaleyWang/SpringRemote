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

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;

import com.mindbright.util.InputStreamPipe;
import com.mindbright.util.OutputStreamPipe;

/**
 * This class implements session channels as defined in the connection protocol
 * spec. It can be used to start shells, commands, and subsystems on
 * the server. An instance of this class is created with the
 * <code>newSession</code> methods found in
 * {@link SSH2Connection SSH2Connection}.
 *
 * @see SSH2Channel
 * @see SSH2Connection
 */
public final class SSH2SessionChannel extends SSH2InternalChannel {

    public static final int EXIT_ON_CLOSE   = -1;
    public static final int EXIT_ON_FAILURE = -2;

    private static final int STATUS_NONE    = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILURE = 2;

    protected boolean started;
    protected boolean blocking;
    protected boolean exited;
    protected Object  exitMonitor;
    protected Object  reqMonitor;
    protected int     exitStatus;
    protected int     reqStatus;
    protected boolean x11Mapping;

    protected InputStreamPipe stderr;
    protected OutputStream    stderrW;

    protected SSH2SessionChannel(SSH2Connection connection) {
        super(SSH2Connection.CH_TYPE_SESSION, connection);
        init();
    }

    protected SSH2SessionChannel(SSH2Connection connection, boolean pty) {
        super(SSH2Connection.CH_TYPE_SESSION, connection, pty);
        init();
    }

    protected SSH2SessionChannel(SSH2Connection connection,
                                 NonBlockingInput in, NonBlockingOutput out,
                                 NonBlockingOutput err) {
        super(SSH2Connection.CH_TYPE_SESSION, connection, in, out);
        init();
    }

    protected SSH2SessionChannel(SSH2Connection connection,
                                 NonBlockingInput in, NonBlockingOutput out,
                                 NonBlockingOutput err, boolean pty) {
        super(SSH2Connection.CH_TYPE_SESSION, connection, in, out, pty);
        init();
    }

    private void init() {
        this.started     = false;
        this.exited      = false;
        this.blocking    = true;
        this.reqStatus   = STATUS_NONE;
        this.x11Mapping  = false;
        this.exitMonitor = new Object();
        this.reqMonitor  = new Object();
        this.stderrW     = null;
    }

    /**
     * Launch the users shell in this session
     *
     * @return true if the shell was started and false of failure
     */
    public boolean doShell() {
        if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }
        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_SHELL);
        started = sendAndBlockUntilReply(pdu);
        return started;
    }

    /**
     * Launch a single command in this session
     *
     * @return true if the command was started and false of failure
     */
    public boolean doSingleCommand(String command) {
        if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }
        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_EXEC);
        pdu.writeString(command);
        started = sendAndBlockUntilReply(pdu);
        return started;
    }

    /**
     * Launch a subsystem
     *
     * @return true if the subsystem was started and false of failure
     */
    public boolean doSubsystem(String subsystem) {
        if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }
        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_SUBSYSTEM);
        pdu.writeString(subsystem);
        started = sendAndBlockUntilReply(pdu);
        return started;
    }

    // draft-ietf-secsh-break
    public boolean doBreak(int length) {
        if(openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }

        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_BREAK);
        pdu.writeInt(length);
        return sendAndBlockUntilReply(pdu);
    }

    /**
     * Wait for the last command to finish. There is no timeout with
     * this call.
     */
    public int waitForExit() {
        return waitForExit(0);
    }

    /**
     * Wait for the last command to exit but return after the
     * specified time has passed even if the command has not exited.
     *
     * @param timeout how long to wait in milliseconds
     *
     * @return the exit status of the command if it has finished. This
     *         value is random if the call timed out. See the
     *         {@link #isFinished() isFinished()} method to help
     *         determine which is the case.
     */
    public int waitForExit(long timeout) {
        synchronized (exitMonitor) {
            if(!exited) {
                try {
                    exitMonitor.wait(timeout);
                } catch (InterruptedException e) { /* don't care */
                }
            }
            // !!! TODO: Handle signals, maybe should throw exception ???
            return exitStatus;
        }
    }

    /**
     * Checks if the last command has already finished.
     * This can be used with the waitForExit(long timeout) method to know if
     * that method has exited because the command was finished or because
     * a the timeout.
     */
    public boolean isFinished() {
        synchronized (exitMonitor) {
            return exited;
        }
    }

    public void changeStdOut(OutputStream out) {
        if (nbout != null) {
            throw new IllegalArgumentException(
                "Instance uses non-blocking IO");
        }
        this.out = out;
    }

    public void changeStdIn(InputStream in) {
        this.in = in;
    }

    public void changeStdErr(OutputStream stderrW) {
        this.stderrW = stderrW;
    }

    public void changeStdOut(NonBlockingOutput out) {
        if (nbout == null) {
            throw new IllegalArgumentException("Instance uses blocking IO");
        }
        this.nbout = out;
    }

    public void enableStdErr() {
        this.stderrW = new OutputStreamPipe();
        this.stderr  = new InputStreamPipe();
        try {
            this.stderr.connect((OutputStreamPipe)stderrW);
        } catch (IOException e) {
            connection.getLog().error("SSH2SessionChannel", "enableStdErr",
                                      "can't happen, bug somewhere!?!");
        }
    }

    public InputStream getStdOut() {
        return getInputStream();
    }

    public OutputStream getStdIn() {
        return getOutputStream();
    }

    public InputStream getStdErr() {
        return stderr;
    }

    public NonBlockingOutput getNBStdIn() {
        return nbout;
    }

    public NonBlockingInput getNBStdOut() {
        return nbin;
    }

    public void stdinWriteNoLatency(String str) {
        byte[] b = str.getBytes();
        stdinWriteNoLatency(b, 0, b.length);
    }

    public void stdinWriteNoLatency(byte[] buf, int off, int len) {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_DATA,
                                                  len + 128);
        pdu.writeInt(peerChanId);
        pdu.writeInt(len);
        pdu.writeRaw(buf, off, len);
        transmit(pdu);
        txCounter += len;
    }

    public void stdinWriteNoLatency(int c) {
        stdinWriteNoLatency(new byte[] { (byte)c }, 0, 1);
    }

    public void setBlocking(boolean value) {
        synchronized (reqMonitor) {
            this.blocking = value;
        }
    }

    public boolean requestPTY(String termType, int rows, int cols,
                              byte[] terminalModes) {
        if(openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }

        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_PTY);
        pdu.writeString(termType);
        pdu.writeInt(cols);
        pdu.writeInt(rows);
        pdu.writeInt(0);
        pdu.writeInt(0);
        if(terminalModes == null)
            terminalModes = new byte[] { 0 };
        pdu.writeString(terminalModes);
        return sendAndBlockUntilReply(pdu);
    }

    public boolean requestX11Forward(String localAddr, int localPort,
                                     byte[] cookie, boolean single, int screen) {
        connection.getPreferences().setPreference(SSH2Preferences.X11_DISPLAY,
                localAddr + ":" + localPort);
        connection.setX11RealCookie(cookie);
        return requestX11Forward(single, screen);
    }

    public boolean requestX11Forward(boolean single, int screen) {
        return requestX11Forward(single, screen, null);
    }

    public boolean requestX11Forward(boolean single, int screen, byte[] cookie) {
        if(openStatus() != SSH2Channel.STATUS_OPEN || x11Mapping) {
            if(x11Mapping)
                connection.getLog().warning("SSH2SessionChannel",
                                            "requesting x11 forward multiple times");
            return false;
        }

        if (cookie != null) {
            connection.setX11RealCookie(cookie);
        }

        byte[] x11FakeCookie = connection.getX11FakeCookie();
        StringBuilder cookieBuf = new StringBuilder();
        for(int i = 0; i < 16; i++) {
            String b = Integer.toHexString(x11FakeCookie[i] & 0xff);
            if(b.length() == 1) {
                cookieBuf.append("0");
            }
            cookieBuf.append(b);
        }
        String           fake = cookieBuf.toString();
        SSH2TransportPDU pdu  = getRequestPDU(SSH2Connection.CH_REQ_X11);

        pdu.writeBoolean(single);
        pdu.writeString("MIT-MAGIC-COOKIE-1");
        pdu.writeString(fake);
        pdu.writeInt(screen);

        x11Mapping = sendAndBlockUntilReply(pdu);

        if(x11Mapping) {
            connection.setX11Mapping(single);
        }

        return x11Mapping;
    }

    public boolean setEnvironment(String name, String value) {
        if(openStatus() != SSH2Channel.STATUS_OPEN) {
            return false;
        }

        SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_ENV);
        pdu.writeString(name);
        pdu.writeString(value);
        return sendAndBlockUntilReply(pdu);
    }

    public void sendWindowChange(int rows, int cols) {
        SSH2TransportPDU pdu =
            getNoReplyRequestPDU(SSH2Connection.CH_REQ_WINCH);
        pdu.writeInt(cols);
        pdu.writeInt(rows);
        pdu.writeInt(0);
        pdu.writeInt(0);
        transmit(pdu);
    }

    public void sendSignal(String signal) {
        SSH2TransportPDU pdu =
            getNoReplyRequestPDU(SSH2Connection.CH_REQ_SIGNAL);
        pdu.writeBoolean(false);
        pdu.writeString(signal);
        transmit(pdu);
    }

    public void doExit(int status) {
        doExit(status, false, null);
    }

    public void doExit(int status, boolean onSignal) {
        doExit(status, onSignal, null);
    }

    public void doExit(int status, boolean onSignal, String signal) {
        synchronized (exitMonitor) {
            if(!exited) {
                exited = true;
                if(x11Mapping) {
                    x11Mapping = false;
                    connection.clearX11Mapping();
                }
                if (signal == null)
                    signal = "<unknown>";
                this.exitStatus     = status;
                exitMonitor.notifyAll();

                connection.getLog().info("SSH2SessionChannel",
                                         "session (ch. #" + channelId +
                                         ") exit with " +
                                         (onSignal ? ("signal " + signal) :
                                          ("status " + status)));
            }
        }
    }

    protected void extData(SSH2TransportPDU pdu) {
        int type = pdu.readInt();
        if(type != SSH2.EXTENDED_DATA_STDERR) {
            connection.getLog().error("SSH2SessionChannel", "extData",
                                      "extended data of unknown type: " + type);
        } else {
            try {

                int    len  = pdu.readInt();
                byte[] data = pdu.getData();
                int    off  = pdu.getRPos();
                rxCounter += len;
                if(stderrW != null) {
                    stderrW.write(data, off, len);
                } else {
                    connection.getLog().debug("SSH2SessionChannel",
                                              "session " + "(ch. #" + channelId +
                                              ") stderr : " +
                                              new String(data, off, len));
                }
                checkRxWindowSize(len);
            } catch (IOException e) {
                connection.getLog().error("SSH2SessionChannel", "extData",
                                          "error writing to stderr: " +
                                          e.getMessage());
            }
        }
    }

    protected void closeImpl() {
        super.closeImpl();
        doExit(EXIT_ON_CLOSE);
        //
        // Just to make sure everybody gets released
        //
        requestFailure((SSH2TransportPDU)null);
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
                                      String langTag) {
        doExit(EXIT_ON_FAILURE);
        return false;
    }

    protected void requestSuccess(SSH2TransportPDU pdu) {
        synchronized (reqMonitor) {
            if (reqStatus == STATUS_NONE) {
                reqStatus = STATUS_SUCCESS;
            }
            reqMonitor.notify();
        }
    }

    protected void requestFailure(SSH2TransportPDU pdu) {
        synchronized (reqMonitor) {
            if (reqStatus == STATUS_NONE) {
                reqStatus = STATUS_FAILURE;
            }
            reqMonitor.notify();
        }
    }

    protected void handleRequestImpl(String type, boolean wantReply,
                                     SSH2TransportPDU pdu) {

        // !!! TODO: Handle exit properly...

        if(type.equals(SSH2Connection.CH_REQ_EXIT_STAT)) {
            int status = pdu.readInt();
            doExit(status);

        } else if(type.equals(SSH2Connection.CH_REQ_EXIT_SIG)) {
            String sig = null;
            try {
                sig  = pdu.readJavaString();
                boolean core = pdu.readBoolean();
                pdu.readJavaString();
                String  lang = pdu.readJavaString();

                connection.getLog().debug("SSH2SessionChannel", "handleRequestImpl",
                                          "got CH_REQ_EXIT_SIG: " +
                                          " sig=" + sig +", core=" + core +
                                          ", msg=" + lang + ", lang=" + lang);
            } catch (Throwable t) {}

            doExit(-1, true, sig);

            // !!! TODO: store msg/core also !!!

        } else {
            if (!(type.equals(SSH2Connection.CH_REQ_OPENSSH_KEEPALIVE) ||
                  type.equals(SSH2Connection.CH_REQ_OPENSSH_EOW))) {
                connection.getLog().error("SSH2SessionChannel", "handleRequestImpl",
                                          "got unknown channel-request: " + type);
            }

            if(wantReply) {
                SSH2TransportPDU reply =
                    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_FAILURE);
                reply.writeInt(peerChanId);
                transmit(reply);
            }
        }
    }

    private boolean sendAndBlockUntilReply(SSH2TransportPDU pdu) {
        synchronized (reqMonitor) {
            transmit(pdu);
            try {
                if(blocking)
                    reqMonitor.wait();
            } catch (InterruptedException e) {
                connection.getLog().error("SSH2SessionChannel",
                                          "sendAndBlockUntilReply",
                                          "wait for reply interrupted");
            }
            boolean s = (reqStatus == STATUS_SUCCESS);
            reqStatus = STATUS_NONE;
            return s;
        }
    }

    private SSH2TransportPDU getRequestPDU(String type) {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_REQUEST);
        pdu.writeInt(peerChanId);
        pdu.writeString(type);
        synchronized (reqMonitor) {
            pdu.writeBoolean(blocking);
        }
        return pdu;
    }

    private SSH2TransportPDU getNoReplyRequestPDU(String type) {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_REQUEST);
        pdu.writeInt(peerChanId);
        pdu.writeString(type);
        pdu.writeBoolean(false);
        return pdu;
    }

}
