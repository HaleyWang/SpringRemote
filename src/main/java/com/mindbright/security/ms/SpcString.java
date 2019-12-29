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

import com.mindbright.asn1.ASN1Choice;
import com.mindbright.asn1.ASN1BMPString;
import com.mindbright.asn1.ASN1IA5String;
import com.mindbright.asn1.ASN1OctetString;

public final class SpcString extends ASN1Choice
{
    public SpcString() {
        super();
        setMember(0, new ASN1BMPString());
        setMember(1, new ASN1IA5String());
        setMember(2, new ASN1OctetString());
    }
}
