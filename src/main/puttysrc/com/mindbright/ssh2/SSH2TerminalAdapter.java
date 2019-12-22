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

import com.mindbright.terminal.TerminalWindow;

/**
 * Glue interface implemented by terminal windows to handle the interaction
 * with the underlying SSH2Session.
 */
public interface SSH2TerminalAdapter {
    /**
     * Get the actual terminal window.
     *
     * @return The terminal interface.
     */
    public TerminalWindow getTerminal();

    /**
     * Attach the terminal to a session
     *
     * @param session The session to attach to.
     */
    public void     attach(SSH2SessionChannel session);

    /**
     * Detach from the session
     */
    public void     detach();

    /**
     * Starts sending chaff. While chaffing is in operation the terminal
     * sends packets at a constant rate. This hides the timing of the
     * actual keypresses made by the user. It is mostly useful while
     * passwords are enterd.
     */
    public void     startChaff();

    /**
     * Stop sending chaff.
     */
    public void     stopChaff();
}
