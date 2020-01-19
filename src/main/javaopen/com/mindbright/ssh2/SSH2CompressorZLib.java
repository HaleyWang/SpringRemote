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

import com.jcraft.jzlib.ZStream;
import com.jcraft.jzlib.JZlib;

/**
 * Implements the zlib compression algorithm as described in the ssh protocol
 * draft. It uses the jzlib provided by JCraft to handle the actual
 * compression/uncompression.
 */
public class SSH2CompressorZLib extends SSH2Compressor {

    private final static int DEFLATE_BUF_SIZE = 49152;
    private final static int INFLATE_BUF_SIZE = 65536;

    private ZStream dStream;
    private ZStream iStream;
    private byte[]  dBuf;
    private byte[]  iBuf;

    public SSH2CompressorZLib() {}

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
            throw new Error("Unknown mode sent to SSH2CompressorZLib");
        }
    }

    public void compress(SSH2DataBuffer data) throws SSH2CompressionException {
        dStream.next_in        = data.getData();
        dStream.next_in_index  = 9;
        dStream.avail_in       = data.getWPos() - 9;
        dStream.next_out_index = 0;
        dStream.avail_out      = DEFLATE_BUF_SIZE;

        int status = dStream.deflate(JZlib.Z_PARTIAL_FLUSH);

        if(status != JZlib.Z_OK) {
            throw new SSH2CompressionException("Error in zlib deflate: " +
                                               status);
        }

        int dLen = DEFLATE_BUF_SIZE - dStream.avail_out;

        if((dStream.next_in.length - 256) < dLen) {
            data.setData(new byte[dLen + (dStream.next_in.length >>> 1)]);
        }

        System.arraycopy(dBuf, 0, data.getData(), 9, dLen);
        data.setWPos(9 + dLen);
    }

    public int uncompress(SSH2DataBuffer data, int len)
    throws SSH2CompressionException {
        iStream.next_in        = data.getData();
        iStream.next_in_index  = 9;
        iStream.avail_in       = len;
        iStream.next_out_index = 0;
        iStream.avail_out      = INFLATE_BUF_SIZE;

        int status = iStream.inflate(JZlib.Z_PARTIAL_FLUSH);

        if(status != JZlib.Z_OK) {
            throw new SSH2CompressionException("Error in zlib inflate: " +
                                               status);
        }

        int iLen = INFLATE_BUF_SIZE - iStream.avail_out;

        if((iStream.next_in.length - 256) < iLen) {
            data.setData(new byte[iLen + (iStream.next_in.length >>> 1)]);
        }

        System.arraycopy(iBuf, 0, data.getData(), 9, iLen);

        return iLen;
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
