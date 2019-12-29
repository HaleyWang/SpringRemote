/******************************************************************************
 *
 * Copyright (c) 2010-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements an ouput stream which class flush after each
 * write. It is useful to override the buffering of a
 * BufferedOutputStream when one do not want the buffering (like the
 * one we get from Process.getOutputStream().
 */
public final class FlushingOutputStream extends FilterOutputStream {
    /**
     * Creates an flushing output stream
     *
     * @param out stream to send and flush data to
     */
    public FlushingOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * See <code>java.io.OutputStream</code>
     */
    public void write(int b) throws IOException {
        out.write(b);
        out.flush();
    }

    /**
     * See <code>java.io.OutputStream</code>
     */
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        out.flush();
    }
}
