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

package com.mindbright.security.ms;

import com.mindbright.asn1.ASN1Explicit;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1OID;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SequenceOf;
import com.mindbright.asn1.ASN1UTCTime;

import com.mindbright.security.x509.AlgorithmIdentifier;
import com.mindbright.security.x509.Extension;

public final class CtlInfo extends ASN1Sequence {
 
    public ASN1SequenceOf subjectUsage;
    public ASN1OctetString listIdentifier;
    public ASN1Integer sequenceNumber;
    public ASN1UTCTime thisUpdate;
    public ASN1UTCTime nextUpdate;
    public AlgorithmIdentifier subjectAlgorithm;
    public ASN1SequenceOf ctlEntries;
    public ASN1SequenceOf extensions;

    /* 
       CTL_INFO ::= SEQUENCE {
            subjectUsage   CTL_USAGE,     (SEQUENCE OF ASN1OID)
            listIdentifier CRYPT_DATA_BLOB,  (OPTIONAL ASN1OctetString)
            sequenceNumber CRYPT_INTEGER_BLOB, (OPTIONAL ASN1Integer )
            thisUpdate     UTCTIME,
            nextUpdate     UTCTIME, (OPTIONAL)
            subjectAlgorithm ALGORITHM_IDENTIFIER,
            ctlEntries     SEQUENCE OF CTL_ENTRY, 
            extensions     SEQUENCE OF X509.Extension
       }

       CTL_ENTRY ::= SEQUENCE {
            subjectIdentifier ASN1OctetString,
            attributes SET OF Attribute
       }

     */
    
    public CtlInfo() {
        subjectUsage     = new ASN1SequenceOf(ASN1OID.class);
        listIdentifier   = new ASN1OctetString();
        sequenceNumber   = new ASN1Integer();
        thisUpdate       = new ASN1UTCTime();
        nextUpdate       = new ASN1UTCTime();
        subjectAlgorithm = new AlgorithmIdentifier();
        ctlEntries       = new ASN1SequenceOf(CtlEntry.class);
        extensions       = new ASN1SequenceOf(Extension.class);

        addComponent(subjectUsage);
        addOptional(listIdentifier);
        addOptional(sequenceNumber);
        addComponent(thisUpdate);
        addOptional(nextUpdate);
        addComponent(subjectAlgorithm);
        addComponent(ctlEntries);
        addOptional(new ASN1Explicit(0, extensions));
    }
}
