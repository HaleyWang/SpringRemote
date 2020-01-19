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
 * Below is some references to useful information about RSA:
 *
 * Bruce Schneier: Applied Cryptography 2nd ed., John Wiley & Sons, 1996
 * Arto Salomaa: Public-Key Cryptography 2nd ed., Springer-Verlag, 1996
 * R. Rivest, A. Shamir, and L. M. Adleman: Cryptographic Communications
 *    System and Method.  US Patent 4,405,829, 1983.
 */
package com.mindbright.jce.provider.publickey;

import java.math.BigInteger;

import java.security.SecureRandom;
import java.security.KeyPair;
import java.security.SignatureException;

public final class RSAAlgorithm {

    private final static BigInteger one = BigInteger.valueOf(1L);

    public static BigInteger doPublic(BigInteger input, BigInteger modulus,
                                      BigInteger publicExponent) {
        return input.modPow(publicExponent, modulus);
    }

    public static BigInteger doPrivate(BigInteger input, BigInteger modulus,
                                       BigInteger privateExponent) {
        return doPublic(input, modulus, privateExponent);
    }

    public static BigInteger doPrivateCrt(BigInteger input,
                                          BigInteger privateExponent,
                                          BigInteger primeP, BigInteger primeQ,
                                          BigInteger crtCoefficient) {
        return doPrivateCrt(input,
                            primeP, primeQ,
                            getPrimeExponent(privateExponent, primeP),
                            getPrimeExponent(privateExponent, primeQ),
                            crtCoefficient);
    }

    public static BigInteger doPrivateCrt(BigInteger input,
                                          BigInteger primeP, BigInteger primeQ,
                                          BigInteger primeExponentP,
                                          BigInteger primeExponentQ,
                                          BigInteger crtCoefficient) {
        if(!crtCoefficient.equals(primeQ.modInverse(primeP))) {
            BigInteger t = primeP;
            primeP = primeQ;
            primeQ = primeP;
            t = primeExponentP;
            primeExponentP = primeExponentQ;
            primeExponentQ = t;
        }
        BigInteger s_1 = input.modPow(primeExponentP, primeP);
        BigInteger s_2 = input.modPow(primeExponentQ, primeQ);
        BigInteger h   = crtCoefficient.multiply(s_1.subtract(s_2)).mod(primeP);
        return s_2.add(h.multiply(primeQ));
    }

    public static BigInteger getPrimeExponent(BigInteger privateExponent,
            BigInteger prime) {
        BigInteger pe = prime.subtract(one);
        return privateExponent.mod(pe);
    }

    public static BigInteger addPKCS1Pad(BigInteger input, int type,
                                         int padLen, SecureRandom rand)
    throws SignatureException {
        BigInteger result;
        BigInteger rndInt;
        int inByteLen  = (input.bitLength() + 7) / 8;

        if(inByteLen > padLen - 11) {
            throw new SignatureException("PKCS1Pad: Input too long to pad");
        }

        byte[] padBytes = new byte[(padLen - inByteLen - 3) + 1];
        padBytes[0] = 0;

        for(int i = 1; i < (padLen - inByteLen - 3 + 1); i++) {
            if(type == 0x01) {
                padBytes[i] = (byte)0xff;
            } else {
                byte[] b = new byte[1];
                do {
                    rand.nextBytes(b);
                } while(b[0] == 0);
                padBytes[i] = b[0];
            }
        }

        rndInt = new BigInteger(1, padBytes);
        rndInt = rndInt.shiftLeft((inByteLen + 1) * 8);
        result = BigInteger.valueOf(type);
        result = result.shiftLeft((padLen - 2) * 8);
        result = result.or(rndInt);
        result = result.or(input);

        return result;
    }

    public static BigInteger stripPKCS1Pad(BigInteger input, int type)
    throws SignatureException {
        byte[] strip = input.toByteArray();
        byte[] val;
        int    i;

        if(strip[0] != type) {
            throw new SignatureException("Invalid PKCS1 padding, type != " +
                                         type);
        }

        for(i = 1; i < strip.length; i++) {
            if(strip[i] == 0) {
                break;
            }
            if(type == 0x01 && strip[i] != (byte)0xff) {
                throw new SignatureException("Invalid PKCS1 padding, " +
                                             "corrupt data");
            }
        }

        if(i == strip.length) {
            throw new SignatureException("Invalid PKCS1 padding, corrupt data");
        }

        val = new byte[strip.length - i];
        System.arraycopy(strip, i, val, 0, val.length);
        return new BigInteger(1, val);
    }

    public static KeyPair generateKeyPair(int bits, SecureRandom secRand) {
        return generateKeyPair(bits, BigInteger.valueOf(0x10001L), secRand);
    }

    public static KeyPair generateKeyPair(int bits, BigInteger e,
                                          SecureRandom secRand) {
        BigInteger p   = null;
        BigInteger q   = null;
        BigInteger t   = null;
        BigInteger phi = null;
        BigInteger d   = null;
        BigInteger u   = null;
        BigInteger n   = null;

        boolean finished = false;

        int pbits = (bits + 1)/ 2;
        int qbits = bits - pbits;

        while(!finished) {
            p = new BigInteger(pbits, 80, secRand);
            q = new BigInteger(qbits, 80, secRand);

            if(p.compareTo(q) == 0) {
                continue;
            } else if(p.compareTo(q) < 0) {
                t = q;
                q = p;
                p = t;
            }

            t = p.gcd(q);
            if(t.compareTo(one) != 0) {
                continue;
            }

            n = p.multiply(q);

            if(n.bitLength() != bits) {
                continue;
            }

            phi = p.subtract(one).multiply(q.subtract(one));
            d   = e.modInverse(phi);
            u   = q.modInverse(p);

            finished = true;
        }

        RSAPrivateCrtKey prvKey = new RSAPrivateCrtKey(n, e, d, p, q, u);
        RSAPublicKey     pubKey = new RSAPublicKey(n, e);

        return new KeyPair(pubKey, prvKey);
    }

}
