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

import com.mindbright.asn1.ASN1Object;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OID;
import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.asn1.ASN1AnyDefinedBy;
import com.mindbright.asn1.ASN1BitString;
import com.mindbright.asn1.ASN1CharString;
import com.mindbright.asn1.ASN1PrintableString;

/**
 * An attribute OID together with a value whose format is defined by
 * the IOD.
 *
 * <pre>
 *   AttributeTypeAndValue ::= SEQUENCE {
 *     type     AttributeType,
 *     value    AttributeValue }
 *
 *   AttributeType ::= OBJECT IDENTIFIER
 *
 *   AttributeValue ::= ANY DEFINED BY AttributeType
 * </pre>
 */
public final class AttributeTypeAndValue extends ASN1Sequence {

    public ASN1OID          type;
    public ASN1AnyDefinedBy value;

    public AttributeTypeAndValue() {
        type  = new ASN1OID();
        value = new ASN1AnyDefinedBy(type, true);
        addComponent(type);
        addComponent(value);
    }

    public String getRFC2253Value() {
        String typeOID  = type.getString();
        String typeName = ASN1OIDRegistry.lookupShortName(typeOID);
        if(typeName == null) {
            typeName = ASN1OIDRegistry.lookupName(typeOID);
        }
        if(typeName == null) {
            typeName = typeOID;
        }
        // TODO should encode as octetstring if type name not known
        return typeName + "=" + valueAsString();
    }

    private String valueAsString() {
        ASN1Object vo = value.getValue();
        String     vs = "<unknown>";
        if(vo instanceof DirectoryString) {
            vs = ((DirectoryString)vo).getString();
        } else if(vo instanceof ASN1CharString) {
            vs = ((ASN1CharString)vo).getValue();
        } else if(vo instanceof ASN1PrintableString) {
            vs = ((ASN1PrintableString)vo).getValue();
        } else if(vo instanceof ASN1BitString) {
            vs = ((ASN1BitString)vo).toPrintableString();
        } // else { TODO, encode as octetstring
        // TODO, escape characters according to rfc2253

        return vs;
    }

}

