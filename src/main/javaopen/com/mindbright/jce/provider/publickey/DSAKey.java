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

import java.security.Key;
import java.security.spec.DSAParameterSpec;
import java.security.interfaces.DSAParams;

public class DSAKey extends DSAParameterSpec
    implements java.security.interfaces.DSAKey, Key {
	private static final long serialVersionUID = 1L;

    protected DSAKey(BigInteger p, BigInteger q, BigInteger g) {
        super(p, q, g);
    }

    public String getAlgorithm() {
        return "DSA";
    }

    public byte[] getEncoded() {
        return null;
    }

    public String getFormat() {
        return null;
    }

    public DSAParams getParams() {
        return this;
    }

}
