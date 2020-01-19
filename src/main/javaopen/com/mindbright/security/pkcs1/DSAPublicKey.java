/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

import com.mindbright.asn1.ASN1Integer;

/**
 * Represents a DSA public key.
 *
 * <pre>
 * Dss-Pub-Key ::= INTEGER  -- Y
 * </pre>
 */
public class DSAPublicKey extends ASN1Integer {

    public DSAPublicKey() {}

    public DSAPublicKey(BigInteger y) {
        setValue(y);
    }

    public BigInteger getY() {
        return getValue();
    }
}
