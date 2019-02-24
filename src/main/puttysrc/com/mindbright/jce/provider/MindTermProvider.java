/******************************************************************************
 *
 * Copyright (c) 2009-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.jce.provider;

import java.security.Provider;

public final class MindTermProvider extends Provider 
{
	private static final long serialVersionUID = 1L;

	public MindTermProvider() {
        super("MindTerm", 5.0, "MindTerm Security Provider v5.0");    

        // digests
        put("MessageDigest.SHA-1", "com.mindbright.jce.provider.digests.SHA1");
        put("Alg.Alias.MessageDigest.SHA1", "SHA-1");
        put("Alg.Alias.MessageDigest.SHA", "SHA-1");
        put("Alg.Alias.MessageDigest.1.3.14.3.2.26", "SHA-1");

        put("MessageDigest.SHA-256", "com.mindbright.jce.provider.digests.SHA256");
        put("Alg.Alias.MessageDigest.SHA256", "SHA-256");
        put("Alg.Alias.MessageDigest.2.16.840.1.101.3.4.2.1", "SHA-256");

        put("MessageDigest.MD2", "com.mindbright.jce.provider.digests.MD2");
        put("Alg.Alias.MessageDigest.1.2.840.113549.2.2", "MD2");
        put("MessageDigest.MD4", "com.mindbright.jce.provider.digests.MD4");
        put("Alg.Alias.MessageDigest.1.2.840.113549.2.4", "MD4");
        put("MessageDigest.MD5", "com.mindbright.jce.provider.digests.MD5");
        put("Alg.Alias.MessageDigest.1.2.840.113549.2.5", "MD5");
  
        put("MessageDigest.RIPEMD160", "com.mindbright.jce.provider.digests.RIPEMD160");
        put("Alg.Alias.MessageDigest.1.3.36.3.2.1", "RIPEMD160");


        // hmacs
        put("Mac.HmacSHA1", "com.mindbright.jce.provider.hmacs.HMACSHA1");
        put("Alg.Alias.Mac.1.3.14.3.2.26",      "HmacSHA1");
        put("Alg.Alias.Mac.1.3.6.1.5.5.8.1.2",  "HmacSHA1");

        put("Mac.HmacSHA256", "com.mindbright.jce.provider.hmacs.HMACSHA256");

        put("Mac.HmacMD5", "com.mindbright.jce.provider.hmacs.HMACMD5");
        put("Alg.Alias.Mac.1.2.840.113549.2.5", "HmacMD5");
        put("Alg.Alias.Mac.1.3.6.1.5.5.8.1.1",  "HmacMD5");

        put("Mac.HmacRIPEMD160", "com.mindbright.jce.provider.hmacs.HMACRIPEMD160");
        put("Alg.Alias.Mac.1.3.6.1.5.5.8.1.4",  "HmacRIPEMD160");


        // ciphers
        put("Cipher.DES",       "com.mindbright.jce.provider.ciphers.DES");
        put("Cipher.DESede",    "com.mindbright.jce.provider.ciphers.DES3");
        put("Cipher.Blowfish",  "com.mindbright.jce.provider.ciphers.Blowfish");
        put("Cipher.Twofish",   "com.mindbright.jce.provider.ciphers.Twofish");
        put("Cipher.AES",       "com.mindbright.jce.provider.ciphers.Rijndael");
        put("Cipher.IDEA",      "com.mindbright.jce.provider.ciphers.IDEA");
        put("Cipher.CAST128",   "com.mindbright.jce.provider.ciphers.CAST128");
        put("Cipher.RC2",       "com.mindbright.jce.provider.ciphers.RC2");
        put("Cipher.RC4",       "com.mindbright.jce.provider.ciphers.ArcFour");
        put("Alg.Alias.Cipher.Rijndael", "AES");
        put("Alg.Alias.Cipher.3DES",     "DESede");
        put("Alg.Alias.Cipher.CAST5",    "CAST128");


        // signatures
        put("Signature.SHA1withDSA", "com.mindbright.jce.provider.publickey.DSAWithSHA1");
        put("Signature.SHA1withRSA", "com.mindbright.jce.provider.publickey.RSAWithSHA1");
        put("Signature.MD5withRSA", "com.mindbright.jce.provider.publickey.RSAWithMD5");
//        put("Signature.MD4withRSA", "com.mindbright.jce.provider.publickey.RSAWithMD4");
        put("Signature.MD2withRSA", "com.mindbright.jce.provider.publickey.RSAWithMD2");
        put("Signature.RIPEMD160withRSA", "com.mindbright.jce.provider.publickey.RSAWithRIPEMD160");
        put("Alg.Alias.Signature.1.3.14.3.2.13", "SHA1withDSA");
        put("Alg.Alias.Signature.1.3.14.3.2.27", "SHA1withDSA");
        put("Alg.Alias.Signature.1.2.840.10040.4.3", "SHA1withDSA");
        put("Alg.Alias.Signature.1.3.14.3.2.29",  "SHA1withRSA");
        put("Alg.Alias.Signature.1.2.840.113549.1.1.5", "SHA1withRSA");
        put("Alg.Alias.Signature.1.3.14.3.2.3", "MD5withRSA");
        put("Alg.Alias.Signature.1.3.14.3.2.25", "MD5withRSA");
        put("Alg.Alias.Signature.1.2.840.113549.1.1.4", "MD5withRSA");
//         put("Alg.Alias.Signature.1.3.14.3.2.2", "MD4withRSA");
//         put("Alg.Alias.Signature.1.2.840.113549.1.1.3", "MD4withRSA");
        put("Alg.Alias.Signature.1.3.14.3.2.24", "MD2withRSA");
        put("Alg.Alias.Signature.1.2.840.113549.1.1.2", "MD2withRSA");
        put("Alg.Alias.Signature.1.3.36.3.3.1.2", "RIPEMD160withRSA");
        
        
        // key factories
        put("KeyFactory.RSA", "com.mindbright.jce.provider.publickey.RSAKeyFactory");
        put("KeyFactory.DSA", "com.mindbright.jce.provider.publickey.DSAKeyFactory");
        put("KeyFactory.DH",  "com.mindbright.jce.provider.publickey.DHKeyFactory");


        // key pair generators
        put("KeyPairGenerator.DH", "com.mindbright.jce.provider.publickey.DHKeyPairGenerator");
        put("KeyPairGenerator.RSA", "com.mindbright.jce.provider.publickey.RSAKeyPairGenerator");
        put("KeyPairGenerator.DSA", "com.mindbright.jce.provider.publickey.DSAKeyPairGenerator");


        // key agreement protocols
        put("KeyAgreement.DH", "com.mindbright.jce.provider.publickey.DHKeyAgreement");


        // key stores
        put("KeyStore.PKCS12", "com.mindbright.jce.provider.keystore.PKCS12KeyStore");
        put("KeyStore.Netscape", "com.mindbright.jce.provider.keystore.NetscapeKeyStore");
    }
}
