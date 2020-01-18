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

public final class DiffieHellman {
    private final static BigInteger one = BigInteger.valueOf(1);

    public static BigInteger generateX(BigInteger p, SecureRandom random) {
        return generateX(p, p.bitLength(), random);
    }

    public static BigInteger generateX(BigInteger p, int l, SecureRandom random) {
        BigInteger q = p.subtract(one).divide(BigInteger.valueOf(2));
        BigInteger x;

        do {
            x = new BigInteger(l, random);
        } while((x.compareTo(one) < 0) && (x.compareTo(q) > 0));

        return x;
    }

    public static BigInteger generateY(BigInteger x,
                                       BigInteger g, BigInteger p) {
        return g.modPow(x, p);
    }

    public static BigInteger computeKey(BigInteger x, BigInteger y,
                                        BigInteger p) {
        return y.modPow(x, p);
    }

    /* !!! DEBUG
    public static void main(String[] argv) {
    BigInteger group1P = new BigInteger(new byte[] {
     (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
     (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xC9, (byte)0x0F, (byte)0xDA,
     (byte)0xA2, (byte)0x21, (byte)0x68, (byte)0xC2, (byte)0x34, (byte)0xC4,
     (byte)0xC6, (byte)0x62, (byte)0x8B, (byte)0x80, (byte)0xDC, (byte)0x1C,
     (byte)0xD1, (byte)0x29, (byte)0x02, (byte)0x4E, (byte)0x08, (byte)0x8A,
     (byte)0x67, (byte)0xCC, (byte)0x74, (byte)0x02, (byte)0x0B, (byte)0xBE,
     (byte)0xA6, (byte)0x3B, (byte)0x13, (byte)0x9B, (byte)0x22, (byte)0x51,
     (byte)0x4A, (byte)0x08, (byte)0x79, (byte)0x8E, (byte)0x34, (byte)0x04,
     (byte)0xDD, (byte)0xEF, (byte)0x95, (byte)0x19, (byte)0xB3, (byte)0xCD,
     (byte)0x3A, (byte)0x43, (byte)0x1B, (byte)0x30, (byte)0x2B, (byte)0x0A,
     (byte)0x6D, (byte)0xF2, (byte)0x5F, (byte)0x14, (byte)0x37, (byte)0x4F,
     (byte)0xE1, (byte)0x35, (byte)0x6D, (byte)0x6D, (byte)0x51, (byte)0xC2,
     (byte)0x45, (byte)0xE4, (byte)0x85, (byte)0xB5, (byte)0x76, (byte)0x62,
     (byte)0x5E, (byte)0x7E, (byte)0xC6, (byte)0xF4, (byte)0x4C, (byte)0x42,
     (byte)0xE9, (byte)0xA6, (byte)0x37, (byte)0xED, (byte)0x6B, (byte)0x0B,
     (byte)0xFF, (byte)0x5C, (byte)0xB6, (byte)0xF4, (byte)0x06, (byte)0xB7,
     (byte)0xED, (byte)0xEE, (byte)0x38, (byte)0x6B, (byte)0xFB, (byte)0x5A,
     (byte)0x89, (byte)0x9F, (byte)0xA5, (byte)0xAE, (byte)0x9F, (byte)0x24,
     (byte)0x11, (byte)0x7C, (byte)0x4B, (byte)0x1F, (byte)0xE6, (byte)0x49,
     (byte)0x28, (byte)0x66, (byte)0x51, (byte)0xEC, (byte)0xE6, (byte)0x53,
     (byte)0x81, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
     (byte)0xFF, (byte)0xFF, (byte)0xFF
    });

    BigInteger group1G = BigInteger.valueOf(2);

    SecureRandom rand = new SecureRandom();

    BigInteger aliceX = generateX(group1P, rand);
    BigInteger aliceY = generateY(aliceX, group1G, group1P);
    BigInteger bobX   = generateX(group1P, rand);
    BigInteger bobY   = generateY(bobX, group1G, group1P);

    BigInteger aliceK = computeKey(aliceX, bobY, group1P);
    BigInteger bobK   = computeKey(bobX, aliceY, group1P);

    System.out.println("Alice's K = Bob's K? " +
      (aliceK.compareTo(bobK) == 0));
    System.out.println("Key = " + aliceK.toString(16));
    }
    */

}
