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

import java.security.SecureRandom;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

public final class DHKeyAgreement extends KeyAgreementSpi {

    private DHPrivateKey prvKey;
    private BigInteger   lastKey;
    private boolean      lastPhase;

    protected void engineInit(Key key, SecureRandom random)
    throws InvalidKeyException {
        if(!(key instanceof DHPrivateKey)) {
            throw new InvalidKeyException("DHKeyAgreement got: " + key);
        }
        this.prvKey  = (DHPrivateKey)key;
    }

    protected void engineInit(Key key, AlgorithmParameterSpec params,
                              SecureRandom random)
    throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException(
            "DHKeyAgreement params not supported: " +
            params);
    }

    protected Key engineDoPhase(Key key, boolean lastPhase)
    throws InvalidKeyException, IllegalStateException {
        if(!(key instanceof DHPublicKey)) {
            throw new InvalidKeyException("Invalid key: " + key);
        }
        this.lastPhase = lastPhase;

        BigInteger y  = ((DHPublicKey)key).getY();

        lastKey = DiffieHellman.computeKey(prvKey.getX(),
                                           y, prvKey.getParams().getP());

        return new DHPublicKey(lastKey, prvKey.getParams().getP(), prvKey.getParams().getG());
    }

    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if(!lastPhase) {
            throw new IllegalStateException("DHKeyAgreement not final");
        }
        byte[] sharedSecret = lastKey.toByteArray();
        if (sharedSecret[0] == 0) {
            byte[] tmp = sharedSecret;
            sharedSecret = new byte[sharedSecret.length-1];
            System.arraycopy(tmp, 1, sharedSecret, 0, sharedSecret.length);
        }
        lastPhase = false;
        lastKey   = null;
        return sharedSecret;
    }

    protected int engineGenerateSecret(byte[] sharedSecret, int offset)
    throws IllegalStateException, ShortBufferException {
        byte[] genSecret = engineGenerateSecret();
        if(genSecret.length > (sharedSecret.length - offset)) {
            throw new ShortBufferException("DHKeyAgreement, buffer too small");
        }
        System.arraycopy(genSecret, 0, sharedSecret, offset, genSecret.length);
        return genSecret.length;
    }

    protected SecretKey engineGenerateSecret(String algorithm)
    throws IllegalStateException, NoSuchAlgorithmException,
        InvalidKeyException {
        throw new Error("DHKeyAgreement.engineGenerateSecret(String) not " +
                        "implemented");
    }

}
