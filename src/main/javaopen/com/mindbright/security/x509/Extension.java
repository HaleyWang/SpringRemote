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

package com.mindbright.security.x509;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1Boolean;
import com.mindbright.asn1.ASN1OID;

public class Extension extends ASN1Sequence {

    public ASN1OID         extnID;
    public ASN1Boolean     critical;
    public ASN1OctetString extnValue;

    public Extension() {
        extnID    = new ASN1OID();
        critical  = new ASN1Boolean();
        extnValue = new ASN1OctetString();
        addComponent(extnID);
        addOptional(critical, false);
        addComponent(extnValue);
    }

}
