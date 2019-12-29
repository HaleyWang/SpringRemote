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

import java.io.InputStream;
import java.io.IOException;

/**
 * An input stream pipe should be connected to an output stream pipe;
 * the input stream pipe then provides whatever data bytes are
 * written to the output stream pipe. This is very close to the
 * <code>PipedInputStream</code> and
 * <code>PipedOutputStream</code>. The main difference is that there
 * is a timeout in the write code so that any waiting write will be
 * aborted if the pipe is closed.
 * <p>
 * The input and output pipes are connected via a circular buffer
 * which decouples write and read operations.
 *
 * @see OutputStreamPipe
 * @see java.io.PipedInputStream
 * @see java.io.PipedOutputStream
 */
public final class InputStreamPipe extends InputStream {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private OutputStreamPipe source;
    private byte[]           circBuf;
    private int              rOffset;
    private int              wOffset;
    private boolean          isWaitGet;
    private boolean          isWaitPut;
    private boolean          eof;
    private boolean          closed;

    /**
     * Create an unconnected InputStreamPipe with the given circular
     * buffer size.
     *
     * @param bufferSize size of circular buffer
     */
    public InputStreamPipe(int bufferSize) {
        this.circBuf    = new byte[bufferSize];
        this.isWaitGet  = false;
        this.isWaitPut  = false;
        this.rOffset    = 0;
        this.wOffset    = 0;
    }

    /**
     * Create an unconnected InputStreamPipe with the default circular
     * buffer size (8192 bytes).
     */
    public InputStreamPipe() {
        this(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Create an InputStreamPipe with the default circular
     * buffer size (8192 bytes) which is connected to the given output
     * stream.
     *
     * @param source the output stream to connect to
     */
    public InputStreamPipe(OutputStreamPipe source) throws IOException {
        this();
        connect(source);
    }

    /**
     * Causes this InputStreamPipe to be connected to the given
     * OutputStreamPipe.
     *
     * @param source the stream to connect to
     */
    public void connect(OutputStreamPipe source) throws IOException {
        if(this.source == source) {
            return;
        }
        if(this.source != null) {
            throw new IOException("Pipe already connected");
        }
        this.source = source;
        source.connect(this);
    }

    /**
     * Read a byte of data from the pipe. This call will wait for data
     * to become available if needed.
     *
     * @return the next byte waiting to be read
     */
    public synchronized int read() throws IOException {
        while(isEmpty()) {
            if(closed) {
                throw new IOException("InputStreamPipe closed");
            }
            if(eof) {
                return -1;
            }
            isWaitGet = true;
            try {
                this.wait();
            } catch (InterruptedException e) {
                // !!!
            }
        }
        isWaitGet = false;

        int b = (circBuf[rOffset++] & 0xff);

        if(rOffset == circBuf.length)
            rOffset = 0;

        if(isWaitPut) {
            this.notifyAll();
            isWaitPut = false;
        }

        return b;
    }

    /**
     * Read data from the pipe. This call will wait for data
     * to become available if needed.
     *
     * @param buf buffer to store read data into
     * @param off where in the buffer the first byte should be stored
     * @param len how many bytes of data to read
     * @return the number of bytes actually read
     */
    public synchronized int read(byte[] buf, int off, int len)
    throws IOException {
        while(isEmpty()) {
            if(closed) {
                throw new IOException("InputStreamPipe closed");
            }
            if(eof) {
                return -1;
            }
            isWaitGet = true;
            try {
                this.wait();
            } catch (InterruptedException e) {
                // !!!
            }
        }
        isWaitGet = false;

        int n = available();
        n = (n > len ? len : n);

        if(rOffset < wOffset) {
            System.arraycopy(circBuf, rOffset, buf, off, n);
        } else {
            int rest = circBuf.length - rOffset;
            if(rest < n) {
                System.arraycopy(circBuf, rOffset, buf, off, rest);
                System.arraycopy(circBuf, 0, buf, off + rest, n - rest);
            } else {
                System.arraycopy(circBuf, rOffset, buf, off, n);
            }
        }

        rOffset += n;
        if(rOffset >= circBuf.length)
            rOffset -= circBuf.length;

        if(isWaitPut) {
            this.notifyAll();
            isWaitPut = false;
        }

        return n;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     *
     * @return the number of bytes that can be read without blocking.
     */
    public synchronized int available() {
        return circBuf.length - freeSpace() - 1;
    }

    /**
     * Close this stream and abort any ongoing write operation in the
     * corresponding OutputStreamPipe.
     */
    public synchronized void close()
    throws IOException {
        closed = true;
        this.notifyAll();
    }

    /**
     * Notify all instances waiting on this stream.
     */
    public synchronized void flush() {
        this.notifyAll();
    }

    /**
     * Put a byte of data into this input stream pipe. If the buffer
     * is full then this call will wait until room becomes available or
     * the stream is closed.
     *
     * @param b the byte of data to put
     */
    protected synchronized void put(int b)
    throws IOException {
        putFlowControl();
        circBuf[wOffset++] = (byte)b;
        if(wOffset == circBuf.length)
            wOffset = 0;
        if(isWaitGet)
            this.notify();
    }

    /**
     * Put data into this input stream pipe. If the buffer
     * is full then this call will wait until room becomes available or
     * the stream is closed. The function handles the case where the
     * amount of data to write is larger than the circular buffer.
     *
     * @param buf array holding data to put
     * @param off offset of first byte to put
     * @param len number of bytes to put
     */
    protected synchronized void put(byte[] buf, int off, int len)
    throws IOException {
        while(len > 0) {
            putFlowControl();
            int n = freeSpace();
            n = (n > len ? len : n);

            if(wOffset < rOffset) {
                System.arraycopy(buf, off, circBuf, wOffset, n);
            } else {
                int rest = circBuf.length - wOffset;
                if(rest < n) {
                    System.arraycopy(buf, off, circBuf, wOffset, rest);
                    System.arraycopy(buf, off + rest, circBuf, 0, n - rest);
                } else {
                    System.arraycopy(buf, off, circBuf, wOffset, n);
                }
            }

            wOffset += n;
            if(wOffset >= circBuf.length) {
                wOffset -= circBuf.length;
            }
            len -= n;
            off += n;

            if(isWaitGet)
                this.notify();
        }
    }

    /**
     * Signal that this stream is closing.
     */
    protected synchronized void eof() {
        eof = true;
        this.notifyAll();
    }

    private int freeSpace() {
        int fSpc = rOffset - wOffset;
        if(fSpc <= 0)
            fSpc += circBuf.length;
        fSpc--;
        return fSpc;
    }

    private synchronized boolean isEmpty() {
        return (rOffset == wOffset) || closed;
    }

    private void putFlowControl() throws IOException {
        if(eof) {
            throw new IOException("InputStreamPipe already got eof");
        }
        if(closed) {
            throw new IOException("InputStreamPipe closed");
        }
        while(freeSpace() == 0) {
            isWaitPut = true;
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
                // !!!
            }

            if(eof) {
                throw new IOException("InputStreamPipe already got eof");
            }
            if(closed) {
                throw new IOException("InputStreamPipe closed");
            }
        }
    }

    /* DEBUG/Test
    public static void main(String[] argv) {
    try {
     final InputStreamPipe  in  = new InputStreamPipe();
     final OutputStreamPipe out = new OutputStreamPipe(in);
     //final java.io.PipedInputStream  in  = new java.io.PipedInputStream();
     //final java.io.PipedOutputStream out = new java.io.PipedOutputStream(in);

     final byte[] msg = new byte[4711];
     for(int i = 0; i < 4711; i++) {
    msg[i] = (byte)((i * i) ^ (i + i));
     }
     Thread w = new Thread(new Runnable() {
      public void run() {
    try {
       for(int i = 0; i < 1000; i++) {
    out.write(msg);
       }
    } catch (IOException e) {
       System.out.println("Error in w: " + e);
       e.printStackTrace();
    }
      }
    });
     Thread r = new Thread(new Runnable() {
      public void run() {
    try {
       byte[] imsg = new byte[4711];
       for(int i = 0; i < 1000; i++) {
    int l = 4711;
    int o = 0;
    while(o < 4711) {
        int n = in.read(imsg, o, l);
        o += n;
        l -= n;
    }
       }
    } catch (IOException e) {
       System.out.println("Error in w: " + e);
       e.printStackTrace();
    }
      }
    });
     long start = System.currentTimeMillis();
     System.out.println("Start: " + (start / 1000));
     w.start();
     r.start();
     r.join();
     long now = System.currentTimeMillis();
     System.out.println("End: " + (now / 1000));
     System.out.println("Lapsed: " + (now - start));

    } catch (Exception e) {
     System.out.println("Error: " + e);
     e.printStackTrace();
    }
    }
    */

}
