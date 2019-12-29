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

/**
 * A queue of objects. New objects can be inserted first or last. The
 * depth of the queue can be controlled and put operations will hang
 * if there is no room. If the queue is full then any put operation
 * will hang until the number of objects in the queue has sunk below
 * the hiwater mark.
 */
public final class Queue {

    final static int DEFAULT_DEPTH   = 64;
    final static int DEFAULT_HIWATER = 32;

    Object[]   queue;
    boolean    isWaitGet;
    boolean    isWaitPut;
    boolean    isBlocking;
    int        rOffset;
    int        wOffset;
    int        depth;
    int        hiwater;
    int        size;

    // Copies used for saving real values when disabling queue
    //
    int        depthCP;
    int        sizeCP;

    /**
     * Constructs a new queue with the default depth (64) and hiwater
     * (32) levels.
     */
    public Queue() {
        this(DEFAULT_DEPTH, DEFAULT_HIWATER);
    }

    /**
     * Constructs a new queue with custom depth and hiwater levels.
     */
    public Queue(int depth, int hiwater) {
        this.queue      = new Object[depth + 1];
        this.isWaitGet  = false;
        this.isWaitPut  = false;
        this.isBlocking = true;
        this.rOffset    = 0;
        this.wOffset    = 0;
        this.size       = 0;
        this.sizeCP     = -1;
        this.depth      = depth;
        this.hiwater    = hiwater;
    }

    /**
     * Append an object to the queue. This function will hang until
     * there is room in the queue.
     *
     * @param obj the object to append
     */
    public synchronized void putLast(Object obj) {
        putFlowControl();
        queue[wOffset++] = obj;
        if(wOffset == (depth + 1))
            wOffset = 0;
        if(isWaitGet)
            this.notify();
        size++;
    }

    /**
     * insert an object at the head of the queue. This function will hang until
     * there is room in the queue.
     *
     * @param obj the object to insert
     */
    public synchronized void putFirst(Object obj) {
        putFlowControl();
        rOffset--;
        if(rOffset == -1)
            rOffset = depth;
        queue[rOffset] = obj;
        if(isWaitGet)
            this.notify();
        size++;
    }

    /**
     * Release any blocked object. That is eventual calls to get are
     * unblocked.
     */
    public synchronized void release() {
        if(isWaitGet)
            this.notify();
    }

    /**
     * Disable the queue. A disabled queue will not accept any new objects
     */
    public synchronized void disable() {
        depthCP = depth;
        sizeCP  = size;
        depth   = 0;
        size    = 0;
        release();
    }

    /**
     * Enable a disabled queue.
     */
    public synchronized void enable() {
        if(sizeCP >= 0) {
            depth = depthCP;
            size          = sizeCP;
            sizeCP        = -1;
            if(!isEmpty()) {
                this.release();
            }
            if(isWaitPut && (size <= hiwater)) {
                this.notifyAll();
                isWaitPut = false;
            }
        }
    }

    /**
     * Controls if the getFirst call should block. The default
     * is that calls are blocking.
     *
     * @param block true if calls to getFirst should block
     */
    public synchronized void setBlocking(boolean block) {
        isBlocking = block;
        release();
    }

    /**
     * Check if the queue is empty
     *
     * @return true if the queue is empty
     */
    public synchronized boolean isEmpty() {
        return size == 0;
    }

    private final void putFlowControl() {
        /*
         * Note this must be a while-loop because of the possibility of unfair
         * thread scheduling due to the fact that the sematics for wait/notify
         * in java is that the order in which wait() unblocks is not defined
         * (i.e. even a thread which wasn't waiting for the lock BEFORE we
         * called wait() here can acquire the lock BEFORE our wait() returns)
         */
        while(size >= depth) {
            isWaitPut = true;
            try {
                this.wait(1000);
            } catch (InterruptedException e) { /* ignore */
            }
        }
    }

    private boolean dowait(long ms) {
        long start = System.currentTimeMillis();
        while (isEmpty()) {
            if(!isBlocking) {
                return false;
            }
            isWaitGet = true;
            try {
                this.wait(ms);
            } catch (InterruptedException e) {
                // !!!
            }

            if (ms > 0) {
                if ((System.currentTimeMillis() - start) > ms)
                    return false;
            }
        }
        return true;
    }

    /**
     * Get the first object on the queue, optionally with a timeout.
     *
     * @param ms how long, in milliseconds, to wait, if the queue is
     * blocking, for a new object.
     */
    public synchronized Object getFirst(long ms) {
        if (!dowait(ms))
            return null;

        Object obj = queue[rOffset];
        isWaitGet = false;
        queue[rOffset++] = null;
        if(rOffset == (depth + 1))
            rOffset = 0;
        if(isWaitPut && (size <= hiwater)) {
            this.notifyAll();
            isWaitPut = false;
        }
        size--;
        return obj;
    }

    /**
     * Get the first object on the queue.
     *
     * @return the first object of the queue or null if the queue is empty
     */
    public Object getFirst() {
        return getFirst(0);
    }
}
