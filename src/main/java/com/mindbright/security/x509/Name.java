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

import com.mindbright.asn1.ASN1SequenceOf;

/**
 * <pre>
 * NOTE: Shortened here, but RDNSequence feels superfluos until more choices
 * are available... 
 * should be: Name ::= CHOICE { -- only one possibility for
 *                              now -- rdnSequence RDNSequence }
 *
 * RDNSequence       ::= SEQUENCE OF RelativeDistinguishedName
 *
 * DistinguishedName ::=   RDNSequence
 * </pre>
 */
public final class Name extends ASN1SequenceOf {

    public Name() {
        super(RelativeDistinguishedName.class);
    }

    public RelativeDistinguishedName getRDN(int i) {
        return (RelativeDistinguishedName)getComponent(i);
    }

    public void setRFC2253Value(String rfc2253DN) {
        // TODO
    }

    public String getRFC2253Value() {
        StringBuilder sb = new StringBuilder();
        int last = getCount() - 1;
        for(int i = last; i >= 0; i--) {
            if(i < last) {
                sb.append(',');
            }
            RelativeDistinguishedName rdn = getRDN(i);
            sb.append(rdn.getRFC2253Value());
        }
        return sb.toString();
    }

    public String toString() {
        return getRFC2253Value();
    }
    
}
