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

public abstract class ASN1CharString extends ASN1String {

    protected ASN1CharString(int tag) {
        super(tag);
    }

    public abstract void setValue(String string);
    public abstract String getValue();

    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof ASN1CharString)) {
            return false;
        }
        ASN1CharString other = (ASN1CharString)obj;
        return getValue().equals(other.getValue());
    }

    public int hashCode() {
        int hc = super.hashCode();
        if(value != null) {
            hc = getValue().hashCode();
        }
        return hc;
    }

}
