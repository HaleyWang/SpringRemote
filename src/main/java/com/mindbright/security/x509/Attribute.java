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

import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OID;
import com.mindbright.asn1.ASN1SetOf;
import com.mindbright.asn1.ASN1Any;

public class Attribute extends ASN1Sequence {

    public  ASN1OID    type;
    public  ASN1SetOf  values;

    public Attribute() {
        type   = new ASN1OID();
        values = new ASN1SetOf() {
                     protected Class<?> ofType() {
                         Class<?> c = lookupAttr();
                         if(c == null) {
                             c = ASN1Any.class;
                         }
                         return c;
                     }
                 };
        addComponent(type);
        addComponent(values);
    }

    protected Class<?> lookupAttr() {
        return ASN1OIDRegistry.lookupType(type);
    }

    public String attributeName() {
        String oid  = type.getString();
        String name = ASN1OIDRegistry.lookupName(oid);
        if(name == null) {
            name = oid;
        }
        return name;
    }

}

