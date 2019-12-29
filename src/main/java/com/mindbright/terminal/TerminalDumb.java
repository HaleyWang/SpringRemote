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

public final class TerminalDumb extends TerminalInterpreter {

    public String terminalType() {
        return "DUMB";
    }

    public boolean setTerminalType() {
        return false;
    }

    public int interpretChar(char c) {
        switch(c) {
        case 7: // BELL
            term.doBell();
            break;
        case 8: // BS/CTRLH
            term.doBS();
            break;
        case '\t':
            term.doTab();
            break;
        case '\r':
            term.doLF();
            break;
        case '\n':
            term.doCR();
            break;
        default:
            return c;
        }
        return IGNORE;
    }

}
