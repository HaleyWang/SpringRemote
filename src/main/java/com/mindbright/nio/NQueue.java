/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.util.ArrayList;

/**
 * Queue to use together with nio handling. This is a first-in
 * first-out queue where handling can be stopped. The handling of
 * objects is started automatically when the queue is created.
 *
 * @see NQueueCallback
 */
public class NQueue {
    private ArrayList<Object> _queue;
    private NQueueCallback _callback;
    private volatile boolean _stopped = false;
    private volatile boolean _running = false;
    private long _currhandler = -1;

    /**
     * Create a new queue which is going to use the provided
     * switchboard and call the provided callback with new objects.
     *
     * @param callback interface to call once new object arrive
     */
    public NQueue(NQueueCallback callback) {
        _queue = new ArrayList<Object>();
        _callback = callback;
    }

    /**
     * Append an object to the tail of this queue
     *
     * @param obj object to append
     */
    public void append(Object obj) {
        synchronized (_queue) {
            if (_running || _stopped) {
                _queue.add (obj);
                return;
            }
            _running = true;
            _currhandler = Thread.currentThread().getId();
        }

        handleQueue (obj);
    }

    private void handleQueue(Object obj) {
        for (;;) {
            if (obj != null)
                try {
                    _callback.handleQueue(obj);
                } catch (Throwable t) {
                }
            synchronized (_queue) {
                if (_stopped || _queue.isEmpty()) {
                    _running = false;
                    _currhandler = -1;
                    break;
                }
                obj = _queue.remove(0);
            }
        }
    }

    /**
     * Stop the processing of objects on this queue. This just means
     * that new objects are "queued" up instead of beeing processed.
     */
    public void stop() {
        synchronized (_queue) {
            _stopped = true;
            if (_currhandler == Thread.currentThread().getId())
                return;
        }
        Thread.yield();
        while (_running && _stopped)
            Thread.yield();
    }

    /**
     * Resume handling of objects. This will release all objects on
     * the queue at the moment.
     */
    public void restart() {
        synchronized (_queue) {
            _stopped = false;
            if (_running) return;
            _running = true;
        }
        handleQueue(null);
    }

    public String toString() {
        synchronized (_queue) {
            return "NQueue[stopped=" + _stopped + ",queued=" + _queue.size() + ",callback=" + _callback + "]";
        }
    }
}
