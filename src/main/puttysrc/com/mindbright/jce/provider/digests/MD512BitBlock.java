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

package com.mindbright.jce.provider.digests;

import java.security.DigestException;
import java.security.MessageDigestSpi;

public abstract class MD512BitBlock extends MessageDigestSpi implements Cloneable {
    protected long   count;
    protected int    rest;
    protected byte[] buffer;
    protected int    digestLength;

    protected final static byte md4_padding[] = {
        (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    protected final static int rotateLeft(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    protected MD512BitBlock(int digestLength) {
        super();
        buffer = new byte[64];
        count  = 0;
        rest   = 0;
        this.digestLength = digestLength;
    }

    public final Object clone() {
        MD512BitBlock c = cloneInternal();
        System.arraycopy(c.buffer, 0, buffer, 0, this.rest);
        c.count = this.count;
        c.rest  = this.rest;
        return c;
    }

    protected final void engineUpdate(byte input) {
        engineUpdate(new byte[] { input }, 0, 1);
    }

    protected final void engineUpdate(byte[] data, int offset, int length) {
        int left = 64 - rest;

        count += length;

        if(rest > 0 && length >= left) {
            System.arraycopy(data, offset, buffer, rest, left);
            transform(buffer, 0);
            offset += left;
            length -= left;
            rest   =  0;
        }

        while(length > 63) {
            transform(data, offset);
            offset += 64;
            length -= 64;
        }

        if(length > 0) {
            System.arraycopy(data, offset, buffer, rest, length);
            rest += length;
        }
    }

    protected final byte[] engineDigest() {
        byte[] buf = new byte[digestLength];
        try {
            engineDigest(buf, 0, buf.length);
        } catch(DigestException e) {
            /* Can't happen... */
        }
        return buf;
    }

    protected final int engineGetDigestLength() {
        return digestLength;
    }

    protected abstract void transform(byte data[], int offset);
    protected abstract MD512BitBlock cloneInternal();
}
