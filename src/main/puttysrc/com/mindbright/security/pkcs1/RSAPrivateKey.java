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

package com.mindbright.security.pkcs1;

import java.math.BigInteger;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1Integer;

/**
 *  NOTE: Currently only supports pkcs#1 v1.5, otherPrimeInfos is left out.
 *
 * <pre>
 * RSAPrivateKey ::= SEQUENCE {
 *   version                 Version,
 *   modulus                 INTEGER, -- (Usually large) n
 *   publicExponent          INTEGER, -- (Usually small) e
 *   privateExponent         INTEGER, -- (Usually large) d
 *   prime1                  INTEGER, -- (Usually large) p
 *   prime2                  INTEGER, -- (Usually large) q
 *   exponent1               INTEGER, -- (Usually large) d mod (p-1)
 *   exponent2               INTEGER, -- (Usually large) d mod (q-1)
 *   coefficient             INTEGER, -- (Usually large) (inverse of q) mod p
 *   otherPrimeInfos         OtherPrimeInfos OPTIONAL
 * }
 *
 * Version ::= INTEGER { two-prime(0), multi(1) }
 *   (CONSTRAINED BY {-- version must be multi if otherPrimeInfos present --})
 *
 * OtherPrimeInfos ::= SEQUENCE SIZE(1..MAX) OF OtherPrimeInfo
 *
 * OtherPrimeInfo ::= SEQUENCE {
 *   prime INTEGER,  -- ri
 *   exponent INTEGER, -- di
 *   coefficient INTEGER -- ti 
 * }
 *
 * </pre>
 */
public class RSAPrivateKey extends ASN1Sequence {

    public ASN1Integer version;
    public ASN1Integer modulus;
    public ASN1Integer publicExponent;
    public ASN1Integer privateExponent;
    public ASN1Integer prime1;
    public ASN1Integer prime2;
    public ASN1Integer exponent1;
    public ASN1Integer exponent2;
    public ASN1Integer coefficient;

    public RSAPrivateKey() {
        version         = new ASN1Integer();
        modulus         = new ASN1Integer();
        publicExponent  = new ASN1Integer();
        privateExponent = new ASN1Integer();
        prime1          = new ASN1Integer();
        prime2          = new ASN1Integer();
        exponent1       = new ASN1Integer();
        exponent2       = new ASN1Integer();
        coefficient     = new ASN1Integer();
        addComponent(version);
        addComponent(modulus);
        addComponent(publicExponent);
        addComponent(privateExponent);
        addComponent(prime1);
        addComponent(prime2);
        addComponent(exponent1);
        addComponent(exponent2);
        addComponent(coefficient);
    }

    private final static BigInteger one = BigInteger.valueOf(1L);

    private static BigInteger getPrimeExponent(BigInteger privateExponent,
                                               BigInteger prime) {
        BigInteger pe = prime.subtract(one);
        return privateExponent.mod(pe);
    }

    public RSAPrivateKey(int version,
                         BigInteger modulus, BigInteger publicExponent,
                         BigInteger privateExponent,
                         BigInteger prime1, BigInteger prime2,
                         BigInteger coefficient) {
        this(version, modulus, publicExponent, privateExponent,
             prime1, prime2,
             getPrimeExponent(privateExponent, prime1),
             getPrimeExponent(privateExponent, prime2),
             coefficient);
    }

    public RSAPrivateKey(int version,
                         BigInteger modulus, BigInteger publicExponent,
                         BigInteger privateExponent,
                         BigInteger prime1, BigInteger prime2,
                         BigInteger exponent1, BigInteger exponent2,
                         BigInteger coefficient) {
        this();
        this.version.setValue(version);
        this.modulus.setValue(modulus);
        this.publicExponent.setValue(publicExponent);
        this.privateExponent.setValue(privateExponent);
        this.prime1.setValue(prime1);
        this.prime2.setValue(prime2);
        this.exponent1.setValue(exponent1);
        this.exponent2.setValue(exponent2);
        this.coefficient.setValue(coefficient);
    }

}

