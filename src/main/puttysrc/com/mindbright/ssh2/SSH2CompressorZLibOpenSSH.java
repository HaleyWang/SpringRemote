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


/**
 * Implements the zlib@openssh.com compression algorithm a implemented
 * in OpenSSH 4.2p1. This is just zlib but the compression starts when
 * the user has authenitcated successfully.
 */
public class SSH2CompressorZLibOpenSSH extends SSH2CompressorZLib {
    protected boolean authenticated = false;

    public void init(int mode, int level) {
        super.init(mode, level);
    }

    public void compress(SSH2DataBuffer data) throws SSH2CompressionException {
        if (authenticated) {
            super.compress(data);
        }
    }

    public int uncompress(SSH2DataBuffer data, int len)
        throws SSH2CompressionException {
        if (authenticated) {
            return super.uncompress(data, len);
        }
        return len;
    }

    public void authSucceeded() {
        authenticated = true;
    }
}
