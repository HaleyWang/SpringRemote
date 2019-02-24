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

package com.mindbright.security.pkcs12;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1Integer;

import com.mindbright.security.pkcs7.DigestInfo;

public final class MacData extends ASN1Sequence {

    public DigestInfo      mac;
    public ASN1OctetString macSalt;
    public ASN1Integer     iterations;

    public MacData() {
        mac        = new DigestInfo();
        macSalt    = new ASN1OctetString();
        iterations = new ASN1Integer();
        addComponent(mac);
        addComponent(macSalt);
        addOptional(iterations, 1);
    }

    public int getIterations() {
        ASN1Integer iter = (iterations.isSet() ?
                            iterations : (ASN1Integer)getDefault(2));
        return iter.getValue().intValue();
    }

}

