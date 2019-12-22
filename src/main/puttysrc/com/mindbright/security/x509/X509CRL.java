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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.mindbright.asn1.ASN1DER;
import com.mindbright.asn1.ASN1OIDRegistry;

public class X509CRL
    extends java.security.cert.CRL {
    private CertificateList certList;

    public X509CRL(byte[] encoded) {
        this(new ByteArrayInputStream(encoded));
    }

    public X509CRL(InputStream in) {
        super("X.509");
        init(in);
    }

    private void init(InputStream in) {
        this.certList = new CertificateList();

        ASN1OIDRegistry.addModule("com.mindbright.security.x509");
        ASN1OIDRegistry.addModule("com.mindbright.security.pkcs1");

        try {
            (new ASN1DER()).decode(in, certList);
        } catch (IOException e) {
            throw new Error("Internal error decoding DER encoded X.509 CRL: " +
                            e.getMessage());
        }
    }

    public boolean isRevoked(java.security.cert.Certificate cert) {
        // TODO
        return true;
    }

    public String toString() {
        return "X509CRL: certList=" + certList;
    }
}
