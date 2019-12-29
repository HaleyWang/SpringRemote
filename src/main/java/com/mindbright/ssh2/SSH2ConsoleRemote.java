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

import com.mindbright.sshcommon.TimeoutException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;

import com.mindbright.sshcommon.SSHConsoleRemote;

import com.mindbright.terminal.TerminalWindow;

/**
 * This class implements a console to a remote command or shell. The underlying
 * mechanism is a session channel which is created on demand from the provided
 * connection layer. It can be used to execute single commands and/or control
 * input/output to/from a shell. It is for example extended to control an SCP1
 * client in the class <code>SSH2SCP1Client</code>.
 * <p>
 * To create a <code>SSH2ConsoleRemote</code> instance a complete
 * connected ssh2 stack is needed from which one provides the
 * <code>SSH2Connection</code> to the constructor.
 *
 * @see SSH2Connection
 * @see SSH2SimpleClient
 */
public class SSH2ConsoleRemote implements SSHConsoleRemote {

    protected SSH2Connection     connection;
    protected SSH2SessionChannel session;
    protected OutputStream       stdout;
    protected OutputStream       stderr;

    /**
     * Basic constructor.
     *
     * @param connection connected connection layer
     */
    public SSH2ConsoleRemote(SSH2Connection connection) {
        this(connection, null, null);
    }

    /**
     * Constructor to be used to preset the stdout and/or stderr output streams
     * to which the correspondings streams should be redirected to. That is
     * when the streams doesn't have to be passed to the
     * <code>command()</code> method.
     *
     * @param connection connected connection layer
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     */
    public SSH2ConsoleRemote(SSH2Connection connection,
                             OutputStream stdout, OutputStream stderr) {
        this.connection = connection;
        this.stdout     = stdout;
        this.stderr     = stderr;
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public boolean command(String command) {
        return command(command, this.stdout);
    }

    /**
     * Runs single command on server. Note that the call will return
     * once the command has been launched on the server and that it does
     * not wait until the command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param pty whether to allocate a pty on the server or not
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, boolean pty) {
        return command(command, this.stdout, pty);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>OutputStream</code>.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdout the output stream to redirect stdout to
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, OutputStream stdout) {
        return command(command, stdout, this.stderr);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>OutputStream</code>. It is also possible to allocate a
     * pty for the command on the server.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdout the output stream to redirect stdout to
     * @param pty whether to allocate a pty or not
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, OutputStream stdout, boolean pty) {
        return command(command, stdout, this.stderr, pty);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>OutputStream</code>. This method also redirects stderr to
     * the given <code>OutputStream</code>.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, OutputStream stdout,
                           OutputStream stderr) {
        return command(command, stdout, stderr, false);
    }


    /**
     * Runs single command on server and redirects stdout to the given
     * <code>NonBlockingOutput</code>. This method also redirects stderr to
     * the given <code>NonBlockingOutput</code>.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdin the input stream to read input from (can be null)
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, NonBlockingInput stdin,
                           NonBlockingOutput stdout, NonBlockingOutput stderr){
        return command(command, stdin, stdout, stderr, false);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>OutputStream</code>. This method also redirects stderr to
     * the given <code>OutputStream</code>. It is also possible to
     * allocate a pty for the command on the server.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     * @param pty whether to allocate a pty or not
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, OutputStream stdout,
                           OutputStream stderr, boolean pty) {
        return command(command, stdout, stderr, pty, "dumb", 0, 0);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>NonBlockingOutput</code>. This method also redirects stderr to
     * the given <code>NonBlockingOutput</code>. It is also possible
     * to allocated a pty for the remote command.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdin the input stream to read input from (can be null)
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     * @param pty whether to allocate a pty or not
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, NonBlockingInput stdin,
                           NonBlockingOutput stdout, NonBlockingOutput stderr,
                           boolean pty) {
        return command(command, stdin, stdout, stderr, pty, "dumb", 0, 0);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>OutputStream</code>. This method also redirects stderr to
     * the given <code>OutputStream</code>. This method can also set
     * the terminal type and layout for the remote command. It is also
     * possible to allocated a pty for the remote command.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     * @param pty whether to allocate a pty or not
     * @param termType indicates which terminal type to request
     * @param rows indicates the number of rows on the terminal
     * @param cols indicates the number of columns on the terminal
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, OutputStream stdout,
                           OutputStream stderr, boolean pty,
                           String termType, int rows, int cols) {
        session = connection.newSession(pty);
        if (session == null) {
            return false;
        }

        if(pty) {
            if(!session.requestPTY(termType, rows, cols, null)) {
                return false;
            }
        }
        if(stdout != null) {
            session.changeStdOut(stdout);
        }
        if(stderr != null) {
            session.changeStdErr(stderr);
        }
        return session.doSingleCommand(command);
    }

    /**
     * Runs single command on server and redirects stdout to the given
     * <code>NonBlockingOutput</code>. This method also redirects stderr to
     * the given <code>NonBlockingOutput</code>. This method can also set
     * the terminal type and layout for the remote command. It is also
     * possible to allocated a pty for the remote command.
     * <p>
     * Note that the call will return once the command has been
     * launched on the server and that it does not wait until the
     * command has completed. For waiting look at the
     * waitForExitStatus() method.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     * @param stdin the input stream to read input from (can be null)
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     * @param pty whether to allocate a pty or not
     * @param termType indicates which terminal type to request
     * @param rows indicates the number of rows on the terminal
     * @param cols indicates the number of columns on the terminal
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command, NonBlockingInput stdin,
                           NonBlockingOutput stdout, NonBlockingOutput stderr,
                           boolean pty, String termType, int rows, int cols) {
        session = connection.newSession(stdin, stdout, stderr, pty);
        if (session == null) {
            return false;
        }

        if(pty) {
            if(!session.requestPTY(termType, rows, cols, null)) {
                return false;
            }
        }
        return session.doSingleCommand(command);
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public boolean shell() {
        return shell(false);
    }

    /**
     * Starts an interactive shell on the server. The parameter
     * <code>pty</code> indicates whether to allocate a PTY or not.
     *
     * @param pty indicates whether to allocate a PTY or not
     *
     * @return a boolean indicating success or failure
     */
    public boolean shell(boolean pty) {
        return shell(pty, "dumb", 0, 0);
    }

    /**
     * Starts an interactive shell on the server. The parameter
     * <code>pty</code> indicates whether to allocate a PTY or not.
     *
     * @param pty indicates whether to allocate a PTY or not
     * @param termType indicates which terminal type to request
     * @param rows indicates the number of rows on the terminal
     * @param cols indicates the number of columns on the terminal
     *
     * @return a boolean indicating success or failure
     */
    public boolean shell(boolean pty, String termType, int rows, int cols) {
        session = connection.newSession();
        if (session == null)
            return false;

        if(pty) {
            if(!session.requestPTY(termType, rows, cols, null)) {
                return false;
            }
        }
        if(stdout != null) {
            session.changeStdOut(stdout);
        }
        if(stderr != null) {
            session.changeStdErr(stderr);
        }
        return session.doShell();
    }


    /**
     * Starts an interactive shell on the server. The parameter
     * <code>pty</code> indicates whether to allocate a PTY or not.
     * This method redirects stdin, stdout and stderr to the provided
     * non blocking streams. It is also possible to setup the terminal
     * type to run in.
     *
     * @param stdin the input stream to read input from (can be null)
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     *
     * @return a boolean indicating success or failure
     */
    public boolean shell(NonBlockingInput stdin, NonBlockingOutput stdout,
                         NonBlockingOutput stderr) {
        return shell(stdin, stdout, stderr, false, "dumb", 0, 0);
    }
    

    /**
     * Starts an interactive shell on the server. The parameter
     * <code>pty</code> indicates whether to allocate a PTY or not.
     * This method redirects stdin, stdout and stderr to the provided
     * non blocking streams. It is also possible to setup the terminal
     * type to run in.
     *
     * @param stdin the input stream to read input from (can be null)
     * @param stdout the output stream to redirect stdout to (can be null)
     * @param stderr the output stream to redirect stderr to (can be null)
     * @param pty indicates whether to allocate a PTY or not
     * @param termType indicates which terminal type to request
     * @param rows indicates the number of rows on the terminal
     * @param cols indicates the number of columns on the terminal
     *
     * @return a boolean indicating success or failure
     */
    public boolean shell(NonBlockingInput stdin, NonBlockingOutput stdout,
                         NonBlockingOutput stderr, boolean pty,
                         String termType, int rows, int cols) {
        session = connection.newSession(stdin, stdout, stderr);
        if (session == null)
            return false;

        if(pty) {
            if(!session.requestPTY(termType, rows, cols, null)) {
                return false;
            }
        }
        return session.doShell();
    }

    /**
     * Start an interactive shell on the server and connect it to the
     * given terminal.
     *
     * @param termAdapter identifies the terminal window to associate
     *                    the shell with.
     *
     * @return a boolean indicating success or failure
     */
    public boolean terminal(SSH2TerminalAdapter termAdapter) {
        TerminalWindow terminal = termAdapter.getTerminal();
        session = connection.newTerminal(termAdapter);
        if (session == null ||
                !session.requestPTY(terminal.terminalType(),
                                    terminal.rows(),
                                    terminal.cols(),
                                    null)) {
            return false;
        }
        return session.doShell();
    }


    /**
     * Run a command on the server and connect it to the
     * given terminal.
     *
     * @param termAdapter identifies the terminal window to associate
     *                    the shell with.
     * @param command command line to run
     *
     * @return a boolean indicating success or failure
     */
    public boolean commandWithTerminal(SSH2TerminalAdapter termAdapter,
                                       String command) {
        TerminalWindow terminal = termAdapter.getTerminal();
        session = connection.newTerminal(termAdapter);
        if(session == null ||
                !session.requestPTY(terminal.terminalType(),
                                    terminal.rows(),
                                    terminal.cols(),
                                    null)) {
            return false;
        }
        return session.doSingleCommand(command);
    }

    /**
     * Retrieves the exit status reported by the command/shell run. Note that
     * this call blocks until the remote command/shell really exits or the
     * session is canceled (e.g. by calling <code>close()</code>) in which case
     * the status returned will be -1.
     *
     * @return the exit status reported
     */
    public int waitForExitStatus() {
        return waitForExitStatus(0);
    }

    
    /**
     * Retrieves the exit status reported by the command/shell run. Note that
     * this call blocks until the remote command/shell really exits or the
     * session is canceled (e.g. by calling <code>close()</code>) in which case
     * the status returned will be -1.
     *
     * @param timeout timeout in milliseconds
     *
     * @return the exit status reported
     */
    public int waitForExitStatus(int timeout) {
	if (session == null)
            return -1;
	int status = session.waitForExit(timeout);
	if (session != null)
	    session.waitUntilClosed(timeout);
        return status;
    }

    /**
     * Waits for the command to finish within the given time.
     *
     * @param timeout timeout time in milliseconds
     */
    public int waitForExitStatus(long timeout) throws TimeoutException {
	if (session == null)
            return -1;
        int status = session.waitForExit(timeout);
        
        if (session != null &&
	    !session.isFinished()) {
            session.close();
            throw new TimeoutException("command did not finish after " +
                                       timeout + " milliseconds");
        }
        
        if(session != null) {
            session.waitUntilClosed();
        }
        return status;
    }

    /**
     * Send a BREAK to the remote session 
     *  (see draft-ietf-secsh-break for more information)
     *
     * @param length the BREAK length (ms)
     *
     * @return a boolean indicating success or failure
     */
    public boolean sendBreak(int length) {
        return (session != null) ? session.doBreak(length) : false;
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public void close() {
        close(false);
    }
    
    public void close(boolean waitforcloseconfirm) {
        if(session != null) {
            session.close();
            if (waitforcloseconfirm)
                session.waitUntilClosed();
	    session = null;
        }
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public void changeStdOut(OutputStream out) {
        session.changeStdOut(out);
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public InputStream getStdOut() {
        return session.getStdOut();
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public OutputStream getStdIn() {
        return session.getStdIn();
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public void changeStdOut(NonBlockingOutput out)
        throws IllegalArgumentException {
        session.changeStdOut(out);
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public NonBlockingOutput getNBStdIn() {
        return session.getNBStdIn();
    }

    // See com.mindbright.sshcommon.SSHConsoleRemote for javadoc
    public NonBlockingInput getNBStdOut() {
        return session.getNBStdOut();
    }

}
