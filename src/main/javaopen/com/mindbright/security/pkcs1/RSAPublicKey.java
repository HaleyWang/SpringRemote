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
 * Represents the public part of an RSA key.
 *
 * <pre>
 * RSAPublicKey ::= SEQUENCE {
 *   modulus          INTEGER, -- (Usually large) n = p*q
 *   publicExponent   INTEGER  -- (Usually small) e 
 * }
 * </pre>
 */
public class RSAPublicKey extends ASN1Sequence {

    public ASN1Integer modulus;
    public ASN1Integer publicExponent;

    public RSAPublicKey() {
        modulus        = new ASN1Integer();
        publicExponent = new ASN1Integer();
        addComponent(modulus);
        addComponent(publicExponent);
    }

    public RSAPublicKey(BigInteger modulus, BigInteger publicExponent) {
        this();
        this.modulus.setValue(modulus);
        this.publicExponent.setValue(publicExponent);
    }

}
