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

public abstract class SSHCompressor {

    public final static int COMPRESS_MODE   = 1;
    public final static int UNCOMPRESS_MODE = 2;

    // !!! TODO
    public static SSHCompressor getInstance(String algorithm,
                                            int mode, int level)
    throws SSHCompressionException {
        if("zlib".equals(algorithm)) {
            try {
                Class<?> compCl =
                    Class.forName("com.mindbright.ssh.SSHCompressorZLib");
                SSHCompressor comp = (SSHCompressor)compCl.newInstance();
                comp.init(mode, level);
                return comp;
            } catch (Exception e) {
                throw new SSHCompressionException(e.getMessage());
            }
        }
        return null;
    }

    public abstract void init(int mode, int level);
    public abstract void compress(SSHPduOutputStream pdu)
    throws SSHCompressionException;
    public abstract void uncompress(SSHPduInputStream pdu)
    throws SSHCompressionException;
    public abstract long numOfCompressedBytes();
    public abstract long numOfUncompressedBytes();

}
