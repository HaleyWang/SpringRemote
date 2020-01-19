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

public final class RIPEMD160 extends MD512BitBlock {

    public static final int DIGEST_LENGTH = 20;

    private int[] x;
    private int[] hash;

    // selection of message word
    private final static int[] r1 = {
                                        0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,
                                        7,  4, 13,  1, 10,  6, 15,  3, 12,  0,  9,  5,  2, 14, 11,  8,
                                        3, 10, 14,  4,  9, 15,  8,  1,  2,  7,  0,  6, 13, 11,  5, 12,
                                        1,  9, 11, 10,  0,  8, 12,  4, 13,  3,  7, 15, 14,  5,  6,  2,
                                        4,  0,  5,  9,  7, 12,  2, 10, 14,  1,  3,  8, 11,  6, 15, 13
                                    };
    private final static int[] r2 = {
                                        5, 14,  7,  0,  9,  2, 11,  4, 13,  6, 15,  8,  1, 10,  3, 12,
                                        6, 11,  3,  7,  0, 13,  5, 10, 14, 15,  8, 12,  4,  9,  1,  2,
                                        15,  5,  1,  3,  7, 14,  6,  9, 11,  8, 12,  2, 10,  0,  4, 13,
                                        8,  6,  4,  1,  3, 11, 15,  0,  5, 12,  2, 13,  9,  7, 10, 14,
                                        12, 15, 10,  4,  1,  5,  8,  7,  6,  2, 13, 14,  0,  3,  9, 11
                                    };

    // amount for rotate left (rol)
    private final static int[] s1  = {
                                         11, 14, 15, 12,  5,  8,  7,  9, 11, 13, 14, 15,  6,  7,  9,  8,
                                         7,  6,  8, 13, 11,  9,  7, 15,  7, 12, 15,  9, 11,  7, 13, 12,
                                         11, 13,  6,  7, 14,  9, 13, 15, 14,  8, 13,  6,  5, 12,  7,  5,
                                         11, 12, 14, 15, 14, 15,  9,  8,  9, 14,  5,  6,  8,  6,  5, 12,
                                         9, 15,  5, 11,  6,  8, 13, 12,  5, 12, 13, 14, 11,  8,  5,  6
                                     };
    private final static int[] s2 = {
                                        8,  9,  9, 11, 13, 15, 15,  5,  7,  7,  8, 11, 14, 14, 12,  6,
                                        9, 13, 15,  7, 12,  8,  9, 11,  7,  7, 12,  7,  6, 15, 13, 11,
                                        9,  7, 15, 11,  8,  6,  6, 14, 12, 13,  5, 14, 13, 13,  7,  5,
                                        15,  5,  8, 11, 14, 14,  6, 14,  6,  9, 12,  9, 12,  5, 15,  8,
                                        8,  5, 12,  9, 12,  5, 14,  6,  8, 13,  6,  5, 15, 13, 11, 11
                                    };

    private static int F00_15(int a, int b, int c, int d, int e, int x, int s) {
        a = a + (b ^ c ^ d) + x;
        return rotateLeft(a, s) + e;
    }

    private static int F16_31(int a, int b, int c, int d, int e,
                              int x, int s, int k) {
        a = a + ((b & c) | (~b & d)) + x + k;
        return rotateLeft(a, s) + e;
    }

    private static int F32_47(int a, int b, int c, int d, int e,
                              int x, int s, int k) {
        a = a + ((b | ~c) ^ d) + x + k;
        return rotateLeft(a, s) + e;
    }

    private static int F48_63(int a, int b, int c, int d, int e,
                              int x, int s, int k) {
        a = a + ((b & d) | (c & ~d)) + x + k;
        return rotateLeft(a, s) + e;
    }

    private static int F64_79(int a, int b, int c, int d, int e,
                              int x, int s, int k) {
        a = a + (b ^ (c | ~d)) + x + k;
        return rotateLeft(a, s) + e;
    }

    protected void transform(byte data[], int offset) {
        int a1, b1, c1, d1, e1, a2, b2, c2, d2, e2, t, i;

        a1 = a2 = hash[0];
        b1 = b2 = hash[1];
        c1 = c2 = hash[2];
        d1 = d2 = hash[3];
        e1 = e2 = hash[4];

        for (i = 0; i < 16; i++) {
            x[i] =
                ((((data[offset++] & 0xff))      ) |
                 (((data[offset++] & 0xff)) <<  8) |
                 (((data[offset++] & 0xff)) << 16) |
                 (((data[offset++] & 0xff)) << 24));
        }

        for (i = 0; i < 16; i++) {
            t = F00_15(a1, b1, c1, d1, e1, x[i], s1[i]);
            a1 = e1;
            e1 = d1;
            d1 = c1 << 10 | c1 >>> 22;
            c1 = b1;
            b1 = t;

            t = F64_79(a2, b2, c2, d2, e2, x[r2[i]], s2[i], 0x50a28be6);
            a2 = e2;
            e2 = d2;
            d2 = c2 << 10 | c2 >>> 22;
            c2 = b2;
            b2 = t;
        }

        for (i = 16; i < 32; i++) {
            t = F16_31(a1, b1, c1, d1, e1, x[r1[i]], s1[i], 0x5a827999);
            a1 = e1;
            e1 = d1;
            d1 = c1 << 10 | c1 >>> 22;
            c1 = b1;
            b1 = t;

            t = F48_63(a2, b2, c2, d2, e2, x[r2[i]], s2[i], 0x5c4dd124);
            a2 = e2;
            e2 = d2;
            d2 = c2 << 10 | c2 >>> 22;
            c2 = b2;
            b2 = t;
        }

        for (i = 32; i < 48; i++) {
            t = F32_47(a1, b1, c1, d1, e1, x[r1[i]], s1[i], 0x6ed9eba1);
            a1 = e1;
            e1 = d1;
            d1 = c1 << 10 | c1 >>> 22;
            c1 = b1;
            b1 = t;

            t = F32_47(a2, b2, c2, d2, e2, x[r2[i]], s2[i], 0x6d703ef3);
            a2 = e2;
            e2 = d2;
            d2 = c2 << 10 | c2 >>> 22;
            c2 = b2;
            b2 = t;
        }

        for (i = 48; i < 64; i++) {
            t = F48_63(a1, b1, c1, d1, e1, x[r1[i]], s1[i], 0x8f1bbcdc);
            a1 = e1;
            e1 = d1;
            d1 = c1 << 10 | c1 >>> 22;
            c1 = b1;
            b1 = t;

            t = F16_31(a2, b2, c2, d2, e2, x[r2[i]], s2[i], 0x7a6d76e9);
            a2 = e2;
            e2 = d2;
            d2 = c2 << 10 | c2 >>> 22;
            c2 = b2;
            b2 = t;
        }

        for (i = 64; i < 80; i++) {
            t = F64_79(a1, b1, c1, d1, e1, x[r1[i]], s1[i], 0xa953fd4e);
            a1 = e1;
            e1 = d1;
            d1 = c1 << 10 | c1 >>> 22;
            c1 = b1;
            b1 = t;

            t = F00_15(a2, b2, c2, d2, e2, x[r2[i]], s2[i]);
            a2 = e2;
            e2 = d2;
            d2 = c2 << 10 | c2 >>> 22;
            c2 = b2;
            b2 = t;
        }

        d2 += c1 + hash[1];
        hash[1] = hash[2] + d1 + e2;
        hash[2] = hash[3] + e1 + a2;
        hash[3] = hash[4] + a1 + b2;
        hash[4] = hash[0] + b1 + c2;
        hash[0] = d2;
    }

    public RIPEMD160() {
        super(DIGEST_LENGTH);
        hash = new int[5];
        x    = new int[16];
        engineReset();
    }

    protected MD512BitBlock cloneInternal() {
        RIPEMD160 md = new RIPEMD160();

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
            throw new DigestException("RIPEMD160, output buffer too short");
        }

        count *= 8;
        byte[] countBytes = {
                                (byte)(count),
                                (byte)(count >>>  8),
                                (byte)(count >>> 16),
                                (byte)(count >>> 24),
                                (byte)(count >>> 32),
                                (byte)(count >>> 40),
                                (byte)(count >>> 58),
                                (byte)(count >>> 56)
                            };

        engineUpdate(md4_padding, 0, padlen);
        engineUpdate(countBytes, 0, 8);

        int i, h;
        for (i = 0; i < 5; i++) {
            h = hash[i];
            dest[off++] = (byte)((h       ) & 0xff);
            dest[off++] = (byte)((h >>>  8) & 0xff);
            dest[off++] = (byte)((h >>> 16) & 0xff);
            dest[off++] = (byte)((h >>> 24) & 0xff);
        }

        engineReset();

        return DIGEST_LENGTH;
    }

}
