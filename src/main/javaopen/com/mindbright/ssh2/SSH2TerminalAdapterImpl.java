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

import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.terminal.TerminalWindow;
import com.mindbright.terminal.TerminalInputChaff;

/**
 * Adapter class which interfaces between terminal windows and ssh2.
 */
public class SSH2TerminalAdapterImpl extends TerminalInputChaff
    implements SSH2TerminalAdapter, SSH2ChannelCloseListener {
    private TerminalWindow     terminal;
    private SSH2SessionChannel session;
    private TerminalOutStream  stdout;
    private boolean            minimumLatency;

    final class TerminalOutStream extends OutputStream {
        public void write(int b) throws IOException {
            terminal.write((char)b);
        }
        public void write(byte b[], int off, int len) throws IOException {
            terminal.write(b, off, len);
        }
    }

    /**
     * Constructor.
     *
     * @param terminal Terminal window to use.
     */
    public SSH2TerminalAdapterImpl(TerminalWindow terminal) {
        this.terminal = terminal;
        this.stdout   = new TerminalOutStream();
    }

    public TerminalWindow getTerminal() {
        return terminal;
    }

    public void attach(SSH2SessionChannel session) {
        this.session   = session;
        minimumLatency =
            "true".equals(session.getConnection().getPreferences().
                          getPreference(SSH2Preferences.TERM_MIN_LAT));
        session.changeStdOut(this.stdout);
        terminal.addInputListener(this);
    }

    public void detach() {
        if(terminal != null) {
            terminal.removeInputListener(this);
        }
        // !!! TODO want to do this ?
        // session.changeStdOut(
    }

    public void startChaff() {
        if (session != null) {
            session.addCloseListener(this);
            super.startChaff();
        }
    }

    public void stopChaff() {
        super.stopChaff();

        if (session != null) {
            session.removeCloseListener(this);
        }

        terminal = null;
        session = null;
        stdout = null;
    }

    public void closed(SSH2Channel channel) {
        stopChaff();
    }

    /**
     * Send an actually typed character.
     *
     * @param c The typed character to send.
     */
    protected void sendTypedChar(int c) {
        if(minimumLatency) {
            session.stdinWriteNoLatency(c);
        } else {
            try {
                session.getStdIn().write(c);
            } catch (IOException e) {
                session.getConnection().getLog().error(
                    "SSH2TerminalAdapterImpl", "typedChar",
                    "error writing to stdin: " + e.getMessage());
            }
        }
    }

    /**
     * Send a fake character. Sends a packet which is the same size as
     * a real keypress packet but it contains no data.
     */
    protected void sendFakeChar() {
        /*
         * 5 bytes of "ignore-payload" makes packet same size as one byte
         * channel data (i.e. a key-press).
         */
        byte[] chaff = new byte[] { 1, 2, 3, 4, 5 };
        session.getConnection().getTransport().sendIgnore(chaff);
    }

    /**
     * Send a number of bytes.
     *
     * @param b Array of bytes to send.
     */
    public void sendBytes(byte[] b) {
        try {
            session.getStdIn().write(b, 0, b.length);
        } catch (IOException e) {
            session.getConnection().getLog().error("SSH2TerminalAdapterImpl",
                                                   "sendBytes",
                                                   "error writing to stdin: " +
                                                   e.getMessage());
        }
    }

    public void sendBytesDirect(byte[] b) {
        sendBytes(b);
    }

    /**
     * This function should be called by the actual terminal window
     * whenever it is resized.
     */
    public void signalWindowChanged(int rows, int cols,
                                    int vpixels, int hpixels) {
        session.sendWindowChange(rows, cols);
    }

    /**
     * This function should be called by the actual terminal window
     * whenever the user wants to send break.
     * A break length of 500ms is used, since most devices will 
     * recognize a break of that length.
     */
    public void sendBreak() {
        if (!session.doBreak(500)) {
            session.getConnection().getLog().error(
                "SSH2TerminalAdapterImpl", "sendBreak",
                "Failed to send break");
        }
    }

}
