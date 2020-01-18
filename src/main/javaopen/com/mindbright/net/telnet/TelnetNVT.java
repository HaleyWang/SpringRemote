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

package com.mindbright.net.telnet;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


/**
 * Implements the client side of the telnet protocol (rfc854). This
 * class handles most of the protocol basics. It is meant to be used
 * together with an <code>TelnetEventHandler</code> which gets the
 * actual data received from the other end.
 * <p>
 * Most of the contants and methods in this class should be fairly
 * obvious to people having deep enough knowledge of rfc854.
 *
 * @see TelnetEventHandler
 */
public class TelnetNVT implements Runnable {

    private static final int STATE_DATA    = 0;
    private static final int STATE_IAC     = 1;
    private static final int STATE_OPTNEG  = 2;
    private static final int STATE_SUBNEG  = 4;
    private static final int STATE_SUBEND  = 5;
    private static final int STATE_CR      = 6;
    private static final int STATE_SKIP    = 7;
    private static final int STATE_ILLEGAL = 8;

    public static final int CODE_LF    = 10;
    public static final int CODE_CR    = 13;
    public static final int CODE_EOR   = 239;
    public static final int CODE_SE    = 240;
    public static final int CODE_NOP   = 241;
    public static final int CODE_DM    = 242;
    public static final int CODE_BREAK = 243;
    public static final int CODE_IP    = 244;
    public static final int CODE_ABORT = 245;
    public static final int CODE_AYT   = 246;
    public static final int CODE_EC    = 247;
    public static final int CODE_EL    = 248;
    public static final int CODE_GA    = 249;
    public static final int CODE_SB    = 250;
    public static final int CODE_WILL  = 251;
    public static final int CODE_WONT  = 252;
    public static final int CODE_DO    = 253;
    public static final int CODE_DONT  = 254;
    public static final int CODE_IAC   = 255;

    /*
     * IANA assigned options numbers:
     * (from http://www.iana.org/assignments/telnet-options)
     *   0     Binary Transmission                                 [RFC856]
     *   1     Echo                                                [RFC857]
     *   2     Reconnection                                      [NIC50005]
     *   3     Suppress Go Ahead                                   [RFC858]
     *   4     Approx Message Size Negotiation                   [ETHERNET]
     *   5     Status                                              [RFC859]
     *   6     Timing Mark                                         [RFC860]
     *   7     Remote Controlled Trans and Echo                    [RFC726]
     *   8     Output Line Width                                 [NIC50005]
     *   9     Output Page Size                                  [NIC50005]
     *  10     Output Carriage-Return Disposition                  [RFC652]
     *  11     Output Horizontal Tab Stops                         [RFC653]
     *  12     Output Horizontal Tab Disposition                   [RFC654]
     *  13     Output Formfeed Disposition                         [RFC655]
     *  14     Output Vertical Tabstops                            [RFC656]
     *  15     Output Vertical Tab Disposition                     [RFC657]
     *  16     Output Linefeed Disposition                         [RFC658]
     *  17     Extended ASCII                                      [RFC698]
     *  18     Logout                                              [RFC727]
     *  19     Byte Macro                                          [RFC735]
     *  20     Data Entry Terminal                         [RFC1043,RFC732]
     *  21     SUPDUP                                       [RFC736,RFC734]
     *  22     SUPDUP Output                                       [RFC749]
     *  23     Send Location                                       [RFC779]
     *  24     Terminal Type                                      [RFC1091]
     *  25     End of Record                                       [RFC885]
     *  26     TACACS User Identification                          [RFC927]
     *  27     Output Marking                                      [RFC933]
     *  28     Terminal Location Number                            [RFC946]
     *  29     Telnet 3270 Regime                                 [RFC1041]
     *  30     X.3 PAD                                            [RFC1053]
     *  31     Negotiate About Window Size                        [RFC1073]
     *  32     Terminal Speed                                     [RFC1079]
     *  33     Remote Flow Control                                [RFC1372]
     *  34     Linemode                                           [RFC1184]
     *  35     X Display Location                                 [RFC1096]
     *  36     Environment Option                                 [RFC1408]
     *  37     Authentication Option                              [RFC2941]
     *  38     Encryption Option                                  [RFC2946]
     *  39     New Environment Option                             [RFC1572]
     *  40     TN3270E                                            [RFC1647]
     *  41     XAUTH                                              [Earhart]
     *  42     CHARSET                                            [RFC2066]
     *  43     Telnet Remote Serial Port (RSP)                     [Barnes]
     *  44     Com Port Control Option                            [RFC2217]
     *  45     Telnet Suppress Local Echo                           [Atmar]
     *  46     Telnet Start TLS                                       [Boe]
     *  47     KERMIT                                             [RFC2840]
     *  48     SEND-URL                                             [Croft]
     *  49     FORWARD_X                                           [Altman]
     * 50-137  Unassigned                                            [IANA]
     * 138     TELOPT PRAGMA LOGON                              [McGregory]
     * 139     TELOPT SSPI LOGON                                [McGregory]
     * 140     TELOPT PRAGMA HEARTBEAT                          [McGregory]
     * 255     Extended-Options-List                               [RFC861]
     */
    public static final int OPT_BINARY   = 0;
    public static final int OPT_ECHO     = 1;
    public static final int OPT_SGA      = 3;
    public static final int OPT_STATUS   = 5;
    public static final int OPT_TTYPE    = 24;
    public static final int OPT_EOR      = 25;
    public static final int OPT_NAWS     = 31;
    public static final int OPT_SPEED    = 32;
    public static final int OPT_REMFCTL  = 33;
    public static final int OPT_XDISP    = 35;
    public static final int OPT_NEWENV   = 39;

    public static final int SB_CMD_IS    = 0;
    public static final int SB_CMD_SEND  = 1;
    public static final int SB_CMD_REPLY = 2;
    public static final int SB_CMD_NAME  = 3;

    private TelnetEventHandler eventHandler;
    private InputStream        in;
    private OutputStream       out;
    private OutputStream       optionOut = null;
    private Thread             myThread;
    private int                reqCode;
    private StringBuilder      sendBuffer;

    /**
     * Create a new instance of the telnet protocol. This will use the
     * given streams to comminucate with the telnet server and pass
     * received data to the given event handler.
     *
     * @param eventHandler event handler which will get the data
     * received
     * @param in stream from which data from the telnet server is read
     * @param out stream over which to send data to the telnet server
     */
    public TelnetNVT(TelnetEventHandler eventHandler,
                     InputStream in, OutputStream out) {
        this.eventHandler = eventHandler;
        this.in           = in;
        this.out          = out;
        this.myThread     = new Thread(this, "TelnetNVT");
        this.sendBuffer   = new StringBuilder();
    }

    public void start() {
        this.myThread.start();
    }

    public void run() {
        int    state      = STATE_DATA;
        byte[] subNegData = new byte[256];
        int    si         = 0;
        int    b;

        try {
            while((b = in.read()) != -1) {
                switch(state) {
                case STATE_DATA:
                    if(b == CODE_CR) {
                        state = STATE_CR;
                    } else if(b == CODE_IAC) {
                        state = STATE_IAC;
                    }
                    break;
                case STATE_IAC:
                    state = interpretAsCommand(b);
                    break;
                case STATE_OPTNEG:
                    optionNegotiation(b, reqCode);
                    state = STATE_SKIP;
                    break;
                case STATE_SUBEND:
                    if(b == CODE_SE) {
                        byte[] tmp = new byte[si - 1];
                        System.arraycopy(subNegData, 1, tmp, 0, si - 1);
                        si = 0;
                        optionOut = new BufferedOutputStream(out);
                        eventHandler.optionSubNegotiation(
                            (subNegData[0]) & 0xff, tmp);
                        optionOut.flush();
                        optionOut = null;
                        state = STATE_SKIP;
                        break;
                    }
                    // Fall through!
                case STATE_SUBNEG:
                    if(b == CODE_IAC && state == STATE_SUBNEG) {
                        state = STATE_SUBEND;
                    } else {
                        if(si == 256) {
                            state = STATE_ILLEGAL;
                        } else {
                            subNegData[si++] = (byte)b;
                        }
                    }
                    break;
                case STATE_CR:
                    if(b == 0) {
                        state = STATE_SKIP;
                    } else {
                        state = STATE_DATA;
                    }
                }
                if(state == STATE_DATA || state == STATE_CR) {
                    eventHandler.receiveData((byte)b);
                } else if(state == STATE_SKIP) {
                    state = STATE_DATA;
                } else if(state == STATE_ILLEGAL) {
                    // !!! FATAL ERROR !!!
                    // note, don't want exception, callback to eventhandler instead
                }
            }
            // !!! REPORT EOF to eventhandler !!!
        } catch (IOException e) {
            // !!! FATAL ERROR !!!
            // note, don't want exception, callback to eventhandler instead
        }
    }

    public Thread getThread() {
        return myThread;
    }

    public synchronized void sendData(byte[] data) throws IOException {
        sendData(data, 0, data.length);
    }

    public synchronized void sendData(byte[] data, int off, int sz)
    throws IOException {
        int lastOff = off + sz;

        // Create one big chunk of data and send it
        while(off < lastOff) {
            int b = 0xff & data[off++];
            if(b == CODE_IAC) {
                sendBuffer.append((char) b);
            }
            sendBuffer.append((char) b);
        }
        out.write(sendBuffer.toString().getBytes());
        sendBuffer.setLength(0);
    }

    public synchronized void sendData(int b) throws IOException {
        if(b == CODE_IAC) {
            out.write(b);
        }
        out.write(b);
    }

    public synchronized void sendBreak() throws IOException {
        String data = String.valueOf((char) CODE_IAC) + (char) CODE_BREAK;
        out.write(data.getBytes());
    }

    public synchronized void doOption(int option) throws IOException {
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_DO);
        optionOut.write(option);
    }

    public synchronized void dontOption(int option) throws IOException {
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_DONT);
        optionOut.write(option);
    }

    public synchronized void willOption(int option) throws IOException {
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_WILL);
        optionOut.write(option);
    }

    public synchronized void wontOption(int option) throws IOException {
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_WONT);
        optionOut.write(option);
    }

    public synchronized void sendOptionSubNegotiation(int option,
                                                      byte[] params)
        throws IOException {
        boolean flush = false;
        if (optionOut == null) {
            flush = true;
            optionOut = new BufferedOutputStream(out);
        }
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_SB);
        optionOut.write(option);
        optionOut.write(params, 0, params.length);
        optionOut.write(CODE_IAC);
        optionOut.write(CODE_SE);
        if (flush) {
            optionOut.flush();
            optionOut = null;
        }
    }

    protected void optionNegotiation(int option, int request)
        throws IOException {
        optionOut = new BufferedOutputStream(out);

        if(!eventHandler.optionNegotiation(option, request)) {
            int reply = -1;
            switch(request) {
            case CODE_WILL:
                reply = CODE_DONT;
                break;
            case CODE_DO:
                reply = CODE_WONT;
                break;
            case CODE_WONT:
            case CODE_DONT:
            }
            if(reply > 0) {
                optionOut.write(CODE_IAC);
                optionOut.write(reply);
                optionOut.write(option);
            }
        }
        optionOut.flush();
        optionOut = null;
    }

    protected int interpretAsCommand(int cmd) {
        int newState = STATE_SKIP;
        switch(cmd) {
        case CODE_SB:
            newState = STATE_SUBNEG;
            break;
        case CODE_WILL:
        case CODE_WONT:
        case CODE_DO:
        case CODE_DONT:
            reqCode  = cmd;
            newState = STATE_OPTNEG;
            break;
        case CODE_IAC:
            newState = STATE_DATA;
            break;
        case CODE_NOP:
        case CODE_AYT:
        case CODE_EC:
        case CODE_EL:
        case CODE_GA:
        case CODE_DM:
        case CODE_BREAK:
        case CODE_IP:
        case CODE_ABORT:
        case CODE_EOR:
            eventHandler.interpretAsCommand(cmd);
            break;
        case CODE_SE:
        default:
            newState = STATE_ILLEGAL;
            break;
        }
        return newState;
    }

}
