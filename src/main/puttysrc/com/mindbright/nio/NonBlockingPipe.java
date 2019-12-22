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

import java.io.IOException;

import java.nio.channels.Pipe;

/**
 * A non-blocking pipe to which one can write and read data.
 */
public class NonBlockingPipe {
    private Pipe _pipe;
    private NonBlockingInput _source = null;
    private NonBlockingOutput _sink = null;

    public NonBlockingPipe() throws IOException {
        _pipe = Pipe.open();
    }

    /**
     * Get the source side of the pipe. That is the side from which
     * one reads data.
     */
    public NonBlockingInput getSource() throws IOException {
        if (_source == null) {
            _source = new NonBlockingInput(_pipe);
        }
        return _source;
    }

    /**
     * Get the sink side of the pipe. That is the side to which one
     * writes data.
     */
    public NonBlockingOutput getSink() throws IOException {
        if (_sink == null) {
            _sink = new NonBlockingOutput(_pipe);
        }
        return _sink;
    }
}
