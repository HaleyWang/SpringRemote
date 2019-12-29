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

import java.util.NoSuchElementException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

import java.awt.Frame;

import com.mindbright.terminal.TerminalWindow;
import com.mindbright.terminal.TerminalFrameTitle;

public class ModuleDebugConsoleImpl extends ModuleBaseTerminal {

    private TerminalWindow terminal;
    private PrintStream    outOrig;
    final class DebugOutStream extends OutputStream {
        public void write(int b) throws IOException {
            terminal.write((char)b);
        }
        public void write(byte b[], int off, int len) throws IOException {
            terminal.write(b, off, len);
        }
    }

    protected void runTerminal(MindTermApp mindterm,
                               TerminalWindow terminal, Frame frame,
                               TerminalFrameTitle frameTitle) {
        this.terminal = terminal;
        outOrig       = System.out;
        try {
            terminal.setProperty("auto-linefeed", "true");
        } catch (NoSuchElementException e) {}

        PrintStream debugOut = new PrintStream(new DebugOutStream());
        try {
            System.setOut(debugOut);
            System.setErr(debugOut);
        } catch (Throwable t) {
            terminal.write("\n\rError, couldn't redirect STDIO: " +
                           t.getMessage());
        }
        synchronized(this) {
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }
    }

    protected boolean closeOnDisconnect() {
        return false;
    }

    protected String getTitle() {
        return mindterm.getAppName() + " - " + "Debug Console";
    }

    protected void doClose() {
        System.setOut(outOrig);
        System.setErr(outOrig);
        this.terminal = null;
        synchronized(this) {
            this.notifyAll();
        }
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return true;
    }

    protected ModuleBaseTerminal newInstance() {
        return this;
    }

}
