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

package com.mindbright.ssh2;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.util.InputStreamPipe;
import com.mindbright.util.OutputStreamPipe;

/**
 * Implements an internal channel which is connected ot a pair of pipes.
 */
public class SSH2InternalChannel extends SSH2StreamChannel {
    protected InputStreamPipe  rxPipe;
    protected OutputStreamPipe txPipe;

    /**
     * Create a new internal channel of the given type. The channel is
     * associated with an ssh connection. Channel types are
     * defined in <code>SSH2Connection</code> and starts with
     * <code>CH_TYPE</code>.
     *
     * @param channelType Type of channel to create.
     * @param connection The ssh connection to associate the channel with.
     */
    public SSH2InternalChannel(int channelType, SSH2Connection connection) {
        this(channelType, connection, false);
	}

    public SSH2InternalChannel(int channelType, SSH2Connection connection, boolean pty) {
        super(channelType, connection, connection, pty);
        int ioBufSz = connection.getPreferences().
                      getIntPreference(SSH2Preferences.INT_IO_BUF_SZ);
        in  = new InputStreamPipe(ioBufSz);
        out = new OutputStreamPipe();

        try {
            this.txPipe = new OutputStreamPipe();
            this.rxPipe = new InputStreamPipe(ioBufSz);
            this.rxPipe.connect((OutputStreamPipe)out);
            this.txPipe.connect((InputStreamPipe)in);
        } catch (IOException e) {
            connection.getLog().error("SSH2InternalChannel", "<constructor>",
                                      "can't happen, bug somewhere!?!");
        }
    }

    /**
     * Create a new internal channel of the given type. The channel is
     * associated with an ssh connection. Channel types are
     * defined in <code>SSH2Connection</code> and starts with
     * <code>CH_TYPE</code>. This constructor causes the channel  to
     * use the provided non-blocking streams for input and output
     *
     * @param channelType Type of channel to create.
     * @param connection  The ssh connection to associate the channel
     *                    with.
     * @param in          Non-blocking stream which the channel reads
     *                    data from, the read dat ais the sent over
     *                    the ssh connection to the server
     * @param out         Non-blocking stream which the channel writes
     *                    data to. The data comes from the ssh server.
     */
    public SSH2InternalChannel(int channelType, SSH2Connection connection,
                               NonBlockingInput in, NonBlockingOutput out) {
        super(channelType, connection, connection, in, out, false);
    }

    public SSH2InternalChannel(int channelType, SSH2Connection connection,
                               NonBlockingInput in, NonBlockingOutput out,
							   boolean pty) {
        super(channelType, connection, connection, in, out, pty);
    }

    /**
     * Get the input stream of the channel.
     *
     * @return The input stream.
     */
    public InputStream getInputStream() {
        return rxPipe;
    }

    /**
     * Get the output stream of the channel.
     *
     * @return The output stream.
     */
    public OutputStream getOutputStream() {
        return txPipe;
    }

}
