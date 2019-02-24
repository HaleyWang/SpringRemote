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

import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;

import java.nio.channels.spi.AbstractSelectableChannel;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A non-blocking input stream.
 */
public class NonBlockingInput {
    private ByteBuffer _pushback;
    private boolean _readWaiting = false;
    private final ReentrantLock _readWaitingLock = new ReentrantLock();
    private final Condition _readWaitingDone = _readWaitingLock.newCondition();
    private AbstractSelectableChannel _channel;
    private Switchboard _switchboard;

    protected NonBlockingInput(Switchboard switchboard,
                               AbstractSelectableChannel channel) 
        throws IOException {
        _switchboard = switchboard;
        _channel = channel;
        _channel.configureBlocking(false);
    }

    /**
     * Create a new NonBlockingInput object which reads data from the
     * given pipe.
     *
     * @param pipe  The pipe the object will read from
     */
    public NonBlockingInput(Pipe pipe) throws IOException {
        this(Switchboard.getSwitchboard(), pipe.source());
    }

    /**
     * Reads data from the stream and places it in the provided
     * buffer. This call will block until all the needed data has been read.
     * <p>
     * The method will fill the buffer from the current position to
     * the limit.
     *
     * @param buf buffer in which to store the read data
     */
    public void read(ByteBuffer buf) throws IOException {
        assert _switchboard.debug2("NBI", "read", "readb " + buf.remaining());

        // Get data from pushback buffer
        if (_pushback != null) {
            while (_pushback.remaining() > 0 && buf.remaining() > 0) {
                buf.put(_pushback.get());
            }
            if (_pushback.remaining() == 0) {
                _pushback = null;
            }
            if (buf.remaining() == 0) {
                return;
            }
        }

        // Check if we have any outstanding reads waiting
	_readWaitingLock.lock();
	try {
            if (_readWaiting) 
		_readWaitingDone.awaitUninterruptibly();
	} finally {
	    _readWaitingLock.unlock();
        }

        // Do the read in blocking mode
        _channel.configureBlocking(true);
        ((ReadableByteChannel)_channel).read(buf);
        _channel.configureBlocking(false);
    }

    /**
     * Reads data from the stream and places it in the provided
     * buffer. The provided callback will be called when the data has
     * been read.
     * <p>
     * The method will fill the buffer from the current position to
     * the limit.
     *
     * @param buf         buffer in which to store the read data
     * @param callback    class to notify when data has been read
     */
    public void read(ByteBuffer buf, NIOCallback callback) 
        throws IOException {
        read(buf, callback, false, false);
    }

    /**
     * Reads data from the stream and places it in the provided
     * buffer. The provided callback will be called when the data has
     * been read unless the data is already available. If the data is
     * available immediately and the mayShortcut parameter is true,
     * then the callback will not be called but the function will
     * return true.
     * <p>
     * The method will try to fill the buffer from the current
     * position to the limit. But if <code>shortDataOk</code> is true
     * then the callback will be called as soon as at least one byte
     * has been read.
     *
     * @param buf         buffer in which to store the read data
     * @param callback    class to notify when data has been read
     *                    (unless data is available immediately)
     * @param mayShortcut If this is true then the function will not
     *                    call the callback if data is available
     *                    immediately but instead return true.
     * @param shortDataOk If this is ok then the call will return/call
     *                    the callback as soon as data is available,
     *                    even if the buffer is not full.
     *
     * @return returns true if the data is available directly and the
     *         mayShortcut parameter is true. In this case the
     *         callback will not be called.
     */
    public boolean read(ByteBuffer buf, NIOCallback callback,
                        boolean mayShortcut, boolean shortDataOk)
        throws IOException {
        return read(buf, buf.position(), buf.limit()-buf.position(),
                    callback, mayShortcut, shortDataOk);
    }

    /**
     * Reads data from the stream and places it in the provided
     * buffer. The provided callback will be called when the data has
     * been read unless the data is already available. If the data is
     * available immediately and the mayShortcut parameter is true,
     * then the callback will not be called but the function will
     * return true.
     * <p>
     * The method will try to fill the buffer from the current
     * position to the limit. But if <code>shortDataOk</code> is true
     * then the callback will be called as soon as at least one byte
     * has been read.
     *
     * @param buf         buffer in which to store the read data
     * @param callback    class to notify when data has been read
     *                    (unless data is available immediately)
     * @param mayShortcut If this is true then the function will not
     *                    call the callback if data is available
     *                    immediately but instead return true.
     * @param shortDataOk If this is ok then the call will return/call
     *                    the callback as soon as data is available,
     *                    even if the buffer is not full.
     *
     * @return returns true if the data is available directly and the
     *         mayShortcut parameter is true. In this case the
     *         callback will not be called.
     */
    public boolean read(ByteBuffer buf, int offset, int length,
			NIOCallback callback,
			boolean mayShortcut,
			boolean shortDataOk) throws IOException {

	_readWaitingLock.lock();
	try {
	    assert _switchboard.debug2("NBI", "read", "read " + length + " o " + offset);

	    buf.position(offset);
	    buf.limit(offset+length);
	    if (_pushback != null) {
		while (_pushback.remaining() > 0 && buf.remaining() > 0) {
		    buf.put(_pushback.get());
		}
		if (_pushback.remaining() == 0) {
		    _pushback = null;
		}
		if (buf.remaining() == 0 ||
		    (shortDataOk && length > buf.remaining())) {

		    assert _switchboard.debug2("NBI", "read", "short:" + mayShortcut + " " + length + " " + hashCode());
		    if (mayShortcut) {
			return true;
		    }
		    assert _switchboard.debug2("NBI", "read", "completed pb " + buf.position());
		    callback.completed(buf);
		    return false;
		}
	    }
	    
	    if (!_readWaiting) {
		((ReadableByteChannel)_channel).read(buf);
	    } else {
			assert _switchboard.debug2("NBI", "read", "already waiting");
	    }
	    
	    if (buf.remaining() == 0 ||
		(shortDataOk && length > buf.remaining())) {
		assert _switchboard.debug2("NBI", "read", "got data: " + length + " " + hashCode());
		if (mayShortcut)
		    return true;
		
		assert _switchboard.debug2("NBI", "read", "completed " + buf.position());
		callback.completed(buf);
		return false;
	    }
	    assert _switchboard.debug2("NBI", "read", "queue: " + length + " " + hashCode());
	    _readWaiting = true;
	    _switchboard.read(_channel, buf, callback, this, shortDataOk);
	    return false;
	} finally {
	    _readWaitingLock.unlock();
	}
	
	
    }

    protected void clearReadWaiting() {
	if (_readWaitingLock.tryLock()) {
	    try {
		if (_readWaiting) {
		    _readWaiting = false;
		    _readWaitingDone.signal();
		}
	    } finally {
		_readWaitingLock.unlock();
	    }    
	}
	
    }

    /**
     * Push back data onto the stream so that future reads will get it.
     *
     * @param data array holding data to push back
     * @param offset offset to start getting data from
     * @param length number of bytes to push back
     */
    public void pushback(byte data[], int offset, int length) {
        _pushback = ByteBuffer.wrap(data, offset, length);
    }

    /**
     * Close the underlying stream. It will not be possible to perform
     * any further reads.
     */
    public void close() throws IOException {
        _switchboard.close(_channel);
    }

    /**
     * Create a ByteBuffer wrapped around the provided array
     */
    public ByteBuffer createBuffer(byte[] array) {
        return createBuffer(array, 0, array.length);
    }

    /**
     * Create a ByteBuffer wrapped around the provided array
     */
    public ByteBuffer createBuffer(byte[] array, int offset) {
        return createBuffer(array, offset, array.length - offset);
    }

    /**
     * Create a ByteBuffer wrapped around the provided array
     */
    public ByteBuffer createBuffer(byte[] array, int offset,
                                   int length) {
        return ByteBuffer.wrap(array, offset, length);
    }
}
