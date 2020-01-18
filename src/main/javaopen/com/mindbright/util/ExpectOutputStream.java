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

package com.mindbright.util;

import java.io.OutputStream;

/**
 * This class implements an ouput stream which captures data until a
 * certain expected string has been encountered. To use it the caller
 * should first have a class which implements
 * <code>Expector</code>. For example:
 * <pre>
 * class MyClass implements ExpectOutputStream.Expector {
 *   public void reached(ExpectOutputStream out, byte[] buf, int len) {
 *     System.out.println("Before this we got " + len + " bytes");
 *   }
 *   public void closed(ExpectOutputStream out, byte[] buf, int len) {
 *     System.out.println("Stream closed");
 *   }
 * </pre>
 * MyClass can then create an instance of
 * <code>ExpectOutputStream</code> which waits for the desired string,
 * which in this example is <code>END_OF_OUTPUT</code>:
 * <pre>
 *   outputStream = new ExpectOutputStream(this, "END_OF_OUTPUT");
 *
 *   // Attach to remote console
 *   SSH2ConsoleRemote console = ...
 *   console.changeStdOut(outputStream);
 * </pre>
 * And that is it. The <code>reached</code> function will be called
 * when the string <code>END_OF_OUTPUT</code> is output from the
 * console. The buffer passed will contain all text up until that
 * point.
 * <p>
 * For a full usage example see the <code>RemoteShellScript</code> example.
 *
 * @see com.mindbright.ssh2.SSH2ConsoleRemote
 * @see examples.RemoteShellScript
 */
public final class ExpectOutputStream extends OutputStream {

    /**
     * Interface to be implemented by classes interested when the
     * event <code>ExpectOutputStream</code> waits for happens.
     *
     * @see ExpectOutputStream
     */
    public static interface Expector {
        /**
         * This function is called whenever the expected string is
         * found in the data stream.
         *
         * @param out the stream this happened on.
         * @param buf a buffer containing all the data which has been
         *            seen on the stream since the last time
         *            <code>reached</code> was called. Note that the
         *            buffer may be bigger than needed.
         * @param len how many bytes of data there is in the buffer.
         */
        public void reached(ExpectOutputStream out, byte[] buf, int len);

        /**
         * This function is called when the data stream is closed.
         *
         * @param out the stream this happened on.
         * @param buf a buffer containing all the data which has been
         *            seen on the stream since the last time
         *            <code>reached</code> was called. Note that the
         *            buffer may be bigger than needed.
         * @param len how many bytes of data there is in the buffer.
         */
        public void closed(ExpectOutputStream out, byte[] buf, int len);
    }

    private Expector expector;
    private byte[]   captBuf;
    private int      captLen;
    private byte[]   boundary;
    private int      matchIdx;

    /**
     * Creates an expecting output stream which does not expect
     * anything.
     *
     * @param expector class interested in when the expected string
     * occurs
     */
    public ExpectOutputStream(Expector expector) {
        this.expector = expector;
        this.captBuf  = new byte[1024];
        this.captLen  = 0;
        this.boundary = null;
    }

    /**
     * Creates an expecting output stream which waits for a specified string.
     *
     * @param expector class interested in when the expected string
     *                 occurs
     * @param boundary the string to wait for
     */
    public ExpectOutputStream(Expector expector, String boundary) {
        this(expector);
        expect(boundary);
    }

    /**
     * Creates an expecting output stream which waits for a specified string.
     *
     * @param expector class interested in when the expected string
     *                 occurs
     * @param buf array holding the string it should wait for
     * @param off offset in array where the string starts
     * @param len length of string in array
     */
    public ExpectOutputStream(Expector expector, byte[] buf, int off,int len) {
        this(expector);
        byte[] boundary = new byte[len];
        System.arraycopy(buf, off, boundary, 0, len);
        expect(boundary);
    }

    /**
     * Changes the string this instance is waiting for.
     *
     * @param boundary the string to wait for
     */
    public void expect(String boundary) {
        expect(boundary.getBytes());
    }

    /**
     * Changes the string this instance is waiting for.
     *
     * @param boundary the string to wait for
     */
    public synchronized void expect(byte[] boundary) {
        this.boundary = boundary;
        this.matchIdx = 0;
        if(captLen > 0) {
            write(captBuf, 0, captLen);
        }
    }

    /**
     * See <code>java.io.OutputStream</code>
     */
    public void write(int b) {
        write(new byte[] { (byte)b }, 0, 1);
    }

    /**
     * See <code>java.io.OutputStream</code>
     */
    public synchronized void write(byte[] b, int off, int len) {
        boolean match = false;
        for(; (!match && len > 0); len--, off++) {
            byte c = b[off];
            if(captLen == captBuf.length) {
                byte[] tmp = captBuf;
                captBuf = new byte[captBuf.length * 2];
                System.arraycopy(tmp, 0, captBuf, 0, captLen);
            }
            captBuf[captLen++] = c;
            if(!match) {
                match = boundaryReached(c);
            }
        }
        if(match) {
            expector.reached(this, captBuf, captLen - boundary.length);
            matchIdx = 0;
            captLen  = 0;
        }
        if(len > 0) {
            write(b, off, len);
        }
    }

    /**
     * See <code>java.io.OutputStream</code>
     */
    public void close() {
        if(captLen > 0) {
            expector.closed(this, captBuf, captLen);
        }
    }

    private boolean boundaryReached(byte b) {
        if(boundary == null) {
            return false;
        }
        if(boundary[matchIdx] == b) {
            matchIdx++;
        } else {
            matchIdx = 0;
        }
        if(matchIdx == boundary.length) {
            return true;
        }
        return false;
    }

}
