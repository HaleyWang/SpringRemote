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

package com.mindbright.security.pkcs1;

import com.mindbright.asn1.ASN1OIDRegistry;

public class RegisteredTypes extends ASN1OIDRegistry {
	private static final long serialVersionUID = 1L;

    public RegisteredTypes() {
        put("1.2.840.113549.1.1.1", "ASN1Null"); // rsaEncryption
        put("1.2.840.113549.1.1.2", "ASN1Null"); // md2WithRSAEncryption
        put("1.2.840.113549.1.1.4", "ASN1Null"); // md5WithRSAEncryption
        put("1.2.840.113549.1.1.5", "ASN1Null"); // sha1WithRSAEncryption

        // Move these !!!
        //
        put("1.3.14.3.2.3", "ASN1Null"); // OIW.secsig md5WithRSA
        put("1.3.14.3.2.24", "ASN1Null"); // OIW.secsig md2WithRSASignature
        put("1.3.14.3.2.25", "ASN1Null"); // OIW.secsig md5WithRSASignature
        // !!!	put("1.3.14.3.2.27", ???); - OIW.secsig dsa With SHA1
        // !!!	put("1.3.14.3.2.28 - ???); OIW.secsig dsa With SHA1 with Common Parameters
        put("1.3.14.3.2.29",  "ASN1Null"); // OIW.secsig SHA1 with RSA signature
        put("1.3.36.3.3.1.2", "ASN1Null"); // Teletrust.alg rsaSignatureWithripemd160

        putName("1.2.840.113549.1.1.1", "rsaEncryption (PKCS#1)");
        putName("1.2.840.113549.1.1.2", "md2WithRSAEncryption (PKCS#1)");
        putName("1.2.840.113549.1.1.4", "md5WithRSAEncryption (PKCS#1)");
        putName("1.2.840.113549.1.1.5", "sha1WithRSAEncryption (PKCS#1)");
        putName("1.3.14.3.2.3", "md5WithRSA (OIW.secsig)");
        putName("1.3.14.3.2.24", "md2WithRSASignature (OIW.secsig)");
        putName("1.3.14.3.2.25", "md5WithRSASignature (OIW.secsig)");
        putName("1.3.14.3.2.27", "sha1WithDSA (OIW.secsig)");
        putName("1.3.14.3.2.28", "dsaWithSHA1Common (OIW.secsig)");
        putName("1.3.14.3.2.29",  "sha1WithRSASignature (OIW.secsig)");
        putName("1.3.36.3.3.1.2", "rsaSignatureWithripemd160 (Teletrust.alg)");
    }

}
