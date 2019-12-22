/******************************************************************************
 *
 * Copyright (c) 2010-2011 Cryptzone Group AB. All Rights Reserved.
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

import com.mindbright.asn1.ASN1Choice;
import com.mindbright.asn1.ASN1CharString;
import com.mindbright.asn1.ASN1IA5String;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1OID;

public class GeneralName extends ASN1Choice 
{
    public GeneralName() {
        // setMember(0, ..); INSTANCE OF TYPE-IDENTIFIER
        setMember(1, new ASN1IA5String());
        setMember(2, new ASN1IA5String());
        // setMember(3, new ORAddress());
        setMember(4, new Name());
        // setMember(5, new EDIPartyName());
        setMember(6, new ASN1IA5String());        
        setMember(7, new ASN1OctetString());
        setMember(8, new ASN1OID());
    }

    public String toString() {
        Object o = getValue();
        if (o instanceof ASN1CharString)
            return ((ASN1CharString)o).toString();        
        return super.toString();
    }
}
