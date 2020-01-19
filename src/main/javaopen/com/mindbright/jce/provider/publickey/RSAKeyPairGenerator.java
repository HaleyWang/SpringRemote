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


import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;

public class RSAKeyPairGenerator extends KeyPairGenerator {

    protected SecureRandom random;
    protected int          keysize;

    public RSAKeyPairGenerator() {
        super("RSA");
    }

    public void initialize(int keysize, SecureRandom random) {
        this.random  = random;
        this.keysize = keysize;
    }

    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
    throws InvalidAlgorithmParameterException {
        throw new Error("Not implemented: " +
                        "'RSAKeyPairGenerator.initialize(int, SecureRandom)'");
    }

    public KeyPair generateKeyPair() {
        if(random == null) {
            random = new SecureRandom();
        }
        return RSAAlgorithm.generateKeyPair(keysize, random);
    }

}
