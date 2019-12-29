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

package com.mindbright.sshcommon;

import java.io.InputStream;
import java.io.OutputStream;

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;

/**
 * Basic interface to classes implementing a console to a remote
 * command or shell.
 */
public interface SSHConsoleRemote {
    /**
     * Runs single command on server. Stdout from the command will be
     * sent to the local stdout.
     * <p>
     * Note, this will create an extra pair of threads to handle the data.
     *
     * @param command command line to run
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command);

    /**
     * Starts an interactive shell on the server, note that no PTY is
     * allocated.
     *
     * @return a boolean indicating success or failure
     */
    public boolean shell();

    /**
     * Closes the session channel. That is cancels a command/shell in
     * progress if it hasn't already exited.
     */
    public void close();

    /**
     * Closes the session channel. If waitforcloseconfirm is true it
     * waits for the remote end to acknowledge the close.
     */
    public void close(boolean waitforcloseconfirm);

    /**
     * Changes the output stream where stdout is written to in the underlying
     * session channel.
     * <p>
     * Note that this method only works if the underlying session uses
     * blocking streams and threads.
     *
     * @param out new stdout stream
     *
     * @throws IllegalArgumetExcpetion if the underlying session uses
     *                                 non-blocking IO
     */
    public void changeStdOut(OutputStream out) throws IllegalArgumentException;

    /**
     * Gets the stdin stream of the underlying session channel. Note, this is
     * an output stream since one wants to <b>write</b> to stdin.
     * <p>
     * Note that this method returns null if the underlying stream
     * uses non-blocking io
     *
     * @return the input stream of stdout stream
     */
    public OutputStream getStdIn();

    /**
     * Gets the stdout stream of the underlying session channel. Note, this is
     * an input stream since one wants to <b>read</b> from stdout.
     * <p>
     * Note that this method returns null if the underlying stream
     * uses non-blocking io
     *
     * @return the input stream of stdout stream
     */
    public InputStream  getStdOut();

    /**
     * Changes the output stream where stdout is written to in the underlying
     * session channel.
     * <p>
     * Note that this method only works if the underlying session uses
     * non-blocking io.
     *
     * @param out new stdout stream
     *
     * @throws IllegalArgumetExcpetion if the underlying session uses
     *                                 blocking IO
     */
    public void changeStdOut(NonBlockingOutput out)
        throws IllegalArgumentException;

    /**
     * Gets the stdin stream of the underlying session channel. Note, this is
     * an output stream since one wants to <b>write</b> to stdin.
     * <p>
     * Note that this method returns null if the underlying stream
     * uses blocking io
     *
     * @return the input stream of stdout stream
     */
    public NonBlockingOutput getNBStdIn();

    /**
     * Gets the stdout stream of the underlying session channel. Note, this is
     * an input stream since one wants to <b>read</b> from stdout.
     * <p>
     * Note that this method returns null if the underlying stream
     * uses blocking io
     *
     * @return the input stream of stdout stream
     */
    public NonBlockingInput getNBStdOut();
}
