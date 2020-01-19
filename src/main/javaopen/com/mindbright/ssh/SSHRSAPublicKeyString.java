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

package com.mindbright.ssh;

import java.io.*;

import java.math.BigInteger;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;


public class SSHRSAPublicKeyString {
    private static final long serialVersionUID = 1L;

    RSAPublicKey key;
    String user;
    String opts;

    public SSHRSAPublicKeyString(String opts, String user, BigInteger e, BigInteger n) throws IOException {
        try {
            KeyFactory       rsaKeyFact = com.mindbright.util.Crypto.getKeyFactory("RSA");
            RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);
            key = (RSAPublicKey)rsaKeyFact.generatePublic(rsaPubSpec);
        } catch (Exception ee) {
            throw new IOException("Failed to generate RSA public key");
        }
        this.opts = opts;
        this.user = user;
    }

    public BigInteger getPublicExponent() {
	return key.getPublicExponent();
    }

    public BigInteger getModulus() {
	return key.getModulus();
    }
    
    public RSAPublicKey getKey() {
	return key;
    }

    public static SSHRSAPublicKeyString createKey(String opts, String pubKey) 
	throws NoSuchElementException, IOException {
        StringTokenizer tok  = new StringTokenizer(pubKey);
        String          user = null;
        String e;
        String n;

        tok.nextToken();
        e    = tok.nextToken();
        n    = tok.nextToken();
        if(tok.hasMoreElements())
            user = tok.nextToken();

        return new SSHRSAPublicKeyString(opts, user, new BigInteger(e), new BigInteger(n));
    }

    public String getOpts() {
        return opts;
    }

    public String getUser() {
        return user;
    }

    public String toString() {
        int bitLen = getModulus().bitLength();
        return ((opts != null ? (opts + " ") : "") +
                bitLen + " " + getPublicExponent() + " " + getModulus() + " " +
                (user != null ? user : ""));
    }

    public void toFile(String fileName) throws IOException {
        FileOutputStream    fileOut = new FileOutputStream(fileName);
        SSHDataOutputStream dataOut = new SSHDataOutputStream(fileOut);
        dataOut.writeBytes(toString());
        dataOut.close();
    }

}
