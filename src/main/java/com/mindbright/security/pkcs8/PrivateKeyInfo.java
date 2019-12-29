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

package com.mindbright.security.pkcs8;

import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SetOf;
import com.mindbright.asn1.ASN1Implicit;

import com.mindbright.security.x509.AlgorithmIdentifier;
import com.mindbright.security.x509.Attribute;

public final class PrivateKeyInfo extends ASN1Sequence {

    public ASN1Integer         version;
    public AlgorithmIdentifier privateKeyAlgorithm;
    public ASN1OctetString     privateKey;
    public ASN1SetOf           attributes;

    public PrivateKeyInfo() {
        version             = new ASN1Integer();
        privateKeyAlgorithm = new AlgorithmIdentifier();
        privateKey          = new ASN1OctetString();
        attributes          = new ASN1SetOf(Attribute.class);
        addComponent(version);
        addComponent(privateKeyAlgorithm);
        addComponent(privateKey);
        addOptional(new ASN1Implicit(0, attributes));
    }

}

