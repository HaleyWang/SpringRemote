/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone AB. All Rights Reserved.
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

package com.mindbright.nio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;

import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import java.util.concurrent.locks.ReentrantLock;


/**
 * A non-blocking output stream.
 */
public class NonBlockingOutput {
    private AbstractInterruptibleChannel _channel;
    private Switchboard _switchboard;
    private boolean _writeWaiting = false;
    private ReentrantLock _writeWaitingLock = new ReentrantLock();

    protected NonBlockingOutput(Switchboard switchboard,
                                AbstractInterruptibleChannel channel)
        throws IOException {
        _switchboard = switchboard;
        _channel = channel;
        if (_channel instanceof AbstractSelectableChannel) {
            AbstractSelectableChannel a = (AbstractSelectableChannel)_channel;
            a.configureBlocking(false);
        }
    }

    /**
     * Creates a new NonBlockingOutput object which sends any output
     * to a pipe.
     *
     * @param pipe Pipe to send output to
     */
    public NonBlockingOutput(Pipe pipe) throws IOException {
        this(Switchboard.getSwitchboard(), pipe.sink());
    }

    /**
     * Creates a new NonBlockingOutput object which sends any output
     * to a file. Note that any write calls to this file will be blocking.
     *
     * @param name Name of file to store data in
     */
    public NonBlockingOutput(String name) throws IOException {
        this(Switchboard.getSwitchboard(),
             new FileOutputStream(name).getChannel());
    }

    /**
     * Writes the specified byte
     */
    public void write(int data) throws IOException {
        write(ByteBuffer.wrap(new byte[] { (byte)data }));
    }

    /**
     * Writes all the bytes in the provided buffer to the stream
     */
    public void write(byte data[]) throws IOException {
        write(data, 0, data.length);
    }

    /**
     * Writes the indicated part of the buffer to the stream
     */
    public void write(byte data[], int offset, int length)
        throws IOException {
        byte[] ndata = new byte[length];
        System.arraycopy(data, offset, ndata, 0, length);
        write(ByteBuffer.wrap(ndata));
    }

    private void write(ByteBuffer buf) throws IOException {
        _writeWaitingLock.lock();
        try {
            assert _switchboard.debug2("NBO", "write", "w=" + _writeWaiting + ", r=" + buf.remaining());
            if (!_writeWaiting) {
                assert _switchboard.debug2("NBO", "write", "writing to pipe");
                ((WritableByteChannel)_channel).write(buf);
                assert _switchboard.debug2("NBO", "write", "r=" + buf.remaining());
            }
            if (buf.remaining() > 0
                && _channel instanceof AbstractSelectableChannel) {
                assert _switchboard.debug2("NBO", "write", "storing");
                _switchboard.write((AbstractSelectableChannel)_channel, buf, this);
                _writeWaiting = true;
            }
        } finally {
            _writeWaitingLock.unlock();
        }
    }

    protected void clearWriteWaiting() {
        if (_writeWaitingLock.tryLock()) {
            try {
                _writeWaiting = false;
            } finally {
                _writeWaitingLock.unlock();
            }
        }
    }

    /**
     * Tries to flush any bytes to the underlying stream
     */
    public void flush() {}

    /**
     * Close the underlying stream. It will not be possible to perform
     * any further writes.
     */
    public void close() throws IOException {
        _writeWaitingLock.lock();
        try {
            assert _switchboard.debug2("NBO", "close", "w=" + _writeWaiting);
            if (!_writeWaiting) {
                _channel.close();
            } else {
                _switchboard.close((AbstractSelectableChannel)_channel);
            }
        } finally {
            _writeWaitingLock.lock();
        }
    }
}
