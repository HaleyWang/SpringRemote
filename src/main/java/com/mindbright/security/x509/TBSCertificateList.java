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

package com.mindbright.security.x509;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SequenceOf;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1Explicit;

public class TBSCertificateList extends ASN1Sequence {

    public ASN1Integer          version;
    public AlgorithmIdentifier  signature;
    public Name                 issuer;
    public Time                 thisUpdate;
    public Time                 nextUpdate;
    public ASN1SequenceOf       revokedCertificates;
    public Extensions           extensions;

    public TBSCertificateList() {
        version              = new ASN1Integer();
        signature            = new AlgorithmIdentifier();
        issuer               = new Name();
        thisUpdate           = new Time();
        nextUpdate           = new Time();
        revokedCertificates  = new ASN1SequenceOf(RevokedCertificate.class);
        extensions           = new Extensions();

        addOptional(version);
        addComponent(signature);
        addComponent(issuer);
        addComponent(thisUpdate);
        addOptional(thisUpdate);
        addOptional(revokedCertificates);
        addOptional(new ASN1Explicit(0, extensions));
    }

    //     public String toString() {
    //         String s =
    //             "version= " + (version.isSet() ? version.getValue().toString() : "<not set>") + NL +
    //             "signature= " + signature.algorithmName() + NL +
    //             "issuer= " + issuer.getRFC2253Value() + NL +
    //             "thisUpdate= " + thisUpdate + NL +
    //             "nextUpdate= " + (nextUpdate.isSet() ? nextUpdate.toString() : "<not set>") + NL;

    //         s += "revokedCertificates= " + NL;
    //         for (int i=0; i<revokedCertificates.getCount(); i++) {
    //             s += "[" + i + "]: " + ((RevokedCertificate)revokedCertificates.getComponent(i)).toString() + NL;
    //         }

    //         s += "extensions= ...";
    //         return s;
    //     }
}
