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

public class RSAPrivateCrtKey extends RSAPrivateKey
    implements java.security.interfaces.RSAPrivateCrtKey {
	private static final long serialVersionUID = 1L;

    protected BigInteger publicExponent;
    protected BigInteger primeP;
    protected BigInteger primeQ;
    protected BigInteger primeExponentP;
    protected BigInteger primeExponentQ;
    protected BigInteger crtCoefficient;

    public RSAPrivateCrtKey(BigInteger modulus,
                            BigInteger publicExponent,
                            BigInteger privateExponent,
                            BigInteger primeP, BigInteger primeQ,
                            BigInteger crtCoefficient) {
        this(modulus, publicExponent, privateExponent, primeP, primeQ,
             RSAAlgorithm.getPrimeExponent(privateExponent, primeP),
             RSAAlgorithm.getPrimeExponent(privateExponent, primeQ),
             crtCoefficient);
    }

    public RSAPrivateCrtKey(BigInteger modulus,
                            BigInteger publicExponent,
                            BigInteger privateExponent,
                            BigInteger primeP, BigInteger primeQ,
                            BigInteger primeExponentP,
                            BigInteger primeExponentQ,
                            BigInteger crtCoefficient) {
        super(modulus, privateExponent);
        this.publicExponent = publicExponent;
        this.primeP         = primeP;
        this.primeQ         = primeQ;
        this.primeExponentP = primeExponentP;
        this.primeExponentQ = primeExponentQ;
        this.crtCoefficient = crtCoefficient;
    }

    public BigInteger getPublicExponent() {
        return publicExponent;
    }

    public BigInteger getPrimeP() {
        return primeP;
    }

    public BigInteger getPrimeQ() {
        return primeQ;
    }

    public BigInteger getPrimeExponentP() {
        return primeExponentP;
    }

    public BigInteger getPrimeExponentQ() {
        return primeExponentQ;
    }

    public BigInteger getCrtCoefficient() {
        return crtCoefficient;
    }

}
