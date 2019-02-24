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
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

import com.mindbright.util.HexDump;

/**
 * Sniffer class which dumps a copy of all data transmitted. This
 * class prints a hex-dump of all data to <code>System.err</code>. It
 * is very useful for debugging.
 */
public class SSH2StreamSniffer implements SSH2StreamFilter,
    SSH2StreamFilterFactory {
    protected int id;

    protected class SniffOutput extends FilterOutputStream {

        public SniffOutput(OutputStream toBeFiltered) {
            super(toBeFiltered);
        }

        public void write(byte b[], int off, int len) throws IOException {
            HexDump.print("ch. #" + id + " tx:", true, b, off, len);
            out.write(b, off, len);
        }

    }

    protected class SniffInput extends FilterInputStream {

        public SniffInput(InputStream toBeFiltered) {
            super(toBeFiltered);
        }

        public int read(byte b[], int off, int len) throws IOException {
            int n = in.read(b, off, len);
            if(n >= 0) {
                HexDump.print("ch. #" + id + " rx:", true, b, off, n);
            } else {
                System.out.println("ch. #" + id + " rx: EOF");
            }
            return n;
        }

    }

    private static SSH2StreamSniffer factoryInstance;

    private SSH2StreamSniffer() {}

    public static synchronized SSH2StreamSniffer getFilterFactory() {
        if(factoryInstance == null) {
            factoryInstance = new SSH2StreamSniffer();
        }
        return factoryInstance;
    }

    public SSH2StreamFilter createFilter(SSH2Connection connection,
                                         SSH2StreamChannel channel) {
        SSH2StreamSniffer sniffer = new SSH2StreamSniffer();
        sniffer.id = channel.getChannelId();
        return sniffer;
    }

    public InputStream getInputFilter(InputStream toBeFiltered) {
        return new SniffInput(toBeFiltered);
    }

    public OutputStream getOutputFilter(OutputStream toBeFiltered) {
        return new SniffOutput(toBeFiltered);
    }

}
