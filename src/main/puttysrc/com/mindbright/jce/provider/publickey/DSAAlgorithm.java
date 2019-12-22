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
import java.security.spec.DSAParameterSpec;

public final class DSAAlgorithm {

    private final static BigInteger one = BigInteger.valueOf(1L);


    /* Return an array with two elements, a[0] = r and a[1] = s
     */
    public static BigInteger[] sign(BigInteger x,
                                    BigInteger p, BigInteger q, BigInteger g,
                                    byte[] data) {
        BigInteger hM = new BigInteger(1, data);

        hM = hM.mod(q);

        BigInteger r = g.modPow(x, p).mod(q);
        BigInteger s = x.modInverse(q).multiply(hM.add(x.multiply(r))).mod(q);

        return new BigInteger[] { r, s };
    }


    public static boolean verify(BigInteger y,
                                 BigInteger p, BigInteger q, BigInteger g,
                                 BigInteger r, BigInteger s, byte[] data) {
        BigInteger hM = new BigInteger(1, data);

        hM = hM.mod(q);
        BigInteger w  = s.modInverse(q);
        BigInteger u1 = hM.multiply(w).mod(q);
        BigInteger u2 = r.multiply(w).mod(q);
        BigInteger v  = g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);

        return (v.compareTo(r) == 0);
    }

    public static DSAParameterSpec
    generateParams(int pBits, int qBits, SecureRandom random) {
        BigInteger[] pq;
        BigInteger   g;

        pq = Math.findRandomStrongPrime(pBits, qBits, random);
        g  = Math.findRandomGenerator(pq[1], pq[0], random);

        return new DSAParameterSpec(pq[0], pq[1], g);
    }

    public static BigInteger generatePrivateKey(BigInteger q,
            SecureRandom random) {
        BigInteger x;
        do {
            x = new BigInteger(q.bitLength(), random);
        } while((x.compareTo(one) < 0) || (x.compareTo(q) > 0));
        return x;
    }

    public static BigInteger generatePublicKey(BigInteger g, BigInteger p,
            BigInteger x) {
        return g.modPow(x, p);
    }

}
