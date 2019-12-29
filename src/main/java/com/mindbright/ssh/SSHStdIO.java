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

import java.io.*;
import java.awt.*;
import com.mindbright.terminal.*;

public final class SSHStdIO implements TerminalInputListener, SSHConsole {

    static public class SSHExternalMessage extends IOException {
    	private static final long serialVersionUID = 1L;
    	public boolean ctrlC;
        public boolean isError;
        public SSHExternalMessage(String msg) {
            super(msg);
            ctrlC = false;
        }
    }

    Container            ownerContainer;
    SSHInteractiveClient client;
    //
    // !!! Changed this to TerminalWin instead of TerminalWindow, we were
    // almost asuming it anyway (theoretical performance gain too! :-)
    //
    TerminalWin          term;

    String               ownerName;

    private Object readLineLock = new Object();
    boolean isReadingLine;
    boolean terminalChange;
    boolean echoStar;
    String  readLineStr;

    protected boolean isConnected;

    public SSHStdIO() {
        this.isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setTerminal(TerminalWin term) {
        this.term = term;
        if(term != null) {
            term.addInputListener(this);
        }
    }

    public void setClient(SSHInteractiveClient client) {
        this.client = client;
    }

    public void setOwnerContainer(Container ownerContainer) {
        this.ownerContainer = ownerContainer;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    SSHExternalMessage extMsg = null;
    public void breakPromptLine() {
        breakPromptLine("");
    }
    public void breakPromptLine(String msg) {
        if(isReadingLine) {
            synchronized(readLineLock) {
                extMsg = new SSHExternalMessage(msg);
                readLineLock.notify();
            }
        }
    }

    public String readLine(String defaultVal) {
        synchronized(readLineLock) {
            if(defaultVal != null) {
                readLineStr   = defaultVal;
                term.write(defaultVal);
            } else {
                readLineStr   = "";
            }
            term.setDumbMode(true);
            isReadingLine = true;
            terminalChange = false;
            try {
                readLineLock.wait();
            } catch (InterruptedException e) {
                // !!!
            }
            isReadingLine = false;
            term.setDumbMode(false);
            if (terminalChange) {
                return null;
            }
        }
        return readLineStr;
    }

    public String promptLine(String prompt, String defaultVal,
                             boolean echoStar) throws IOException {
        String line = null;
        if(term != null) {
            term.setAttributeBold(true);
            term.write(prompt);
            term.setAttributeBold(false);
            this.echoStar = echoStar;
            line = readLine(defaultVal);
            this.echoStar = false;
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(prompt);
            line = br.readLine();
        }
        if(extMsg != null) {
            SSHExternalMessage msg = extMsg;
            extMsg = null;
            throw msg;
        }

        return line;
    }

    public void updateTitle() {
        int rows = 0;
        int cols = 0;

        if(term == null || ownerContainer == null)
            return;

        String title = null;
        title = term.getTitle();

        if(title == null) {
            if(client.isOpened()) {
                title = client.propsHandler.getProperty("usrname");
                title += "@" + client.propsHandler.getProperty("server");
                title += " <" + client.getServerVersion() + ">";
            } else {
                title = ownerName;
            }

            if(!client.activateTunnels) {
                title += " (CLONE)";
            }
        }

        rows = term.rows();
        cols = term.cols();
        title += " [" + cols + "x" + rows + "]";

        if(ownerContainer instanceof Frame)
            ((Frame)ownerContainer).setTitle(title);
    }

    // SSHConsole interface
    //
    public TerminalWindow getTerminal() {
        return term;
    }
    public void stdoutWriteString(byte[] str) {
        if(isConnected)
            print(new String(str));
    }
    public void stderrWriteString(byte[] str) {
        if(isConnected)
            print(new String(str));
    }
    public void print(String str) {
        if(term != null) {
            term.write(str);
        } else {
            System.out.print(str);
        }
    }
    public void println(String str) {
        if(term != null) {
            term.write(str + "\n\r");
        } else {
            System.out.println(str);
        }
    }
    public void serverConnect(SSHChannelController controller,
                              SSHCipher sndCipher) {
        isConnected      = true;
    }
    public void serverDisconnect(String reason) {
        isConnected      = false;
        println(reason);
    }

    // TerminalInputListener interface
    //
    public void typedChar(char c) {
        try {
            if(isConnected) {
                client.stdinWriteChar(c);
            } else {
                synchronized(readLineLock) {
                    if(isReadingLine) {
                        if((c == (char)0x03) || (c == (char)0x04)) {
                            extMsg = new SSHExternalMessage("");
                            extMsg.ctrlC = (c == (char)0x03);
                            readLineLock.notify();
                        } else if(c == (char)127 || c == (char)0x08) {
                            if(readLineStr.length() > 0) {
                                boolean ctrlChar = false;
                                if(readLineStr.charAt(readLineStr.length() - 1) < ' ') {
                                    ctrlChar = true;
                                }
                                readLineStr = readLineStr.substring(0, readLineStr.length() - 1);
                                term.write((char)8);
                                if(ctrlChar)
                                    term.write((char)8);
                                term.write(' ');
                                if(ctrlChar)
                                    term.write(' ');
                                term.write((char)8);
                                if(ctrlChar)
                                    term.write((char)8);
                            } else
                                term.ringBell();
                        } else if(c == '\r') {
                            //	    readLineStr = readLineStr + "\r";
                            readLineLock.notify();
                            term.write("\n\r");
                        } else {
                            readLineStr = readLineStr + c;
                            if(echoStar)
                                term.write('*');
                            else
                                term.write(c);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // !!!
            System.out.println("**** Error in SSHStdIO.typedChar: " + e);
        }
    }
    public void typedChar(byte[] b) {
        for (int i=0; i<b.length; i++) {
            typedChar((char)b[i]);
        }
    }
    public void sendBytes(byte[] b) {
        try {
            if(isConnected) {
                client.stdinWriteString(b);
            } else {
                for(int i = 0; i < b.length; i++)
                    typedChar((char)b[i]);
            }
        } catch (IOException e) {
            // !!!
            System.out.println("**** Error in SSHStdIO.sendBytes: " + e);
        }
    }

    public void sendBytesDirect(byte[] b) {
        try {
            if(isConnected) {
                client.stdinWriteString(b);
            }
        } catch (IOException e) {
            // !!!
            System.out.println("**** Error in SSHStdIO.sendBytesDirect: " + e);
        }
    }

    public void signalWindowChanged(int rows, int cols, int vpixels, int hpixels) {
        if(isConnected) {
            client.signalWindowChanged(rows, cols, vpixels, hpixels);
        } else {
            // !!!
        }
        updateTitle();
    }
    public void signalTermTypeChanged(String newTermType) {
        synchronized (readLineLock) {
            terminalChange = true;
            readLineLock.notify();
        }
    }
    public void sendBreak() {}

}
