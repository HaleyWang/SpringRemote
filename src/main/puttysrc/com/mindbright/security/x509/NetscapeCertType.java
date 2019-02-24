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

import com.mindbright.asn1.ASN1BitString;

public class NetscapeCertType extends ASN1BitString {


    public NetscapeCertType() {
        super();
    }
    
    public boolean getField(int i) {
        if (i > getBitCount()-1)
            return false;

        boolean[] b = getBooleanArray();
        return b[i];
    }

    public String toString() {
        return "netscape-cert-type: " + getValue();
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;
        String names[] =
            { "ssl-client",
              "ssl-server",
              "smime",
              "object-signing",
              "reserved",
              "ssl-ca",
              "smime-ca",
              "object-signing-ca" };

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
