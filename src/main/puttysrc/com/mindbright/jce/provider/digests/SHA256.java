/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone Group AB. All Rights Reserved.
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

public final class SHA256 extends MD512BitBlock {

    // implemented from pseudocode on http://en.wikipedia.org/wiki/SHA-1

    public static final int DIGEST_LENGTH = 32;

    private static final int H0 = 0x6a09e667;
    private static final int H1 = 0xbb67ae85;
    private static final int H2 = 0x3c6ef372;
    private static final int H3 = 0xa54ff53a;
    private static final int H4 = 0x510e527f;
    private static final int H5 = 0x9b05688c;
    private static final int H6 = 0x1f83d9ab;
    private static final int H7 = 0x5be0cd19;

    private static final int[] K = {
	0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
	0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
	0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
	0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
	0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
	0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
	0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
	0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2 };


    private int[] hash;
    private int[] W;


    private final static int RR(int x, int l) {
	return (x >>> l) | (x << (32 - l));
    }

    protected void transform(byte data[], int offset) {
        int a = hash[0];
        int b = hash[1];
        int c = hash[2];
        int d = hash[3];
        int e = hash[4];
        int f = hash[5];
        int g = hash[6];
        int h = hash[7];

        int i;

        for (i = 0; i < 16; i++) {
            W[i] =
                ((((data[offset++] & 0xff)) << 24) |
                 (((data[offset++] & 0xff)) << 16) |
                 (((data[offset++] & 0xff)) <<  8) |
                 (((data[offset++] & 0xff))));
        }

        for(i = 16; i < 64; i++) {
	    int s0 = RR(W[i-15],7) ^ RR(W[i-15],18) ^ W[i-15] >>> 3;
	    int s1 = RR(W[i-2],17) ^ RR(W[i-2],19) ^ W[i-2] >>> 10;
	    W[i] = W[i-16] + s0 + W[i-7] + s1;
        }

	for (i = 0; i < 64; i++) {
	    int s0 = RR(a,2) ^ RR(a,13) ^ RR(a,22);
	    int maj = (a & b) ^ (a & c) ^ (b & c);
	    int s1 = RR(e,6) ^ RR(e,11) ^ RR(e,25);
	    int ch = (e & f) ^ (~e & g);
	    int t1 = h + s1 + ch + K[i] + W[i]; 
	    h = g;
	    g = f;
	    f = e;
	    e = d + t1;
	    d = c;
	    c = b;
	    b = a;
	    a = t1 + s0 + maj;
	}

        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
        hash[5] += f;
        hash[6] += g;
        hash[7] += h;
    }

    public SHA256() {
        super(DIGEST_LENGTH);
        hash   = new int[8];
        W      = new int[64];
        engineReset();
    }

    protected MD512BitBlock cloneInternal() {
        SHA256 md = new SHA256();
	for (int i=0; i<hash.length; i++)
	    md.hash[i] = this.hash[i];
        return md;
    }

    protected void engineReset() {
        hash[0] = H0;
        hash[1] = H1;
        hash[2] = H2;
        hash[3] = H3;
        hash[4] = H4;
        hash[5] = H5;
        hash[6] = H6;
        hash[7] = H7;
        count   = 0;
        rest    = 0;
    }

    protected int engineDigest(byte[] dest, int off, int len)
    throws DigestException {
        int padlen = (rest < 56) ? (56 - rest) : (120 - rest);

        if (len < DIGEST_LENGTH) {
            throw new DigestException("SHA256, output buffer too short");
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
        for (i = 0; i < hash.length; i++) {
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
