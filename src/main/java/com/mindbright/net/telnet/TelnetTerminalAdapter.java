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

import java.util.NoSuchElementException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.AccessControlException;

import com.mindbright.terminal.TerminalWindow;
import com.mindbright.terminal.TerminalInputChaff;

/**
 * Glue class which connects a <code>TerminalWindow</code> to a
 * telnet server.
 *
 * @see TelnetNVT
 * @see TerminalWindow
 * @see TerminalInputChaff
 */
public class TelnetTerminalAdapter extends TerminalInputChaff
    implements TelnetEventHandler {
    private TelnetNVT      telnet;
    private TerminalWindow terminal;
    private StringBuffer   lineBuffer;
    private boolean        bufferedInput;
    private boolean        doNAWS;
    private boolean        doBinary = false;
    private boolean        crmod;
    private boolean        crlf = true;

    /**
     * Connects the given <code>TerminalWindow</code> to a telnet
     * server at the other end of the provided streams.
     *
     * @param in stream from which data from the telnet server is read
     * @param out stream over which to send data to the telnet server
     * @param terminal instance of terminal window to connect to
     */
    public TelnetTerminalAdapter(InputStream in, OutputStream out,
                                 TerminalWindow terminal) {
        this.telnet        = new TelnetNVT(this, in, out);
        this.terminal      = terminal;
        this.lineBuffer    = new StringBuffer();
        this.bufferedInput = true;

        terminal.addInputListener(this);
        crmod = true;
        try {
            crlf = Boolean.parseBoolean(terminal.getProperty("crlf"));
        } catch (NoSuchElementException e) {
        } catch (AccessControlException e) {}

        telnet.start();
    }

    /**
     * Get the underlying telnet protocol instance.
     */
    public TelnetNVT getTelnetNVT() {
        return telnet;
    }

    /**
     * Tells if input is sent directly to server (raw mode) or line by
     * line (cooked).
     *
     * @return true if input is sent line-by-line
     */
    public boolean isBuffered() {
        return bufferedInput;
    }

    public void interpretAsCommand(int cmd) {}

    public void optionSubNegotiation(int option, byte[] params)
    throws IOException {
        byte[] reply = null;

        if(params[0] == (byte)TelnetNVT.SB_CMD_SEND) {
            switch(option) {
            case TelnetNVT.OPT_TTYPE:
                reply = terminal.terminalType().getBytes();
                break;
            case TelnetNVT.OPT_SPEED:
                reply = "38400,38400".getBytes();
                break;
            }
            //	case TelnetNVT.OPT_XDISP:
            //	case TelnetNVT.OPT_NEWENV:
        }
        if(reply != null) {
            byte[] tmp = reply;
            int    n   = tmp.length;
            reply = new byte[n + 1];
            reply[0] = (byte)TelnetNVT.SB_CMD_IS;
            System.arraycopy(tmp, 0, reply, 1, n);
            telnet.sendOptionSubNegotiation(option, reply);
        }
    }

    public boolean optionNegotiation(int option, int request)
        throws IOException {
        boolean handled = false;
        switch(option) {
        case TelnetNVT.OPT_ECHO:
            if(request == TelnetNVT.CODE_WILL) {
                handled = true;
                bufferedInput = false;
                crmod = false;
            } else if (request == TelnetNVT.CODE_WONT) {
                handled = true;
                bufferedInput = true;
                crmod = true;
            }
            break;
        case TelnetNVT.OPT_SGA:
        case TelnetNVT.OPT_STATUS:
            if(request == TelnetNVT.CODE_WILL) {
                handled = true;
                telnet.doOption(option);
            }
            break;
        case TelnetNVT.OPT_BINARY:
            if (request == TelnetNVT.CODE_WILL) {
                handled = true;
                telnet.doOption(option);
            } else if(request == TelnetNVT.CODE_DO) {
                handled = true;
                doBinary = true;
                telnet.willOption(option);
            }
            break;
        case TelnetNVT.OPT_NAWS:
        case TelnetNVT.OPT_TTYPE:
        case TelnetNVT.OPT_SPEED:
        case TelnetNVT.OPT_REMFCTL:
            //	case TelnetNVT.OPT_XDISP:
            //	case TelnetNVT.OPT_NEWENV:
            if(request == TelnetNVT.CODE_DO) {
                handled = true;
                telnet.willOption(option);
                if(option == TelnetNVT.OPT_NAWS) {
                    doNAWS = true;
                    signalWindowChanged(terminal.rows(), terminal.cols(),
                                        terminal.vpixels(), terminal.hpixels());
                }
            }
            break;
        }
        return handled;
    }

    public void receiveData(byte b) {
        terminal.write(b);
    }

    /**
     * Send a typed char to the server. The character may not be sent
     * directly if the terminal is in buffered mode.
     *
     * @param c typed character
     */
    protected void sendTypedChar(int c) {
        try {
            if(bufferedInput) {
                if(c == 127 || c == 0x08) {
                    if(lineBuffer.length() > 0) {
                        boolean ctrlChar = false;
                        if(lineBuffer.charAt(lineBuffer.length() - 1) < ' ') {
                            ctrlChar = true;
                        }
                        lineBuffer.setLength(lineBuffer.length() - 1);
                        terminal.write((char)8);
                        if(ctrlChar)
                            terminal.write((char)8);
                        terminal.write(' ');
                        if(ctrlChar)
                            terminal.write(' ');
                        terminal.write((char)8);
                        if(ctrlChar)
                            terminal.write((char)8);
                    } else {
                        terminal.ringBell();
                    }
                } else if(c == '\n' || c == '\r') {
                    terminal.write("\r\n");
                    if (doBinary) {
                        lineBuffer.append((char)c);
                    } else {
                        lineBuffer.append('\r');
                        if (!crlf) {
                            lineBuffer.append('\0');
                        } else {
                            lineBuffer.append('\n');
                        }
                    }
                    byte[] line = lineBuffer.toString().getBytes();
                    telnet.sendData(line, 0, line.length);
                    lineBuffer.setLength(0);
                } else {
                    lineBuffer.append((char)c);
                    terminal.write((char)c);
                }
            } else {
                if (c == '\n' && !doBinary) {
                    if (crmod) {
                        telnet.sendData(new byte[] {(byte)TelnetNVT.CODE_CR,
                                                    (byte)TelnetNVT.CODE_LF});
                    } else {
                        telnet.sendData('\n');
                    }
                } else if (c == '\r' && !doBinary) {
                    byte b2;
                    if (!crlf) {
                        b2 = (byte)'\0';
                    } else {
                        b2 = (byte)TelnetNVT.CODE_LF;
                    }
                    telnet.sendData(new byte[] {(byte)TelnetNVT.CODE_CR, b2});
                } else {
                    telnet.sendData(c);
                }
            }
        } catch (IOException e) {
            // !!! TODO, terminate this session
        }
    }

    protected void sendFakeChar() {
        /* Must be implemented in a subclass if we want chaff */
    }

    /**
     * Send a number of bytes to the server.
     */
    public void sendBytes(byte[] b) {
        if (isChaffActive() || bufferedInput) {
            for(int i = 0; i < b.length; i++) {
                typedChar((char)b[i]);
            }
        } else {
            try {
                telnet.sendData(b, 0, b.length);
            } catch (IOException e) {
                // !!! TODO, terminate this session
            }
        }
    }

    /**
     * Send a number of bytes directly to the server.
     */
    public void sendBytesDirect(byte[] b) {
        try {
            telnet.sendData(b, 0, b.length);
        } catch (IOException e) {
            // !!! TODO, terminate this session
        }
    }

    /**
     * Tell the telnet server that our window has changed size.
     *
     * @param rows new number of rows
     * @param cols new number of columns
     * @param vpixels number of verticial pixels
     * @param hpixels number of horizontal pixels
     */
    public void signalWindowChanged(int rows, int cols,
                                    int vpixels, int hpixels) {
        if(doNAWS) {
            byte[] size = new byte[4];
            size[0] = (byte)((cols >>>  8) & 0xff);
            size[1] = (byte)(cols & 0xff);
            size[2] = (byte)((rows >>>  8) & 0xff);
            size[3] = (byte)(rows & 0xff);
            try {
                telnet.sendOptionSubNegotiation(TelnetNVT.OPT_NAWS, size);
            } catch (IOException e) {
                // !!! TODO FATAL HANGUP
            }
        }
    }

    /**
     * Send a break to the terminal server.
     */
    public void sendBreak() {
        try {
            telnet.sendBreak();
        } catch (IOException e) {
            // !!! TODO FATAL HANGUP
        }

    }

}
