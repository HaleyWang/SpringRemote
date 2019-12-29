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

public final class DES3 extends BlockCipher {
    private final static int BLOCK_SIZE = 8; // bytes in a data-block

    private DES des1;
    private DES des2;
    private DES des3;

    public DES3() {
        this.des1 = new DES();
        this.des2 = new DES();
        this.des3 = new DES();
    }

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    public synchronized void initializeKey(byte[] key)
    throws InvalidKeyException {
        if(key.length < (192 / 8)) {
            throw new InvalidKeyException("Key too short for 3des");
        }
        byte[] subKey = new byte[8];
        System.arraycopy(key, 0, subKey, 0, 8);
        des1.initializeKey(subKey);
        System.arraycopy(key, 8, subKey, 0, 8);
        des2.initializeKey(subKey);
        System.arraycopy(key, 16, subKey, 0, 8);
        des3.initializeKey(subKey);
    }

    public void blockEncrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int t, i;
        int[] lr = new int[2];

        lr[0] = getIntLSBO(in, inOffset);
        lr[1] = getIntLSBO(in, inOffset + 4);

        DES.initPerm(lr);

        t = (lr[1] << 1) | (lr[1] >>> 31);
        lr[1] = (lr[0] << 1) | (lr[0] >>> 31);
        lr[0] = t;

        for (i = 0; i < 32; i += 4) {
            des1.desCipher1(lr, i);
            des1.desCipher2(lr, i + 2);
        }
        for (i = 30; i > 0; i -= 4) {
            des2.desCipher2(lr, i);
            des2.desCipher1(lr, i - 2);
        }
        for (i = 0; i < 32; i += 4) {
            des3.desCipher1(lr, i);
            des3.desCipher2(lr, i + 2);
        }

        lr[0] = (lr[0] >>> 1) | (lr[0] << 31);
        lr[1] = (lr[1] >>> 1) | (lr[1] << 31);

        DES.finalPerm(lr);

        putIntLSBO(lr[0], out, outOffset);
        putIntLSBO(lr[1], out, outOffset + 4);

    }

    public void blockDecrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int t, i;
        int[] lr = new int[2];

        lr[0] = getIntLSBO(in, inOffset);
        lr[1] = getIntLSBO(in, inOffset + 4);

        DES.initPerm(lr);

        t = (lr[1] << 1) | (lr[1] >>> 31);
        lr[1] = (lr[0] << 1) | (lr[0] >>> 31);
        lr[0] = t;

        for (i = 30; i > 0; i -= 4) {
            des3.desCipher1(lr, i);
            des3.desCipher2(lr, i - 2);
        }
        for (i = 0; i < 32; i += 4) {
            des2.desCipher2(lr, i);
            des2.desCipher1(lr, i + 2);
        }
        for (i = 30; i > 0; i -= 4) {
            des1.desCipher1(lr, i);
            des1.desCipher2(lr, i - 2);
        }

        lr[0] = (lr[0] >>> 1) | (lr[0] << 31);
        lr[1] = (lr[1] >>> 1) | (lr[1] << 31);

        DES.finalPerm(lr);

        putIntLSBO(lr[0], out, outOffset);
        putIntLSBO(lr[1], out, outOffset + 4);

    }

    /* !!! DEBUG
    public static void main(String[] argv) {
    try {
     DES3 des3 = new DES3();

     byte[] key  = "abcd1234xxxxyyyyzzzz0000".getBytes();
     byte[] data = "abcd1234xxxxyyyyzzzz0000".getBytes();
     byte[] enc  = new byte[128];

     com.mindbright.jce.crypto.spec.SecretKeySpec keyspec =
    new com.mindbright.jce.crypto.spec.SecretKeySpec(key, "DESede");

     des3.engineSetMode("CBC");
     des3.engineInit(2, keyspec, null, null);

     int n = des3.engineDoFinal(data, 0, data.length, enc, 0);

     com.mindbright.util.HexDump.hexDump(enc, 0, 10);

    } catch (Exception e) {
     System.out.println("Error: " + e);
    }
    }
    */

}
