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

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1Integer;

/**
 * Represents a DSA private key.
 *
 * <pre>
 *  Dss-Parms ::= SEQUENCE {
 *    p INTEGER,
 *    q INTEGER,
 *    g INTEGER
 * }
 * </pre>
 */
public class DSAParams extends ASN1Sequence {

    public ASN1Integer p;
    public ASN1Integer q;
    public ASN1Integer g;

    public DSAParams() {
        p = new ASN1Integer();
        q = new ASN1Integer();
        g = new ASN1Integer();
        addComponent(p);
        addComponent(q);
        addComponent(g);
    }

    public DSAParams(BigInteger p, BigInteger q, BigInteger g) {
        this();
        this.p.setValue(p);
        this.q.setValue(q);
        this.g.setValue(g);
    }
}
