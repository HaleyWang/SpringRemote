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

import java.security.InvalidKeyException;
import java.security.SignatureException;

public abstract class RSAWithAny extends BaseSignature {

    // OID { 1.3.14.3.2.26 }
    // iso(1) identified-org(3) OIW(14) secsig(3) alg(2) sha1(26)
    private final static byte[] SHA1_ASN_ID = {
                0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
                0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
            };

    // OID { 1.3.36.3.2.1 }
    // iso(1) identified-org(3) teletrust(36) alg(3) hashAlg(2) ripemd160(1)
    private static final byte[] RIPEMD160_ASN_ID = {
                0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x24,
                0x03, 0x02, 0x01, 0x05, 0x00, 0x04, 0x14
            };

    // OID { 1.2.840.113549.2.5 }
    // iso(1) member-body(2) US(840) rsadsi(113549) digestAlg(2) md5(5)
    private final static byte[] MD5_ASN_ID = {
                0x30, 0x20, 0x30, 0x0c, 0x06, 0x08, 0x2a, (byte)0x86,
                0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x02, 0x05, 0x05, 0x00,
                0x04, 0x10
            };

    // OID { 1.2.840.113549.2.2 }
    // iso(1) member-body(2) US(840) rsadsi(113549) digestAlg(2) md2(2)
    private final static byte[] MD2_ASN_ID = {
                0x30, 0x20, 0x30, 0x0c, 0x06, 0x08, 0x2a, (byte)0x86,
                0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x02, 0x02, 0x05, 0x00,
                0x04, 0x10
            };

    private byte[] asnId;

    protected RSAWithAny(String digAlg) {
        super(digAlg);
        //
        // !!! TODO Handle this correctly with ASN1
        //
        if(digAlg.equals("SHA") || digAlg.equals("SHA1")) {
            asnId = SHA1_ASN_ID;
        } else if(digAlg.equals("MD5")) {
            asnId = MD5_ASN_ID;
        } else if(digAlg.equals("MD2")) {
            asnId = MD2_ASN_ID;
        } else if(digAlg.equals("RIPEMD160")) {
            asnId = RIPEMD160_ASN_ID;
        } else {
            throw new Error("Non supported digest algorithm: " + digAlg);
        }
    }

    protected void initVerify() throws InvalidKeyException {
        if(publicKey == null || !(publicKey instanceof RSAPublicKey)) {
            throw new InvalidKeyException("Wrong key for RSAWithAny verify: " +
                                          publicKey);
        }
    }

    protected void initSign() throws InvalidKeyException {
        if(privateKey == null || !(privateKey instanceof RSAPrivateKey)) {
            throw new InvalidKeyException("Wrong key for RSAWithAny sign: " +
                                          privateKey);
        }
    }

    protected byte[] sign(byte[] data) throws SignatureException {
        byte[] tmp = new byte[data.length + asnId.length];
        System.arraycopy(asnId, 0, tmp, 0, asnId.length);
        System.arraycopy(data, 0, tmp, asnId.length, data.length);
        data = tmp;

        BigInteger dataInt = new BigInteger(1, data);
        int        mLen    =
            (((RSAPrivateKey)privateKey).getModulus().bitLength() + 7) / 8;

        dataInt = RSAAlgorithm.addPKCS1Pad(dataInt, 1, mLen, getRandom());

        BigInteger signatureInt = null;

        if(privateKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey key      = (RSAPrivateCrtKey)privateKey;
            BigInteger primeP         = key.getPrimeP();
            BigInteger primeQ         = key.getPrimeQ();
            BigInteger primeExponentP = key.getPrimeExponentP();
            BigInteger primeExponentQ = key.getPrimeExponentQ();
            BigInteger crtCoefficient = key.getCrtCoefficient();

            signatureInt = RSAAlgorithm.doPrivateCrt(dataInt,
                           primeP, primeQ,
                           primeExponentP,
                           primeExponentQ,
                           crtCoefficient);
        } else {
            RSAPrivateKey key = (RSAPrivateKey)privateKey;
            BigInteger    privateExponent = key.getPrivateExponent();
            BigInteger    modulus         = key.getModulus();
            signatureInt = RSAAlgorithm.doPrivate(dataInt,
                                                  modulus, privateExponent);
        }

        byte[] sig = unsignedBigIntToBytes(signatureInt, mLen);

        return sig;
    }

    protected boolean verify(byte[] signature, byte[] data)
    throws SignatureException {
        RSAPublicKey key            = (RSAPublicKey)publicKey;
        BigInteger   publicExponent = key.getPublicExponent();
        BigInteger   modulus        = key.getModulus();
        BigInteger   signatureInt   = new BigInteger(1, signature);

        signatureInt = RSAAlgorithm.doPublic(signatureInt,
                                             modulus, publicExponent);

        signatureInt = RSAAlgorithm.stripPKCS1Pad(signatureInt, 1);
        signature    = signatureInt.toByteArray();

        if(data.length != (signature.length - asnId.length)) {
            return false;
        }

        byte[] cmp = asnId;
        for(int i = 0, j = 0; i < signature.length; i++, j++) {
            if(i == asnId.length) {
                cmp = data;
                j   = 0;
            }
            if(signature[i] != cmp[j]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] unsignedBigIntToBytes(BigInteger bi, int size) {
        byte[] tmp  = bi.toByteArray();
        byte[] tmp2 = null;
        if(tmp.length > size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, tmp.length - size, tmp2, 0, size);
        } else if(tmp.length < size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, 0, tmp2, size - tmp.length, tmp.length);
        } else {
            tmp2 = tmp;
        }
        return tmp2;
    }

}
