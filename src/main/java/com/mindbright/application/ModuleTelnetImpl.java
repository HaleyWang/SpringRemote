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

package com.mindbright.application;

import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.awt.Frame;

import com.mindbright.net.telnet.TelnetTerminalAdapter;
import com.mindbright.ssh2.SSH2InternalChannel;
import com.mindbright.ssh2.SSH2Channel;
import com.mindbright.terminal.TerminalWindow;
import com.mindbright.terminal.LineReaderTerminal;
import com.mindbright.terminal.TerminalFrameTitle;
import com.mindbright.util.Util;

public class ModuleTelnetImpl extends ModuleBaseTerminal {
    private String                remoteHost;
    private int                   remotePort;
    private SSH2InternalChannel   channel;
    private TelnetTerminalAdapter telnetAdapter;
    private InputStream           telnetIn;
    private OutputStream          telnetOut;
    private LineReaderTerminal    lineReader;
    private boolean               localConnect;
    private Socket                socket;

    protected class ChaffedTelnet extends TelnetTerminalAdapter {

        public ChaffedTelnet(InputStream in, OutputStream out,
                             TerminalWindow terminalWin) {
            super(in, out, terminalWin);
        }

        protected void sendFakeChar() {
            if(channel != null) {
                /*
                 * 5 bytes of "ignore-payload" makes packet same size as one
                 * byte channel data (i.e. a key-press). 
                 */
                byte[] chaff = new byte[] { 1, 2, 3, 4, 5 };
                channel.getConnection().getTransport().sendIgnore(chaff);
            }
        }

    }

    public void runTerminal(MindTermApp mindterm, TerminalWindow terminal,
                            Frame frame, TerminalFrameTitle frameTitle) {
        try {
            remoteHost = mindterm.getProperty("module.telnet.host");
            remotePort = getPort(mindterm.getProperty("module.telnet.port"));

            localConnect = true;

            if(remoteHost == null) {
                lineReader = new LineReaderTerminal(terminal);
                while (remoteHost == null) {
                    terminal.clearScreen();
                    if(!(mindterm.isConnected() &&
                            (mindterm.getConnection() != null))) {
                        terminal.write("Not connected to ssh2 server, " +
                                       "can only make direct connections.\r\n");
                    } else {
                        terminal.write("Prefix hostname with '|' to make"+
                                       " a tunneled connection.\r\n");
                    }
                    lineReader.print("\r\n");
                    remoteHost = lineReader.promptLine(
                                     "remote host[:port] : ", null, false);
                }
                if(remoteHost.trim().length() == 0) {
                    return;
                }

                if(remoteHost.charAt(0) == '|') {
                    localConnect = false;
                    remoteHost = remoteHost.substring(1, remoteHost.length());
                }

                lineReader.detach();
            }

            remotePort = Util.getPort(remoteHost, remotePort);
            remoteHost = Util.getHost(remoteHost);

            if(localConnect) {
                try {
                    socket    = new Socket(remoteHost, remotePort);
                    telnetIn  = socket.getInputStream();
                    telnetOut = socket.getOutputStream();
                } catch (Exception e) {
                    mindterm.alert("Local connection failed: " +
                                   e.getMessage());
                    return;
                }
            } else {
                channel =
                    mindterm.getConnection().newLocalInternalForward(remoteHost,
                            remotePort);

                if(channel.openStatus() != SSH2Channel.STATUS_OPEN) {
                    mindterm.alert("Failed to open tunnel for telnet");
                    return;
                }

                telnetIn  = channel.getInputStream();
                telnetOut = channel.getOutputStream();
            }

            frameTitle.setTitleName("telnet@" + remoteHost + (remotePort != 23 ?
                                    (":" + remotePort)
                                    : ""));

            telnetAdapter = new ChaffedTelnet(telnetIn, telnetOut, terminal);

            if(!localConnect && useChaff()) {
                telnetAdapter.startChaff();
            }

            telnetAdapter.getTelnetNVT().getThread().join();
            if(telnetAdapter.isBuffered()) {
                lineReader = new LineReaderTerminal(terminal);
                lineReader.promptLine("\n\rTelnet session was closed, press <return> to close window", null,
                                      false);
            }

            telnetAdapter.stopChaff();

        } catch (Exception e) {
            /* don't care */
        }
    }

    protected boolean closeOnDisconnect() {
        return !localConnect;
    }

    private int getPort(String port) {
        int p;
        try {
            p = Integer.parseInt(port);
        } catch (Exception e) {
            p = 23;
        }
        return p;
    }

    public String getTitle() {
        return mindterm.getAppName() + " - " + "Telnet (not connected)";
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return true;
    }

    protected boolean haveMenus() {
        return Boolean.valueOf(mindterm.getProperty("module.telnet.havemenus")).
               booleanValue();
    }

    public void doClose() {
        if(telnetOut != null) {
            try {
                telnetOut.close();
            } catch (IOException ee) { }
        }
        if(telnetIn != null) {
            try {
                telnetIn.close();
            } catch (IOException ee) { }
        }
        if(channel != null) {
            channel.close();
        }
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException ee) { }
        }
        if(lineReader != null) {
            lineReader.breakPromptLine("");
        }
    }

    protected ModuleBaseTerminal newInstance() {
        return new ModuleTelnetImpl();
    }

}
