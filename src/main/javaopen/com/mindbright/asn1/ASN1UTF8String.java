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

package com.mindbright.asn1;

import java.io.UnsupportedEncodingException;

public class ASN1UTF8String extends ASN1CharString {

    public ASN1UTF8String() {
        super(ASN1.TAG_UTF8STRING);
    }

    public void setValue(String string) {
        try {
            byte[] tmp = string.getBytes("UTF8");
            setRaw(tmp);
        } catch (UnsupportedEncodingException e) {
            throw new Error("Can't handle UTF8String");
        }
    }

    public String getValue() {
        try {
            return new String(value, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("Can't handle UTF8String");
        }
    }

}

