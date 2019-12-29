/******************************************************************************
 *
 * Copyright (c) 2007-2011 Cryptzone Group AB. All Rights Reserved.
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

public final class MD4 extends MD512BitBlock {

    public static final int DIGEST_LENGTH = 16;

    private int[] x;
    private int[] hash;

    private static int FF(int a, int b, int c, int d, int x, int s, int ac) {
        a = a + (((c ^ d) & b) ^ d) + x + ac;
        return rotateLeft(a, s);
    }

    private static int GG(int a, int b, int c, int d, int x, int s, int ac) {
        a = a + (b&c|b&d|c&d) + x + ac;
        return rotateLeft(a, s);
    }

    private static int HH(int a, int b, int c, int d, int x, int s, int ac) {
        a = a + (b ^ c ^ d) + x + ac;
        return rotateLeft(a, s);
    }

    protected void transform(byte data[], int offset) {
        int a = hash[0];
        int b = hash[1];
        int c = hash[2];
        int d = hash[3];
        int i;
        for (i = 0; i < 16; i++) {
            x[i] =
                ((data[offset++] & 0xff))        |
                (((data[offset++] & 0xff)) << 8)  |
                (((data[offset++] & 0xff)) << 16) |
                (((data[offset++] & 0xff)) << 24);
        }

        // Round 1
        a = FF (a, b, c, d, x[ 0],   3, 0); // 1
        d = FF (d, a, b, c, x[ 1],   7, 0); // 2
        c = FF (c, d, a, b, x[ 2],  11, 0); // 3
        b = FF (b, c, d, a, x[ 3],  19, 0); // 4
        a = FF (a, b, c, d, x[ 4],   3, 0); // 5
        d = FF (d, a, b, c, x[ 5],   7, 0); // 6
        c = FF (c, d, a, b, x[ 6],  11, 0); // 7
        b = FF (b, c, d, a, x[ 7],  19, 0); // 8
        a = FF (a, b, c, d, x[ 8],   3, 0); // 9
        d = FF (d, a, b, c, x[ 9],   7, 0); // 10
        c = FF (c, d, a, b, x[10],  11, 0); // 11
        b = FF (b, c, d, a, x[11],  19, 0); // 12
        a = FF (a, b, c, d, x[12],   3, 0); // 13
        d = FF (d, a, b, c, x[13],   7, 0); // 14
        c = FF (c, d, a, b, x[14],  11, 0); // 15
        b = FF (b, c, d, a, x[15],  19, 0); // 16

        // Round 2
        a = GG (a, b, c, d, x[ 0],   3, 0x5a827999); // 17
        d = GG (d, a, b, c, x[ 4],   5, 0x5a827999); // 18
        c = GG (c, d, a, b, x[ 8],   9, 0x5a827999); // 19
        b = GG (b, c, d, a, x[12],  13, 0x5a827999); // 20
        a = GG (a, b, c, d, x[ 1],   3, 0x5a827999); // 21
        d = GG (d, a, b, c, x[ 5],   5, 0x5a827999); // 22
        c = GG (c, d, a, b, x[ 9],   9, 0x5a827999); // 23
        b = GG (b, c, d, a, x[13],  13, 0x5a827999); // 24
        a = GG (a, b, c, d, x[ 2],   3, 0x5a827999); // 25
        d = GG (d, a, b, c, x[ 6],   5, 0x5a827999); // 26
        c = GG (c, d, a, b, x[10],   9, 0x5a827999); // 27
        b = GG (b, c, d, a, x[14],  13, 0x5a827999); // 28
        a = GG (a, b, c, d, x[ 3],   3, 0x5a827999); // 29
        d = GG (d, a, b, c, x[ 7],   5, 0x5a827999); // 30
        c = GG (c, d, a, b, x[11],   9, 0x5a827999); // 31
        b = GG (b, c, d, a, x[15],  13, 0x5a827999); // 32

        // Round 3
        a = HH (a, b, c, d, x[ 0],   3, 0x6ed9eba1); // 33
        d = HH (d, a, b, c, x[ 8],   9, 0x6ed9eba1); // 34
        c = HH (c, d, a, b, x[ 4],  11, 0x6ed9eba1); // 35
        b = HH (b, c, d, a, x[12],  15, 0x6ed9eba1); // 36
        a = HH (a, b, c, d, x[ 2],   3, 0x6ed9eba1); // 37
        d = HH (d, a, b, c, x[10],   9, 0x6ed9eba1); // 38
        c = HH (c, d, a, b, x[ 6],  11, 0x6ed9eba1); // 39
        b = HH (b, c, d, a, x[14],  15, 0x6ed9eba1); // 40
        a = HH (a, b, c, d, x[ 1],   3, 0x6ed9eba1); // 41
        d = HH (d, a, b, c, x[ 9],   9, 0x6ed9eba1); // 42
        c = HH (c, d, a, b, x[ 5],  11, 0x6ed9eba1); // 43
        b = HH (b, c, d, a, x[13],  15, 0x6ed9eba1); // 44
        a = HH (a, b, c, d, x[ 3],   3, 0x6ed9eba1); // 45
        d = HH (d, a, b, c, x[11],   9, 0x6ed9eba1); // 46
        c = HH (c, d, a, b, x[ 7],  11, 0x6ed9eba1); // 47
        b = HH (b, c, d, a, x[15],  15, 0x6ed9eba1); // 48

        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
    }

    public MD4() {
        super(DIGEST_LENGTH);
        hash = new int[4];
        x    = new int[16];
        engineReset();
    }

    protected MD512BitBlock cloneInternal() {
        MD4 md = new MD4();

        md.hash[0] = this.hash[0];
        md.hash[1] = this.hash[1];
        md.hash[2] = this.hash[2];
        md.hash[3] = this.hash[3];

        return md;
    }

    protected void engineReset() {
        hash[0] = 0x67452301;
        hash[1] = 0xefcdab89;
        hash[2] = 0x98badcfe;
        hash[3] = 0x10325476;
        count   = 0;
        rest    = 0;
    }

    protected int engineDigest(byte[] dest, int off, int len)
    throws DigestException {
        int padlen = (rest < 56) ? (56 - rest) : (120 - rest);

        if(len < DIGEST_LENGTH) {
            throw new DigestException("MD4, output buffer too short");
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
        for (i = 0; i < 4; i++) {
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
