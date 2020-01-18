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

package com.mindbright.jce.provider.ciphers;

import java.security.InvalidKeyException;

public final class RC2 extends BlockCipher {

    private final static int BLOCK_SIZE = 8; // bytes in a data-block

    int[] K;

    private static final byte[] PITABLE = {
                                              (byte)0xd9, (byte)0x78, (byte)0xf9, (byte)0xc4, (byte)0x19, (byte)0xdd,
                                              (byte)0xb5, (byte)0xed, (byte)0x28, (byte)0xe9, (byte)0xfd, (byte)0x79,
                                              (byte)0x4a, (byte)0xa0, (byte)0xd8, (byte)0x9d, (byte)0xc6, (byte)0x7e,
                                              (byte)0x37, (byte)0x83, (byte)0x2b, (byte)0x76, (byte)0x53, (byte)0x8e,
                                              (byte)0x62, (byte)0x4c, (byte)0x64, (byte)0x88, (byte)0x44, (byte)0x8b,
                                              (byte)0xfb, (byte)0xa2, (byte)0x17, (byte)0x9a, (byte)0x59, (byte)0xf5,
                                              (byte)0x87, (byte)0xb3, (byte)0x4f, (byte)0x13, (byte)0x61, (byte)0x45,
                                              (byte)0x6d, (byte)0x8d, (byte)0x09, (byte)0x81, (byte)0x7d, (byte)0x32,
                                              (byte)0xbd, (byte)0x8f, (byte)0x40, (byte)0xeb, (byte)0x86, (byte)0xb7,
                                              (byte)0x7b, (byte)0x0b, (byte)0xf0, (byte)0x95, (byte)0x21, (byte)0x22,
                                              (byte)0x5c, (byte)0x6b, (byte)0x4e, (byte)0x82, (byte)0x54, (byte)0xd6,
                                              (byte)0x65, (byte)0x93, (byte)0xce, (byte)0x60, (byte)0xb2, (byte)0x1c,
                                              (byte)0x73, (byte)0x56, (byte)0xc0, (byte)0x14,	(byte)0xa7, (byte)0x8c,
                                              (byte)0xf1, (byte)0xdc, (byte)0x12, (byte)0x75, (byte)0xca, (byte)0x1f,
                                              (byte)0x3b, (byte)0xbe, (byte)0xe4, (byte)0xd1, (byte)0x42, (byte)0x3d,
                                              (byte)0xd4, (byte)0x30, (byte)0xa3, (byte)0x3c, (byte)0xb6, (byte)0x26,
                                              (byte)0x6f, (byte)0xbf, (byte)0x0e, (byte)0xda, (byte)0x46, (byte)0x69,
                                              (byte)0x07, (byte)0x57, (byte)0x27, (byte)0xf2, (byte)0x1d, (byte)0x9b,
                                              (byte)0xbc, (byte)0x94, (byte)0x43, (byte)0x03, (byte)0xf8, (byte)0x11,
                                              (byte)0xc7, (byte)0xf6, (byte)0x90, (byte)0xef, (byte)0x3e, (byte)0xe7,
                                              (byte)0x06, (byte)0xc3, (byte)0xd5, (byte)0x2f,	(byte)0xc8, (byte)0x66,
                                              (byte)0x1e, (byte)0xd7, (byte)0x08, (byte)0xe8, (byte)0xea, (byte)0xde,
                                              (byte)0x80, (byte)0x52, (byte)0xee, (byte)0xf7, (byte)0x84, (byte)0xaa,
                                              (byte)0x72, (byte)0xac, (byte)0x35, (byte)0x4d, (byte)0x6a, (byte)0x2a,
                                              (byte)0x96, (byte)0x1a, (byte)0xd2, (byte)0x71, (byte)0x5a, (byte)0x15,
                                              (byte)0x49, (byte)0x74, (byte)0x4b, (byte)0x9f, (byte)0xd0, (byte)0x5e,
                                              (byte)0x04, (byte)0x18, (byte)0xa4, (byte)0xec,	(byte)0xc2, (byte)0xe0,
                                              (byte)0x41, (byte)0x6e, (byte)0x0f, (byte)0x51, (byte)0xcb, (byte)0xcc,
                                              (byte)0x24, (byte)0x91, (byte)0xaf, (byte)0x50, (byte)0xa1, (byte)0xf4,
                                              (byte)0x70, (byte)0x39,	(byte)0x99, (byte)0x7c, (byte)0x3a, (byte)0x85,
                                              (byte)0x23, (byte)0xb8, (byte)0xb4, (byte)0x7a, (byte)0xfc, (byte)0x02,
                                              (byte)0x36, (byte)0x5b, (byte)0x25, (byte)0x55, (byte)0x97, (byte)0x31,
                                              (byte)0x2d, (byte)0x5d, (byte)0xfa, (byte)0x98, (byte)0xe3, (byte)0x8a,
                                              (byte)0x92, (byte)0xae, (byte)0x05, (byte)0xdf, (byte)0x29, (byte)0x10,
                                              (byte)0x67, (byte)0x6c, (byte)0xba, (byte)0xc9,	(byte)0xd3, (byte)0x00,
                                              (byte)0xe6, (byte)0xcf, (byte)0xe1, (byte)0x9e, (byte)0xa8, (byte)0x2c,
                                              (byte)0x63, (byte)0x16, (byte)0x01, (byte)0x3f, (byte)0x58, (byte)0xe2,
                                              (byte)0x89, (byte)0xa9,	(byte)0x0d, (byte)0x38, (byte)0x34, (byte)0x1b,
                                              (byte)0xab, (byte)0x33, (byte)0xff, (byte)0xb0, (byte)0xbb, (byte)0x48,
                                              (byte)0x0c, (byte)0x5f, (byte)0xb9, (byte)0xb1, (byte)0xcd, (byte)0x2e,
                                              (byte)0xc5, (byte)0xf3, (byte)0xdb, (byte)0x47, (byte)0xe5, (byte)0xa5,
                                              (byte)0x9c, (byte)0x77, (byte)0x0a, (byte)0xa6, (byte)0x20, (byte)0x68,
                                              (byte)0xfe, (byte)0x7f, (byte)0xc1, (byte)0xad
                                          };

    public RC2() {
        K = new int[64];
    }

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    private static final int getWordLSBO(byte[] src, int srcOffset) {
        return (( src[srcOffset    ] & 0xff)        |
                ((src[srcOffset + 1] & 0xff) << 8));
    }

    protected static final void putWordLSBO(int val, byte[] dest, int destOffset) {
        dest[destOffset    ] = (byte)( val         & 0xff);
        dest[destOffset + 1] = (byte)((val >>> 8 ) & 0xff);
    }

    private static int rotateLeft(int x, int n) {
        return ((x << n) | (x >>> (16 - n))) & 0xffff;
    }

    private static int rotateRight(int x, int n) {
        return ((x << (16 - n)) | (x >>> n)) & 0xffff;
    }

    public synchronized void initializeKey(byte[] key)
    throws InvalidKeyException {
        byte[] k = new byte[128];
        int T8 = key.length;
        if(T8 < 1 || T8 > 128) {
            throw new InvalidKeyException("Invalid key size for RC2");
        }
        int i;
        for(i = 0; i < T8; i++) {
            k[i] = key[i];
        }
        for(i = T8; i < 128; i++) {
            k[i] = PITABLE[((k[i - 1] & 0xff) + (k[i - T8] & 0xff)) & 0xff];
        }
        k[128 - T8] = PITABLE[k[128 - T8] & 0xff];
        for(i = 127 - T8; i >= 0; i--) {
            k[i] = PITABLE[((k[i + 1] & 0xff) ^ (k[i + T8] & 0xff)) & 0xff];
        }

        for(i = 0; i < 64; i++) {
            K[i] = getWordLSBO(k, 2 * i);
        }
    }

    public void blockEncrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int r0 = getWordLSBO(in, inOffset + 0);
        int r1 = getWordLSBO(in, inOffset + 2);
        int r2 = getWordLSBO(in, inOffset + 4);
        int r3 = getWordLSBO(in, inOffset + 6);
        int i, j;

        for(i = 0; i < 5; i++) {
            j = i * 4;
            r0 = rotateLeft((r0 + (r1 & ~r3) + (r2 & r3) + K[j + 0]) & 0xffff,
                            1);
            r1 = rotateLeft((r1 + (r2 & ~r0) + (r3 & r0) + K[j + 1]) & 0xffff,
                            2);
            r2 = rotateLeft((r2 + (r3 & ~r1) + (r0 & r1) + K[j + 2]) & 0xffff,
                            3);
            r3 = rotateLeft((r3 + (r0 & ~r2) + (r1 & r2) + K[j + 3]) & 0xffff,
                            5);
        }
        r0 = (r0 + K[r3 & 63]) & 0xffff;
        r1 = (r1 + K[r0 & 63]) & 0xffff;
        r2 = (r2 + K[r1 & 63]) & 0xffff;
        r3 = (r3 + K[r2 & 63]) & 0xffff;
        for(i = 5; i < 11; i++) {
            j = i * 4;
            r0 = rotateLeft((r0 + (r1 & ~r3) + (r2 & r3) + K[j + 0]) & 0xffff,
                            1);
            r1 = rotateLeft((r1 + (r2 & ~r0) + (r3 & r0) + K[j + 1]) & 0xffff,
                            2);
            r2 = rotateLeft((r2 + (r3 & ~r1) + (r0 & r1) + K[j + 2]) & 0xffff,
                            3);
            r3 = rotateLeft((r3 + (r0 & ~r2) + (r1 & r2) + K[j + 3]) & 0xffff,
                            5);
        }
        r0 = (r0 + K[r3 & 63]) & 0xffff;
        r1 = (r1 + K[r0 & 63]) & 0xffff;
        r2 = (r2 + K[r1 & 63]) & 0xffff;
        r3 = (r3 + K[r2 & 63]) & 0xffff;
        for(i = 11; i < 16; i++) {
            j = i * 4;
            r0 = rotateLeft((r0 + (r1 & ~r3) + (r2 & r3) + K[j + 0]) & 0xffff,
                            1);
            r1 = rotateLeft((r1 + (r2 & ~r0) + (r3 & r0) + K[j + 1]) & 0xffff,
                            2);
            r2 = rotateLeft((r2 + (r3 & ~r1) + (r0 & r1) + K[j + 2]) & 0xffff,
                            3);
            r3 = rotateLeft((r3 + (r0 & ~r2) + (r1 & r2) + K[j + 3]) & 0xffff,
                            5);
        }

        putWordLSBO(r0, out, outOffset + 0);
        putWordLSBO(r1, out, outOffset + 2);
        putWordLSBO(r2, out, outOffset + 4);
        putWordLSBO(r3, out, outOffset + 6);
    }

    public void blockDecrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int r0 = getWordLSBO(in, inOffset + 0);
        int r1 = getWordLSBO(in, inOffset + 2);
        int r2 = getWordLSBO(in, inOffset + 4);
        int r3 = getWordLSBO(in, inOffset + 6);
        int i, j;

        for(i = 15; i > 10; i--) {
            j = i * 4;
            r3 = (rotateRight(r3, 5) - (r0 & ~r2) - (r1 & r2) - K[j + 3]) &
                 0xffff;
            r2 = (rotateRight(r2, 3) - (r3 & ~r1) - (r0 & r1) - K[j + 2]) &
                 0xffff;
            r1 = (rotateRight(r1, 2) - (r2 & ~r0) - (r3 & r0) - K[j + 1]) &
                 0xffff;
            r0 = (rotateRight(r0, 1) - (r1 & ~r3) - (r2 & r3) - K[j + 0]) &
                 0xffff;
        }
        r3 = (r3 - K[r2 & 63]) & 0xffff;
        r2 = (r2 - K[r1 & 63]) & 0xffff;
        r1 = (r1 - K[r0 & 63]) & 0xffff;
        r0 = (r0 - K[r3 & 63]) & 0xffff;
        for(i = 10; i > 4; i--) {
            j = i * 4;
            r3 = (rotateRight(r3, 5) - (r0 & ~r2) - (r1 & r2) - K[j + 3]) &
                 0xffff;
            r2 = (rotateRight(r2, 3) - (r3 & ~r1) - (r0 & r1) - K[j + 2]) &
                 0xffff;
            r1 = (rotateRight(r1, 2) - (r2 & ~r0) - (r3 & r0) - K[j + 1]) &
                 0xffff;
            r0 = (rotateRight(r0, 1) - (r1 & ~r3) - (r2 & r3) - K[j + 0]) &
                 0xffff;
        }
        r3 = (r3 - K[r2 & 63]) & 0xffff;
        r2 = (r2 - K[r1 & 63]) & 0xffff;
        r1 = (r1 - K[r0 & 63]) & 0xffff;
        r0 = (r0 - K[r3 & 63]) & 0xffff;
        for(i = 4; i >= 0; i--) {
            j = i * 4;
            r3 = (rotateRight(r3, 5) - (r0 & ~r2) - (r1 & r2) - K[j + 3]) &
                 0xffff;
            r2 = (rotateRight(r2, 3) - (r3 & ~r1) - (r0 & r1) - K[j + 2]) &
                 0xffff;
            r1 = (rotateRight(r1, 2) - (r2 & ~r0) - (r3 & r0) - K[j + 1]) &
                 0xffff;
            r0 = (rotateRight(r0, 1) - (r1 & ~r3) - (r2 & r3) - K[j + 0]) &
                 0xffff;
        }

        putWordLSBO(r0, out, outOffset + 0);
        putWordLSBO(r1, out, outOffset + 2);
        putWordLSBO(r2, out, outOffset + 4);
        putWordLSBO(r3, out, outOffset + 6);
    }

}
