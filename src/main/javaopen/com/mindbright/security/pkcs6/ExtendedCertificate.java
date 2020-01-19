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

package com.mindbright.security.pkcs6;

import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1SetOf;
import com.mindbright.security.x509.Attribute;
import com.mindbright.security.x509.Certificate;

public final class ExtendedCertificate extends ASN1Sequence {
    
    public ASN1Integer version;
    public Certificate certificate;
    public ASN1SetOf   attributes;

    public ExtendedCertificate() {
        version = new ASN1Integer();
        certificate = new Certificate();
        attributes = new ASN1SetOf(Attribute.class);
        addComponent(version);
        addComponent(certificate);
        addComponent(attributes);
    }
}
