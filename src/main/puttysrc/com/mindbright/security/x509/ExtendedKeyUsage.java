/******************************************************************************
 *
 * Copyright (c) 2010-2011 Cryptzone Group AB. All Rights Reserved.
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

import com.mindbright.asn1.ASN1OID;
import com.mindbright.asn1.ASN1SequenceOf;

public class ExtendedKeyUsage extends ASN1SequenceOf {

    public ExtendedKeyUsage() {
        super(ASN1OID.class);
    }

    public String toString() {
        return "extKeyUsage: " + getValue();
    }

    public String getValue() {
        String s = "";
        for (int i=0; i<getCount(); i++) {
            if (i>0) s+=",";
            String oid = ((ASN1OID)getComponent(i)).getString();
            if (oid.equals("1.3.6.1.5.5.7.3.1")) {
                s+="serverAuth";
            } else if (oid.equals("1.3.6.1.5.5.7.3.2")) {
                s+="clientAuth";
            } else if (oid.equals("1.3.6.1.5.5.7.3.3")) {
                s+="codeSigning";
            } else if (oid.equals("1.3.6.1.5.5.7.3.4")) {
                s+="emailProtection";
            } else if (oid.equals("1.3.6.1.5.5.7.3.5")) {
                s+="ipsecEndSystem";
            } else if (oid.equals("1.3.6.1.5.5.7.3.6")) {
                s+="ipsecTunnel";
            } else if (oid.equals("1.3.6.1.5.5.7.3.7")) {
                s+="ipsecUser";
            } else if (oid.equals("1.3.6.1.5.5.7.3.8")) {
                s+="timestamp";
            } else if (oid.equals("1.3.6.1.5.5.7.3.9")) {
                s+="OCSPSigning";
            } else {
                s+=oid;
            }
            
        }
        return s;
    }
}
