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
 * Interface for printers which may be attached to terminals.
 */
public interface TerminalPrinter extends TerminalOutputListener {
    /**
     * Print a dump of the current screen.
     */
    public void printScreen();

    /**
     * Start printing everything which is shown on the terminal from
     * now on.
     */
    public void startPrinter(boolean tofile);

    /**
     * Stop dumping data to the printer.
     */
    public void stopPrinter(boolean resetterminal);
}
