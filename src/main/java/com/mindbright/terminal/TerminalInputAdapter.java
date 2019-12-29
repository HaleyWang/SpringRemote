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
 * An abstract adapter class for receiving terminal input events. The
 * methods in this class are empty. This class exists as convenience
 * for creating listener objects.
 */
public class TerminalInputAdapter implements TerminalInputListener {

    public void typedChar(char c) {}

    public void typedChar(byte[] b) {}

    public void sendBytes(byte[] b) {}

    public void sendBytesDirect(byte[] b) {}

    public void signalWindowChanged(int rows, int cols,
                                    int vpixels, int hpixels) {}
    public void signalTermTypeChanged(String newTermType) {}

    public void sendBreak() {}

}
