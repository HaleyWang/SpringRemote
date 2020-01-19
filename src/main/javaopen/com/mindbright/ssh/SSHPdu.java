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

import com.mindbright.nio.NonBlockingOutput;

public interface SSHPdu {
    public void   writeTo(OutputStream out) throws IOException;
    public void   writeTo(NonBlockingOutput out) throws IOException;
    public void   readFrom(InputStream in) throws IOException;
    public SSHPdu createPdu() throws IOException;

    public byte[] rawData();
    public void   rawSetData(byte[] raw);
    public int    rawOffset();
    public int    rawSize();
    public void   rawAdjustSize(int size);

    //  public SSHPdu preProcess();
    //  public SSHPdu postProcess();
}
