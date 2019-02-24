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

package com.mindbright.ssh;

import com.jcraft.jzlib.ZStream;
import com.jcraft.jzlib.JZlib;

public final class SSHCompressorZLib extends SSHCompressor {

    private final static int DEFLATE_BUF_SIZE = 49152;
    private final static int INFLATE_BUF_SIZE = 65536;

    private ZStream dStream;
    private ZStream iStream;
    private byte[]  dBuf;
    private byte[]  iBuf;

    public SSHCompressorZLib() {}

    public void init(int mode, int level) {
        switch(mode) {
        case COMPRESS_MODE:
            dStream = new ZStream();
            dStream.deflateInit(level);
            dStream.next_out = dBuf = new byte[DEFLATE_BUF_SIZE];
            break;
        case UNCOMPRESS_MODE:
            iStream = new ZStream();
            iStream.inflateInit();
            iStream.next_out = iBuf = new byte[INFLATE_BUF_SIZE];
            break;
        default:
            throw new Error("Unknown mode sent to SSHCompressorZLib");
        }
    }

    public void compress(SSHPduOutputStream pdu) throws SSHCompressionException {
        SSHPduOutputStream.PduByteArrayOutputStream data = pdu.getOut();

        dStream.next_in        = data.getBuf();
        dStream.next_in_index  = 12;
        dStream.avail_in       = data.size() - 12;
        dStream.next_out_index = 0;
        dStream.avail_out      = DEFLATE_BUF_SIZE;

        int status = dStream.deflate(JZlib.Z_PARTIAL_FLUSH);

        if(status != JZlib.Z_OK) {
            throw new SSHCompressionException("Error in zlib deflate: " +
                                              status);
        }

        int dLen = DEFLATE_BUF_SIZE - dStream.avail_out;

        if((dStream.next_in.length - 128) < dLen) {
            data.setBuf(new byte[dLen + (dStream.next_in.length >>> 1)]);
        }

        System.arraycopy(dBuf, 0, data.getBuf(), 12, dLen);

        data.setCount(12 + dLen);
    }

    public void uncompress(SSHPduInputStream pdu)
    throws SSHCompressionException {
        SSHPduInputStream.PduByteArrayInputStream data = pdu.getIn();
        iStream.next_in        = data.getBuf();
        iStream.next_in_index  = data.getPos();
        iStream.avail_in       = pdu.length - 4;
        iStream.next_out_index = 0;
        iStream.avail_out      = INFLATE_BUF_SIZE;

        int status = iStream.inflate(JZlib.Z_PARTIAL_FLUSH);

        if(status != JZlib.Z_OK) {
            throw new SSHCompressionException("Error in zlib inflate: " +
                                              status);
        }

        int iLen = INFLATE_BUF_SIZE - iStream.avail_out;

        if(iStream.next_in.length < iLen) {
            pdu.bytes = new byte[iLen];
            data.setBuf(pdu.bytes);
        }

        System.arraycopy(iBuf, 0, data.getBuf(), 0, iLen);
        data.setPos(0);
        pdu.length = iLen;
    }

    public long numOfCompressedBytes() {
        if(iStream != null) {
            return iStream.total_in;
        }
        return dStream.total_out;
    }

    public long numOfUncompressedBytes() {
        if(iStream != null) {
            return iStream.total_out;
        }
        return dStream.total_in;
    }

}
