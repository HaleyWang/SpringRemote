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

import com.mindbright.sshcommon.TimeoutException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.terminal.TerminalWindow;
import com.mindbright.util.InputStreamPipe;
import com.mindbright.util.OutputStreamPipe;
import com.mindbright.sshcommon.SSHConsoleRemote;

public class SSHConsoleClient extends SSHClientUserAdaptor
    implements SSHConsole, SSHConsoleRemote {
    public final static int DEFAULT_COPY_BUFFER_SZ = 16384;

    protected SSHClient         client;
    protected SSHClientUser     proxyUser;

    protected InputStreamPipe   inTop;
    protected OutputStreamPipe  inBottom;
    protected OutputStream      stdout;
    protected TerminalOutStream stdin;

    /**
     * Where to store standard error bytes.
     */
    protected OutputStream      stderr;

    final class TerminalOutStream extends OutputStream {
        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            buf[0] = (byte)b;
            client.stdinWriteString(buf);
        }
        public void write(byte b[], int off, int len) throws IOException {
            client.stdinWriteString(b, off, len);
        }
    }

    public SSHConsoleClient(String sshHost, int port,
                            SSHAuthenticator authenticator,
                            SSHInteractor interactor,
                            SSHClientUser user)
    throws IOException {
        this(sshHost, port, authenticator, interactor, user, DEFAULT_COPY_BUFFER_SZ);
    }

    public SSHConsoleClient(String sshHost, int port,
                            SSHAuthenticator authenticator,
                            SSHInteractor interactor)
    throws IOException {
        this(sshHost, port, authenticator, interactor, null, DEFAULT_COPY_BUFFER_SZ);
    }

    public SSHConsoleClient(String sshHost, int port,
                            SSHAuthenticator authenticator,
                            SSHInteractor interactor, int bufferSize)
    throws IOException {
        this(sshHost, port, authenticator, interactor, null, bufferSize);
    }
        
    public SSHConsoleClient(String sshHost, int port,
                            SSHAuthenticator authenticator,
                            SSHInteractor interactor, 
                            SSHClientUser user,
                            int bufferSize)
    throws IOException {
        super(sshHost, port);

        // OUCH: Note must be set before constructing SSHClient since
        // its constructor calls getInteractor to fetch this
        //
        this.interactor = interactor;

        client         = new SSHClient(authenticator, (user != null) ? user : this);
        inTop          = new InputStreamPipe(bufferSize);
        inBottom       = new OutputStreamPipe(inTop);
        stdin          = new TerminalOutStream();

        stdout = inBottom;

        client.setConsole(this);
        client.activateTunnels = false;
    }

    public boolean command(String command) {
        try {
            client.doSingleCommand(command, true, 0);
        } catch (IOException e) {
            if(interactor != null) {
                interactor.alert("Error connecting: " + e.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Send a command and wait for a given timeout for the it to finish.
     */
    public int command(String command, long timeout) throws TimeoutException, IOException {
        try {
            client.doSingleCommand(command, false, timeout);
            Integer exitStatus = client.getExitStatus();
            if (exitStatus != null) {
                return exitStatus.intValue();
            }
			throw new TimeoutException("command did not finish after " + timeout + " milliseconds");
        } catch (IOException e) {
            if(interactor != null) {
                interactor.alert("Error connecting: " + e.getMessage());
            }
            throw e;
        }
    }

    public boolean shell() {
        try {
            client.bootSSH(false, false);
        } catch (IOException e) {
            if(interactor != null) {
                interactor.alert("Error connecting: " + e.getMessage());
            }
            return false;
        }
        return true;
    }

    public void close() {
        close(false);
    }

    public void close(boolean waitforcloseconfirm) {
        interactor = null;
        client.forcedDisconnect();
        try {
            if (stdout != null)
                stdout.close();
        } catch (IOException e) {
            // !!!
        }
        try {
            if (inTop != null) inTop.close();
        } catch (IOException e) {
            // !!!
        }
        inTop = null;
        stdout = null; 
        inBottom = null;
    }

    public void changeStdOut(OutputStream out) {
        this.stdout = out;
        inBottom    = null;
        inTop       = null;
    }

    /**
     * Where to store standard error bytes.
     */
    public void changeStdErr(OutputStream err) {
        this.stderr = err;
    }

    public OutputStream getStdIn() {
        return stdin;
    }

    public InputStream getStdOut() {
        return inTop;
    }

    public void changeStdOut(NonBlockingOutput out)
        throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    public NonBlockingOutput getNBStdIn() {
        return null;
    }

    public NonBlockingInput getNBStdOut() {
        return null;
    }

    public void setClientUser(SSHClientUser proxyUser) {
        this.proxyUser = proxyUser;
    }

    public void stdoutWriteString(byte[] str) {
        try {
            if (stdout != null)
                stdout.write(str);
        } catch(IOException e) {
            try {
                if (stdout != null)
                    stdout.close();
            } catch (IOException ee) {
                // !!!
            }
            if(interactor != null)
                interactor.alert("Error writing data to stdout-pipe");
        }
    }

    public void stderrWriteString(byte[] str) {
        if (stderr != null) {
            try {
                stderr.write(str);
            } catch(IOException e) {
                try {
                    stderr.close();
                } catch (IOException ee) {
                    // !!!
                }
                if(interactor != null)
                    interactor.alert("Error writing data to stderr-pipe");
            }
        } else {
            if(interactor != null)
                interactor.alert("Remote warning/error: " + new String(str));
        }
    }

    public TerminalWindow getTerminal() {
        return null;
    }
    public void print(String str) {}
    public void println(String str) {}
    public void serverConnect(SSHChannelController controller,
                              SSHCipher sndCipher) {}
    public void serverDisconnect(String reason) {
        try {
            if (stdout != null)
                stdout.close();
        } catch (IOException e) {}
    }
    public boolean wantPTY() {
        return false;
    }
    public NetworkConnection getProxyConnection() throws IOException {
        if(proxyUser != null) {
            return proxyUser.getProxyConnection();
        }
        return null;
    }
}
