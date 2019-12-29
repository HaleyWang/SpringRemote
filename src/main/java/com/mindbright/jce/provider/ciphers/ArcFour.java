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

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.Key;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.CipherSpi;
import javax.crypto.spec.SecretKeySpec;

public final class ArcFour extends CipherSpi {
    int    x;
    int    y;
    int[]  state = new int[256];

    int arcfour_byte() {
        int x;
        int y;
        int sx, sy;
        x = (this.x + 1) & 0xff;
        sx = state[x];
        y = (sx + this.y) & 0xff;
        sy = state[y];
        this.x = x;
        this.y = y;
        state[y] = (sx & 0xff);
        state[x] = (sy & 0xff);
        return state[((sx + sy) & 0xff)];
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
        byte[] output = new byte[engineGetOutputSize(inputLen)];
        engineDoFinal(input, inputOffset, inputLen, output, 0);
        return output;
    }
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output = new byte[engineGetOutputSize(inputLen)];
        engineUpdate(input, inputOffset, inputLen, output, 0);
        return output;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }    

    protected int engineDoFinal(byte[] input,
                                int inputOffset,
                                int inputLen,
                                byte[] output,
                                int outputOffset) {
        int n = engineUpdate(input, inputOffset, inputLen, output, outputOffset);
        // XXX: reset key
        return n;
    }


    protected int engineUpdate(byte[] input, int inputOffset, int inputLen,
                               byte[] output, int outputOffset) {
        int end = inputOffset + inputLen;
        for(int si = inputOffset, di = outputOffset; si < end; si++, di++)
            output[di] = (byte)((input[si] ^ arcfour_byte()) & 0xff);
        return inputLen;
    }

    public void initializeKey(byte[] key) {
        int t, u = 0;
        int keyindex;
        int stateindex;
        int counter;

        for(counter = 0; counter < 256; counter++)
            state[counter] = (byte)counter;
        keyindex = 0;
        stateindex = 0;
        for(counter = 0; counter < 256; counter++) {
            t = state[counter];
            stateindex = (stateindex + key[keyindex] + t) & 0xff;
            u = state[stateindex];
            state[stateindex] = t;
            state[counter] = u;
            if(++keyindex >= key.length)
                keyindex = 0;
        }
    }

    protected int engineGetBlockSize() {
        return 1;
    }

    protected byte[] engineGetIV() {
        return null;
    }

    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    protected void engineInit(int opmode, Key key,
                              AlgorithmParameterSpec params,
                              SecureRandom random)
    throws InvalidKeyException {
        initializeKey(((SecretKeySpec)key).getEncoded());
    }

    protected void engineInit(int opmode,
                              Key key,
                              AlgorithmParameters params,
                              SecureRandom random) 
        throws InvalidKeyException {
        // XXX:
        engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
    }

    protected void engineInit(int opmode, Key key,
                              SecureRandom random) throws InvalidKeyException {
        engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
    }

    protected void engineSetMode(String mode) {}

    protected void engineSetPadding(String padding) {}

}
