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

package com.mindbright.security.pkcs7;

import com.mindbright.asn1.ASN1Explicit;
import com.mindbright.asn1.ASN1Implicit;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SequenceOf;
import com.mindbright.asn1.ASN1SetOf;

import com.mindbright.security.x509.AlgorithmIdentifier;
import com.mindbright.security.x509.Attribute;

public final class SignerInfo extends ASN1Sequence {
    public ASN1Integer           version;
    public IssuerAndSerialNumber issuerAndSerialNumber;
    public AlgorithmIdentifier   digestAlgorithm;
    public ASN1SetOf             aaSet;
    public ASN1SequenceOf        aaSequence;
    public AlgorithmIdentifier   digestEncryptionAlgorithm;
    public ASN1OctetString       encryptedDigest;
    public ASN1SetOf             uaSet;
    public ASN1SequenceOf        uaSequence;
    
    public SignerInfo() {
        version                   = new ASN1Integer();
        issuerAndSerialNumber     = new IssuerAndSerialNumber();
        digestAlgorithm           = new AlgorithmIdentifier();
        aaSet                     = new ASN1SetOf(Attribute.class);
        aaSequence                = new ASN1SequenceOf(Attribute.class);
        digestEncryptionAlgorithm = new AlgorithmIdentifier();        
        encryptedDigest           = new ASN1OctetString();
        uaSet                     = new ASN1SetOf(Attribute.class);
        uaSequence                = new ASN1SequenceOf(Attribute.class);
        
        addComponent(version);
        addComponent(issuerAndSerialNumber);
        addComponent(digestAlgorithm);
        addOptional(new ASN1Implicit(0, aaSet));
        addOptional(new ASN1Explicit(2, aaSequence));
        addComponent(digestEncryptionAlgorithm);
        addComponent(encryptedDigest);
        addOptional(new ASN1Implicit(1, uaSet));
        addOptional(new ASN1Explicit(3, uaSequence));
    }
    
}
