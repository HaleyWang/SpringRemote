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

package com.mindbright.terminal;

/**
 * Interface for classes listening on input events in a terminal
 * window. That is the actions performed by the user on the terminal
 * window are signalled using this interface.
 *
 * @see TerminalWindow
 */
public interface TerminalInputListener {
    /**
     * Called when the user types a character
     *
     * @param c typed character
     */
    public void typedChar(char c);

    /**
     * Called when the user types a character
     *
     * @param b byte code representation fo the character encoded in
     *          the current encoding.
     */
    public void typedChar(byte[] b);

    /**
     * May be called when the user pastes data. It may also be called
     * by some external entity whishing to simulate multiple key
     * presses.
     *
     * @param b array of bytes representing characters to input
     */
    public void sendBytes(byte[] b);

    /**
     * Send some bytes directly to the host. This does not echo the
     * characters and bypasses any line buffering. This is typically
     * used when replying to some query from the host.
     *
     * @param b array of bytes representing characters to send
     */
    public void sendBytesDirect(byte[] b);

    /**
     * Called when the size of the terminal window has changed.
     *
     * @param rows new number of rows
     * @param cols new number of columns
     * @param vpixels new number of vertical pixels
     * @param hpixels new number of horizontal pixels
     */
    public void signalWindowChanged(int rows, int cols,
                                    int vpixels, int hpixels);

    /**
     * Called when the terminal type has changed. That is when the
     * user has changed which terminal type this terminal window should
     * emulate.
     *
     * @param newTermType new terminal type to emulate
     */
    public void signalTermTypeChanged(String newTermType);

    /**
     * Called when the user sends a break to the terminal
     */
    public void sendBreak();
}
