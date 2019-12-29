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

package com.mindbright.jce.provider.publickey;

import java.math.BigInteger;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

public class DSAKeyPairGenerator extends KeyPairGenerator {

    protected SecureRandom     random;
    protected int              keysize;
    protected DSAParameterSpec params;

    public DSAKeyPairGenerator() {
        super("DSA");
    }

    public void initialize(int keysize, SecureRandom random) {
        this.random  = random;
        this.keysize = keysize;
    }

    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
    throws InvalidAlgorithmParameterException {
        if(!(params instanceof DSAParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Invalid params: " +
                    params);
        }
        this.params = (DSAParameterSpec)params;
        this.random = random;
    }

    public KeyPair generateKeyPair() {
        if(random == null) {
            random = new SecureRandom();
        }
        if(params == null) {
            params = DSAAlgorithm.generateParams(keysize, 160, random);
        }

        BigInteger p = params.getP();
        BigInteger q = params.getQ();
        BigInteger g = params.getG();
        BigInteger x = DSAAlgorithm.generatePrivateKey(q, random);
        BigInteger y = DSAAlgorithm.generatePublicKey(g, p, x);

        return new KeyPair(new DSAPublicKey(y, p, q, g),
                           new DSAPrivateKey(x, p, q, g));
    }

}
