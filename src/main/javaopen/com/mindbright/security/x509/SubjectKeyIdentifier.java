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

import com.mindbright.asn1.ASN1OctetString;


/**
 * Represents a SubjectKeyIdentifier.
 *
 * <pre>
 *  SubjectKeyIdentifier ::= KeyIdentifier
 *  KeyIdentifier ::= OCTET STRING
 * </pre>
 */

public class SubjectKeyIdentifier extends ASN1OctetString {

    public SubjectKeyIdentifier() {
        super();
    }

    public String toString() {
        byte[] b = getRaw();
        return "subjectKeyIdentifier: 0x" + com.mindbright.util.HexDump.toString(b, 0, b.length);
    }
}
