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

import java.io.IOException;

/**
 * Interface for class which wants to receive data from a telnet session.
 */
public interface TelnetEventHandler {
    /**
     * Called to handle some telnet protocol commands from the server.
     */
    public void interpretAsCommand(int cmd);

    /**
     * Called to handle option negotiation packets from the server.
     * The implementation is expected to modify its internal state
     * accordingly and reply by invoking the relevant function (like
     * <code>doOption</code> and <code>willOption</code>
     */
    public boolean optionNegotiation(int option, int request)
        throws IOException;

    /**
     * Called to handle option negotiation packets from the server.
     * The implementation is expected to modify its internal state
     * accordingly and reply by invoking the relevant function (like
     * <code>sendOptionSubNegotiation</code>
     */
    public void optionSubNegotiation(int option, byte[] params)
        throws IOException;

    /**
     * Handle a byte received from the server.
     */
    public void receiveData(byte b);
}
