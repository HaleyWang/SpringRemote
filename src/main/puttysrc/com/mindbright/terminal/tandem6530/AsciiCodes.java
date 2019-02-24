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

package com.mindbright.terminal.tandem6530;

public interface AsciiCodes {
    public final static char NUL  = (char) 0x00;
    public final static char SOH  = (char) 0x01;
    public final static char ETX  = (char) 0x03;
    public final static char EOT  = (char) 0x04;
    public final static char ENQ  = (char) 0x05;
    public final static char BELL = (char) 0x07;
    public final static char BS   = (char) 0x08;
    public final static char HT   = (char) 0x09;
    public final static char LF   = (char) 0x0a;
    public final static char CR   = (char) 0x0d;
    public final static char DC1  = (char) 0x11;
    public final static char DC2  = (char) 0x12;
    public final static char DC3  = (char) 0x13;
    public final static char DC4  = (char) 0x14;
    public final static char FS   = (char) 0x1c;
    public final static char GS   = (char) 0x1d;
    public final static char ESC  = (char) 0x1b;

    // XXX This is some kind of redundency character where every
    // byte in the message should be xor:ed with the next byte.
    // But in the snoop from verizoa, LRC is allways set to 0 no
    // matter what bytes the message consists of. Perhaps they
    // depend on the IP checksum. Who knows?
    public final static char LRC  = (char) 0x00;

}

