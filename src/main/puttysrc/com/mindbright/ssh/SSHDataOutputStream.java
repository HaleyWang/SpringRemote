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

import java.io.*;
import java.math.BigInteger;

public class SSHDataOutputStream extends DataOutputStream {

    SSHDataOutputStream(OutputStream out) {
        super(out);
    }

    public void writeBigInteger(BigInteger bi) throws IOException {
        short bytes = (short)((bi.bitLength() + 7) / 8);
        byte[] raw  = bi.toByteArray();
        writeShort(bi.bitLength());
        if(raw[0] == 0)
            write(raw, 1, bytes);
        else
            write(raw, 0, bytes);
    }

    public void writeString(String str) throws IOException {
        byte[] raw = str.getBytes();
        writeInt(raw.length);
        write(raw);
    }
}
