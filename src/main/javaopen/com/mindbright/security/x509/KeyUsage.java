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

import com.mindbright.asn1.ASN1BitString;

/**
 * Represents the key usage parameters of a certificate.
 * 
 * <pre>
 * KeyUsage ::= BIT STRING {
	digitalSignature(0),
	nonRepudiation(1),
	keyEncipherment(2),
	dataEncipherment(3),
	keyAgreement(4),
	keyCertSign(5),
	cRLSign(6),
        encipherOnly(7),
        decipherOnly(8)
}
 * </pre>
 */

public class KeyUsage extends ASN1BitString {

    public final static int DigitalSignature = 0;
    public final static int NonRepudiation   = 1;
    public final static int KeyEncipherment  = 2;
    public final static int DataEncipherment = 3;
    public final static int KeyAgreement     = 4;
    public final static int KeyCertSign      = 5;
    public final static int CRLSign          = 6;
    public final static int EncipherOnly     = 7;
    public final static int DecipherOnly     = 8;

    public KeyUsage() {
        super();
    }

    public boolean getField(int i) {
        if (i > getBitCount()-1)
            return false;

        boolean[] b = getBooleanArray();
        return b[i];
    }

    public String toString() {
        return "keyUsage: " + getValue();
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;
        String names[] =
            { "digitalSignature", "nonRepudiation", "keyEncipherment",
              "dataEncipherment", "keyAgreement", "keyCertSign",
              "cRLSign", "encipherOnly", "decipherOnly" };

        for (int i=0; i<names.length; i++) {
            if (getField(i)) {
                if (comma)
                    sb.append(",");
                sb.append(names[i]);
                comma = true;
            }
        }

        return sb.toString();
    }
}
