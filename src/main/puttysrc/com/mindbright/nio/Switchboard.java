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

import java.net.InetAddress;

import java.nio.ByteBuffer;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import java.nio.channels.spi.AbstractSelectableChannel;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.concurrent.locks.ReentrantLock;

import com.mindbright.util.Log;


/**
 * A central switchboard class which handles all the waiting on
 * different events. This subclass handles the java.nio case.
 */
public class Switchboard implements Runnable {
    // The global default instance
    private static Switchboard _switchboard;

    private final static int OP_CONNECT = SelectionKey.OP_CONNECT;
    private final static int OP_READ    = SelectionKey.OP_READ;
    private final static int OP_WRITE   = SelectionKey.OP_WRITE;

    private Selector _selector;
    private Hashtable<Object, TimerData> _timers;
    private Hashtable<SelectionKey, ChannelData> _channels;
    private ReentrantLock _channelslock;
    private long _id = 0;
    private Thread _handler;
    private boolean _running = true;
    private Log _log = null;
    
    private Switchboard() throws IOException {
        _timers = new Hashtable<Object, TimerData>();
        _channels = new Hashtable<SelectionKey, ChannelData>();
        _channelslock = new ReentrantLock(true);
        _selector = Selector.open();

        _handler = new Thread(this, "Switchboard");
        _handler.setDaemon(true);
        _handler.start();

    }

    /**
     * Get the global switchboard instance. This will create a new
     * instance if none exists.
     */
    public synchronized static Switchboard getSwitchboard() {
        try {
            if (_switchboard == null) {
                _switchboard = new Switchboard();
            }
        } catch (Exception e) {}
        return _switchboard;
    }

    /**
     * Stop the switchboard instance
     */
    public static void stop() {
        if (_switchboard == null) 
            return;
        _switchboard._running = false;
        _switchboard = null;
    }
    
    
    /**
     * Set the log to use
     *
     * @param log log to use
     */
    public void setLog(Log log) {
        _log = log;
    }
    
    /**
     * Register a callback which is called regularly
     *
     * @param interval minimum number of milliseconds between calls
     * @param callback class to call back
     *
     * @return a key which should be used to cancel this callback
     *         once it is not longer needed.
     */
    public synchronized Object registerTimer(long interval,
                                             TimerCallback callback) {
        Object key = new Long(_id++);
        synchronized (_timers) {
            _timers.put(key, new TimerData(interval, callback));
        }
        notify();
        return key;
    }

    /**
     * Unregister a previously registered timer callback
     *
     * @param key the handler returned from registerTimer
     *
     */
    public synchronized void unregisterTimer(Object key) {
        synchronized (_timers) {
            _timers.remove(key);
        }
        notify();
    }

    /**
     * Establish a new network connection.
     *
     * @param addr  address of server to connect to
     * @param port  port to connect to
     * @param block true if the call should block until the connection
     *              is established
     *
     * @return the new network connection object. Note that this
     *         object may not be actually connected yet.
     */
    public NetworkConnection connect(InetAddress addr, int port, boolean block)
        throws IOException {
        return NetworkConnection.open(this, addr, port, block);
    }

    /**
     * Wait for a network connection to become connected.
     *
     * @param conn the network connection to wait on
     * @param timeout max time, in milliseconds, the connect may take
     * @param callback function to call back once connection is done
     *                 or has timed out.
     */
    public void notifyWhenConnected(NetworkConnection conn,
                                    long timeout,
                                    NIOCallback callback) {
        SocketChannel channel = conn.getChannel();
        if (channel.isConnected()) {
            try {
                callback.connected(false);
            } catch (Throwable t) {}
        } else {
            try {
                SelectionKey key;
                
                _channelslock.lock();
                try {
                    _selector.wakeup();
                    key = channel.register(_selector, OP_CONNECT);
                    _channels.put(key, new ChannelData(callback, timeout,
                                                       channel));
                } finally {
                    _channelslock.unlock();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    public final boolean debug2(String cls, String meth, String msg) {
        try {
			// System.out.println("["+Thread.currentThread().getId() + "] " + cls + "/" + meth + ": "+msg);
            if (_log != null)
				_log.debug2(cls, "[" + Thread.currentThread().getId() + "] " + meth, msg);
        } catch (Throwable t) {
            // t.printStackTrace();
        }
		return true;
    }

    private boolean dbg(String meth, String msg) {
		return debug2("SB", meth, msg);
    }

    protected void read(AbstractSelectableChannel channel, ByteBuffer buf,
                        NIOCallback callback, NonBlockingInput lock,
                        boolean shortDataOk) throws IOException {
        _channelslock.lock();
        try {
            _selector.wakeup();
            SelectionKey key = channel.keyFor(_selector);
            if (key == null) {
                key = channel.register(_selector, OP_READ);
                _channels.put(key, new ChannelData(
                                  callback, buf, channel, lock, shortDataOk));
                assert dbg("read", "new key for channel=" + channel + ", key="+key);
            } else if (!key.isValid()) {
                assert dbg("read", "channel=" + channel + ", invalid key=" + key);
            } else if ((key.interestOps() & OP_READ) == 0) {
                assert dbg("read", "channel=" + channel + ", key exists, key=" + key);
                ChannelData data = _channels.get(key);
                data.prepareRead(callback, buf, lock, shortDataOk);
                try {
                    key.interestOps(key.interestOps() | OP_READ);
                } catch (CancelledKeyException cke) {
                    // we can ignore this here
                }
            } else {
                assert dbg("read", "channel=" + channel + ", already reading, key=" + key);
            }
        } finally {
            _channelslock.unlock();
        }
    }

    /**
     * Check if the current thread is a thread which is allowed to
     * wait. This is uspposed to be called before waiting on an event
     * and should help guard against deadlocks. The function will
     * throw an exception if the thread may not wait.
     *
     * @throws Exception if the current thread is the event handling thread
     */
    public void checkForDeadlock() throws Exception {
        if (_handler.equals(Thread.currentThread())) {
            throw new Exception(
                "Can't wait here since it might cause a deadlock");
        }
    }

    protected void write(AbstractSelectableChannel channel, ByteBuffer buf,
                         NonBlockingOutput lock) throws IOException {
        _channelslock.lock();
        try {
            _selector.wakeup();
            SelectionKey key = channel.keyFor(_selector);
            if (key == null) {
                key = channel.register(_selector, OP_WRITE);
                _channels.put(key, new ChannelData(buf, channel, lock));
                assert dbg("write", "new key for channel: " + channel + ", key="+key);
            } else if ((key.interestOps() & OP_WRITE) == 0) {
                assert dbg("write", "preparing write, key=" + key);
                ChannelData data = _channels.get(key);
                data.prepareWrite(buf, lock);
                key.interestOps(key.interestOps() | OP_WRITE);
            } else {
                assert dbg("write", "adding data");
                ChannelData data = _channels.get(key);
                if (data._writeLock != lock)
                    assert dbg("write", "data._writeLock != lock");
                data.prepareWrite(buf, lock);
            }
        } finally {
            _channelslock.unlock();
        }
    }

    protected void close(AbstractSelectableChannel channel) throws IOException {
        _channelslock.lock();
        try {
            _selector.wakeup();
            SelectionKey key = channel.keyFor(_selector);
            assert dbg("close", "close " + channel + ","+channel.hashCode()+","+key);
            if (key != null && key.isValid() && (key.interestOps() & OP_WRITE) != 0) {
                assert dbg("close", "adding delayed close");
                ChannelData data = _channels.get(key);
                data.prepareClose();
            } else {
                // no key or no one waiting for write, so we can close it
                assert dbg("close", "closing");
                channel.close();
            }
        } finally {
            _channelslock.unlock();
        }
    }

    /**
     * This is the main loop which never exits. It waits for stuff to
     * happen and calls the relevant callbacks.
     */
    public void run() {
        assert dbg("run", "starting");

        while(_running) {
            try {
                long wait = 42000; // arbitrary value = 42 seconds
                long now = System.currentTimeMillis();
                boolean connectTimeout = false;
                
                /*
                 * The synchronized() here is really to make this code
                 * hang before it gets to _selector.select()
                 */
                _channelslock.lock();
                try {
                    Iterator<ChannelData> iter = _channels.values().iterator();
                    while (iter.hasNext()) {
                        ChannelData data = iter.next();
                        if (data._timeoutWhen != 0
                            && now+wait > data._timeoutWhen) {
                            wait = data._timeoutWhen - now;
                            connectTimeout = true;
                        }
                    }

                    Enumeration<SelectionKey> e = _channels.keys();
                    while (e.hasMoreElements()) {
                        SelectionKey key = e.nextElement();
                        if (!key.isValid()) {
                            ChannelData data = _channels.get(key);
                            if (data != null) data.clear(); // help out with GC.
                            _channels.remove(key);
                        }
                    }
                } finally {
                    _channelslock.unlock();
                }

                // Figure out how long to sleep
                synchronized (_timers) {
                    Iterator<TimerData> iter = _timers.values().iterator();
                    while (iter.hasNext()) {
                        TimerData t = iter.next();
                        if (t._next < now+wait) {
                            wait = t._next-now;
                        }
                    }
                }

                if (wait > 0) {
                    try {
                        _selector.select(wait);
                    } catch (Error err) {
                        String msg = err.getMessage();
                        if (msg == null || msg.indexOf("POLLNVAL") == -1) {
                            assert dbg("run", "got error: " + err);
                            if (_log != null)
                                _log.debug(err);
                        }
                        continue;
                    } catch (Exception e) {
                        continue;
                    }
                }

                _channelslock.lock();
                try {
                    Iterator<SelectionKey> iter = _selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        try {
                            handleKey(key);
                        } catch (CancelledKeyException e) {}
                    }
                } finally {
                    _channelslock.unlock();
                }

                // Call expired timers
                now = System.currentTimeMillis();
                synchronized (_timers) {
                    Iterator<TimerData> iter = _timers.values().iterator();
                    while (iter.hasNext()) {
                        TimerData t = iter.next();
                        if (now >= t._next) {
                            try {
                                assert dbg("run", "calling timer trig: " + t._callback);
                                t._callback.timerTrig();
                            } catch (Throwable uppkast) {}
                            t._next = now+t._interval;
                        }
                    }
                }
            
                if (connectTimeout) {
                    _channelslock.lock();
                    try {
                        Iterator<SelectionKey> iter = _channels.keySet().iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = iter.next();
                            ChannelData data = _channels.get(key);
                            if (data._timeoutWhen != 0 && data._timeoutWhen < now) {
                                data._timeoutWhen = 0;
                                try {
                                    assert dbg("run", "calling connected callback timeout: " + data._callback);
                                    data._callback.connected(true);
                                } catch (Throwable t) {}
                                if (key.isValid())
                                    key.cancel();
                                iter.remove();
                            }
                        }
                    } finally {
                        _channelslock.unlock();
                    }
                }

                Thread.yield();
            } catch(Throwable t) {
                assert dbg("run", "got throwable: " + t);
                // t.printStackTrace();
                if (_log != null)
                    _log.debug(t);
            }
        }
    }

    /**
     * Perform the suitable actions on a triggered key
     */
    private void handleKey(SelectionKey key) throws CancelledKeyException {
        ChannelData data = _channels.get(key);

        // Check if channel has been connected
        if (key.isConnectable() && (key.interestOps() & OP_CONNECT) != 0) {
            try {
                ((SocketChannel)data._channel).finishConnect();
                try {
                    assert dbg("handleKey", "calling connected callback: " + data._callback);
                    data._callback.connected(false);
                } catch (Throwable t) {}
            } catch (Exception e) {
                try {
                    assert dbg("handleKey", "calling connFail callback: " + data._callback);
                    data._callback.connectionFailed(e);
                } catch (Throwable t) {}
            }
            data._timeoutWhen = 0;
            key.interestOps(key.interestOps() & ~OP_CONNECT);
        }

        // Check if channel is ready to read
        if (key.isReadable() && (key.interestOps() & OP_READ) != 0) {
            ByteBuffer buf = data._readBuf;
            int n = -1;
            Exception e = null;

            assert dbg("read",
                buf.remaining() + " " + data._channel.hashCode());

            try {
                n = ((ReadableByteChannel)data._channel).read(buf);
                assert dbg("handleKey", "read, channel=" + data._channel + ", n=" + n);
                assert dbg("handleKey", "read, channel=" + data._channel + ", buf=" + buf);
            } catch (IOException ioe) {
                assert dbg("handleKey", "read failed, channel=" + data._channel + ", " + ioe);
                if (_log != null)
                    _log.debug(ioe);
                e = ioe;
            }

            if (n < 1 || e != null) {
                assert dbg("handleKey", "read failed, channel=" + data._channel);
                key.interestOps(key.interestOps() & ~OP_READ);
                data._readLock.clearReadWaiting();
                try {
                    assert dbg("handleKey", "calling read failed: " + data._callback);
                    data._callback.readFailed(e);
                } catch (Throwable t) {
                    // t.printStackTrace();
                }
            } else if (buf.remaining() == 0 || data._shortReadOk) {
                assert dbg("handleKey", "read done, channel=" + data._channel);
                key.interestOps(key.interestOps() & ~OP_READ);
                data._readLock.clearReadWaiting();
                try {
                    assert dbg("handleKey", "calling read completed "
                        + data._readBuf.position() + ": "
                        + data._callback);
                    data._callback.completed(data._readBuf);
                } catch (Throwable t) {
                    // t.printStackTrace();
                }
            }
        }

        // Check for channels ready to write
        if (key.isWritable() && (key.interestOps() & OP_WRITE) != 0) {
            assert dbg("handleKey", "write, channel=" + data._channel);

            while (data._writeBufs.size() > 0) {
                ByteBuffer buf = data._writeBufs.getFirst();
                try {
                    if (buf == null) {
                        // this is a delayed close
                        assert dbg("handleKey", "close, channel=" + data._channel);
                        data._channel.close();
                    } else {
                        assert dbg("handleKey", "write, channel=" + data._channel + ", buf=" + buf);
                        int n = ((WritableByteChannel)data._channel).write(buf);
                        assert dbg("handleKey", "write, channel=" + data._channel + ", n=" + n);
                    }
                } catch (IOException e) {
                    assert dbg("handleKey", "write failed, channel=" + data._channel);
                    if (_log != null)
                        _log.debug(e);
                    try {
                        data._channel.close();
                    } catch (Throwable t) {}
                    key.interestOps(key.interestOps() & ~OP_WRITE);
                    data._writeLock.clearWriteWaiting();
                    try {
                        assert dbg("handleKey", "calling write failed: " + data._callback);
                        data._callback.writeFailed();
                    } catch (Throwable t) {}
                    break;
                }
                // Did all data get written?
                if (buf != null && buf.remaining() > 0) {
                    break;
                }
                data._writeBufs.removeFirst();
            }
            if (data._writeBufs.size() == 0) {
                key.interestOps(key.interestOps() & ~OP_WRITE);
                data._writeLock.clearWriteWaiting();
            }
        }
    }

    /**
     * Class holding data about a registered timer
     */
    private static class TimerData {
        TimerCallback _callback;
        long _interval;
        long _next;

        private TimerData(long interval, TimerCallback callback) {
            _interval = interval;
            _callback = callback;
            _next = System.currentTimeMillis() + interval;
        }
    }

    /**
     * Class holding data about a registered NetworkConnection
     */
    private static class ChannelData {
        NIOCallback _callback;
        long _timeoutWhen;
        AbstractSelectableChannel _channel;
        ByteBuffer _readBuf;
        LinkedList<ByteBuffer> _writeBufs = new LinkedList<ByteBuffer>();
        NonBlockingInput _readLock;
        NonBlockingOutput _writeLock;
        boolean _shortReadOk;
        boolean _closing = false;

        private ChannelData(NIOCallback callback, long timeout,
                            SocketChannel channel) {
            _callback = callback;
            _timeoutWhen = System.currentTimeMillis() + timeout;
            _channel = channel;
        }

        private ChannelData(NIOCallback callback, ByteBuffer buf,
                            AbstractSelectableChannel channel,
                            NonBlockingInput lock, boolean shortOk) {
            this(callback, buf, channel, lock, null, shortOk);
        }

        private ChannelData(NIOCallback callback, ByteBuffer buf,
                            SocketChannel channel, NonBlockingOutput lock) {
            this(callback, buf, channel, null, lock, false);
        }

        private ChannelData(NIOCallback callback, ByteBuffer buf,
                            AbstractSelectableChannel channel,
                            NonBlockingInput readLock,
                            NonBlockingOutput writeLock,
                            boolean shortOk) {
            _callback = callback;
            _readBuf = buf;
            _channel = channel;
            _readLock = readLock;
            _writeLock = writeLock;
            _shortReadOk = shortOk;
        }

        private ChannelData(ByteBuffer buf, AbstractSelectableChannel channel,
                            NonBlockingOutput lock) {
            _writeBufs.addLast(buf);
            _channel = channel;
            _writeLock = lock;
        }

        private void prepareRead(NIOCallback callback, ByteBuffer buf,
                                 NonBlockingInput lock, boolean shortOk) {
            _callback = callback;
            _readBuf = buf;
            _readLock = lock;
            _shortReadOk = shortOk;
        }

        private void prepareWrite(ByteBuffer buf, NonBlockingOutput lock) {
            _writeBufs.addLast(buf);
            _writeLock = lock;
        }

        private void prepareClose() {
            if (_closing)
                return;
            _closing = true;
            _writeBufs.addLast(null);
        }

        private void clear() {
            _callback  = null;
            _readBuf   = null;
            if (_writeBufs != null) {
                _writeBufs.clear();
                _writeBufs = null;
            }
            _channel   = null;
            _writeLock = null;
            _readLock  = null;
        }
    }
}
