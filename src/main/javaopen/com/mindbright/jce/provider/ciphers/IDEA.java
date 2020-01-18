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

/*
 * Author's comment: The contents of this file is heavily based upon Bruce
 * Schneier's c-code found in his book: Bruce Schneier: Applied Cryptography 2nd
 * ed., John Wiley & Sons, 1996
 *
 * The IDEA mathematical formula may be covered by one or more of the following
 * patents: PCT/CH91/00117, EP 0 482 154 B1, US Pat. 5,214,703.
 * Hence it might be subject to licensing for commercial use.
 */
package com.mindbright.jce.provider.ciphers;

public final class IDEA extends BlockCipher {
    private final static int BLOCK_SIZE = 8; // bytes in a data-block

    private int[] e_key_schedule;
    private int[] d_key_schedule;

    public IDEA() {
        e_key_schedule = new int[52];
        d_key_schedule = new int[52];
    }

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    public synchronized void initializeKey(byte[] key) {
        ideaExpandKey(key, e_key_schedule);
        ideaInvertKey(e_key_schedule, d_key_schedule);
    }

    private void ideaExpandKey(byte[] key, int[] key_schedule) {
        int i, ki = 0, j = 0;
        for(i = 0; i < 8; i++)
            key_schedule[i] = ((key[2 * i] & 0xff) << 8) | (key[(2 * i) + 1] & 0xff);

        for(i = 8, j = 0; i < 52; i++) {
            j++;
            key_schedule[ki + j + 7] =
                ((key_schedule[ki + (j & 7)] << 9) |
                 (key_schedule[ki + ((j + 1) & 7)] >>> 7)) & 0xffff;
            ki += j & 8;
            j &= 7;
        }
    }

    private void ideaInvertKey(int[] key, int[] key_schedule) {
        int i, j, k, t1, t2, t3;

        j = 0;
        k = 51;

        t1 = mulInv(key[j++]);
        t2 = (- key[j++]) & 0xffff;
        t3 = (- key[j++]) & 0xffff;
        key_schedule[k--] = mulInv(key[j++]);
        key_schedule[k--] = t3;
        key_schedule[k--] = t2;
        key_schedule[k--] = t1;

        for(i = 1; i < 8; i++) {
            t1 = key[j++];
            key_schedule[k--] = key[j++];
            key_schedule[k--] = t1;

            t1 = mulInv(key[j++]);
            t2 = (- key[j++]) & 0xffff;
            t3 = (- key[j++]) & 0xffff;
            key_schedule[k--] = mulInv(key[j++]);
            key_schedule[k--] = t2;
            key_schedule[k--] = t3;
            key_schedule[k--] = t1;
        }

        t1 = key[j++];
        key_schedule[k--] = key[j++];
        key_schedule[k--] = t1;

        t1 = mulInv(key[j++]);
        t2 = (- key[j++]) & 0xffff;
        t3 = (- key[j++]) & 0xffff;
        key_schedule[k--] = mulInv(key[j++]);
        key_schedule[k--] = t3;
        key_schedule[k--] = t2;
        key_schedule[k--] = t1;
    }

    private void ideaCipher(byte[] in, int inOffset, byte[] out, int outOffset,
                            int[] key_schedule) {
        int t1 = 0, t2 = 0, x1, x2, x3, x4, ki = 0;
        int l = getIntMSBO(in, inOffset);
        int r = getIntMSBO(in, inOffset + 4);

        x1 = (l >>> 16);
        x2 = (l & 0xffff);
        x3 = (r >>> 16);
        x4 = (r & 0xffff);

        for(int round = 0; round < 8; round++) {
            x1 = mul(x1 & 0xffff, key_schedule[ki++]);
            x2 = (x2 + key_schedule[ki++]);
            x3 = (x3 + key_schedule[ki++]);
            x4 = mul(x4 & 0xffff, key_schedule[ki++]);

            t1 = (x1 ^ x3);
            t2 = (x2 ^ x4);
            t1 = mul(t1 & 0xffff, key_schedule[ki++]);
            t2 = (t1 + t2);
            t2 = mul(t2 & 0xffff, key_schedule[ki++]);
            t1 = (t1 + t2);

            x1 = (x1 ^ t2);
            x4 = (x4 ^ t1);
            t1 = (t1 ^ x2);
            x2 = (t2 ^ x3);
            x3 = t1;
        }

        t2 = x2;
        x1 = mul(x1 & 0xffff, key_schedule[ki++]);
        x2 = (t1 + key_schedule[ki++]);
        x3 = ((t2 + key_schedule[ki++]) & 0xffff);
        x4 = mul(x4 & 0xffff, key_schedule[ki]);

        putIntMSBO((x1 << 16) | (x2 & 0xffff), out, outOffset);
        putIntMSBO((x3 << 16) | (x4 & 0xffff), out, outOffset + 4);
    }

    public void blockEncrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        ideaCipher(in, inOffset, out, outOffset, e_key_schedule);
    }

    public void blockDecrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        ideaCipher(in, inOffset, out, outOffset, d_key_schedule);
    }

    private static final int mul(int a, int b) {
        int ab = a * b;
        if(ab != 0) {
            int lo = ab & 0xffff;
            int hi = (ab >>> 16) & 0xffff;
            return ((lo - hi) + ((lo < hi) ? 1 : 0));
        }
        if(a == 0)
            return (1 - b);
        return  (1 - a);
    }

    private static final int mulInv(int x) {
        int t0, t1, q, y;
        if(x <= 1) {
            return x;
        }
        t1 = 0x10001 / x;
        y  = 0x10001 % x;
        if(y == 1) {
            return ((1 - t1) & 0xffff);
        }
        t0 = 1;
        do {
            q = x / y;
            x = x % y;
            t0 += q * t1;
            if (x == 1) {
                return t0;
            }
            q = y / x;
            y = y % x;
            t1 += q * t0;
        } while(y != 1);
        return ((1 - t1) & 0xffff);
    }
}

