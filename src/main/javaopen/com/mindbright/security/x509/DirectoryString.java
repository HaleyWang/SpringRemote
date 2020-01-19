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

import com.mindbright.asn1.ASN1Choice;
import com.mindbright.asn1.ASN1CharString;
import com.mindbright.asn1.ASN1TeletexString;
import com.mindbright.asn1.ASN1PrintableString;
import com.mindbright.asn1.ASN1UniversalString;
import com.mindbright.asn1.ASN1UTF8String;
import com.mindbright.asn1.ASN1BMPString;

public class DirectoryString extends ASN1Choice {

    public DirectoryString() {
        setMember(new ASN1TeletexString());
        setMember(new ASN1PrintableString());
        setMember(new ASN1UniversalString());
        setMember(new ASN1UTF8String());
        setMember(new ASN1BMPString());
    }

    public String getString() {
        return ((ASN1CharString)getValue()).getValue();
    }

    public void setString(String s) {
        ((ASN1CharString)getValue()).setValue(s);
    }

}
