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

package com.mindbright.security.pkcs7;

import com.mindbright.asn1.ASN1OIDRegistry;

public class RegisteredTypes extends ASN1OIDRegistry {
	private static final long serialVersionUID = 1L;

    public RegisteredTypes() {
        /* !!! TODO Support these too
           signedAndEnvelopedData  OBJECT IDENTIFIER ::= { pkcs-7 4 }
           digestedData            OBJECT IDENTIFIER ::= { pkcs-7 5 }
        */
        // !!! TODO find used PKI algorithms for EnvelopedData if needed

        put("1.2.840.113549.1.7.1", "ASN1OctetString");
        put("1.2.840.113549.1.7.2", 
            "com.mindbright.security.pkcs7.SignedData");
        put("1.2.840.113549.1.7.3",
            "com.mindbright.security.pkcs7.EnvelopedData");
        put("1.2.840.113549.1.7.6",
            "com.mindbright.security.pkcs7.EncryptedData");
        
        // Move to somewhere else?
        // OIW.secsig SHA1
        put("1.3.14.3.2.26", "ASN1Null");
        // RSA MD5
        put("1.2.840.113549.2.5", "ASN1Null");
        // RSA MD2
        put("1.2.840.113549.2.2", "ASN1Null");
        // Teletrust.alg RIPEMD160
        put("1.3.36.3.2.1", "ASN1Null");

        putName("1.3.14.3.2.26", "SHA1");
        putName("1.2.840.113549.2.5", "MD5");
        putName("1.2.840.113549.2.2", "MD2");
        putName("1.3.36.3.2.1", "RIPEMD160");
    }

}
