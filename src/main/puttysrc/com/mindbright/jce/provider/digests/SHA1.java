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

public final class SHA1 extends MD512BitBlock {

    public static final int DIGEST_LENGTH = 20;

    private int[] hash;
    private int[] W;

    private static int F00_19(int a, int b, int c, int d, int e, int x) {
        a = (a << 5) | (a >>> 27);
        return a + (((c ^ d) & b) ^ d) + 0x5a827999 + e + x;
    }

    private static int F20_39(int a, int b, int c, int d, int e, int x) {
        a = (a << 5) | (a >>> 27);
        return a + (b ^ c ^ d) + 0x6ed9eba1 + e + x;
    }

    private static int F40_59(int a, int b, int c, int d, int e, int x) {
        a = (a << 5) | (a >>> 27);
        return a + ((b & c) | ((b | c) & d)) + 0x8f1bbcdc + e + x;
    }

    private static int F60_79(int a, int b, int c, int d, int e, int x) {
        a = (a << 5) | (a >>> 27);
        return a + (b ^ c ^ d) + 0xca62c1d6 + e + x;
    }

    protected void transform(byte data[], int offset) {
        int a = hash[0];
        int b = hash[1];
        int c = hash[2];
        int d = hash[3];
        int e = hash[4];
        int t, i;

        for (i = 0; i < 16; i++) {
            W[i] =
                ((((data[offset++] & 0xff)) << 24) |
                 (((data[offset++] & 0xff)) << 16) |
                 (((data[offset++] & 0xff)) <<  8) |
                 (((data[offset++] & 0xff))));
        }

        for(i = 16; i < 80; i++) {
            t = W[i-3] ^ W[i-8] ^ W[i-14] ^ W[i-16];
            W[i] = (t << 1) | (t >>> 31);
        }

        for(i = 0; i < 20; i++) {
            t = F00_19(a, b, c, d, e, W[i]);
            e = d;
            d = c;
            c = (b << 30) | (b >>> 2);
            b = a;
            a = t;
        }

        for(i = 20; i < 40; i++) {
            t = F20_39(a, b, c, d, e, W[i]);
            e = d;
            d = c;
            c = (b << 30) | (b >>> 2);
            b = a;
            a = t;
        }

        for(i = 40; i < 60; i++) {
            t = F40_59(a, b, c, d, e, W[i]);
            e = d;
            d = c;
            c = (b << 30) | (b >>> 2);
            b = a;
            a = t;
        }

        for(i = 60; i < 80; i++) {
            t = F60_79(a, b, c, d, e, W[i]);
            e = d;
            d = c;
            c = (b << 30) | (b >>> 2);
            b = a;
            a = t;
        }

        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
    }

    public SHA1() {
        super(DIGEST_LENGTH);
        hash   = new int[5];
        W      = new int[80];
        engineReset();
    }

    protected MD512BitBlock cloneInternal() {
        SHA1 md = new SHA1();

        md.hash[0] = this.hash[0];
        md.hash[1] = this.hash[1];
        md.hash[2] = this.hash[2];
        md.hash[3] = this.hash[3];
        md.hash[4] = this.hash[4];

        return md;
    }

    protected void engineReset() {
        hash[0] = 0x67452301;
        hash[1] = 0xefcdab89;
        hash[2] = 0x98badcfe;
        hash[3] = 0x10325476;
        hash[4] = 0xc3d2e1f0;
        count   = 0;
        rest    = 0;
    }

    protected int engineDigest(byte[] dest, int off, int len)
        throws DigestException {
        int padlen = (rest < 56) ? (56 - rest) : (120 - rest);
        
        if(len < DIGEST_LENGTH) {
            throw new DigestException("SHA1, output buffer too short");
        }

        count *= 8;
        byte[] countBytes = {
                                (byte)(count >>> 56),
                                (byte)(count >>> 58),
                                (byte)(count >>> 40),
                                (byte)(count >>> 32),
                                (byte)(count >>> 24),
                                (byte)(count >>> 16),
                                (byte)(count >>>  8),
                                (byte)(count)
                            };

        engineUpdate(md4_padding, 0, padlen);
        engineUpdate(countBytes, 0, 8);

        int i, h;
        for (i = 0; i < 5; i++) {
            h = hash[i];
            dest[off++] = (byte) ((h >>> 24) & 0xff);
            dest[off++] = (byte) ((h >>> 16) & 0xff);
            dest[off++] = (byte) ((h >>>  8) & 0xff);
            dest[off++] = (byte) ((h       ) & 0xff);
        }

        engineReset();

        return DIGEST_LENGTH;
    }

}
