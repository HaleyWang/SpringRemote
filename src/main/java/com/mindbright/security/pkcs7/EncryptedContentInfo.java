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

package com.mindbright.security.pkcs7;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OID;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1Implicit;

import com.mindbright.security.x509.AlgorithmIdentifier;

public final class EncryptedContentInfo extends ASN1Sequence {

    public ASN1OID             contentType;
    public AlgorithmIdentifier contentEncryptionAlgorithm;
    public ASN1OctetString     encryptedContent;

    public EncryptedContentInfo() {
        contentType                = new ASN1OID();
        contentEncryptionAlgorithm = new AlgorithmIdentifier();
        encryptedContent           = new ASN1OctetString();
        addComponent(contentType);
        addComponent(contentEncryptionAlgorithm);
        addOptional(new ASN1Implicit(0, encryptedContent));
    }

}

