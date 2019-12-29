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

import java.security.Key;

import java.math.BigInteger;

public abstract class RSAKey
    implements java.security.interfaces.RSAKey, Key {
	private static final long serialVersionUID = 1L;

    protected BigInteger modulus;

    protected RSAKey(BigInteger modulus) {
        this.modulus = modulus;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public String getAlgorithm() {
        return "RSA";
    }

    public byte[] getEncoded() {
        return null;
    }

    public String getFormat() {
        return null;
    }

}
