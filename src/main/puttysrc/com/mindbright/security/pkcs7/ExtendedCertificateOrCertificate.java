
/******************************************************************************
 *
 * Copyright (c) 2008-2011 Cryptzone Group AB. All Rights Reserved.
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

import com.mindbright.asn1.ASN1Choice;
import com.mindbright.security.pkcs6.ExtendedCertificate;
import com.mindbright.security.x509.Certificate;

public final class ExtendedCertificateOrCertificate extends ASN1Choice {
    
    public Certificate certificate;
    public ExtendedCertificate extendedCertificate;    

    public ExtendedCertificateOrCertificate() {
        certificate         = new Certificate();
        extendedCertificate = new ExtendedCertificate();

        setMember(certificate);
        setMember(0, extendedCertificate);
    }
}
