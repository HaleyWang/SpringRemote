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
import com.mindbright.asn1.ASN1AnyDefinedBy;
import com.mindbright.asn1.ASN1Explicit;
import com.mindbright.asn1.ASN1SetOf;
import com.mindbright.asn1.ASN1OID;

import com.mindbright.security.x509.Attribute;


public final class SafeBag extends ASN1Sequence {

    public final static int TYPE_UNKNOWN               = 0;
    public final static int TYPE_KEYBAG                = 1;
    public final static int TYPE_PKCS8_SHROUDED_KEYBAG = 2;
    public final static int TYPE_CERTBAG               = 3;
    public final static int TYPE_CRLBAG                = 4;
    public final static int TYPE_SECRETBAG             = 5;
    public final static int TYPE_SAFECONTENTSBAG       = 6;

    public ASN1OID          bagId;
    public ASN1AnyDefinedBy bagValue;
    public ASN1SetOf        bagAttributes;

    public SafeBag() {
        bagId         = new ASN1OID();
        bagValue      = new ASN1AnyDefinedBy(bagId);
        bagAttributes = new ASN1SetOf(Attribute.class);

        addComponent(bagId);
        addComponent(new ASN1Explicit(0, bagValue));
        addOptional(bagAttributes);
    }

    public int getBagType() {
        int type = TYPE_UNKNOWN;
        if(bagId != null) {
            String id = bagId.getString();
            if("1.2.840.113549.1.12.10.1.1".equals(id)) {
                type = TYPE_KEYBAG;
            } else if("1.2.840.113549.1.12.10.1.2".equals(id)) {
                type = TYPE_PKCS8_SHROUDED_KEYBAG;
            } else if("1.2.840.113549.1.12.10.1.3".equals(id)) {
                type = TYPE_CERTBAG;
            } else if("1.2.840.113549.1.12.10.1.4".equals(id)) {
                type = TYPE_CRLBAG;
            } else if("1.2.840.113549.1.12.10.1.5".equals(id)) {
                type = TYPE_SECRETBAG;
            } else if("1.2.840.113549.1.12.10.1.6".equals(id)) {
                type = TYPE_SAFECONTENTSBAG;
            }
        }
        return type;
    }

}

