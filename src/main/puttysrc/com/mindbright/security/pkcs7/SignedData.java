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

import com.mindbright.asn1.ASN1Implicit;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SequenceOf;
import com.mindbright.asn1.ASN1SetOf;

import com.mindbright.security.x509.AlgorithmIdentifier;
import com.mindbright.security.x509.Certificate;
import com.mindbright.security.x509.CertificateList;

public final class SignedData extends ASN1Sequence {

    public ASN1Integer    version;
    public ASN1SetOf      digestAlgorithms;
    public ContentInfo    contentInfo;
    public ASN1SequenceOf certSequence;
    public ASN1SetOf      certSet;
    public ASN1Sequence   crlSequence;
    public ASN1SetOf      crlSet;
    public ASN1SetOf      signerInfos;
    
    public SignedData() {
        version          = new ASN1Integer();
        digestAlgorithms = new ASN1SetOf(AlgorithmIdentifier.class);
        contentInfo      = new ContentInfo();
        certSequence     = new ASN1SequenceOf(Certificate.class);
        certSet          = new ASN1SetOf(Certificate.class);
        crlSequence      = new ASN1SequenceOf(CertificateList.class);
        crlSet           = new ASN1SetOf(CertificateList.class);
        signerInfos      = new ASN1SetOf(SignerInfo.class);
        addComponent(version);
        addComponent(digestAlgorithms);
        addComponent(contentInfo);
        addOptional(new ASN1Implicit(0, certSet));
        addOptional(new ASN1Implicit(2, certSequence));
        addOptional(new ASN1Implicit(1, crlSet));
        addOptional(new ASN1Implicit(3, crlSequence));
        addComponent(signerInfos);
    }
}

